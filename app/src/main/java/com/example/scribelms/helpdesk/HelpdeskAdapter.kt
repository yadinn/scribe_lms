package com.example.scribelms.helpdesk

import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scribelms.R
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class HelpdeskAdapter(
    private val messages: List<Map<String, Any>>,
    private val myUid: String,
    private val onQuickReply: ((String) -> Unit)? = null
) : RecyclerView.Adapter<HelpdeskAdapter.VH>() {

    companion object {
        const val TYPE_SENT = 0
        const val TYPE_RECV = 1
        const val TYPE_BOT  = 2
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvText:   TextView?     = v.findViewById(R.id.tvMessageText)
        val tvSender: TextView?     = v.findViewById(R.id.tvSenderName)
        val tvTime:   TextView?     = v.findViewById(R.id.tvTime)
        val llChips:  LinearLayout? = v.findViewById(R.id.llQuickReplies)
        val ivCover:  ImageView?    = v.findViewById(R.id.ivBookCover)
    }

    override fun getItemViewType(pos: Int): Int {
        val msg = messages[pos]
        return when (msg["senderRole"]?.toString()) {
            "bot", "system" -> TYPE_BOT
            else            -> if (msg["senderUid"] == myUid) TYPE_SENT else TYPE_RECV
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout = when (viewType) {
            TYPE_SENT -> R.layout.item_helpdesk_sent
            TYPE_BOT  -> R.layout.item_helpdesk_bot
            else      -> R.layout.item_helpdesk_received
        }
        return VH(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg    = messages[position]
        val isLast = position == itemCount - 1

        holder.tvText?.text   = msg["text"]?.toString() ?: ""
        holder.tvSender?.text = msg["senderName"]?.toString() ?: ""

        val ts = msg["timestamp"] as? Timestamp
        holder.tvTime?.text = if (ts != null)
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(ts.toDate()) else ""

        // Book cover image (only for bot messages)
        val imageUrl = msg["imageUrl"]?.toString()
        if (holder.ivCover != null) {
            if (!imageUrl.isNullOrBlank()) {
                holder.ivCover.visibility = View.VISIBLE
                try {
                    Glide.with(holder.ivCover.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_book_placeholder)
                        .error(R.drawable.ic_book_placeholder)
                        .centerCrop()
                        .into(holder.ivCover)
                } catch (e: Exception) {
                    holder.ivCover.visibility = View.GONE
                }
            } else {
                holder.ivCover.visibility = View.GONE
            }
        }

        // Quick reply chips — only on the LAST bot message
        val llChips = holder.llChips
        if (llChips != null) {
            llChips.removeAllViews()
            val replies = (msg["quickReplies"] as? List<*>)
                ?.mapNotNull { it?.toString() }
                ?: emptyList()

            if (replies.isNotEmpty() && isLast) {
                llChips.visibility = View.VISIBLE
                replies.forEach { label ->
                    val chip = LayoutInflater.from(llChips.context)
                        .inflate(R.layout.item_chip_quick_reply, llChips, false) as TextView
                    chip.text = label
                    chip.setOnClickListener { onQuickReply?.invoke(label) }
                    llChips.addView(chip)
                }
            } else {
                llChips.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = messages.size
}
