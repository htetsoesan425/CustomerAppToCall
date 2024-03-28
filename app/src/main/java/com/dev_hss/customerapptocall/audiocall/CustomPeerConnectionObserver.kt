package com.dev_hss.customerapptocall.audiocall

import android.util.Log
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver

open class CustomPeerConnectionObserver() : PeerConnection.Observer {

    override fun onIceCandidate(iceCandidate: IceCandidate) {
        Log.d(TAG, "onIceCandidate: $iceCandidate")

    }

    override fun onDataChannel(dataChannel: DataChannel) {
        Log.d(TAG, "onDataChannel: $dataChannel")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Log.d(TAG, "onIceConnectionReceivingChange: $p0")

    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState) {
        Log.d(TAG, "onIceConnectionChange: $p0")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState) {
        Log.d(TAG, "onIceGatheringChange: $p0")

    }

    override fun onAddStream(p0: MediaStream) {
        Log.d(TAG, "onAddStream: $p0")

    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState) {
        Log.d(TAG, "onSignalingChange: $p0")

    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>) {
        Log.d(TAG, "onIceCandidatesRemoved: $p0")

    }

    override fun onRemoveStream(p0: MediaStream) {
        Log.d(TAG, "onRemoveStream: $p0")
    }

    override fun onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded: ")
    }

    override fun onAddTrack(p0: RtpReceiver, p1: Array<out MediaStream>) {}

    companion object {
        const val TAG = "CustomPeerConnectionObserver"
    }
}

