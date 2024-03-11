package com.dev_hss.customerapptocall

import android.content.Context
import android.content.res.Resources
import android.location.Geocoder
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val SOCKET_URL = "http://174.138.19.57:5000"

fun getCompleteAddressString(context: Context, LATITUDE: Double, LONGITUDE: Double): String {
    var strAdd = ""
    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        val addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1)
        if (addresses != null) {
            val returnedAddress = addresses[0]
            val strReturnedAddress = StringBuilder()
            for (i in 0..returnedAddress.maxAddressLineIndex) {
                strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n")
            }
            strAdd = strReturnedAddress.toString()
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return strAdd
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager =
        getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Fragment.hideKeyboard() {
    view?.let {
        activity?.hideKeyboard(it)
    }
}

fun Fragment.setupActionBar(
    title: String,
    displayHome: Boolean = false,
    hasOptionsMenu: Boolean = false
) {
    setHasOptionsMenu(hasOptionsMenu)
    (activity as? AppCompatActivity)?.invalidateOptionsMenu()
    (activity as? AppCompatActivity)?.supportActionBar?.apply {
        this.title = title
        setDisplayShowHomeEnabled(displayHome)
        setDisplayHomeAsUpEnabled(displayHome)
        this.show()
    }
}

fun Date.toString(format: String): String {
    val formatter = SimpleDateFormat(format, Locale.US)
    return formatter.format(this)
}

fun toDate(dateString: String): Date? {
    val dateFormat = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)")

    // Set the time zone to Myanmar Time
    dateFormat.timeZone = TimeZone.getTimeZone("GMT+0630")

    return try {
        val date = dateFormat.parse(dateString)
        println(date) // This will print the parsed date
        date

    } catch (e: Exception) {
        println("Invalid date format")
        null
    }
}

fun thousandSeparator(n: Int): String =
    DecimalFormat("#,###", DecimalFormatSymbols(Locale.US)).format(n)

fun getTimeFromTimestamp(timestamp: String): String {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    inputFormat.timeZone = TimeZone.getTimeZone("Asia/Yangon")
    val outputFormat = SimpleDateFormat("hh:mm a", Locale.US)
    val date = inputFormat.parse(timestamp)
    return outputFormat.format(date as Date)
}

fun getDateTimeFromTimestamp(timestamp: String): String {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    inputFormat.timeZone = TimeZone.getTimeZone("Asia/Yangon")
    val outputFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
    val date = inputFormat.parse(timestamp)
    return outputFormat.format(date as Date)
}

fun formatDateToISOString(calendar: Calendar): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
    sdf.timeZone =
        TimeZone.getTimeZone("Asia/Yangon")
    return sdf.format(calendar.time)
}

fun getApiLevel(): Int {
    return Build.VERSION.SDK_INT
}

fun marginInDpToPx(marginInDp: Int): Int {
    val density = Resources.getSystem().displayMetrics.density
    return (marginInDp * density).toInt()
}


fun getMonthString(month: Int): String {
    val result = when (month) {
        0 -> "Jan"
        1 -> "Feb"
        2 -> "Mar"
        3 -> "Apr"
        4 -> "May"
        5 -> "Jun"
        6 -> "Jul"
        7 -> "Aug"
        8 -> "Sept"
        9 -> "Oct"
        10 -> "Nov"
        11 -> "Dec"
        else -> {
            "Apr"
        }
    }
    return result
}


fun append(value: String, appendText: String): String {
    return "$value $appendText"
}


fun pxFromDp(dp: Float, mContext: Context): Float {
    return dp * mContext.resources.displayMetrics.density
}

fun formatDateRange(startDate: Date, endDate: Date): String {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.US)
    val startFormatted = dateFormat.format(startDate)
    val endFormatted = dateFormat.format(endDate)
    return "$startFormatted - $endFormatted"
}

