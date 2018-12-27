package dropit.mobile.ui.sending.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dropit.application.dto.TokenStatus
import dropit.mobile.R
import dropit.mobile.domain.entity.Computer

class ServerListAdapter(
    val context: Context,
    var items: ArrayList<Computer>,
    val onClickListener: (item: Computer) -> Unit) : RecyclerView.Adapter<ServerListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.server_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val computer = items[position]
        holder.serverName.text = computer.name
        holder.serverIp.text = computer.ipAddress
        holder.serverIsDefault.visibility = if (computer.default) View.VISIBLE else View.INVISIBLE
        holder.serverIsPaired.visibility = if (computer.tokenStatus == TokenStatus.AUTHORIZED) View.VISIBLE else View.INVISIBLE
        if (computer.default) {
            val color = context.resources.getColor(R.color.materialGreen)
            holder.serverName.setTextColor(color)
            holder.serverIp.setTextColor(color)
            holder.serverIsDefault.setTextColor(color)
            holder.serverIsPaired.setTextColor(color)
        } else if (computer.tokenStatus == TokenStatus.AUTHORIZED) {
            val color = context.resources.getColor(R.color.listGray)
            holder.serverName.setTextColor(color)
            holder.serverIp.setTextColor(color)
            holder.serverIsDefault.setTextColor(color)
            holder.serverIsPaired.setTextColor(color)
        } else {
            holder.serverName.setTextColor(holder.defaultTextColor)
            holder.serverIp.setTextColor(holder.defaultTextColor)
            holder.serverIsDefault.setTextColor(holder.defaultTextColor)
            holder.serverIsPaired.setTextColor(holder.defaultTextColor)
        }

        holder.view.setOnClickListener { onClickListener.invoke(computer) }
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val serverName = view.findViewById<TextView>(R.id.serverName)!!
        val serverIp = view.findViewById<TextView>(R.id.serverIp)!!
        val serverIsDefault = view.findViewById<TextView>(R.id.serverIsDefault)!!
        val serverIsPaired = view.findViewById<TextView>(R.id.serverIsPaired)!!

        val defaultTextColor = serverName.currentTextColor
    }


}