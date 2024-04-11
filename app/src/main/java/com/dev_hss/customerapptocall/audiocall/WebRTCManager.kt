package com.dev_hss.customerapptocall.audiocall

import android.app.Application
import android.util.Log
import com.dev_hss.customerapptocall.socket.SocketHandler
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class WebRTCManager(
    private val application: Application,
    private val customPeerConnectionObserver: CustomPeerConnectionObserver
) {
    init {
        initPeerConnectionFactory(application)
    }

    companion object {
        const val TAG = "WebRTCManager"
    }

    private val eglContext = EglBase.create()
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )
    private val peerConnection by lazy { createPeerConnection(customPeerConnectionObserver) }

    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }


    private fun initPeerConnectionFactory(application: Application) {
        val options = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder().setVideoEncoderFactory(
            DefaultVideoEncoderFactory(
                eglContext.eglBaseContext, true, true
            )
        ).setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            }).createPeerConnectionFactory()
    }

    private fun createPeerConnection(customPeerConnectionObserver: CustomPeerConnectionObserver): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(iceServer, customPeerConnectionObserver)
    }

    private fun createPeerConnectionIfNeeded() {
        // Just accessing the lazy property will create the peer connection if it's null
        peerConnection
    }

    private fun reopenPeerConnection() {
        // Close the existing peer connection if it exists
        endCall()
        // Create a new peer connection by accessing the lazy property
        createPeerConnectionIfNeeded()
    }

    fun call(data: JSONObject) {
        //reopenPeerConnection()
        val mediaConstraints = MediaConstraints()
        // Modify constraints for audio-only call
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mediaConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "false"
            )
        )
        Log.d(TAG, "call: ${peerConnection?.signalingState()}")
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(
                    object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {
                            Log.d(TAG, "onCreateSuccess: #")

                        }

                        override fun onSetSuccess() {
                            val from = data.getString("from")
                            val to = data.getString("to")
                            val offerJson = JSONObject()
                            offerJson.put("to", from) //rider
                            offerJson.put("from", to)
                            offerJson.put("type", "offer")

                            sendSdpToRemotePeer(sdp, "make-answer", offerJson)
                        }

                        override fun onCreateFailure(p0: String?) {
                            Log.d(TAG, "onCreateFailure: $p0")

                        }

                        override fun onSetFailure(p0: String?) {
                            Log.d(TAG, "onSetFailure: $p0")
                        }

                    }, sdp
                )

            }

            override fun onSetSuccess() {
                Log.d(TAG, "onSetSuccess: #")
            }

            override fun onCreateFailure(p0: String?) {
                Log.d(TAG, "onCreateFailure: $p0")
            }

            override fun onSetFailure(p0: String?) {
                Log.d(TAG, "onSetFailure: $p0")

            }
        }, mediaConstraints)
    }

    // Your WebRTCManager class should have a method like this to send SDP to the remote peer
    fun sendSdpToRemotePeer(
        sessionDescription: SessionDescription? = null, eventName: String, data: JSONObject
    ) {
        // Implement the logic to send the SDP to the remote peer using your signaling mechanism
        data.put("sdp", sessionDescription?.description)
        Log.d(TAG, "emit: eventName= $eventName data= $data")
        SocketHandler.emit(eventName, data)
    }

    fun parseSdpOffer(sdpOfferJson: JSONObject): SessionDescription {
        val sdpTypeString = sdpOfferJson.getString("type")
        val sdpDescription = sdpOfferJson.getString("sdp")
        val sdpType = when (sdpTypeString) {
            "offer" -> SessionDescription.Type.OFFER
            "answer" -> SessionDescription.Type.ANSWER
            else -> throw IllegalArgumentException("Invalid SDP type: $sdpTypeString")
        }
        return SessionDescription(sdpType, sdpDescription)
    }


    fun answer(offerJson: JSONObject) {
        //reopenPeerConnection()
        val offer = parseSdpOffer(offerJson)
        val from = offerJson.getString("from")
        val to = offerJson.getString("to")

        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))

        Log.d(TAG, "answer:State ${peerConnection?.signalingState()}")

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(answer: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                    }

                    override fun onSetSuccess() {

                        val data = JSONObject()
                        data.put("to", from)
                        data.put("from", to)
                        data.put("type", "answer")
                        //client.startAudioCall(data)
                        sendSdpToRemotePeer(answer, "answer-success", data)
                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.d(TAG, "onCreateFailure:setLocalDescription $p0")

                    }

                    override fun onSetFailure(p0: String?) {
                        Log.d(TAG, "onSetFailure:setLocalDescription $p0")
                    }

                }, answer)
            }

            override fun onSetSuccess() {
                Log.d(TAG, "onSetSuccess:createAnswer Success")

            }

            override fun onCreateFailure(p0: String?) {
                Log.d(TAG, "onCreateFailure:createAnswer $p0")
            }

            override fun onSetFailure(p0: String?) {
                Log.d(TAG, "onSetFailure:createAnswer $p0")
            }

        }, constraints)
    }

    fun addIceCandidate(p0: IceCandidate?) {
        peerConnection?.addIceCandidate(p0)

    }

    fun onRemoteSessionReceived(sdp: SessionDescription) {
        Log.d(TAG, "Answer received: ${sdp.type}")
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                Log.d(TAG, "Remote description set successfully: $sdp")

            }

            override fun onSetSuccess() {
                Log.d(TAG, "Remote description successfully applied.")
                // Proceed with the call setup
            }

            override fun onCreateFailure(p0: String?) {
                Log.d(TAG, "create fail: $p0")

            }

            override fun onSetFailure(p0: String?) {
                Log.d(TAG, "set fail: $p0")

            }

            // Handle other callbacks as needed
        }, sdp)

    }


//    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
//        return peerConnectionFactory.createPeerConnection(iceServer, observer)
//    }

    fun initializeSurfaceView(surface: SurfaceViewRenderer) {
        surface.run {
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglContext.eglBaseContext, null)
        }
    }

    fun startLocalVideo(surface: SurfaceViewRenderer) {
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, eglContext.eglBaseContext)
        videoCapturer = getVideoCapturer(application)
        videoCapturer?.initialize(
            surfaceTextureHelper, surface.context, localVideoSource.capturerObserver
        )
        videoCapturer?.startCapture(320, 240, 30)
        localVideoTrack = peerConnectionFactory.createVideoTrack("local_track", localVideoSource)
        localVideoTrack?.addSink(surface)
        localAudioTrack =
            peerConnectionFactory.createAudioTrack("local_track_audio", localAudioSource)
        val localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        localStream.addTrack(localAudioTrack)
        localStream.addTrack(localVideoTrack)

        peerConnection?.addStream(localStream)
    }

    private fun getVideoCapturer(application: Application): CameraVideoCapturer {
        return Camera2Enumerator(application).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }
    }

    fun startLocalAudio() {
        // Start capturing and sending local audio
        val localStream = peerConnectionFactory.createLocalMediaStream("local_audio_stream")
        localAudioTrack =
            peerConnectionFactory.createAudioTrack("local_audio_track", localAudioSource)
        localStream.addTrack(localAudioTrack)

        // Add the local audio stream to the PeerConnection
        peerConnection?.addStream(localStream)

    }

    fun endCall() {
        peerConnection?.close()
    }

    fun toggleAudio(mute: Boolean) {
        localAudioTrack?.setEnabled(mute)
    }
}


