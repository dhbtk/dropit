package dropit.mobile.ui.transfer.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dropit.mobile.R


import dropit.mobile.ui.transfer.model.ListServer

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class ListServerAdapter(val items: List<ListServer>)
    : RecyclerView.Adapter<ListServerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.server_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.ip.text = item.ip

        with(holder.view) {
            tag = item
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.serverName)
        val ip = view.findViewById<TextView>(R.id.serverIp)

        override fun toString(): String {
            return super.toString() + " '" + name.text + "'"
        }
    }
}
