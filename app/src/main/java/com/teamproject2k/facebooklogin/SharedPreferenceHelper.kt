package com.teamproject2k.facebooklogin

import android.content.Context
import android.content.SharedPreferences

class SharedPreferenceHelper(context: Context) {
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_shared_pref", Context.MODE_PRIVATE)

    companion object {
        private const val USER_PROFILE_PICTURE = "user_profile_picture"
        private const val USER_FIRST_NAME = "user_first_ame"
        private const val USER_LAST_NAME = "user_last_name"
        private const val USER_EMAIL_ADDRESS = "user_email_address"
    }


    var userProfilePicture
        get() = sharedPreferences.getString(USER_PROFILE_PICTURE, "") ?: ""
        set(picUrl) {
            val editor = sharedPreferences.edit()
            editor.putString(USER_PROFILE_PICTURE, picUrl)
            editor.apply()
        }


    var userFirstName
        get() = sharedPreferences.getString(USER_FIRST_NAME, "") ?: ""
        set(firstName) {
            val editor = sharedPreferences.edit()
            editor.putString(USER_FIRST_NAME, firstName)
            editor.apply()
        }


    var userLastName
        get() = sharedPreferences.getString(USER_LAST_NAME, "") ?: ""
        set(mobileNumber) {
            val editor = sharedPreferences.edit()
            editor.putString(USER_LAST_NAME, mobileNumber)
            editor.apply()
        }

    var userEmailAddress
        get() = sharedPreferences.getString(USER_EMAIL_ADDRESS, "") ?: ""
        set(mobileNumber) {
            val editor = sharedPreferences.edit()
            editor.putString(USER_EMAIL_ADDRESS, mobileNumber)
            editor.apply()
        }

}