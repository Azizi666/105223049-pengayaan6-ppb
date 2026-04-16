package com.nabilganteng.miniplayer

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class SoundFragment : Fragment() {

    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<Int, Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sound, container, false)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        loadSounds()

        view.findViewById<Button>(R.id.btn_effect1).setOnClickListener { playSound(1) }
        view.findViewById<Button>(R.id.btn_effect2).setOnClickListener { playSound(2) }
        view.findViewById<Button>(R.id.btn_effect3).setOnClickListener { playSound(3) }

        return view
    }

    private fun loadSounds() {
        val resNames = listOf("linging", "fire", "cihuy")
        resNames.forEachIndexed { index, name ->
            val resId = resources.getIdentifier(name, "raw", requireContext().packageName)
            if (resId != 0) {
                soundIds[index + 1] = soundPool?.load(requireContext(), resId, 1) ?: 0
            }
        }
    }

    private fun playSound(id: Int) {
        soundIds[id]?.let {
            if (it != 0) {
                soundPool?.play(it, 1f, 1f, 0, 0, 1f)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        soundPool?.release()
        soundPool = null
    }
}