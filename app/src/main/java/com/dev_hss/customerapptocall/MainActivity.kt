package com.dev_hss.customerapptocall

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dev_hss.customerapptocall.audiocall.CustomPeerConnectionObserver
import com.dev_hss.customerapptocall.audiocall.RTCAudioManager
import com.dev_hss.customerapptocall.audiocall.WebRTCManager
import com.dev_hss.customerapptocall.databinding.ActivityMainBinding
import com.dev_hss.customerapptocall.socket.SocketHandler
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.permissionx.guolindev.PermissionX
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var mData: JSONObject
    private val accessToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY1ODExZjNiNWE5OGE2M2U0NWFhMTNmMiIsIm5hbWUiOiJIdGV0IFNhbiIsImVtYWlsIjoiIiwidXNlciI6Ijk1OTk4NDQ1ODk2OSIsInJ0b2tlbiI6Ik1CbC9BTWhuRURCNzBubC84U2xVVmZmaytJd256WnFKcEtiMGNZOEsyaTIvb0FvNEtXNGI5citGaW91dDFjVnpPbnQ1V3F2V0lldTh3cVNYIiwiaWF0IjoxNzAyOTYwOTk2LCJleHAiOjE3MzQwNjQ5OTZ9.xf_m8zuoPAOsPlin9kng_C8RfiV1r7JdhLZf1weDuWU" //customer
    private val riderId = "64e6c8004aab3aca24c77b8a"
    private val customerId = "65811f3b5a98a63e45aa13f2"

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var client: WebRTCManager
    private var fcmToken: String = ""
    private var isMute = false
    private var isCameraPause = false
    private val rtcAudioManager by lazy { RTCAudioManager.create(this) }
    private var isSpeakerMode = true
    private var callStartTime: Long = 0
    private var callDurationTimer: Timer? = null


    private val answerMade = Emitter.Listener { args ->
        runOnUiThread {
            try {
                val data = args[0] as JSONObject
                val offer = client.parseSdpOffer(data)
                setCallLayoutVisible()
                setWhoToCallLayoutGone()
                client.startLocalAudio()
                client.onRemoteSessionReceived(offer)
                client.answer(data)
                startCallDurationTimer()

            } catch (e: JSONException) {
                Log.d(TAG, "${e.message}")
            }
        }
    }


    private val answerReceived = Emitter.Listener { args ->
        runOnUiThread {
            try {
                val data = args[0] as JSONObject
                Log.d(TAG, "answerReceived:$data")
                val session = SessionDescription(
                    SessionDescription.Type.ANSWER, data.getString("sdp")
                )
                client.onRemoteSessionReceived(session)
                startCallDurationTimer()
                setWhoToCallLayoutGone()

            } catch (e: JSONException) {
                Log.d(TAG, "answerReceivedErr-${e.message}")
                //mBinding.tv2.text = e.message
            }
        }
    }


    private val icCandidate = Emitter.Listener { args ->
        val data1 = args[0] as JSONObject
        Log.d(TAG, "icCandidate:$data1")
        try {
            val data = args[0] as JSONObject
            val receivingCandidate = data.getJSONObject("iceCandidate")
            Log.d(TAG, "icCandidateJson:$receivingCandidate")
            client.addIceCandidate(
                IceCandidate(
                    receivingCandidate.getString("sdpMid"),
                    Math.toIntExact(receivingCandidate.getString("sdpMLineIndex").toLong()),
                    receivingCandidate.getString("sdpCandidate")
                )
            )
        } catch (e: JSONException) {
            Log.d(TAG, "${e.message}")
        }
    }

    private val onEndCall = Emitter.Listener { args ->
        runOnUiThread {
            val data1 = args[0] as JSONObject
            Log.d(TAG, "onEndCall:$data1")
            try {
                val data = args[0] as JSONObject
                setCallLayoutGone()
                setWhoToCallLayoutVisible()
                setIncomingCallLayoutGone()
                stopCallDurationTimer()
                client.endCall()
                finish()
            } catch (e: JSONException) {
                Log.d(TAG, "${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        FirebaseApp.initializeApp(this)

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            fcmToken = task.result

            // Log and toast
            //val msg = getString(R.string, token)
            Log.d(TAG, "fcm token: $fcmToken")
        }).addOnFailureListener {
            Log.d(TAG, "failure: $it")
        }


        connectSocket()
        listenSocket()

        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)

        mBinding.callBtn.setOnClickListener {
            PermissionX.init(this).permissions(
                Manifest.permission.RECORD_AUDIO
            ).request { allGranted, _, _ ->
                if (allGranted) {
                    val data = JSONObject()
                    data.put("to", riderId)
                    data.put("from", customerId)
                    data.put("type", "offer")
                    client.sendSdpToRemotePeer(null, "call-other", data)
                } else {
                    Toast.makeText(this, "you should accept all permissions", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

        mBinding.acceptButton.setOnClickListener {
            setIncomingCallLayoutGone()
            setCallLayoutVisible()
            setWhoToCallLayoutGone()
            client.startLocalAudio()
            client.call(mData)
        }

        mBinding.micButton.setOnClickListener {
            if (isMute) {
                isMute = false
                mBinding.micButton.setImageResource(R.drawable.ic_baseline_mic_off_24)
            } else {
                isMute = true
                mBinding.micButton.setImageResource(R.drawable.ic_baseline_mic_24)
            }
            client.toggleAudio(isMute)
        }

        mBinding.audioOutputButton.setOnClickListener {
            if (isSpeakerMode) {
                isSpeakerMode = false
                mBinding.audioOutputButton.setImageResource(R.drawable.ic_baseline_hearing_24)
                rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
            } else {
                isSpeakerMode = true
                mBinding.audioOutputButton.setImageResource(R.drawable.ic_baseline_speaker_up_24)
                rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
            }

        }

        mBinding.endCallButton.setOnClickListener {
            val data = JSONObject()
            data.put("from", customerId)
            data.put("to", riderId)
            SocketHandler.emit("call-end", data)

            setCallLayoutGone()
            setWhoToCallLayoutVisible()
            setIncomingCallLayoutGone()
            stopCallDurationTimer()
            client.endCall()
            finish()
        }
    }

    private fun listenSocket() {
        SocketHandler.observeEventData("call-made") { data ->
            runOnUiThread {
                try {
                    Log.d(TAG, "callMade:$data")
                    setIncomingCallLayoutVisible()
                    setWhoToCallLayoutGone()
                    mData = data

                } catch (e: JSONException) {
                    Log.d(TAG, "CallMadeErr-${e.message}")
                }
            }
        } //listen from rider
        SocketHandler.observeEventData("answer-made") { data ->
            runOnUiThread {
                try {
                    val offer = client.parseSdpOffer(data)
                    setCallLayoutVisible()
                    setWhoToCallLayoutGone()
                    client.startLocalAudio()
                    client.onRemoteSessionReceived(offer)
                    client.answer(data)
                    startCallDurationTimer()

                } catch (e: JSONException) {
                    Log.d(TAG, "${e.message}")
                }
            }
        }
        SocketHandler.observeEventData("answer-response") { data ->
            runOnUiThread {
                try {
                    Log.d(TAG, "answerReceived:$data")
                    val session = SessionDescription(
                        SessionDescription.Type.ANSWER, data.getString("sdp")
                    )
                    client.onRemoteSessionReceived(session)
                    startCallDurationTimer()
                    setWhoToCallLayoutGone()

                } catch (e: JSONException) {
                    Log.d(TAG, "answerReceivedErr-${e.message}")
                    //mBinding.tv2.text = e.message
                }
            }
        }
        SocketHandler.observeEventData("ice-candidate-response") { data ->
            try {
                val receivingCandidate = data.getJSONObject("iceCandidate")
                Log.d(TAG, "icCandidateJson:$receivingCandidate")
                client.addIceCandidate(
                    IceCandidate(
                        receivingCandidate.getString("sdpMid"),
                        Math.toIntExact(receivingCandidate.getString("sdpMLineIndex").toLong()),
                        receivingCandidate.getString("sdpCandidate")
                    )
                )
            } catch (e: JSONException) {
                Log.d(TAG, "${e.message}")
            }
        }
        SocketHandler.observeEventData("call-end-response") {
            runOnUiThread {
                try {
                    setCallLayoutGone()
                    setWhoToCallLayoutVisible()
                    setIncomingCallLayoutGone()
                    stopCallDurationTimer()
                    client.endCall()
                    finish()
                } catch (e: JSONException) {
                    Log.d(TAG, "${e.message}")
                }
            }
        }
    }


    private fun connectSocket() {
        SocketHandler.setSocket(accessToken)
        SocketHandler.establishConnection()
        SocketHandler.emit("joinCustomer", customerId)

    }

    override fun onDestroy() {
        super.onDestroy()
        SocketHandler.closeConnection()
    }

    private fun setCallLayoutVisible() {
        mBinding.callLayout.visibility = View.VISIBLE
    }

    private fun setIncomingCallLayoutVisible() {
        mBinding.incomingCallLayout.visibility = View.VISIBLE
    }

    private fun setCallLayoutGone() {
        mBinding.callLayout.visibility = View.GONE
    }


    private fun setWhoToCallLayoutGone() {
        mBinding.whoToCallLayout.visibility = View.GONE
    }

    private fun setWhoToCallLayoutVisible() {
        mBinding.whoToCallLayout.visibility = View.VISIBLE
    }

    private fun setIncomingCallLayoutGone() {
        mBinding.incomingCallLayout.visibility = View.GONE
    }

    private fun startCallDurationTimer() {
        callStartTime = System.currentTimeMillis()
        callDurationTimer = Timer()
        callDurationTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - callStartTime
                val minutes = elapsedTime / (1000 * 60)
                val seconds = (elapsedTime / 1000) % 60
                runOnUiThread {
                    mBinding.callDurationTextView.text =
                        String.format("%02d:%02d", minutes, seconds)
                }
            }
        }, 0, 1000)
    }

    private fun stopCallDurationTimer() {
        callDurationTimer?.cancel()
        callDurationTimer = null
        mBinding.callDurationTextView.text = "00:00"
    }

    override fun onResume() {
        super.onResume()
        PermissionX.init(this).permissions(
            Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA
        ).request { allGranted, _, _ ->
            if (allGranted) {

                client = WebRTCManager(application, object : CustomPeerConnectionObserver() {

                    override fun onIceCandidate(iceCandidate: IceCandidate) {
                        super.onIceCandidate(iceCandidate)
                        client.addIceCandidate(iceCandidate)
                        val candidateJson = JSONObject().apply {
                            put("sdpMid", iceCandidate.sdpMid)
                            put("sdpMLineIndex", iceCandidate.sdpMLineIndex)
                            put("sdpCandidate", iceCandidate.sdp)
                        }
                        val data = JSONObject()
                        data.put("from", customerId)
                        data.put("to", riderId)
                        data.put("iceCandidate", candidateJson)
                        SocketHandler.emit("gather-ice-candidate", data)

                        // Send ICE candidate to the other peer


                    }

                    override fun onAddStream(p0: MediaStream) {
                        super.onAddStream(p0)
                        val remoteAudioTrack = p0.audioTracks.firstOrNull()
                        remoteAudioTrack?.setEnabled(true) // Ensure audio is enabled
                    }

                })
            } else {
                Toast.makeText(this, "you should accept all permissions", Toast.LENGTH_LONG).show()
            }
        }
    }
}