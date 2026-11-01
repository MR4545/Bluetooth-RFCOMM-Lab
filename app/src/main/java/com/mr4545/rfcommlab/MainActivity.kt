package com.mr4545.rfcommlab

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : Activity() {
    companion object {
        private const val REQUEST_PERMISSIONS = 10
        private const val REQUEST_DISCOVERABLE = 11
        private const val SERVICE_NAME = "RFCOMM Lab SPP"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var adapter: BluetoothAdapter? = null
    @Volatile private var running = false
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null

    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 24)
        }
        status = TextView(this).apply { textSize = 18f; text = "Status: stopped" }
        startButton = Button(this).apply {
            text = "Start RFCOMM server"
            setOnClickListener { ensurePermissionsAndStart() }
        }
        stopButton = Button(this).apply {
            text = "Stop server"
            isEnabled = false
            setOnClickListener { stopServer() }
        }
        val scroll = ScrollView(this)
        log = TextView(this).apply { textSize = 14f; text = "Log:\n" }
        scroll.addView(log)
        layout.addView(status)
        layout.addView(startButton)
        layout.addView(stopButton)
        layout.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(layout)

        if (adapter == null) {
            status.text = "Status: this device has no Bluetooth adapter"
            startButton.isEnabled = false
        }
    }

    private fun ensurePermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= 31) {
            val needed = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ).filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            if (needed.isNotEmpty()) {
                requestPermissions(needed.toTypedArray(), REQUEST_PERMISSIONS)
                return
            }
        }
        beginDiscoverability()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS &&
            grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            beginDiscoverability()
        } else if (requestCode == REQUEST_PERMISSIONS) {
            appendLog("Bluetooth permissions were not granted.")
        }
    }

    private fun beginDiscoverability() {
        val a = adapter ?: return
        if (!a.isEnabled) {
            appendLog("Please turn Bluetooth on, then tap Start again.")
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        val discoverable = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivityForResult(discoverable, REQUEST_DISCOVERABLE)
    }

    @Deprecated("For this small compatibility test, use the system discoverability prompt.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DISCOVERABLE) {
            if (resultCode > 0) startServer() else appendLog("Discoverability was declined.")
        }
    }

    private fun startServer() {
        if (running) return
        running = true
        startButton.isEnabled = false
        stopButton.isEnabled = true
        status.text = "Status: waiting for Windows RFCOMM connection"
        appendLog("Starting SPP server: $SPP_UUID")

        thread(name = "RFCOMM-accept") {
            try {
                val server = adapter!!.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SPP_UUID)
                serverSocket = server
                appendLog("Server listening. Keep this app open.")
                while (running) {
                    val socket = server.accept()
                    clientSocket = socket
                    appendLog("Connected: ${socket.remoteDevice.name ?: socket.remoteDevice.address}")
                    statusOnUi("Status: connected")
                    handleClient(socket)
                    clientSocket = null
                    appendLog("Client disconnected; waiting for another connection.")
                    statusOnUi("Status: waiting for connection")
                }
            } catch (e: Exception) {
                if (running) appendLog("Server error: ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                running = false
                try { serverSocket?.close() } catch (_: Exception) {}
                statusOnUi("Status: stopped")
                runOnUiThread { startButton.isEnabled = true; stopButton.isEnabled = false }
            }
        }
    }

    private fun handleClient(socket: BluetoothSocket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
            val writer = OutputStreamWriter(socket.outputStream, Charsets.UTF_8)
            while (running && !socket.isClosed) {
                val line = reader.readLine() ?: break
                appendLog("PC says: $line")
                writer.write("PHONEKEYS_ACK:$line\n")
                writer.flush()
                appendLog("Sent acknowledgement.")
            }
        } catch (e: Exception) {
            if (running) appendLog("Connection ended: ${e.message}")
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun stopServer() {
        running = false
        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        appendLog("Stop requested.")
        status.text = "Status: stopping"
    }

    private fun appendLog(message: String) {
        runOnUiThread { log.append("$message\n") }
    }

    private fun statusOnUi(message: String) {
        runOnUiThread { status.text = message }
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }
}
