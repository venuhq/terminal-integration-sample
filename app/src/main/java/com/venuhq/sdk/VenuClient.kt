package com.venuhq.sdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.lang.Exception
import kotlin.concurrent.thread
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.withTimeout

class VenuClient(private val activity: ComponentActivity) {
    private val TAG = "VenuClient"
    private val REQUEST_TIMEOUT_MS = 45000L // 45 seconds

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        allowStructuredMapKeys = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val serviceManager = ServiceManager()
    private val activityResultHandler = ActivityResultHandler()
    private val defaultResponse = VenuResponse(action = Action.NONE, intent = null)

    init {
        activityResultHandler.setup(activity, json)
    }

    suspend fun cardPresented(request: VenuCardRequest): VenuCardPresentedResult {
        val response = callServerWithFallback(request, VenuMessage.REQUEST_CARD_PRESENTED)

        if (response.action == Action.LAUNCH_INTENT && response.intent != null) {
            return activityResultHandler.handleCardPresented(response.intent)
        }
        
        return VenuCardPresentedResult(discountAmount = "0")
    }

    suspend fun transactionAccepted(request: VenuCardRequest) {
        val response = callServerWithFallback(request, VenuMessage.REQUEST_TRANSACTION_ACCEPTED)

        if (response.action == Action.LAUNCH_INTENT && response.intent != null) {
            activityResultHandler.handleTransactionAccepted(response.intent)
        }
    }

    suspend fun initialise(request: VenuInitialiseRequest) {
        callServerWithFallback(request, VenuMessage.REQUEST_INITIALISE)
    }

    private suspend inline fun <reified T>callServerWithFallback(request: T, type: VenuMessage): VenuResponse {
        return try {
            callServer(request, type)
        } catch (e: Exception) {
            Log.d(TAG, "Exception while calling Venu", e)
            defaultResponse
        }
    }

    private suspend inline fun <reified T, reified U>callServer(request: T, type: VenuMessage): U {
        serviceManager.ensureConnected()

        val requestStr = json.encodeToString(request)
        val responseStr = serviceManager.sendRequest(requestStr, type)
        return json.decodeFromString<U>(responseStr)
    }

    fun connect() = serviceManager.connect(activity)
    fun disconnect() = serviceManager.disconnect(activity)

    private inner class ServiceManager {
        private var serverMessenger: Messenger? = null
        private var clientMessenger: Messenger? = null
        private var serviceConnection: ServiceConnection? = null
        private var currentDeferred: CompletableDeferred<String>? = null
        private var connectionDeferred: CompletableDeferred<Boolean>? = null
        private var isConnecting = false

        fun connect(activity: ComponentActivity) {
            serviceConnection = createServiceConnection()
            val intent = Intent("com.venuhq.terminal.SERVICE").apply {
                `package` = "com.venuhq.terminal"
            }
            activity.applicationContext.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
            connectionDeferred = CompletableDeferred()
        }

        private fun createServiceConnection() = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                Log.d(TAG, "Service connected")
                serverMessenger = Messenger(binder)
                setupResponseHandler()
                connectionDeferred?.complete(true)
                connectionDeferred = null
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d(TAG, "Service disconnected")
                clientMessenger?.send(Message.obtain(null, VenuMessage.RESPONSE_QUIT.value))
                serverMessenger = null
                clientMessenger = null
            }
        }

        private fun setupResponseHandler() {
            thread(isDaemon = true) {
                Looper.prepare()
                val handler = createMessageHandler()
                clientMessenger = Messenger(handler)
                Looper.loop()
            }
        }

        private fun createMessageHandler() = object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                when (msg.arg1) {
                    VenuMessage.RESPONSE_QUIT.value -> Looper.myLooper()?.quitSafely()
                    VenuMessage.RESPONSE_JSON.value -> {
                        val json = msg.data.getString("json") ?: ""
                        Log.d(TAG, "Message received: $json")
                        currentDeferred?.complete(json)
                        currentDeferred = null
                    }
                }
            }
        }

        suspend fun ensureConnected() {
        if (serverMessenger == null) {
            synchronized(this) {
                if (!isConnecting) {
                    isConnecting = true
                    connect(activity)
                }
            }

            try {
                withTimeout(REQUEST_TIMEOUT_MS) {
                    connectionDeferred?.await() ?: throw IllegalStateException("Connection failed")
                }
            } finally {
                isConnecting = false
            }
        }
    }

        suspend fun sendRequest(requestStr: String, type: VenuMessage): String {
            if (currentDeferred != null) {
                throw IllegalStateException("Already waiting for a response")
            }

            val deferred = CompletableDeferred<String>()
            currentDeferred = deferred

            val msg = Message.obtain().apply {
                arg1 = type.value
                data = Bundle().apply { putString("json", requestStr) }
                replyTo = clientMessenger
            }

            try {
                serverMessenger?.send(msg) ?: throw IllegalStateException("Not connected to server")
                Log.d(TAG, "Sent message: $requestStr")
            } catch (e: RemoteException) {
                currentDeferred = null
                throw IOException("Failed to send message", e)
            }

            return withContext(Dispatchers.IO) {
                try {
                    withTimeout(REQUEST_TIMEOUT_MS) {
                        deferred.await()
                    }
                } catch (e: Exception) {
                    currentDeferred = null
                    throw IOException("Request timed out or failed", e)
                }
            }
        }

        fun disconnect(activity: ComponentActivity) {
            serviceConnection?.let { activity.applicationContext.unbindService(it) }
            clientMessenger?.send(Message.obtain(null, VenuMessage.RESPONSE_QUIT.value))
        }
    }

    private inner class ActivityResultHandler {
        private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
        private var jsonDeferred: CompletableDeferred<String?>? = null

        fun setup(activity: ComponentActivity, json: Json) {
            activityResultLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                handleActivityResult(result)
            }
        }

        private fun handleActivityResult(
            result: androidx.activity.result.ActivityResult
        ) {
            val resultJson = result.data?.getStringExtra("json")
            Log.d(TAG, "Activity result received: $resultJson")
            jsonDeferred?.complete(resultJson)
            jsonDeferred = null
        }

        suspend fun handleCardPresented(intent: String): VenuCardPresentedResult {
            val resultJson = launchIntent(intent)
            if (resultJson == null) {
                return VenuCardPresentedResult(discountAmount = null)
            }

            return try {
                json.decodeFromString<VenuCardPresentedResult>(resultJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse card presented result", e)
                VenuCardPresentedResult(discountAmount = null)
            }
        }
        
        suspend fun handleTransactionAccepted(intent: String) {
            launchIntent(intent)
        }

        private suspend fun launchIntent(intent: String): String? {
            jsonDeferred = CompletableDeferred()
            val i = Intent(intent).apply {
                flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            activityResultLauncher.launch(i)

            return withTimeout(REQUEST_TIMEOUT_MS) {
                jsonDeferred?.await()
            }
        }
    }
}