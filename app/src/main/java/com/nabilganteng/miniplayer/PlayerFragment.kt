package com.nabilganteng.miniplayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment

class PlayerFragment : Fragment() {

    private var mediaPlayer: MediaPlayer? = null
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
                seekBar.progress = it.currentPosition
                tvDuration.text = "${formatTime(it.currentPosition)} / ${formatTime(it.duration)}"
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
        // Note: Assumes R.raw.song exists. If not, this will fail to compile.
        // For demonstration, we'll try to initialize it.
        try {
            val resId = resources.getIdentifier("laguhiwong", "raw", requireContext().packageName)
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(requireContext(), resId)
                mediaPlayer?.let {
                    seekBar.max = it.duration
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupControls() {
        btnPlay.setOnClickListener {
            if (requestAudioFocus()) {
                mediaPlayer?.start()
                handler.post(updateSeekBar)
            }
        }
        
        btnPause.setOnClickListener {
            mediaPlayer?.pause()
        }
        
        btnStop.setOnClickListener {
            mediaPlayer?.stop()
            mediaPlayer?.prepare()
            mediaPlayer?.seekTo(0)
            seekBar.progress = 0
            handler.removeCallbacks(updateSeekBar)
        }
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun requestAudioFocus(): Boolean {
        val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> mediaPlayer?.pause()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> mediaPlayer?.pause()
                    AudioManager.AUDIOFOCUS_GAIN -> mediaPlayer?.start()
                }
            }
            .build()

        val result = audioManager.requestAudioFocus(focusRequest!!)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateSeekBar)
        mediaPlayer?.release()
        mediaPlayer = null
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }
}