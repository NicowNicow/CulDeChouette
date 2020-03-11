package fr.isen.culdechouette

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.media.MediaPlayer
import android.os.Binder
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner


class BackgroundMusicService: Service(){

    private var musicPref : SharedPreferences? = null
    private lateinit var backgroundMusic: MediaPlayer
    private val binder = LocalBinder()
    private var mediaPauseLength: Int = 0
    private var firstStart: Boolean = false

    override fun onBind(arg0: Intent): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): BackgroundMusicService = this@BackgroundMusicService
    }

    inner class AppLifecycleObserver : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onForeground() {
            if ((::backgroundMusic.isInitialized)&&(firstStart)) {
                restartMusic()
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onBackground() {
            if (::backgroundMusic.isInitialized) {
                pauseMusic()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        musicPref = getSharedPreferences("preferedMusic", Context.MODE_PRIVATE)
        backgroundMusic = MediaPlayer.create(this, R.raw.background_music)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
        backgroundMusic.isLooping = true
        val volumeSaved = musicPref?.getString("volumeKey", null)?:"100"
        changeVolume(volumeSaved.toInt())
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val muteSaved = musicPref?.getString("muteKey", null)?:"false"
        if (muteSaved == "false")
        {
            backgroundMusic.start()
            firstStart = true
            return START_STICKY
        }
        return START_STICKY
    }

    override fun onStart(intent: Intent, startId: Int) {}

    fun changeVolume(volume: Int) {
        backgroundMusic.setVolume((volume/100.0).toFloat(),(volume/100.0).toFloat())
    }

    fun pauseMusic() {
        mediaPauseLength = backgroundMusic.currentPosition
        backgroundMusic.pause()
    }

    fun restartMusic() {
        val muteSaved = musicPref?.getString("muteKey", null)?:"false"
        try {
            if (muteSaved == "false") {
                backgroundMusic.seekTo(mediaPauseLength)
                backgroundMusic.start()
            }
        }
        catch (error: IllegalStateException) {
            Log.i("IllegalStateException", error.toString())
        }
    }

    override fun onDestroy() {
        backgroundMusic.stop()
        backgroundMusic.release()
    }

    override fun onLowMemory() {} //Method of the Service interface

}