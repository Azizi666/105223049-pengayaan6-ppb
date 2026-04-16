package com.nabilganteng.miniplayer

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.IOException

class RecorderFragment : Fragment() {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var fileName: String = ""
    
    private lateinit var tvStatus: TextView
    private lateinit var btnRecord: Button
    private lateinit var btnStopRecord: Button
    private lateinit var btnPlayRecording: Button

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recorder, container, false)
        
        fileName = "${requireContext().externalCacheDir?.absolutePath}/audiorecordtest.3gp"
        
        tvStatus = view.findViewById(R.id.tv_status)
        btnRecord = view.findViewById(R.id.btn_record)
        btnStopRecord = view.findViewById(R.id.btn_stop_record)
        btnPlayRecording = view.findViewById(R.id.btn_play_recording)
        
        btnRecord.setOnClickListener {
            if (checkPermissions()) {
                startRecording()
            } else {
                requestPermissions(permissions, REQUEST_RECORD_AUDIO_PERMISSION)
            }
        }
        
        btnStopRecord.setOnClickListener {
            stopRecording()
        }
        
        btnPlayRecording.setOnClickListener {
            startPlaying()
        }
        
        return view
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (permissionToRecordAccepted) {
            startRecording()
        } else {
            Toast.makeText(context, "DITOLAK!!!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startRecording() {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            start()
        }
        tvStatus.text = "Status: Sedang Merekamm..."
        btnRecord.isEnabled = false
        btnStopRecord.isEnabled = true
        btnPlayRecording.isEnabled = false
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        tvStatus.text = "Status: Rekaman Selesai"
        btnRecord.isEnabled = true
        btnStopRecord.isEnabled = false
        btnPlayRecording.isEnabled = true
    }

    private fun startPlaying() {
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}