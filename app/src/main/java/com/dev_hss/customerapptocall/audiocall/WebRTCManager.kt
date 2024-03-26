package com.dev_hss.customerapptocall.audiocall

import android.content.Context
import android.util.Log
import com.dev_hss.customerapptocall.MainActivity
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
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

class WebRTCManager(private val context: Context, param: CustomPeerConnectionObserver) {

    companion object {
        const val TAG = "WebRTCManager"
    }

    private lateinit var mData: JSONObject
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var localPeer: PeerConnection? = null
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
            object : CustomPeerConnectionObserver() {})
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


    fun call(data: JSONObject) {
        val mediaConstraints = MediaConstraints()
        //mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        localPeer?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                localPeer?.setLocalDescription(
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
                            sendSdpToRemotePeer(sdp, "make-answer", data)
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

        localPeer?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(answer: SessionDescription) {
                localPeer?.setLocalDescription(object : SdpObserver {
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
        localPeer?.addIceCandidate(p0)
    }

    fun onRemoteSessionReceived(sdp: SessionDescription) {
        Log.d(TAG, "Answer received: $sdp")

        localPeer?.setRemoteDescription(object : SdpObserver {
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


