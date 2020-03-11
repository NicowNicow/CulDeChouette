package fr.isen.culdechouette


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_tutorial.*
import java.util.*
import kotlin.concurrent.schedule


class TutorialActivity : AppCompatActivity() {

    private lateinit var matchmakingAnnulation: MatchmakingAnnulation
    private lateinit var  firebaseRef: DatabaseReference
    private var matchmakingSettings : SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        backButton.setOnClickListener { doBack() }
        settingsButton.setOnClickListener { doSettings() }
        setUpFirebaseSharedPref()
        setUpMatchmakingCancellation()
        startService(Intent(this@TutorialActivity, MatchmakingDisconnectedService::class.java))
    }

    private fun setUpFirebaseSharedPref() {
        matchmakingSettings = getSharedPreferences("currentRoom", Context.MODE_PRIVATE)
        firebaseRef= FirebaseDatabase.getInstance().getReference("waiting_rooms").child(matchmakingSettings?.getString("roomKey", null)?:"")
    }

    private fun setUpMatchmakingCancellation() {
        val displayParam: DisplayMetrics? = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayParam)
        matchmakingAnnulation = MatchmakingAnnulation(this@TutorialActivity, displayParam!!,
            matchmakingSettings?.getString("userKey", null)?:"",
            matchmakingSettings?.getString("roomKey", null)?:"", firebaseRef)
    }

    override fun onBackPressed() {
        matchmakingAnnulation.windowStopMatchmakingInGame()
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
