package fr.isen.culdechouette

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageButton
import com.google.firebase.database.*
import java.util.*
import kotlin.concurrent.schedule

class MatchmakingAnnulation(var context: Context, var displayParam: DisplayMetrics, var userKey: String,  var roomKey: String,  var firebaseRef: DatabaseReference) {


    fun windowStopMatchmaking() {
        val matchmakingDialogBuilder: AlertDialog.Builder? = context.let { AlertDialog.Builder(it) }
        val layoutParams: WindowManager.LayoutParams? = WindowManager.LayoutParams()
        val layoutInflater = LayoutInflater.from(context)
        layoutParams?.width = (displayParam.widthPixels.times(0.9f)).toInt()
        layoutParams?.height = (displayParam.heightPixels.times(0.9f)).toInt()
        matchmakingDialogBuilder?.setView(layoutInflater.inflate(R.layout.matchmaking_stop_popup, null))?.create()
        val matchmakingDialog = matchmakingDialogBuilder?.show()
        matchmakingDialog?.window?.attributes = layoutParams
        matchmakingDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        matchmakingDialog?.window?.setGravity(Gravity.CENTER)
        matchmakingDialog?.findViewById<ImageButton>(R.id.cancelButton)?.setOnClickListener { matchmakingDialog.dismiss() }
        matchmakingDialog?.findViewById<ImageButton>(R.id.confirmButton)?.setOnClickListener { cancelMatchmaking(matchmakingDialog, context, firebaseRef, userKey , roomKey) }
    }

    private fun cancelMatchmaking(matchmakingDialog: AlertDialog, context: Context, firebaseRef: DatabaseReference, userKey: String, roomKey: String) {
        deleteUserData(firebaseRef, userKey, roomKey)
        var childrenCount: Int
        firebaseRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.i("DatabaseError", error.toString())
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    childrenCount = snapshot.child(roomKey).child("users").childrenCount.toInt()
                    if (childrenCount == 0) {
                        deleteRoomData(firebaseRef, roomKey)
                    }
                    else {
                        firebaseRef.child(roomKey).child("user_count").setValue(childrenCount)
                    }
                }
            }
        })
        matchmakingDialog.dismiss()
        goToMainMenu(context)
    }

    fun cancelMatchmakingAppKilled(firebaseRef: DatabaseReference, userKey: String, roomKey: String) {
        deleteUserData(firebaseRef, userKey, roomKey)
        var childrenCount: Int
        firebaseRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.i("DatabaseError", error.toString())
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    childrenCount = snapshot.child(roomKey).child("users").childrenCount.toInt()
                    if (childrenCount == 0) {
                        deleteRoomData(firebaseRef, roomKey)
                    }
                    else {
                        firebaseRef.child(roomKey).child("user_count").setValue(childrenCount)
                    }
                }
            }
        })
    }

    private fun deleteUserData(firebaseRef: DatabaseReference, userKey: String, roomKey: String) {
        firebaseRef.child(roomKey).child("users").child(userKey).removeValue()
    }

    private fun deleteRoomData(firebaseRef: DatabaseReference, roomKey: String) {
            firebaseRef.child(roomKey).removeValue()
    }

    private fun goToMainMenu(context: Context) {
        val scrollSound = MediaPlayer.create(context, R.raw.elder_scroll)
        scrollSound.start()
        Timer("SoundTemporisation", false).schedule(500) {
            val intentMain = Intent(context, MainActivity::class.java)
            intentMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intentMain)
        }
    }
}