package fr.isen.culdechouette


import android.content.Context
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
import androidx.core.view.GestureDetectorCompat
import java.util.*
import kotlin.concurrent.schedule
import android.view.KeyEvent.KEYCODE_BACK
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.media.MediaPlayer
import android.view.*
import android.widget.Toast


//Creation of the main menu, nothing really interesting here

class MainActivity : AppCompatActivity(), ShakeDetector.Listener {

    private lateinit var shakeDetector: ShakeDetector


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shakeDetector = ShakeDetector(this)

        setContentView(R.layout.activity_main)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or SYSTEM_UI_FLAG_IMMERSIVE
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        actionBar?.hide()
        //mainID.setOnClickListener{ doLogin() }
    }

    override fun onResume () {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or SYSTEM_UI_FLAG_IMMERSIVE
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        actionBar?.hide()
    }

    private fun doLogin() {
        val scrollSound = MediaPlayer.create(this,R.raw.door)
        scrollSound.start()
        Timer("SoundTemporisation", false).schedule(500) {
            val intentHome = Intent( this@MainActivity, HostChoiceActivity::class.java)
            intentHome.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
            this@MainActivity.startActivity(intentHome)
        }
    }
    override fun hearShake() {

    }

    override fun onStart() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        Toast.makeText(this, "onStart", Toast.LENGTH_SHORT).show()
        shakeDetector.start(sensorManager)
        super.onStart()
    }

    override fun onStop() {
        shakeDetector.stop()
        Toast.makeText(this, "onStop", Toast.LENGTH_SHORT).show()
        super.onStop()
    }
}

