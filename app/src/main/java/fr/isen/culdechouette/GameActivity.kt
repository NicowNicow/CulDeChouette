package fr.isen.culdechouette

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.SensorManager
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_game.*
import java.lang.NumberFormatException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


class GameActivity : AppCompatActivity(), ShakeDetector.Listener   {

    private var imageList = ArrayList<Int>()
    private var culDeChouetteList = ArrayList<Int>()
    private var valuedice1 = 0
    private var valuedice2 = 0
    private var valueCDC = 0
    private var playerCount = 0
    private var sirop = 0
    private var animationDone = false
    private var toastPrinted = false
    private var nextTurn = false
    private lateinit var usersList: MutableList<User>
    private lateinit var gameParameters: GameParameters
    private lateinit var matchmakingAnnulation: MatchmakingAnnulation
    private lateinit var  firebaseRef: DatabaseReference
    private var matchmakingSettings : SharedPreferences? = null
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var timer: CountDownTimer
    private lateinit var mainHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fillDiceLists()
        setUpShakeDetector()
        setContentView(R.layout.activity_game)
        setUpFirebaseSharedPref()
        initialFetchFirebase()
        setUpMatchmakingCancellation()
        setUpListeners()
        startService(Intent(this@GameActivity, MatchmakingDisconnectedService::class.java))
        fetchFirebaseForGame()
        timer = object: CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                firebaseRef.child("game_parameters").child("timer_launched").setValue(2)
            }
        }
        mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                mainGameAlgorithm()
                checkVictory(this)
                mainHandler.postDelayed(this, 3000)
            }
        })
    }

    override fun onResume () {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or SYSTEM_UI_FLAG_IMMERSIVE
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        actionBar?.hide()
    }

    override fun onStop() {
        shakeDetector.stop()
        super.onStop()
    }

    override fun onBackPressed() {
        matchmakingAnnulation.windowStopMatchmakingInGame()
    }

    private fun fillDiceLists() {
        imageList.add(R.drawable.chouette1)
        imageList.add(R.drawable.chouette2)
        imageList.add(R.drawable.chouette3)
        imageList.add(R.drawable.chouette4)
        imageList.add(R.drawable.chouette5)
        imageList.add(R.drawable.chouette6)
        culDeChouetteList.add(R.drawable.culdechouette1)
        culDeChouetteList.add(R.drawable.culdechouette2)
        culDeChouetteList.add(R.drawable.culdechouette3)
        culDeChouetteList.add(R.drawable.culdechouette4)
        culDeChouetteList.add(R.drawable.culdechouette5)
        culDeChouetteList.add(R.drawable.culdechouette6)
    }

    private fun setUpShakeDetector() {
        shakeDetector = ShakeDetector(this)
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector.start(sensorManager)
    }

    private fun setUpFirebaseSharedPref() {
        matchmakingSettings = getSharedPreferences("currentRoom", Context.MODE_PRIVATE)
        firebaseRef= FirebaseDatabase.getInstance().getReference("waiting_rooms").child(matchmakingSettings?.getString("roomKey", null)?:"")
    }

    private fun setUpMatchmakingCancellation() {
        val displayParam: DisplayMetrics? = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayParam)
        matchmakingAnnulation = MatchmakingAnnulation(this@GameActivity, displayParam!!,
            matchmakingSettings?.getString("userKey", null)?:"",
            matchmakingSettings?.getString("roomKey", null)?:"", firebaseRef)
    }

    private fun setUpListeners() {
        tabletop.setOnClickListener {
            animation()
            tabletop.isClickable = false
            shakeDetector.stop()
            val diceValuesTempo = DiceValues(valuedice1, valuedice2, valueCDC)
            val gameParametersTempo = GameParameters(gameParameters.user_turn, 0, diceValuesTempo, true, DiceFigure())
            firebaseRef.child("game_parameters").setValue(gameParametersTempo)
        }
        quitButton.setOnClickListener { matchmakingAnnulation.windowStopMatchmakingInGame() }
        rulesButton.setOnClickListener { doRules() }
        leaderboardsButton.setOnClickListener { doLeaderboardPopup() }
        setActionBarButtonListerners()
    }

    private fun setActionBarButtonListerners() {
        grelotteButton.setOnClickListener { doGrelotte() }
        mouCaillouButton.setOnClickListener { doPasMou() }
        siropButton.setOnClickListener { doSirop() }
        nextButton.setOnClickListener { nextTurn = true }
        lockActionBarButtons()
    }

    private fun lockActionBarButtons() {
        grelotteButton.isClickable = false
        grelotteButton.setColorFilter(Color.GRAY)
        mouCaillouButton.isClickable = false
        mouCaillouButton.setColorFilter(Color.GRAY)
        siropButton.isClickable = false
        siropButton.setColorFilter(Color.GRAY)
        nextButton.isClickable = false
        nextButton.setColorFilter(Color.GRAY)
    }

    private fun unlockActionBarButtons() {
        grelotteButton.isClickable = true
        grelotteButton.setColorFilter(Color.TRANSPARENT)
        mouCaillouButton.isClickable = true
        mouCaillouButton.setColorFilter(Color.TRANSPARENT)
        if (matchmakingSettings?.getString("userKey", null)?:"" == gameParameters.user_turn) {
            nextButton.isClickable = true
            nextButton.setColorFilter(Color.TRANSPARENT)
        }
        if (gameParameters.dice_figure.figure == "chouette") {
            siropButton.isClickable = true
            siropButton.setColorFilter(Color.TRANSPARENT)
        }
    }

    private fun doRules() {
        val intentRules = Intent(this@GameActivity, TutorialActivity::class.java)
        intentRules.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        this@GameActivity.startActivity(intentRules)
    }

    private fun initialFetchFirebase() {
        usersList = mutableListOf()
        gameParameters = GameParameters()
        firebaseRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.i("DatabaseError", error.toString())
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    gameParameters = snapshot.child("game_parameters").getValue(GameParameters::class.java)!!
                    usersList.clear()
                    fetchUserValues(snapshot.child("users"))
                }
            }
        })
    }

    private fun fetchUserValues(snapshot: DataSnapshot) {
        playerCount = 0
        for (index in snapshot.children){
            try {
                val user = index.getValue(User::class.java)
                usersList.add(user!!)
                playerCount++
            }
            catch(error: NumberFormatException) {
                Log.i("NumberFormatException", error.toString())
                continue
            }
        }
        if (playerCount == 1) {
            doGameCancelled()
        }
    }

    override fun hearShake() {
        animation()
        tabletop.isClickable = false
        shakeDetector.stop()
        val diceValuesTempo = DiceValues(valuedice1, valuedice2, valueCDC)
        val gameParametersTempo = GameParameters(gameParameters.user_turn, 0, diceValuesTempo, true, DiceFigure())
        firebaseRef.child("game_parameters").setValue(gameParametersTempo)
    }

    private fun animation() {
        valuedice1 = (0..5).random()
        valuedice2 = (0..5).random()
        valueCDC = (0..5).random()
        val hyperspaceJump: Animation = AnimationUtils.loadAnimation(this, R.anim.hyperspace_jump)
        hyperspaceJump.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                sirop = 0
            }

            override fun onAnimationStart(animation: Animation?) {
                when (sirop){
                    0 ->{
                        countDownDices.start()
                        shakeDetector.stop()
                    }
                    1->countDownDices.start()
                    2->countDownDices.start()
                    3->countDownDices.start()
                }
            }
        })
        when (sirop){
            0->{
                dice1.startAnimation(hyperspaceJump)
                dice2.startAnimation(hyperspaceJump)
                culDeChouette.startAnimation(hyperspaceJump)
            }
            1->dice1.startAnimation(hyperspaceJump)
            2->dice2.startAnimation(hyperspaceJump)
            3->culDeChouette.startAnimation(hyperspaceJump)
        }
    }

    var countDownDices = object: CountDownTimer(2600,100){
        private var index = 0
        override fun onTick(millisUntilFinished: Long) {
            val random1 = (0..5).random()
            val random2 = (0..5).random()
            val randomCDC = (0..5).random()
            index = applyAnimation(index, random1, random2, randomCDC)
        }

        override fun onFinish() {
            dice1.setImageResource(imageList[valuedice1])
            dice2.setImageResource(imageList[valuedice2])
            culDeChouette.setImageResource(culDeChouetteList[valueCDC])
            index=0
        }

    }

    private fun animationFromDatabase() {
        val hyperspaceJump: Animation = AnimationUtils.loadAnimation(this, R.anim.hyperspace_jump)
        hyperspaceJump.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                sirop = 0
            }

            override fun onAnimationStart(animation: Animation?) {
                    countDownDicesFromDatabase.start()
            }
        })
        when (sirop){
            0->{
                dice1.startAnimation(hyperspaceJump)
                dice2.startAnimation(hyperspaceJump)
                culDeChouette.startAnimation(hyperspaceJump)
            }
            1->dice1.startAnimation(hyperspaceJump)
            2->dice2.startAnimation(hyperspaceJump)
            3->culDeChouette.startAnimation(hyperspaceJump)
        }
        animationDone = true
    }


    var countDownDicesFromDatabase = object: CountDownTimer(2600,100){
        private var index = 0
        override fun onTick(millisUntilFinished: Long) {
            val random1 = (0..5).random()
            val random2 = (0..5).random()
            val randomCDC = (0..5).random()
            index = applyAnimation(index, random1, random2, randomCDC)
        }

        override fun onFinish() {
            dice1.setImageResource(imageList[gameParameters.dice_values.dice_1])
            dice2.setImageResource(imageList[gameParameters.dice_values.dice_2])
            culDeChouette.setImageResource(culDeChouetteList[gameParameters.dice_values.dice_3])
            index=0
        }

    }

    private fun applyAnimation(index: Int, valueDice1: Int, valueDice2: Int, valueDice3: Int): Int {
        var indexTempo = index
        when(sirop) {
            0-> {
                when(indexTempo){
                    in 0..14 -> {
                        dice1.setImageResource(imageList[valueDice1])
                        dice2.setImageResource(imageList[valueDice2])
                        culDeChouette.setImageResource(culDeChouetteList[valueDice3])
                    }
                    in 15..21-> if(indexTempo%2 == 0) {
                        dice1.setImageResource(imageList[valueDice1])
                        dice2.setImageResource(imageList[valueDice2])
                        culDeChouette.setImageResource(culDeChouetteList[valueDice3])
                    }
                    in 22..24 -> if(indexTempo%3 == 0) {
                        dice1.setImageResource(imageList[valueDice1])
                        dice2.setImageResource(imageList[valueDice2])
                        culDeChouette.setImageResource(culDeChouetteList[valueDice3])
                    }
                }
                indexTempo++
            }
            1-> {
                when(indexTempo){
                    in 0..14 -> { dice1.setImageResource(imageList[valueDice1]) }
                    in 15..21-> if(indexTempo%2 == 0) { dice1.setImageResource(imageList[valueDice1]) }
                    in 22..24 -> if(indexTempo%3 == 0) { dice1.setImageResource(imageList[valueDice1]) }
                }
                indexTempo++
            }
            2-> {
                when(indexTempo){
                    in 0..14 -> { dice2.setImageResource(imageList[valueDice2]) }
                    in 15..21-> if(indexTempo%2 == 0) { dice2.setImageResource(imageList[valueDice2]) }
                    in 22..24 -> if(indexTempo%3 == 0) { dice2.setImageResource(imageList[valueDice2]) }
                }
                indexTempo++
            }
            3-> {
                when(indexTempo){
                    in 0..14 -> { culDeChouette.setImageResource(culDeChouetteList[valueDice3]) }
                    in 15..21-> if(indexTempo%2 == 0) { culDeChouette.setImageResource(culDeChouetteList[valueDice3]) }
                    in 22..24 -> if(indexTempo%3 == 0) { culDeChouette.setImageResource(culDeChouetteList[valueDice3]) }
                }
                indexTempo++
            }
        }
        return(indexTempo)
    }

    private fun fetchFirebaseForGame() {
        firebaseRef.addValueEventListener(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.i("DatabaseError", error.toString())
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    usersList.clear()
                    fetchUserValues(snapshot.child("users"))
                    gameParameters = snapshot.child("game_parameters").getValue(GameParameters::class.java)!!
                }
            }
        })
    }

    private fun doGameCancelled() {
        Toast.makeText(this@GameActivity, getString(R.string.ragequit), Toast.LENGTH_LONG).show()
        firebaseRef.removeValue()
        val doorSound = MediaPlayer.create(this, R.raw.door)
        doorSound.start()
        val intentMain = Intent(this@GameActivity, MainActivity::class.java)
        intentMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        this@GameActivity.startActivity(intentMain)
    }

    private fun mainGameAlgorithm() {
        tabletop.isClickable = false
        shakeDetector.stop()
        if (matchmakingSettings?.getString("userKey", null)?:"" == gameParameters.user_turn) {
            if (!gameParameters.dice_value_changed) {
                if (!toastPrinted) {
                    Toast.makeText(this@GameActivity, getString(R.string.yourTurn), Toast.LENGTH_LONG).show()
                    toastPrinted = true
                }
                tabletop.isClickable = true
                setUpShakeDetector()
            }
            else if ((gameParameters.timer_launched == 2) || (nextTurn)) {
                lockActionBarButtons()
                doCalculatePoints()
                sirop = 0
                animationDone = false
                toastPrinted = false
                nextTurn = false
                doChangeTurn()
                doCleanFirebase()
            }
            else {
                checkFigure()
                firebaseRef.child("game_parameters").child("timer_launched").setValue(1)
                if (gameParameters.timer_launched == 0) {
                    timer.start()
                }
                unlockActionBarButtons()
            }
        }
        else if (matchmakingSettings?.getString("userKey", null)?:"" != gameParameters.user_turn) {
            if ((!animationDone) && (gameParameters.dice_value_changed)) {
                if (!toastPrinted) {
                    val indexPlayer = usersList.binarySearchBy(gameParameters.user_turn) { it.user_key }
                    Toast.makeText(this@GameActivity, getString(R.string.turn, usersList[indexPlayer].username), Toast.LENGTH_LONG).show()
                }
                animationFromDatabase()
            }
            if (gameParameters.timer_launched == 1) {
                unlockActionBarButtons()
            }
            else {
                lockActionBarButtons()
                sirop = 0
                animationDone = false
                toastPrinted = false
            }
        }
    }

    private fun doLeaderboardPopup() {
        val leaderboardDialogBuilder: AlertDialog.Builder? = this@GameActivity.let { AlertDialog.Builder(it) }
        val displayParam: DisplayMetrics? = DisplayMetrics()
        val layoutParams: WindowManager.LayoutParams? = WindowManager.LayoutParams()
        windowManager.defaultDisplay.getMetrics(displayParam)
        layoutParams?.width = (displayParam?.widthPixels?.times(0.9f))?.toInt()!!
        layoutParams?.height = (displayParam.heightPixels.times(0.9f)).toInt()
        leaderboardDialogBuilder?.setView(layoutInflater.inflate(R.layout.leaderboard_popup, null))?.create()
        val leaderboardDialog = leaderboardDialogBuilder?.show()
        leaderboardDialog?.window?.attributes = layoutParams
        leaderboardDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        leaderboardDialog?.window?.setGravity(Gravity.CENTER)
        leaderboardDialog?.findViewById<ImageButton>(R.id.leaderboardBackButton)?.setOnClickListener{
            val scrollSound = MediaPlayer.create(this, R.raw.elder_scroll)
            scrollSound.start()
            leaderboardDialog.dismiss()
        }
        val listview = leaderboardDialog?.findViewById<ListView>(R.id.leaderboardViewPopup)
        val adapter = ResultsAdapter(applicationContext, R.layout.scoreboard_element, usersList )
        listview?.adapter = adapter
    }

    private fun checkVictory(runnable: Runnable) {
        val listVictoryIndex: MutableList<Int> = mutableListOf()
        for (index in (0 until usersList.size)) {
            if (usersList[index].score >= 342) {
                listVictoryIndex.add(index)
            }
        }
        if (listVictoryIndex.size == 0) {
            return
        }
        if (listVictoryIndex.size ==1) {
            Toast.makeText(this@GameActivity, getString(R.string.victory, usersList[listVictoryIndex[0]].username), Toast.LENGTH_LONG).show()
            goToLeaderboard()
        }
        else {
            var maxValue = 0
            var indexTemp = -1
            for (index in (0 until listVictoryIndex.size)) {
                if (usersList[listVictoryIndex[index]].score >= maxValue) {
                    maxValue = usersList[listVictoryIndex[index]].score
                    indexTemp = listVictoryIndex[index]
                }
            }
            Toast.makeText(this@GameActivity, getString(R.string.victory, usersList[indexTemp].username), Toast.LENGTH_LONG).show()
            mainHandler.removeCallbacks(runnable)
            goToLeaderboard()
        }
    }


    private fun goToLeaderboard() {
        val doorSound = MediaPlayer.create(this, R.raw.door)
        doorSound.start()
        Timer("SoundTemporisation", false).schedule(500) {
            val intentLeaderboard = Intent(this@GameActivity, ScoreboardActivity::class.java)
            intentLeaderboard.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            this@GameActivity.startActivity(intentLeaderboard)
        }
    }

    private fun doGrelotte() {
        if (gameParameters.dice_figure.figure == "suite") {
            firebaseRef.child("users").child(matchmakingSettings?.getString("userKey", null)?:"").child("sum_grelotte").setValue(usersList.find { it.user_key == matchmakingSettings?.getString("userKey", null)?:"" }?.sum_grelotte?.plus(1))
        }
        else {
            Toast.makeText(this@GameActivity, R.string.fail, Toast.LENGTH_LONG).show()
            firebaseRef.child("users").child(matchmakingSettings?.getString("userKey", null)?:"").child("score").setValue(usersList.find { it.user_key == matchmakingSettings?.getString("userKey", null)?:"" }?.score?.minus(5))
        }
    }

    private fun doPasMou() {
        if (gameParameters.dice_figure.figure == "chouettevelute") {
            firebaseRef.child("users").child(matchmakingSettings?.getString("userKey", null)?:"").child("sum_pasmou").setValue(usersList.find { it.user_key == matchmakingSettings?.getString("userKey", null)?:"" }?.sum_pasmou?.plus(1))
        }
        else {
            Toast.makeText(this@GameActivity, R.string.fail, Toast.LENGTH_LONG).show()
            firebaseRef.child("users").child(matchmakingSettings?.getString("userKey", null)?:"").child("score").setValue(usersList.find { it.user_key == matchmakingSettings?.getString("userKey", null)?:"" }?.score?.minus(5))
        }
    }

    private fun doSirop() {
        /* Sirop:
        Lance une animation pour le dé à siroter chez tout le monde
        */
    }

    private fun doCalculatePoints() {
        if (gameParameters.dice_figure.figure == "chouette") {
            firebaseRef.child("users").child(matchmakingSettings?.getString("userKey", null)?:"").child("score").setValue(usersList.find { it.user_key == matchmakingSettings?.getString("userKey", null)?:"" }?.score?.plus(gameParameters.dice_figure.valueFigure*gameParameters.dice_figure.valueFigure))
        }
        else if (gameParameters.dice_figure.figure == "culdechouette") {
            firebaseRef.child("users").child(matchmakingSettings?.getString("userKey", null)?:"").child("score").setValue(usersList.find { it.user_key == matchmakingSettings?.getString("userKey", null)?:"" }?.score?.plus((gameParameters.dice_figure.valueFigure + 4)*10))
        }
        /*Implementer le Sirop*/
        else if (gameParameters.dice_figure.figure == "velute") {
            var scoreTemp = 0
            when (gameParameters.dice_figure.valueFigure) {
                2-> {scoreTemp = 8}
                3-> {scoreTemp = 18}
                4-> {scoreTemp = 32}
                5-> {scoreTemp = 50}
                6-> {scoreTemp = 72}
            }
            firebaseRef.child("users").child(matchmakingSettings?.getString("userKey", null)?:"").child("score").setValue(usersList.find { it.user_key == matchmakingSettings?.getString("userKey", null)?:"" }?.score?.plus(scoreTemp))
        }
        else if (gameParameters.dice_figure.figure == "suite") {
            var min = 800
            var key: String = ""
            for (index in 0 until usersList.size) {
                if (usersList[index].sum_grelotte <= min) {
                    min = usersList[index].sum_grelotte
                    key = usersList[index].user_key
                }
            }
            firebaseRef.child("users").child(key).child("score").setValue(usersList.find { it.user_key == key }?.score?.minus(10))
        }
    }

    private fun doChangeTurn() {
        var currentUser = usersList.find { it.user_key == matchmakingSettings?.getString("userKey", null)?:""}
        var listIndex = -1
        for (index in (0 until usersList.size)) {
            if (usersList[index] == currentUser) {
                listIndex = index
                break
            }
        }
        if (listIndex == usersList.size - 1) {
              listIndex = 0
        }
        else {
            listIndex++
        }
        Log.i("testDice", usersList[listIndex].user_key)
        firebaseRef.child("game_parameters").child("user_turn").setValue(usersList[listIndex].user_key)
    }

    private fun doCleanFirebase() {
        for (index in (0 until usersList.size)) {
            firebaseRef.child("users").child(usersList[index].user_key).child("sum_pasmou").setValue(0)
            firebaseRef.child("users").child(usersList[index].user_key).child("sum_grelotte").setValue(0)
        }
        firebaseRef.child("game_parameters").child("dice_figure").setValue(DiceFigure())
        firebaseRef.child("game_parameters").child("timer_launched").setValue(0)
        firebaseRef.child("game_parameters").child("dice_value_changed").setValue(false)
    }

    private fun checkFigure() {
        val figure = DiceFigure()
        val minValue: Int = mutableListOf(valuedice1, valuedice2, valueCDC).min()?.plus(1)!!
        val maxValue: Int = mutableListOf(valuedice1, valuedice2, valueCDC).max()?.plus(1)!!
        val mediumValueList: MutableList<Int> = mutableListOf(valuedice1, valuedice2, valueCDC)
        mediumValueList.remove(minValue)
        mediumValueList.remove(maxValue)
        val mediumValue = mediumValueList[0].plus(1)
        if(( valuedice1 == valuedice2)&&(valuedice2 == valueCDC)) {
            figure.figure = "culdechouette"
            figure.valueFigure = valueCDC + 1
        }
        else if ((valuedice1 == valuedice2)||(valuedice1 == valueCDC)||(valuedice2 == valueCDC)) {
            val lastIndexD1: Int = listOf(valuedice1, valuedice2, valueCDC).lastIndexOf(valuedice1)
            figure.figure = "chouette"
            when (lastIndexD1) {
                0-> {
                    figure.valueFigure = valuedice2 + 1
                    figure.sirop_dice = valuedice1
                }
                1-> {
                    figure.valueFigure = valuedice2 + 1
                    figure.sirop_dice = valueCDC
                }
                2-> {
                    figure.valueFigure = valueCDC + 1
                    figure.sirop_dice = valuedice2
                }
            }
        }
        else if ((mediumValue == minValue + 1)&&(maxValue == mediumValue + 1)) {
            figure.figure = "suite"
        }
        else if (minValue + mediumValue == maxValue) {
            if (minValue == mediumValue) {
                figure.figure = "chouettevelute"
            }
            else {
                figure.figure = "velute"
            }
            figure.valueFigure = maxValue
        }
        firebaseRef.child("game_parameters").child("dice_figure").setValue(figure)
    }

}
