package com.dev_hss.customerapptocall.audiocall

import android.app.Application
import android.util.Log
import com.dev_hss.customerapptocall.MainActivity
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class WebRTCManager(
    application: Application,
    private val customPeerConnectionObserver: CustomPeerConnectionObserver,
    private val socket: Socket
) {

    companion object {
        const val TAG = "WebRTCManager"
    }

    private val eglContext = EglBase.create()
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )
    private val peerConnection by lazy { createPeerConnection(customPeerConnectionObserver) }
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private val callMade = Emitter.Listener { args ->
        val test = args[0] as JSONObject
        Log.d(MainActivity.TAG, "callMade:$test")
        try {
            val data = args[0] as JSONObject
            Log.d(MainActivity.TAG, "callMade:$data")
            //client.
            //mBinding.tv1.text = data.toString()


        } catch (e: JSONException) {
            Log.d(MainActivity.TAG, "CallMadeErr-${e.message}")
            //mBinding.tv1.text = e.message

        }
    }

    init {
        initPeerConnectionFactory(application)
    }

    private fun initPeerConnectionFactory(application: Application) {
        val options = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglContext.eglBaseContext,
                    true,
                    true
                )
            )
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            }).createPeerConnectionFactory()
    }

    private fun createPeerConnection(customPeerConnectionObserver: CustomPeerConnectionObserver): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(iceServer, customPeerConnectionObserver)
    }

    fun call(data: JSONObject) {
        val mediaConstraints = MediaConstraints()
        //mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(
                    object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {

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
                        }

                        override fun onSetFailure(p0: String?) {
                        }

                    }, sdp
                )

            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }
        }, mediaConstraints)
    }

    // Your WebRTCManager class should have a method like this to send SDP to the remote peer
    fun sendSdpToRemotePeer(
        sessionDescription: SessionDescription, eventName: String, data: JSONObject
    ) {
        // Implement the logic to send the SDP to the remote peer using your signaling mechanism
        data.put("sdp", sessionDescription.description)
        Log.d(TAG, "emit: eventName= $eventName description= ${sessionDescription.description}")
        Log.d(TAG, "emit: eventName= $eventName type= ${sessionDescription.type}")
        socket.emit(eventName, data)
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


    fun receive(offerJson: JSONObject) {

        val offer = parseSdpOffer(offerJson)
        val from = offerJson.getString("from")
        val to = offerJson.getString("to")

        val constraints = MediaConstraints()
        //constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

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
                    }

                    override fun onSetFailure(p0: String?) {
                    }

                }, offer)
            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }

        }, constraints)
    }

    fun addIceCandidate(p0: IceCandidate?) {
        peerConnection?.addIceCandidate(p0)
    }

    fun onRemoteSessionReceived(sdp: SessionDescription) {
        Log.d(TAG, "Answer received: $sdp")

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

}


