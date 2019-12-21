package meehan.matthew.musicplayerdemo

import android.media.browse.MediaBrowser
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class SongRecyclerAdapter(private val songList: MutableList<MediaBrowser.MediaItem>, private val clickListener: (MediaBrowser.MediaItem) -> Unit) : RecyclerView.Adapter<SongRecyclerAdapter.SongRecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongRecyclerViewHolder {
        return SongRecyclerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.song_recycler_item, parent, false))
    }

    override fun getItemCount(): Int = songList.size

    override fun onBindViewHolder(holder: SongRecyclerViewHolder, position: Int) {
        holder.titleTextView.text = songList[position].description.title
        holder.artistTextView.text = songList[position].description.subtitle
        holder.itemFrameLayout.setOnClickListener {
            clickListener.invoke(songList[position])
        }
        Picasso.get().load(songList[position].description.iconUri).placeholder(R.drawable.ic_launcher_foreground).into(holder.albumArtImageView)
    }
    
    inner class SongRecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.title_text_view)
        val artistTextView: TextView = view.findViewById(R.id.artist_text_view)
        val albumArtImageView: ImageView = view.findViewById(R.id.album_art_image_view)
        val itemFrameLayout: FrameLayout = view.findViewById(R.id.item_frame_layout)
    }
}