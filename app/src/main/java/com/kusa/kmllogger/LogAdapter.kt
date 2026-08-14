package com.kusa.kmllogger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class LogEntry(
    val time: String,
    val lat: Double,
    val lng: Double,
    val event: String? = null
)

class LogAdapter : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    private val entries = mutableListOf<LogEntry>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvCoords: TextView = view.findViewById(R.id.tvCoords)
        val tvEvent: TextView = view.findViewById(R.id.tvEvent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.tvTime.text = entry.time
        holder.tvCoords.text = String.format("%.6f, %.6f", entry.lat, entry.lng)
        holder.tvEvent.text = entry.event ?: ""
        holder.tvEvent.visibility = if (entry.event != null) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = entries.size

    fun addEntry(entry: LogEntry) {
        entries.add(0, entry) // Add to top
        notifyItemInserted(0)
    }

    fun clear() {
        val size = entries.size
        entries.clear()
        notifyItemRangeRemoved(0, size)
    }
}
