package fr.isen.culdechouette

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso


class LobbyListAdapter(private val ctxt: Context, private val layoutID: Int, private val userList: List<User>) : ArrayAdapter<User>(ctxt, layoutID, userList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater : LayoutInflater = LayoutInflater.from(ctxt)
        val view : View = layoutInflater.inflate(layoutID, parent, false)
        val user = userList[position]
        val usernameValue = view.findViewById<TextView>(R.id.usernameListElement)
        val userReady = view.findViewById<ImageView>(R.id.readyBox)
        usernameValue.text = user.username
        if (user.ready_boolean) {
            Picasso.get().load(android.R.drawable.checkbox_on_background).into(userReady)
        }
        else {
            Picasso.get().load(android.R.drawable.checkbox_off_background).into(userReady)
        }
        return view
    }
}

class ResultsAdapter(private val ctxt: Context, private val layoutID: Int, private val userList: List<User>) : ArrayAdapter<User>(ctxt, layoutID, userList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater : LayoutInflater = LayoutInflater.from(ctxt)
        val view : View = layoutInflater.inflate(layoutID, parent, false)
        val user = userList[position]
        val usernameValue = view.findViewById<TextView>(R.id.username)
        val rankValue = view.findViewById<TextView>(R.id.rank)
        usernameValue.text = user.username
        rankValue.text = user.score.toString()
        return view
    }
}

