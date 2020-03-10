package fr.isen.culdechouette


import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_tutorial.*
import java.util.*
import kotlin.concurrent.schedule


class TutorialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        backButton.setOnClickListener { doBack() }
        settingsButton.setOnClickListener { doSettings() }
    }

    override fun onResume () {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        actionBar?.hide()
    }

    private fun doBack() {
        val scrollSound = MediaPlayer.create(this, R.raw.elder_scroll)
        scrollSound.start()
        Timer("SoundTemporisation", false).schedule(500) {
            finish()
        }
    }

    private fun doSettings() {
        val intentSettings = Intent(this@TutorialActivity, SettingsActivity::class.java)
        intentSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        this@TutorialActivity.startActivity(intentSettings)
    }
}
