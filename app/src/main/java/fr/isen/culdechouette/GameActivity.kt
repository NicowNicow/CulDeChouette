package fr.isen.culdechouette

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.SensorManager
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_game.*
import java.lang.NumberFormatException
import kotlinx.android.synthetic.main.activity_scoreboard.*


class GameActivity : AppCompatActivity(), ShakeDetector.Listener   {

    private var imageList = ArrayList<Int>()
    private var culDeChouetteList = ArrayList<Int>()
    private var valuedice1 = 0
    private var valuedice2 = 0
    private var valueCDC = 0
    private var playerCount = 0
    private var sirop = 0
    private var animationDone = false
    private lateinit var usersList: MutableList<User>
    private lateinit var usersListCopy: MutableList<User>
    private lateinit var gameParameters: GameParameters
    private lateinit var gameParametersCopy: GameParameters
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
            val gameParametersTempo = GameParameters(gameParametersCopy.user_turn, false, diceValuesTempo, true)
            firebaseRef.child("game_parameters").setValue(gameParametersTempo)
        }
        quitButton.setOnClickListener { matchmakingAnnulation.windowStopMatchmakingInGame() }
        rulesButton.setOnClickListener { doRules() }
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
                    //val adapter = LobbyListAdapter(applicationContext, R.layout.lobby_users_list, usersList )
                    //lobbyList.adapter = adapter
                    gameParametersCopy = gameParameters
                    usersListCopy = usersList
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

    private fun diceClickable() {
        dice1.isClickable = true
        dice2.isClickable = true
        culDeChouette.isClickable = true
    }

    override fun hearShake() {
        animation()
        tabletop.isClickable = false
        shakeDetector.stop()
        val diceValuesTempo = DiceValues(valuedice1, valuedice2, valueCDC)
        val gameParametersTempo = GameParameters(gameParametersCopy.user_turn, false, diceValuesTempo, true)
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
                    //val adapter = LobbyListAdapter(applicationContext, R.layout.lobby_users_list, usersList )
                    //lobbyList.adapter = adapter
                    tabletop.isClickable = false
                    shakeDetector.stop()
                    if ((matchmakingSettings?.getString("userKey", null)?:"" == gameParametersCopy.user_turn)
                        && (!snapshot.child("game_parameters").child("dice_value_changed").toString().toBoolean())) {
                        Log.i("Jeu", "Test4")
                        Toast.makeText(this@GameActivity, getString(R.string.yourTurn), Toast.LENGTH_LONG).show()
                        tabletop.isClickable = true
                        setUpShakeDetector()

                    }
                    else if ((matchmakingSettings?.getString("userKey", null)?:"" != gameParametersCopy.user_turn)
                        && (!animationDone) && (snapshot.child("game_parameters").child("dice_value_changed").toString().toBoolean())) {
                        Log.i("Jeu", "Test6")
                        Toast.makeText(this@GameActivity, getString(R.string.turn, snapshot.child("users").child(snapshot.child("game_parameters").child("user_turn").toString()).child("username").toString()), Toast.LENGTH_LONG).show()
                        animationFromDatabase()
                        Log.i("Jeu", "Test7")
                    }
                    //Verification de Victoire ici
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

}
