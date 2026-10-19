package com.kane.luno.utils

import android.content.Context
import android.provider.ContactsContract

data class DeviceContact(
    val name: String,
    val phoneNumber: String
)

object ContactsHelper {

    fun getDeviceContacts(context: Context): List<DeviceContact> {
        val contacts = mutableListOf<DeviceContact>()
        val resolver = context.contentResolver

        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "" else ""
                val rawNumber = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""
                val cleanNumber = normalizePhoneNumber(rawNumber)

                if (cleanNumber.isNotBlank()) {
                    contacts.add(DeviceContact(name, cleanNumber))
                }
            }
        }

        return contacts.distinctBy { it.phoneNumber }
    }

    fun normalizePhoneNumber(phone: String): String {
        var clean =
            phone.replace("\\s+".toRegex(), "").replace("-", "").replace("(", "").replace(")", "")
        if (clean.startsWith("0")) {
            clean = "+84" + clean.substring(1)
        }
        return clean
    }
}
