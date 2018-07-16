package dropit.mobile.ui.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import dropit.mobile.R
import dropit.mobile.ui.model.ListFile

class ListFileAdapter(val context: Context, val items: ArrayList<ListFile>) : RecyclerView.Adapter<ListFileAdapter.ListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.send_list_item, parent, false)
        return ListViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.fileRequest.fileName
        holder.size.text = Formatter.formatShortFileSize(context, item.fileRequest.fileSize ?: 0)
        holder.icon.setImageDrawable(item.getIcon(context))
    }

    class ListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.name)
        val size = view.findViewById<TextView>(R.id.size)
        val icon = view.findViewById<ImageView>(R.id.icon)
        val background = view.findViewById<RelativeLayout>(R.id.view_background)
        val foreground = view.findViewById<RelativeLayout>(R.id.view_foreground)
    }

    fun add(item: ListFile, pos: Int) {
        items.add(pos, item)
        notifyItemInserted(pos)
    }

    fun remove(pos: Int) {
        items.removeAt(pos)
        notifyItemRemoved(pos)
    }
}