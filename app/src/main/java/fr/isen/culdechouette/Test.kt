package fr.isen.culdechouette

import android.content.Context
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.view.animation.*
import android.view.animation.Animation.AnimationListener
import kotlinx.android.synthetic.main.activity_test.*
import kotlinx.android.synthetic.main.activity_test.view.*
import kotlin.collections.ArrayList

class Test : AppCompatActivity(), ShakeDetector.Listener {

    var imageList = ArrayList<Int>()
    var culDeChouetteList = ArrayList<Int>()
    var finaldice1 = 0
    var finaldice2 = 0
    var finalCDC = 0
    var premierLancer = true

    private lateinit var shakeDetector: ShakeDetector
    var i =0


    var countDownCDC = object : CountDownTimer(2600,100){
        override fun onTick(millisUntilFinished: Long) {
            val random = (0..5).random()

            when (i){
                in 0..14 -> culDeChouette.setImageResource(culDeChouetteList[random])
                in 15..21-> if(i%2 == 0) { culDeChouette.setImageResource(culDeChouetteList[random])}
                in 22..24 -> if(i%3 == 0) {culDeChouette.setImageResource(culDeChouetteList[random])}
            }
            i++
        }

        override fun onFinish() {
            i=0
            culDeChouette.setImageResource(culDeChouetteList[finalCDC])
            premierLancer = true
        }
    }



    var countDownTimer = object: CountDownTimer(2600,100){

        override fun onTick(millisUntilFinished: Long) {
            val randomdice1 = (0..5).random()
            val randomdice2 = (0..5).random()

            when (i){
                in 0..14 -> {dice1.setImageResource(imageList[randomdice1])
                            dice2.setImageResource(imageList[randomdice2])}
                in 15..21-> if(i%2 == 0) { dice1.setImageResource(imageList[randomdice1])
                                dice2.setImageResource(imageList[randomdice2]) }
                in 22..24 -> if(i%3 == 0) {dice1.setImageResource(imageList[randomdice1])
                    dice2.setImageResource(imageList[randomdice2]) }
            }
            i++
        }

        override fun onFinish() {
            dice1.setImageResource(imageList[finaldice1])
            dice2.setImageResource(imageList[finaldice2])
            premierLancer = false
            i=0
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shakeDetector = ShakeDetector(this)


        setContentView(R.layout.activity_test)


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


        Button.setOnClickListener {
            animation()
        }
    }


    private fun animation() {
        finaldice1 = (0..5).random()
        finaldice2 = (0..5).random()
        finalCDC = (0..5).random()

        val hyperspaceJump: Animation =
            AnimationUtils.loadAnimation(this, R.anim.hyperspace_jump)
        hyperspaceJump.setAnimationListener(object : AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                shakeDetector.start(sensorManager)
                Button.isEnabled = true
                Button.isClickable = true
            }


            override fun onAnimationStart(animation: Animation?) {
                if(premierLancer){
                    countDownTimer.start()
                }
                else{
                    countDownCDC.start()
                }
                shakeDetector.stop()
                Button.isEnabled = false
                Button.isClickable = false
            }
        })
        if(premierLancer){
            dice1.startAnimation(hyperspaceJump)
            dice2.startAnimation(hyperspaceJump)
        }
        else {
            culDeChouette.startAnimation(hyperspaceJump)
        }

    }
    override fun onResume () {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        actionBar?.hide()
    }



    override fun hearShake() {
        animation()
    }

    override fun onStart() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector.start(sensorManager)
        super.onStart()
    }

    override fun onStop() {
        shakeDetector.stop()
        super.onStop()
    }



}

