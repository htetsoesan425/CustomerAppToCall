package com.dev_hss.customerapptocall

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dev_hss.customerapptocall.audiocall.WebRTCManager
import com.dev_hss.customerapptocall.databinding.ActivityMainBinding
import com.dev_hss.customerapptocall.socket.SocketHandler
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mSocket: Socket
    private lateinit var client: WebRTCManager
    private var fcmToken: String = ""


    private val callMade = Emitter.Listener { args ->
        val test = args[0] as JSONObject
        Log.d(TAG, "callMade:$test")
        try {
            val offerJson = args[0] as JSONObject
            Log.d(TAG, "callMade:$offerJson")
            client.receiveOffer(offerJson)

        } catch (e: JSONException) {
            Log.d(TAG, "CallMadeErr-${e.message}")
            //mBinding.tv1.text = e.message

        }
    }

    private val answerMade = Emitter.Listener { args ->
        val data1 = args[0] as JSONObject
        Log.d(TAG, "answerMade:$data1")
        try {
            val data = args[0] as JSONObject
            Log.d(TAG, "answerMade:$data")
            //mBinding.tv2.text = data.toString()
        } catch (e: JSONException) {
            Log.d(TAG, "answerMadeErr-${e.message}")
            //mBinding.tv2.text = e.message

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
        //val accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY1ODExZjNiNWE5OGE2M2U0NWFhMTNmMiIsIm5hbWUiOiJIdGV0IFNhbiIsImVtYWlsIjoiIiwidXNlciI6Ijk1OTk4NDQ1ODk2OSIsInJ0b2tlbiI6Ik1CbC9BTWhuRURCNzBubC84U2xVVmZmaytJd256WnFKcEtiMGNZOEsyaTIvb0FvNEtXNGI5citGaW91dDFjVnpPbnQ1V3F2V0lldTh3cVNYIiwiaWF0IjoxNzAyOTYwOTk2LCJleHAiOjE3MzQwNjQ5OTZ9.xf_m8zuoPAOsPlin9kng_C8RfiV1r7JdhLZf1weDuWU" //customer
//        SocketHandler.setSocket("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY1ODExZjNiNWE5OGE2M2U0NWFhMTNmMiIsIm5hbWUiOiJIdGV0IFNhbiIsImVtYWlsIjoiIiwidXNlciI6Ijk1OTk4NDQ1ODk2OSIsInJ0b2tlbiI6Ik1CbC9BTWhuRURCNzBubC84U2xVVmZmaytJd256WnFKcEtiMGNZOEsyaTIvb0FvNEtXNGI5citGaW91dDFjVnpPbnQ1V3F2V0lldTh3cVNYIiwiaWF0IjoxNzAyOTYwOTk2LCJleHAiOjE3MzQwNjQ5OTZ9.xf_m8zuoPAOsPlin9kng_C8RfiV1r7JdhLZf1weDuWU")
//        SocketHandler.establishConnection()
//        mSocket = SocketHandler.getSocket()
//        mSocket.emit("joinCustomer", "65f00e0beee998e9e176b5e7")


        connectSocket()

        client = WebRTCManager(this)
        client.setSocket(mSocket)

        mBinding.btn.setOnClickListener {

            val data = JSONObject()
            data.put("to", "64897db0131c98f765390895") //rider
            data.put("from", "65f289b3c2a2cec84d9546a4")
            data.put("type", "offer")
            client.startAudioCall(data)
        }

        listenSocket()
    }

    private fun listenSocket() {
        mSocket.on("call-made", callMade) //listen from rider
        mSocket.on("answer-made", answerMade)

//        socket.on("call-made") { args ->
//            val sdpOffer =
//                args[0] as JSONObject // Assuming the SDP offer is contained in a JSONObject
//            Log.d(WebRTCManager.TAG, "setSocket: ${sdpOffer.get("sdp")}")
//
//            // Parse and process the SDP offer as needed
//            val sessionDescription = parseSdpOffer(sdpOffer)
//            // Call receiveSdpFromRemotePeer to handle the SDP offer
//            receiveSdpFromRemotePeer(sessionDescription)
//        }
    }


    private fun connectSocket() {
        val accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY1ODExZjNiNWE5OGE2M2U0NWFhMTNmMiIsIm5hbWUiOiJIdGV0IFNhbiIsImVtYWlsIjoiIiwidXNlciI6Ijk1OTk4NDQ1ODk2OSIsInJ0b2tlbiI6Ik1CbC9BTWhuRURCNzBubC84U2xVVmZmaytJd256WnFKcEtiMGNZOEsyaTIvb0FvNEtXNGI5citGaW91dDFjVnpPbnQ1V3F2V0lldTh3cVNYIiwiaWF0IjoxNzAyOTYwOTk2LCJleHAiOjE3MzQwNjQ5OTZ9.xf_m8zuoPAOsPlin9kng_C8RfiV1r7JdhLZf1weDuWU" //customer
        SocketHandler.setSocket(accessToken)
        SocketHandler.establishConnection()
        mSocket = SocketHandler.getSocket()
        mSocket.emit("joinCustomer", "65f289b3c2a2cec84d9546a4")

    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket.close()
    }

}