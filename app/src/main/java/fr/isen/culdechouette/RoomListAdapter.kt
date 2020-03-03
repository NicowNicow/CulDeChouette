package fr.isen.culdechouette

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class RoomListAdapter(private val ctxt: Context, private val layoutID: Int, private val roomList: List<Room>) : ArrayAdapter<Room>(ctxt, layoutID, roomList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater : LayoutInflater = LayoutInflater.from(ctxt)
        val view : View = layoutInflater.inflate(layoutID, null)
        val room = roomList[position]
        val roomName = view.findViewById<TextView>(R.id.roomName)
        val roomCapacity = view.findViewById<TextView>(R.id.roomCapacity)
        val lockIcon = view.findViewById<ImageView>(R.id.lockIcon)
        roomName.text = room.room_name
        roomCapacity.text = ctxt.getString(R.string.capacityRoom, room.user_count, room.capacity)
        if (room.room_password_needed) {
            Log.i("pouet",room.room_name)
            lockIcon.visibility = View.VISIBLE
        }
        else {
            lockIcon.visibility = View.INVISIBLE
        }
        return view
    }
}