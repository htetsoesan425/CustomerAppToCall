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
        val data1 = args[0] as JSONObject
        Log.d(TAG, "callMade:$data1")
        try {
            val data = args[0] as JSONObject
            Log.d(TAG, "callMade:$data")
            //client.
            mBinding.tv1.text = data.toString()

        } catch (e: JSONException) {
            Log.d(TAG, "${e.message}")
            mBinding.tv1.text = e.message

        }
    }

    private val answerMade = Emitter.Listener { args ->
        val data1 = args[0] as JSONObject
        Log.d(TAG, "answerMade:$data1")
        try {
            val data = args[0] as JSONObject
            Log.d(TAG, "answerMade:$data")
            mBinding.tv2.text = data.toString()
        } catch (e: JSONException) {
            Log.d(TAG, "${e.message}")
            mBinding.tv2.text = e.message

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
            Log.d(TAG, "failur: $it")
        }

        connectSocket()

        client = WebRTCManager(this)

        mBinding.btn.setOnClickListener {
            client.setSocket(mSocket)

            val data = JSONObject()
            data.put("to", "65ee60d16a17dc9a8a8866d4")
            data.put("from", "6440c33803562ce831a1a090")
            client.startAudioCall(data)
        }

        listenSocket()
    }

    private fun listenSocket() {
        mSocket.on("call-made", callMade)
        mSocket.on("answer-made", answerMade)
    }


    private fun connectSocket() {
        SocketHandler.setSocket(fcmToken) //customer
        SocketHandler.establishConnection()
        mSocket = SocketHandler.getSocket()

    }

}