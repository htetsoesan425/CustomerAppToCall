package com.dev_hss.riderappfor360food.utils.audiocall

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class CustomSdpObserver(private val logTag: String) : SdpObserver {
    companion object {
        const val TAG = "CustomSdpObserver"
    }
    override fun onCreateSuccess(sessionDescription: SessionDescription) {
        // Implement your logic for a successful SDP creation
        Log.d(TAG, "onCreateSuccess: $sessionDescription")
    }

    override fun onSetSuccess() {
        // Implement your logic for a successful SDP setting
        Log.d(TAG, "onSetSuccess#")

    }

    override fun onCreateFailure(error: String) {
        // Implement your logic for a failed SDP creation
        Log.d(TAG, "onCreateFailure: $error")

    }

    override fun onSetFailure(error: String) {
        // Implement your logic for a failed SDP setting
        Log.d(TAG, "onSetFailure: $error")

    }
}
