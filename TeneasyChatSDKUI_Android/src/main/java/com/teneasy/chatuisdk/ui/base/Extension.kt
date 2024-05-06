package com.teneasy.chatuisdk.ui.base

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Extension {

    // Create an extension function on Context to check for permissions
    fun Context.hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    // Create an extension function on View to set the background color
    fun View.setBackgroundColorRes(@ColorRes colorRes: Int) {
        setBackgroundColor(ContextCompat.getColor(context, colorRes))
    }

    // Create an extension function on TextView to set the text color
    fun TextView.setTextColorRes(@ColorRes colorRes: Int) {
        setTextColor(ContextCompat.getColor(context, colorRes))
    }

    // Create an extension function on EditText to get the text as a string
    fun EditText.textString(): String {
        return text.toString()
    }

    // Create an extension function on RecyclerView to set a vertical layout manager
    fun RecyclerView.verticalLayoutManager() {
        layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    }

    // Create an extension function on RecyclerView to set a horizontal layout manager
    fun RecyclerView.horizontalLayoutManager() {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    }

    // Create an extension function on RecyclerView to set a grid layout manager
    fun RecyclerView.gridLayoutManager(spanCount: Int) {
        layoutManager = GridLayoutManager(context, spanCount)
    }

    // Create an extension function on Fragment to show a toast message
    fun Fragment.showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // Create an extension function on Activity to show a toast message
    fun Activity.showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}