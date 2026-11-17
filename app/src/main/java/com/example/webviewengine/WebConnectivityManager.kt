package com.example.webviewengine

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper

/**
 * Observes device network availability and triggers main-thread reconnect callbacks
 * when connectivity is restored after an offline error.
 */
class WebConnectivityManager(private val context: Context) {

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isNetworkAvailable(): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return@runCatching true
        val active = cm.activeNetwork ?: return@runCatching false
        val caps = cm.getNetworkCapabilities(active) ?: return@runCatching false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(true)

    fun startReconnectMonitor(onNetworkRestored: () -> Unit) {
        stopReconnectMonitor()
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    mainHandler.post { onNetworkRestored() }
                }
            }
            cm.registerDefaultNetworkCallback(cb)
            networkCallback = cb
        }
    }

    fun stopReconnectMonitor() {
        runCatching {
            val cb = networkCallback ?: return
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            cm.unregisterNetworkCallback(cb)
        }
        networkCallback = null
        mainHandler.removeCallbacksAndMessages(null)
    }
}
