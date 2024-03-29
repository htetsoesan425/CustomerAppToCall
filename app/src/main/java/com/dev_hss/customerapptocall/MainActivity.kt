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
import com.dev_hss.customerapptocall.audiocall.WebRTCManager
import com.dev_hss.customerapptocall.databinding.ActivityMainBinding
import com.dev_hss.customerapptocall.socket.SocketHandler
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.permissionx.guolindev.PermissionX
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private val accessToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY1ODExZjNiNWE5OGE2M2U0NWFhMTNmMiIsIm5hbWUiOiJIdGV0IFNhbiIsImVtYWlsIjoiIiwidXNlciI6Ijk1OTk4NDQ1ODk2OSIsInJ0b2tlbiI6Ik1CbC9BTWhuRURCNzBubC84U2xVVmZmaytJd256WnFKcEtiMGNZOEsyaTIvb0FvNEtXNGI5citGaW91dDFjVnpPbnQ1V3F2V0lldTh3cVNYIiwiaWF0IjoxNzAyOTYwOTk2LCJleHAiOjE3MzQwNjQ5OTZ9.xf_m8zuoPAOsPlin9kng_C8RfiV1r7JdhLZf1weDuWU" //customer
    private val riderId = "64e6c8004aab3aca24c77b8a"
    private val customerId = "65811f3b5a98a63e45aa13f2"

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mSocket: Socket
    private lateinit var client: WebRTCManager
    private var fcmToken: String = ""


    private val callMade = Emitter.Listener { args ->
        runOnUiThread {
            try {
                val data = args[0] as JSONObject
                Log.d(TAG, "callMade:$data")
                setCallLayoutVisible()
                client.initializeSurfaceView(mBinding.localView)
                client.initializeSurfaceView(mBinding.remoteView)
                client.startLocalVideo(mBinding.localView)
                client.call(data)

            } catch (e: JSONException) {
                Log.d(TAG, "CallMadeErr-${e.message}")
            }
        }
    }

    private val answerMade = Emitter.Listener { args ->
        runOnUiThread {
            try {
                val data = args[0] as JSONObject
                Log.d(TAG, "answerMade:$data")
                client.receive(data)

            } catch (e: JSONException) {
                Log.d(TAG, "answerMadeErr-${e.message}")

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
            } catch (e: JSONException) {
                Log.d(TAG, "answerReceivedErr-${e.message}")
                //mBinding.tv2.text = e.message
            }
        }
    }


    private val icCandidate = Emitter.Listener { args ->
        runOnUiThread {
            try {
                val receivingCandidate = args[0] as JSONObject
                Log.d(TAG, "icCandidateJson:$receivingCandidate")
                //client.addIceCandidate()

//            val receivingCandidate = gson.fromJson(gson.toJson(message.data),
//                IceCandidateModel::class.java)
//            rtcClient?.addIceCandidate(IceCandidate(receivingCandidate.sdpMid,
//                Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),receivingCandidate.sdpCandidate))
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



        mBinding.endCallButton.setOnClickListener {
            client.endCall()
        }


        PermissionX.init(this)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            ).request { allGranted, _, _ ->
                if (allGranted) {
                    listenSocket()

                    client = WebRTCManager(application, object : CustomPeerConnectionObserver() {

                        override fun onIceCandidate(iceCandidate: IceCandidate) {
                            super.onIceCandidate(iceCandidate)
                            client.addIceCandidate(iceCandidate)
                            val candidateJson = JSONObject().apply {
                                put("sdpMid", iceCandidate.sdpMid)
                                put("sdpMLineIndex", iceCandidate.sdpMLineIndex)
                                put("candidate", iceCandidate.sdp)
                            }
                            mSocket.emit("iceCandidate", candidateJson)
                            // Send ICE candidate to the other peer


                        }

                        override fun onAddStream(p0: MediaStream) {
                            super.onAddStream(p0)
                            p0.videoTracks?.get(0)?.addSink(mBinding.remoteView)
                            Log.d(TAG, "onAddStream: $p0")

                        }
                    }, mSocket)
                } else {
                    Toast.makeText(this, "you should accept all permissions", Toast.LENGTH_LONG)
                        .show()
                }
            }
    }

    private fun listenSocket() {
        mSocket.on("call-made", callMade) //listen from rider
        mSocket.on("answer-made", answerMade)
        mSocket.on("answer-response", answerReceived)
        mSocket.on("ice-candidate-response", icCandidate)
    }


    private fun connectSocket() {
        SocketHandler.setSocket(accessToken)
        SocketHandler.establishConnection()
        mSocket = SocketHandler.getSocket()
        mSocket.emit("joinCustomer", customerId)

    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket.close()
    }

    private fun setCallLayoutVisible() {
        mBinding.callLayout.visibility = View.VISIBLE
    }
}