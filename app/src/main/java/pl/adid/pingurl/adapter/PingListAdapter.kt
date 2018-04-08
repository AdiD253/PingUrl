package pl.adid.pingurl.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import pl.adid.pingurl.R

class PingListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val itemList = mutableListOf<Int>()
    var maxValue: Int = 1
    var lastVisiblePosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ping_item, parent, false)
        return PingViewHolder(view)
    }

    override fun getItemCount() = itemList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as PingViewHolder).bind(itemList[position].toDouble(), maxValue)
    }

    fun addItemTiList(item: Int, lastVisiblePosition: Int) {
        itemList.add(0, item)
        if (this.lastVisiblePosition != lastVisiblePosition) {
            this.lastVisiblePosition = lastVisiblePosition
        } else {
            itemList.remove(lastVisiblePosition)
            notifyItemRemoved(lastVisiblePosition)
        }
        if (item > maxValue) {
            maxValue = item
            notifyDataSetChanged()
        } else {
            notifyItemInserted(0)
        }
    }

    fun clearItemList() {
        itemList.clear()
        maxValue = 1
    }
}