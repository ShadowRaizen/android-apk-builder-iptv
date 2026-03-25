package com.example.pinoyworldtv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class Channel(val name: String, val url: String)

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var recycler: RecyclerView
    private val channelList = mutableListOf<Channel>()
    private lateinit var adapter: ChannelAdapter

    // Public playlists (always up-to-date 2026)
    private val PH_PLAYLIST = "https://iptv-org.github.io/iptv/countries/ph.m3u"
    private val INT_PLAYLIST = "https://iptv-org.github.io/iptv/categories/news.m3u"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        recycler = findViewById(R.id.recyclerChannels)

        // ExoPlayer setup (Full HD support)
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        adapter = ChannelAdapter(channelList) { channel ->
            playChannel(channel)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Button clicks
        findViewById<View>(R.id.btnPhilippines).setOnClickListener {
            loadPlaylist(PH_PLAYLIST, "Philippine Local Channels")
        }
        findViewById<View>(R.id.btnInternational).setOnClickListener {
            loadPlaylist(INT_PLAYLIST, "International News Channels")
        }
        findViewById<View>(R.id.btnAddUrl).setOnClickListener {
            showAddUrlDialog()
        }

        // Load Philippine channels automatically on start
        loadPlaylist(PH_PLAYLIST, "Philippine Local Channels")
    }

    private fun loadPlaylist(url: String, title: String) {
        Toast.makeText(this, "Loading $title...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                val input = BufferedReader(InputStreamReader(connection.inputStream))
                val content = input.use { it.readText() }
                val channels = parseM3U(content)

                runOnUiThread {
                    channelList.clear()
                    channelList.addAll(channels)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "${channels.size} channels loaded!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Failed to load playlist: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun parseM3U(m3u: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = m3u.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF")) {
                val name = line.substringAfterLast(",").trim()
                i++
                if (i < lines.size) {
                    val url = lines[i].trim()
                    if (url.startsWith("http")) {
                        channels.add(Channel(name, url))
                    }
                }
            }
            i++
        }
        return channels
    }

    private fun playChannel(channel: Channel) {
        val mediaItem = MediaItem.fromUri(channel.url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        Toast.makeText(this, "Playing: ${channel.name}", Toast.LENGTH_SHORT).show()
    }

    private fun showAddUrlDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Paste full M3U playlist URL here"
            setPadding(40, 40, 40, 40)
        }
        AlertDialog.Builder(this)
            .setTitle("Add Custom Playlist")
            .setView(input)
            .setPositiveButton("Load") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) loadPlaylist(url, "Custom Playlist")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

// RecyclerView Adapter
class ChannelAdapter(
    private val channels: List<Channel>,
    private val onClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = channels[position]
        holder.name.text = "▶ ${channel.name}"
        holder.itemView.setOnClickListener { onClick(channel) }
    }

    override fun getItemCount() = channels.size
}
