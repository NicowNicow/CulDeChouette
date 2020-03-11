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
import kotlinx.android.synthetic.main.activity_scoreboard.*
import java.lang.NumberFormatException


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
    private lateinit var usersList: MutableList<User>
    private lateinit var previousUsersList: MutableList<User>
    private lateinit var gameParameters: GameParameters
    private lateinit var previousGameParameters: GameParameters
    private lateinit var matchmakingAnnulation: MatchmakingAnnulation
    private lateinit var  firebaseRef: DatabaseReference
    private var matchmakingSettings : SharedPreferences? = null
    private lateinit var shakeDetector: ShakeDetector

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
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                mainGameAlgorithm()
                mainHandler.postDelayed(this, 2000)
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
        dice1.setOnClickListener{
            diceNotClickables()
            sirop = 1
            animation()
        }
        dice2.setOnClickListener{
            diceNotClickables()
            sirop = 2
            animation()
        }
        culDeChouette.setOnClickListener {
            diceNotClickables()
            sirop = 3
            animation()
        }
        diceNotClickables()
        tabletop.setOnClickListener {
            animation()
            tabletop.isClickable = false
            shakeDetector.stop()
            val diceValuesTempo = DiceValues(valuedice1, valuedice2, valueCDC)
            val gameParametersTempo = GameParameters(previousGameParameters.user_turn, false, diceValuesTempo, true)
            firebaseRef.child("game_parameters").setValue(gameParametersTempo)
        }
        quitButton.setOnClickListener { matchmakingAnnulation.windowStopMatchmakingInGame() }
        rulesButton.setOnClickListener { doRules() }
        leaderboardsButton.setOnClickListener { doLeaderboardPopup() }
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
                    previousGameParameters = gameParameters
                    previousUsersList = usersList
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

    private fun diceNotClickables() {
        dice1.isClickable = false
        dice2.isClickable = false
        culDeChouette.isClickable = false
    }

    override fun hearShake() {
        animation()
        tabletop.isClickable = false
        shakeDetector.stop()
        val diceValuesTempo = DiceValues(valuedice1, valuedice2, valueCDC)
        val gameParametersTempo = GameParameters(previousGameParameters.user_turn, false, diceValuesTempo, true)
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
        if (matchmakingSettings?.getString("userKey", null)?:"" == previousGameParameters.user_turn) {
            if (!gameParameters.dice_value_changed) {
                if (!toastPrinted) {
                    Toast.makeText(this@GameActivity, getString(R.string.yourTurn), Toast.LENGTH_LONG).show()
                    toastPrinted = true
                }
                tabletop.isClickable = true
                setUpShakeDetector()
            }
            if (gameParameters.timer_launched) { //&& timer = 10 secondes {
                //StopTimer
                //Calcul score des joueurs
                //Changement de tour [Reinitialisation des parametres, locaux et Firebase
            }
            else {
                //CheckFigures
                //Lance Timer
                //Débloque les listeners de passage de tour, pas mou le caillou et grelotte ca picotte
                //Débloque sous condition le sirotage
            }
        }
        else if (matchmakingSettings?.getString("userKey", null)?:"" != gameParameters.user_turn) {
            if ((!animationDone) && (gameParameters.dice_value_changed)) {
                if (!toastPrinted) {
                    val indexPlayer = usersList.binarySearchBy(previousGameParameters.user_turn) { it.user_key }
                    Toast.makeText(this@GameActivity, getString(R.string.turn, usersList[indexPlayer].username), Toast.LENGTH_LONG).show()
                }
                animationFromDatabase()
            }
            if (gameParameters.timer_launched) {
                //Débloque les listeners de pas mou le caillou et grelotte ca picotte
            }
            //else: reinitialisation des parametres locaux
        }
        //Condition de victoire à vérifier
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
        leaderboardDialog?.findViewById<ImageButton>(R.id.leaderboardBackButton)?.setOnClickListener{ leaderboardDialog.dismiss()}
        var listview = leaderboardDialog?.findViewById<ListView>(R.id.leaderboardViewPopup)
        val adapter = ResultsAdapter(applicationContext, R.layout.scoreboard_element, usersList )
        listview?.adapter = adapter
    }

}

//Ajouter dans la firebase:

