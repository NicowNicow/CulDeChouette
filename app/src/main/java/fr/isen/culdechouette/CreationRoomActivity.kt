package fr.isen.culdechouette


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_creation_room.*
import java.util.*
import kotlin.concurrent.schedule


class CreationRoomActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var usernamePref: SharedPreferences? = null
    private lateinit var firebaseRef: DatabaseReference
    private lateinit var firebaseRoomKey: String
    private lateinit var roomCreation: Room

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usernamePref = getSharedPreferences("preferedName", Context.MODE_PRIVATE)
        firebaseRef = FirebaseDatabase.getInstance().getReference("waiting_rooms")
        firebaseRoomKey = firebaseRef.push().key!!
        roomCreation = Room()
        setContentView(R.layout.activity_creation_room)
        setNamePlaceholderValue()
        backButton.setOnClickListener { doBack() }
        creationButton.setOnClickListener { doCreate() }
        createSpinnerAdapter()
        numberPlayerValue.onItemSelectedListener = this
    }

    override fun onResume () {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        actionBar?.hide()
    }

    private fun setNamePlaceholderValue() {
        val username = usernamePref?.getString("usernameKey", null)?:""
        val placeholder = applicationContext.getString(R.string.baseRoomName, username)
        roomNameValue?.setText(placeholder)
    }

    private fun doBack() {
        val scrollSound = MediaPlayer.create(this, R.raw.elder_scroll)
        scrollSound.start()
        Timer("SoundTemporisation", false).schedule(500) {
            val intentBack = Intent(this@CreationRoomActivity, HostChoiceActivity::class.java)
            intentBack.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            this@CreationRoomActivity.startActivity(intentBack)
        }
    }

    private fun doCreate() {
        modifyGameRoomParameters()
    }

    // Demande de password

    private fun createSpinnerAdapter() {
        ArrayAdapter.createFromResource(this,R.array.player_number_choices,R.layout.custom_spinner_text).also { adapter ->
            adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown)
            numberPlayerValue.adapter = adapter
        }
    }

    private fun modifyGameRoomParameters() {
        roomCreation.room_key = firebaseRoomKey
        roomCreation.game_started_boolean = false
        roomCreation.game_parameters = GameParameters()
        roomCreation.room_name = roomNameValue.text.toString()
        firebaseRef.child(firebaseRoomKey).setValue(roomCreation)
        val keyInitialUser = firebaseRef.child(firebaseRoomKey).child("users").push().key!!
        val initialUser = User(keyInitialUser,usernamePref?.getString("usernameKey", null)?:"", 0, false)
        firebaseRef.child(firebaseRoomKey).child("users").child(keyInitialUser).setValue(initialUser)
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) { //Method of the OnItemSelected Interface
        var value = parent.getItemAtPosition(pos).toString().toInt()
        roomCreation.capacity=value
    }

    override fun onNothingSelected(parent: AdapterView<*>) {} //Method of the OnItemSelected Interface
}
