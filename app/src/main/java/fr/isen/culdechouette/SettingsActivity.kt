package fr.isen.culdechouette


import android.content.*
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*
import kotlin.concurrent.schedule



class SettingsActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener{

    private var musicPref: SharedPreferences? = null
    private lateinit var musicService: BackgroundMusicService
    private var musicServiceBound: Boolean = false
    private var muted: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BackgroundMusicService.LocalBinder
            musicService = binder.getService()
            musicServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            musicServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        musicPref = getSharedPreferences("preferedMusic", Context.MODE_PRIVATE)
        Intent(this, BackgroundMusicService::class.java).also { intent -> bindService(intent, connection, Context.BIND_AUTO_CREATE) }
        backButton.setOnClickListener { doBack() }
        muteButton.setOnClickListener { doMute() }
        volumeBar.setOnSeekBarChangeListener(this)
        checkPreferences()
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        actionBar?.hide()
        if (!musicServiceBound) {
            Intent(this, BackgroundMusicService::class.java).also { intent -> bindService(intent, connection, Context.BIND_AUTO_CREATE) }
        }
    }

    override fun onPause() {
        super.onPause()
        if (musicServiceBound) {
            musicService.pauseMusic()
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        musicServiceBound = false
    }

    private fun doBack() {
        val scrollSound = MediaPlayer.create(this, R.raw.elder_scroll)
        scrollSound.start()
        Timer("SoundTemporisation", false).schedule(500) {
            finish()
        }
    }

    private fun doMute() {
        val muteSaved = musicPref?.getString("muteKey", null)?:"false"
        if (muteSaved == "false") {
            Picasso.get().load(android.R.drawable.ic_lock_silent_mode).into(muteButton)
            musicPref?.edit()?.putString("muteKey", "true")?.apply()
            musicService.pauseMusic()
        }
        if (muteSaved == "true") {
            Picasso.get().load(android.R.drawable.ic_lock_silent_mode_off).into(muteButton)
            musicPref?.edit()?.putString("muteKey", "false")?.apply()
            musicService.restartMusic()
        }
    }

    private fun checkPreferences() {
        val volumeSaved = musicPref?.getString("volumeKey", null)?:"100"
        val muteSaved = musicPref?.getString("muteKey", null)?:"false"
        volumeText.text = applicationContext.getString(R.string.volumePercentage, volumeSaved)
        volumeBar.progress = volumeSaved.toInt()
        if (muteSaved == "false") {
            Picasso.get().load(android.R.drawable.ic_lock_silent_mode_off).into(muteButton)
        }
        if (muteSaved == "true") {
            Picasso.get().load(android.R.drawable.ic_lock_silent_mode).into(muteButton)
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) { //Method of the OnSeekBarChangeListener Interface
        val volumePercentage = applicationContext.getString(R.string.volumePercentage, progress.toString())
        volumeText.text = volumePercentage
        musicPref?.edit()?.putString("volumeKey", progress.toString())?.apply()
        if (musicServiceBound) {
            musicService.changeVolume(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {} //Method of the OnSeekBarChangeListener Interface

    override fun onStopTrackingTouch(seekBar: SeekBar) {} //Method of the OnSeekBarChangeListener Interface

}
