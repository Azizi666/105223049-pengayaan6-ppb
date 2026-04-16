package com.nabilganteng.miniplayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class PlayerFragment : Fragment() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private var hasAudioFocus = false

    private lateinit var seekBar: SeekBar
    private lateinit var tvDuration: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button

    private lateinit var audioManager: AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBar = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    val currentPos = it.currentPosition
                    seekBar.progress = currentPos
                    tvDuration.text = "${formatTime(currentPos)} / ${formatTime(it.duration)}"
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player, container, false)

        seekBar = view.findViewById(R.id.seek_bar)
        tvDuration = view.findViewById(R.id.tv_duration)
        btnPlay = view.findViewById(R.id.btn_play)
        btnPause = view.findViewById(R.id.btn_pause)
        btnStop = view.findViewById(R.id.btn_stop)

        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setupMediaPlayer()
        setupControls()

        return view
    }

    private fun setupMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                try {
                    val assetFileDescriptor = resources.openRawResourceFd(R.raw.laguhiwong)
                    setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                    assetFileDescriptor.close()

                    setOnPreparedListener {
                        isPrepared = true
                        seekBar.max = it.duration
                        tvDuration.text = "00:00 / ${formatTime(it.duration)}"
                        Log.d("PlayerFragment", "MediaPlayer Prepared")
                    }

                    setOnCompletionListener {
                        pausePlayback()
                        it.seekTo(0)
                        seekBar.progress = 0
                        tvDuration.text = "00:00 / ${formatTime(it.duration)}"
                    }

                    setOnErrorListener { mp, what, extra ->
                        Log.e("PlayerFragment", "MediaPlayer Error: $what, $extra")
                        mp.reset()
                        isPrepared = false
                        setupMediaPlayer()
                        true
                    }

                    prepareAsync()
                } catch (e: Exception) {
                    Log.e("PlayerFragment", "Error setting data source", e)
                }
            }
        }
    }

    private fun setupControls() {
        btnPlay.setOnClickListener {
            if (isPrepared) {
                if (mediaPlayer?.isPlaying == false) {
                    if (!hasAudioFocus && !requestAudioFocus()) {
                        Toast.makeText(context, "Gagal fokus audio", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                mediaPlayer?.start()
                handler.post(updateSeekBar)
                Log.d("PlayerFragment", "Playback Started/Resumed")
            } else {
                Toast.makeText(context, "Media sedang bersiap...", Toast.LENGTH_SHORT).show()
                setupMediaPlayer()
            }
        }

        btnPause.setOnClickListener {
            pausePlayback()
        }

        btnStop.setOnClickListener {
            mediaPlayer?.let {
                it.pause()
                it.seekTo(0)
                seekBar.progress = 0
                tvDuration.text = "00:00 / ${formatTime(it.duration)}"
                Log.d("PlayerFragment", "Playback Stopped (Reset to 0)")
            }
            stopUpdatingSeekBar()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isPrepared) {
                    mediaPlayer?.seekTo(progress)
                    tvDuration.text = "${formatTime(progress)} / ${formatTime(mediaPlayer?.duration ?: 0)}"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun pausePlayback() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            Log.d("PlayerFragment", "Playback Paused")
        }
        stopUpdatingSeekBar()
    }

    private fun stopUpdatingSeekBar() {
        handler.removeCallbacks(updateSeekBar)
    }

    private fun requestAudioFocus(): Boolean {
        val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        hasAudioFocus = false
                        pausePlayback()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausePlayback()
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        hasAudioFocus = true
                        if (isPrepared) mediaPlayer?.start()
                    }
                }
            }
            .build()

        val result = audioManager.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        hasAudioFocus = result
        return result
    }

    private fun formatTime(milliseconds: Int): String {
        val secondsTotal = milliseconds / 1000
        val minutes = secondsTotal / 60
        val seconds = secondsTotal % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopUpdatingSeekBar()
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }
}