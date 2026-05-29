package com.miui.dynamicisland.data.repository

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

data class CallHistoryItem(
    val number: String,
    val name: String?,
    val type: Int, // Incoming, Outgoing, Missed
    val timestamp: Long,
    val duration: String?
)

data class ContactItem(
    val id: String,
    val name: String,
    val number: String,
    val photoUri: String?
)

class PhoneRepository(private val context: Context) {

    fun getCallHistory(): List<CallHistoryItem> {
        val history = mutableListOf<CallHistoryItem>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return history
        }

        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            CallLog.Calls.DATE + " DESC"
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)

            while (it.moveToNext()) {
                history.add(
                    CallHistoryItem(
                        number = it.getString(numberIndex),
                        name = it.getString(nameIndex),
                        type = it.getInt(typeIndex),
                        timestamp = it.getLong(dateIndex),
                        duration = it.getString(durationIndex)
                    )
                )
            }
        }
        return history.distinctBy { it.number + it.timestamp }.take(50)
    }

    fun getContacts(query: String = ""): List<ContactItem> {
        val contacts = mutableListOf<ContactItem>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return contacts
        }

        val selection = if (query.isNotEmpty()) {
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        } else null
        
        val selectionArgs = if (query.isNotEmpty()) {
            arrayOf("%$query%")
        } else null

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            selection,
            selectionArgs,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

            while (it.moveToNext()) {
                contacts.add(
                    ContactItem(
                        id = it.getString(idIndex),
                        name = it.getString(nameIndex),
                        number = it.getString(numberIndex),
                        photoUri = it.getString(photoIndex)
                    )
                )
            }
        }
        return contacts.distinctBy { it.number }
    }
}
