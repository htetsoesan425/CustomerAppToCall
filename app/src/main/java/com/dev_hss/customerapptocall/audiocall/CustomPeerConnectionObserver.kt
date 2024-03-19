package com.dev_hss.customerapptocall.audiocall

import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver

open class CustomPeerConnectionObserver() : PeerConnection.Observer {

    override fun onIceCandidate(iceCandidate: IceCandidate) {}

    override fun onDataChannel(dataChannel: DataChannel) {}

    override fun onIceConnectionReceivingChange(p0: Boolean) {}

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState) {}

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState) {}

    override fun onAddStream(p0: MediaStream) {}

    override fun onSignalingChange(p0: PeerConnection.SignalingState) {}

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>) {}

    override fun onRemoveStream(p0: MediaStream) {}

    override fun onRenegotiationNeeded() {}

    override fun onAddTrack(p0: RtpReceiver, p1: Array<out MediaStream>) {}
}

