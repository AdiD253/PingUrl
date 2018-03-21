package pl.adid.pingurl.adapter

import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.ping_item.view.*


class PingViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    fun bind(ping: Double, maxValue: Int) {

        val percent: Double = (ping/maxValue)

        val layoutParams = view.pingValue.layoutParams as LinearLayout.LayoutParams
        layoutParams.height = (50 * percent).toInt().dpToPx()
        view.pingValue.layoutParams = layoutParams
    }

    private fun Int.dpToPx(): Int {
        val res = itemView.context.resources
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), res.displayMetrics).toInt()
    }
}