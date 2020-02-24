package fr.isen.culdechouette


import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.util.*
import kotlin.concurrent.schedule
import android.media.MediaPlayer
import android.view.*




//Creation of the main menu, nothing really interesting here

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val intentMusic = Intent(this@MainActivity, BackgroundMusicService::class.java)
        startService(intentMusic)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainID.setOnClickListener{ doLogin() }
    }

    override fun onResume () {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        actionBar?.hide()
    }

    private fun doLogin() {
        val doorSound = MediaPlayer.create(this, R.raw.door)
        doorSound.start()
        Timer("SoundTemporisation", false).schedule(500) {
            val intentHome = Intent(this@MainActivity, HostChoiceActivity::class.java)
            intentHome.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            this@MainActivity.startActivity(intentHome)
        }
    }

}
