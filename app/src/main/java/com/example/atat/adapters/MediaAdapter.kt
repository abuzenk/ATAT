package com.example.atat.adapters

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.atat.R

class MediaAdapter(
    private val isVideoMode: Boolean,
    private val onItemClick: (Uri) -> Unit
) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
    
    private var mediaUris = listOf<Uri>()
    private var selectedPosition = -1
    
    fun submitList(list: List<Uri>) {
        mediaUris = list
        notifyDataSetChanged()
    }
    
    fun clearSelection() {
        selectedPosition = -1
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mediaUris[position], position)
    }
    
    override fun getItemCount() = mediaUris.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val imageView: ImageView = itemView.findViewById(R.id.ivMediaThumbnail)
        private val videoIndicator: FrameLayout = itemView.findViewById(R.id.videoIndicator)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val selectedOverlay: View = itemView.findViewById(R.id.selectedOverlay)
        
        fun bind(uri: Uri, position: Int) {
            val context = itemView.context
            
            selectedOverlay.visibility = if (selectedPosition == position) View.VISIBLE else View.GONE
            
            if (isVideoMode) {
                videoIndicator.visibility = View.VISIBLE
                tvDuration.visibility = View.VISIBLE
                
                try {
                    val bitmap = context.contentResolver.loadThumbnail(
                        uri, 
                        android.util.Size(200, 200), 
                        null
                    )
                    imageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Glide.with(context)
                        .load(uri)
                        .centerCrop()
                        .into(imageView)
                }
                
                loadVideoDuration(context.contentResolver, uri) { duration ->
                    tvDuration.text = duration
                }
                
            } else {
                videoIndicator.visibility = View.GONE
                tvDuration.visibility = View.GONE
                
                Glide.with(context)
                    .load(uri)
                    .centerCrop()
                    .thumbnail(0.1f)
                    .into(imageView)
            }
            
            itemView.setOnClickListener {
                selectedPosition = position
                notifyDataSetChanged()
                onItemClick(uri)
            }
        }
        
        private fun loadVideoDuration(contentResolver: ContentResolver, uri: Uri, callback: (String) -> Unit) {
            try {
                val cursor = contentResolver.query(
                    uri,
                    arrayOf(MediaStore.Video.Media.DURATION),
                    null,
                    null,
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val durationMs = it.getLong(0)
                        val durationSec = durationMs / 1000
                        val minutes = durationSec / 60
                        val seconds = durationSec % 60
                        callback(String.format("%02d:%02d", minutes, seconds))
                    } else {
                        callback("00:00")
                    }
                }
            } catch (e: Exception) {
                callback("00:00")
            }
        }
    }
}
