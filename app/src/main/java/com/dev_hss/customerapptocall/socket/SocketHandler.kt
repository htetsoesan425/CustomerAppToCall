package com.dev_hss.customerapptocall.socket

import android.util.Log
import com.dev_hss.customerapptocall.SOCKET_URL
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.Collections.singletonList
import java.util.Collections.singletonMap


object SocketHandler {

    private lateinit var mSocket: Socket
    private const val TAG = "SocketHandler"


    @Synchronized
    fun setSocket(token: String?) {
        try {
            Log.d("SocketManager", "userId:: $token")
            val options = IO.Options.builder()
                .setExtraHeaders(singletonMap("Authorization", singletonList("Bearer $token")))
                .setPath("/socket.io/").setQuery("x=42").setTransports(arrayOf(WebSocket.NAME))
                .build()

            mSocket = IO.socket(SOCKET_URL, options)


            mSocket.on(Socket.EVENT_CONNECT, onConnect)
            mSocket.on(Socket.EVENT_CONNECT_ERROR, onError)
            mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect)

        } catch (e: URISyntaxException) {
            Log.d(TAG, "setSocket: $e")
        }
    }

    private val onConnect = Emitter.Listener { args ->
        Log.d(TAG, "EVENT_CONNECT: connected to $args")
    }

    private val onError = Emitter.Listener { error ->
        if (error.isNotEmpty() && error[0] is io.socket.engineio.client.EngineIOException) {
            // Handle EngineIOException
            val engineIOException = error[0] as io.socket.engineio.client.EngineIOException
            Log.d(TAG, "EVENT_CONNECT_ERROR: connection error ${engineIOException.cause?.message}!")
        } else {
            Log.d(TAG, "EVENT_CONNECT_ERROR: connection error ${error[0]}!")
        }
    }

    private val onDisconnect = Emitter.Listener { error ->
        if (error.isNotEmpty() && error[0] is io.socket.engineio.client.EngineIOException) {
            // Handle EngineIOException
            val engineIOException = error[0] as io.socket.engineio.client.EngineIOException
            Log.d(TAG, "EVENT_CONNECT_ERROR: connection error ${engineIOException.cause?.message}!")
        } else {
            Log.d(TAG, "EVENT_CONNECT_ERROR: connection error ${error[0]}!")
        }
    }

    @Synchronized
    fun getSocket(): Socket {
        return mSocket
    }

    @Synchronized
    fun establishConnection() {
        mSocket.connect()
    }

    @Synchronized
    fun closeConnection() {
        mSocket.disconnect()
    }

    @Synchronized
    fun observeEventData(eventName: String, callback: (JSONObject) -> Unit) {
        mSocket.on(eventName) { args ->
            if (args.isNotEmpty() && args[0] is JSONObject) {
                val jsonData = args[0] as JSONObject
                callback(jsonData)
            }
        }
    }


}