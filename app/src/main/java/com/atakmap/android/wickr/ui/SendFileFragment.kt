package com.atakmap.android.wickr.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.atakmap.android.gui.ImportFileBrowser
import com.atakmap.android.gui.ImportFileBrowserDialog
import com.atakmap.android.maps.MapView
import com.atakmap.android.util.FileProviderHelper
import com.atakmap.android.wickr.*
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.ui.util.FileUtils
import com.atakmap.android.wickr.utils.SettingsManager
import com.atakmap.coremap.filesystem.FileSystemUtils
import com.wickr.android.api.WickrAPIObjects
import kotlinx.android.synthetic.main.dialog_create_convo.*
import kotlinx.android.synthetic.main.file_actions.*
import kotlinx.android.synthetic.main.fragment_contacts.*
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.util.*
import kotlin.concurrent.timer

class SendFileFragment(
    private val pluginContext: Context,
    val wickrId: String = "",
    var convoId: String = "",
    val standAlone: Boolean = false
) : Fragment() {

    companion object {
        val SENDFILE_ID = "sendFileMessage"
    }

    val requests = Requests(pluginContext)
    var fullName: String? = ""
    var chosenFile: File? = null
    var chosenMimeType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(pluginContext).inflate(R.layout.file_actions, container, false)
    }

    @Subscribe
    internal fun onWickrAPIEvent(event: Any) {
        when (event) {
            is WickrConvoCreatedForFileEvent -> {
                convoId = event.convo?.id ?: ""
                chooseFile.isEnabled = convoId.isNotEmpty()
            }
            is FileChosenEvent -> {
                sendFile.isEnabled = true
                chosenFile = event.f
                chosenMimeType = event.mimeType
                fileNameText.text = event.f.name
                fileExtensionText.text = event.f.extension.uppercase()
                fileExtensionText.visibility = View.VISIBLE
                fileIcon.setOnClickListener {
                    val fileUtils = FileUtils()
                    fileUtils.externalOpen(
                        FileProviderHelper.fromFile(
                            MapView.getMapView().context,
                            chosenFile
                        ), chosenMimeType!!
                    )
                }
            }
        }
    }

    fun setRecipientTitle(fullname: String) {
        this.fullName = fullname
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val r = Requests(pluginContext)
        if (wickrId != null && (convoId == null || convoId.isEmpty())) {
            r.createConvo(wickrId, SENDFILE_ID)
        }

        chooseFile.isEnabled = convoId.isNotEmpty()
        chooseFile.setOnClickListener {
            val wickrSendFileAction = WickrSendFileAction(pluginContext, wickrId, convoId)

            ImportFileBrowserDialog.show(
                pluginContext.getString(R.string.video_browser),
                null,
                wickrSendFileAction,
                MapView.getMapView().context
            )
        }
        if (fullName != null) {
            recipientTitle.visibility = View.VISIBLE
            recipientTitle.text = fullName
        }

        sendFile.isEnabled = false
        sendFile.setOnClickListener {
            val r = Requests(pluginContext)
            if (convoId != null && chosenFile != null && chosenMimeType != null) {
                r.sendFile(
                    convoId!!,
                    chosenFile!!,
                    FileSystemUtils.getFileSize(chosenFile!!),
                    chosenMimeType!!
                )

                if (standAlone) {
                    WickrMapComponent.EVENTBUS.post(RequestCloseDropDownEvent())
                } else {
                    WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
                }
            }
        }

        cancelSend.setOnClickListener {
            if (standAlone) {
                WickrMapComponent.EVENTBUS.post(RequestCloseDropDownEvent())
            } else {
                WickrMapComponent.EVENTBUS.post(PopFragmentEvent())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        WickrMapComponent.EVENTBUS.register(this)
    }

    override fun onStop() {
        super.onStop()
        WickrMapComponent.EVENTBUS.unregister(this)
    }
}

class WickrSendFileAction(
    private val pluginContext: Context,
    private val wickrId: String,
    private val convoId: String?
) : ImportFileBrowserDialog.DialogDismissed {

    var fileUri: String = ""

    override fun onFileSelected(f: File?) {
        if (FileSystemUtils.isFile(f)) {
            fileUri = f?.toURI().toString()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f?.extension)
            if (convoId != null && convoId.isNotEmpty() && f != null) {
                WickrMapComponent.EVENTBUS.post(FileChosenEvent(convoId, f!!, mimeType ?: ""))
            }
        }
    }

    override fun onDialogClosed() {
    }


}