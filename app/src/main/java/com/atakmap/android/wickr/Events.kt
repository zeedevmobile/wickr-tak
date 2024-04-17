package com.atakmap.android.wickr

import com.wickr.android.api.WickrAPIObjects.*
import com.wickr.android.api.WickrAPIResponses.CreateConvoResponse.CreateError
import com.wickr.android.api.WickrAPIResponses.EditConvoResponse.EditError
import com.wickr.android.api.WickrAPIResponses.SendMessageResponse.SendError
import java.io.File

class WickrAPIPairedEvent
class WickrAPIUnpairedEvent
class WickrConvoListEvent(val identifier: String, val convos: List<WickrConvo>)
class WickrConvoUpdateEvent(val identifier: String, val convo: WickrConvo)
class WickrConvoEditEvent(val error: EditError? = null, val convo: WickrConvo)
class WickrMessageListEvent(val convoID: String, val messages: List<WickrMessage>)
class WickrMessageUpdateResponse(val convoID: String, val message: WickrMessage)
class WickrMessageSendEvent(val convoID: String, val error: SendError? = null, val message: WickrMessage? = null)
class WickrContactListEvent(val contacts: List<WickrUser>)
class WickrContactSearchListEvent(val contacts: List<WickrUser>)
class WickrConvoCreatedEvent(val error: CreateError? = null, val convo: WickrConvo? = null)
class WickrConvoCreatedForPhoneEvent(val error: CreateError? = null, val convo: WickrConvo? = null)
class WickrConvoCreatedForFileEvent(val error: CreateError? = null, val convo: WickrConvo? = null)
class WickrUserSettingsEvent(val userSettings: WickrUserSettings)
class WickrUserAvatarUpdateEvent(val userID: String)
class WickrSyncingEvent()
class WickrInvalidRequestEvent(val error: APIError)

// Used in place of intents
class MessageFragmentEvent(val convo: WickrConvo?)
class MainFragmentEvent()
class PopFragmentEvent()
class CreateConvoFragmentEvent(val checkedUsers: List<String>)
class RequestCloseDropDownEvent()
class RequestDeleteFileEvent()
class RefreshConvoFragmentEvent(val convoID: String, val message: WickrMessage)
class SendFileFragmentEvent(val userId: String, val fullname: String)
class SendFileInlineFragmentEvent(val userId: String, val convoId: String, val fullname: String)
class SendVoiceMessageFragmentEvent(val userId: String, val convoId: String, val fullname: String)
class FileChosenEvent(val convoId: String, val f: File, val mimeType: String)
class RoomOrGroupDetailsEvent(val convo: WickrConvo?)
class SelectContactsEvent(val newConvo: Boolean, val convo: WickrConvo? = null)
class AddUsersToConvoEvent(val userIdList: List<String>)