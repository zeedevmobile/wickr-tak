package com.atakmap.android.wickr.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.atakmap.android.wickr.*
import com.atakmap.android.wickr.plugin.R
import com.atakmap.coremap.filesystem.FileSystemUtils
import kotlinx.android.synthetic.main.voice_message.*
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.time.Instant


class SendVoiceMessageFragment(
    private val pluginContext: Context,
    val wickrId: String = "",
    var convoId: String = "",
    val standAlone: Boolean = false
) : Fragment() {

    companion object {
        val SENDFILE_ID = "sendVoiceMessage"
    }

    val requests = Requests(pluginContext)
    var fullName: String? = ""
    var chosenMimeType: String? = null
    private var _tempDirPath: File? = null
    private var _filePath: File? = null
    private var _mediaRecorder: MediaRecorder? = null
    private var _mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var hasRecorded = false
    lateinit var gestureDetector: GestureDetector

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isRecording = false
        hasRecorded = false
        _tempDirPath = null
        _filePath = null

        if (isMicPresent()) {
            getMicPermission();
        }
        return LayoutInflater.from(pluginContext).inflate(R.layout.voice_message, container, false)
    }

    fun setRecipientTitle(fullname: String) {
        this.fullName = fullname
    }

    fun sendRecording() {
        if (isRecording) {
            stopRecording()
        }

        val r = Requests(pluginContext)
        if (wickrId != null && (convoId == null || convoId.isEmpty())) {
            r.createConvo(wickrId, SENDFILE_ID)
        }

        chosenMimeType = "audio/mpeg"

        if (convoId != null && _filePath != null && chosenMimeType != null) {
            r.sendFile(
                convoId!!,
                _filePath!!,
                FileSystemUtils.getFileSize(_filePath!!),
                chosenMimeType!!
            )
            if (standAlone) {
                WickrMapComponent.EVENTBUS.post(RequestCloseDropDownEvent())
            }
        }
    }

    fun cancelSend() {
        if (standAlone) {
            WickrMapComponent.EVENTBUS.post(RequestCloseDropDownEvent())
        } else {
            if (hasRecorded) {
                cleanupTemp()
            }

            WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pushToTalk.setOnTouchListener(View.OnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRecording()
                    pushToTalk.text = pluginContext.getText(R.string.release_to_send)
                }
                MotionEvent.ACTION_UP -> {
                    stopRecording()
                    sendRecording()
                    pushToTalk.text = pluginContext.getText(R.string.hold_to_record)
                }
            }

            v?.onTouchEvent(event) ?: true
        })
    }

    override fun onStart() {
        super.onStart()
        WickrMapComponent.EVENTBUS.register(this)
    }

    override fun onStop() {
        super.onStop()
        WickrMapComponent.EVENTBUS.unregister(this)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun getMicPermission() {
        if (pluginContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) === PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 200)
        }
    }

    private fun initFile() {
        _tempDirPath = FileSystemUtils.createTempDir(
            "temp",
            null,
            FileSystemUtils.getItem(FileSystemUtils.TOOL_DATA_DIRECTORY)
        )
        _filePath = File(_tempDirPath, "vm" + Instant.now().toEpochMilli().toString() + ".mp3")
    }

    private fun cleanupTemp() {
        if (_tempDirPath!!.exists()) {
            if (_filePath!!.exists()) {
                _filePath!!.delete()
            }
            _tempDirPath!!.delete()
        }
    }


    private fun isMicPresent(): Boolean {
        return pluginContext.getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
    }

    private fun noPluginDataRoot(): Boolean {
        return !_tempDirPath!!.exists() && !_tempDirPath!!.mkdirs()
    }

    private fun startRecording() {
        initFile()
        _mediaRecorder = MediaRecorder()
        _mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        _mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        _mediaRecorder!!.setOutputFile(_filePath)
        if (noPluginDataRoot()) {
            return
        }
        _mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        _mediaRecorder!!.prepare()
        _mediaRecorder!!.start()
        isRecording = true

        //Toast.makeText(pluginContext, "Record Start", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        if (!isRecording) {
            return
        }

        if (isRecording) {
            fileTitle.text = pluginContext.getString(R.string.title_voice_message_ptt)
            _mediaRecorder!!.stop()
            hasRecorded = true
            _mediaRecorder!!.release()
            isRecording = false
            //Toast.makeText(pluginContext, "Record Stop", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playRecording(view: View?) {
        if (hasRecorded) {
            _mediaPlayer = MediaPlayer()
            _mediaPlayer!!.setDataSource(_filePath!!.path)
            _mediaPlayer!!.prepare()
            _mediaPlayer!!.start()
        }
    }

    @Subscribe
    internal fun onWickrAPIEvent(event: Any) {
        if (event is RequestDeleteFileEvent) {
            cleanupTemp()
            WickrMapComponent.EVENTBUS.post((PopFragmentEvent()))
        }
    }
}
