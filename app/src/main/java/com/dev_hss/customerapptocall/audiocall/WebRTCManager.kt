package com.dev_hss.customerapptocall.audiocall

import android.content.Context
import android.util.Log
import com.dev_hss.riderappfor360food.utils.audiocall.CustomPeerConnectionObserver
import com.dev_hss.riderappfor360food.utils.audiocall.CustomSdpObserver
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaSource
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class WebRTCManager(private val context: Context) {

    companion object {
        const val TAG = "WebRTCManager"
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var localPeer: PeerConnection? = null

    init {
        // Initialize PeerConnectionFactory
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()

        // Create audio source
        localAudioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audioTrack", localAudioSource)

        // Create PeerConnection
        localPeer = createPeerConnection()
    }

    private fun createPeerConnection(): PeerConnection? {
        // Implement your own PeerConnection configuration
        val configuration = PeerConnection.RTCConfiguration(
            arrayListOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        )
        val constraints = MediaConstraints()
        return peerConnectionFactory?.createPeerConnection(
            configuration,
            constraints,
            object : CustomPeerConnectionObserver("localPeerCreation") {})
    }

    interface IStateChangeListener {
        /**
         * Called when status of client is changed.
         */
        fun onStateChanged(state: MediaSource.State)
    }

    private lateinit var socket: Socket


    fun setSocket(socket: Socket) {
        this.socket = socket
    }

    fun startAudioCall(data: JSONObject) {

        // Your WebRTCManager class should have a method like this to send SDP to the remote peer
        fun sendSdpToRemotePeer(sessionDescription: SessionDescription) {
            // Implement the logic to send the SDP to the remote peer using your signaling mechanism
            Log.d("TAG", "sendSdpToRemotePeer: ${sessionDescription.description}")
            Log.d("TAG", "sendSdpToRemotePeer: ${sessionDescription.type}")
            data.put("sdp", sessionDescription)
            socket.emit("call-other", data)
        }

        // Assuming remotePeerConnection is an instance of your WebRTCManager class

        // Step 1: Create Offer
        localPeer?.createOffer(object : SdpObserver by CustomSdpObserver("createOffer") {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                // Step 2: Exchange SDP
                localPeer?.setLocalDescription(
                    object : CustomSdpObserver("setLocalDescription") {},
                    sessionDescription
                )
                // Send the offer SDP to the remote peer through your signaling mechanism
                sendSdpToRemotePeer(sessionDescription)
            }
        }, MediaConstraints())



        // Assuming you have a method to receive SDP from the remote peer
        // Your WebRTCManager class should have a method like this to receive SDP from the remote peer
        fun receiveSdpFromRemotePeer(sessionDescription: SessionDescription) {
            // Step 3: Create Answer
            localPeer?.setRemoteDescription(
                object : CustomSdpObserver("setRemoteDescription") {},
                sessionDescription
            )

            localPeer?.createAnswer(object : SdpObserver by CustomSdpObserver("createAnswer") {
                override fun onCreateSuccess(answer: SessionDescription) {
                    // Step 4: Exchange SDP
                    localPeer?.setLocalDescription(object :
                        CustomSdpObserver("setLocalDescription") {}, answer)
                    // Send the answer SDP to the remote peer through your signaling mechanism
                    sendSdpToRemotePeer(answer)
                    Log.d(TAG, "onCreateSuccess: $answer")
                }
            }, MediaConstraints())
        }

        // Assuming you have a method to receive ICE candidates from the remote peer
        // Your WebRTCManager class should have a method like this to receive ICE candidates from the remote peer
        fun receiveIceCandidateFromRemotePeer(iceCandidate: IceCandidate) {
            // Step 5: Exchange ICE Candidates
            localPeer?.addIceCandidate(iceCandidate)
        }

    }
}
