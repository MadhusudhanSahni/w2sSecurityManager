package com.example.flutter_w2s_security_manger

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import com.mantra.mfs100.MFS100
import com.mantra.mfs100.FingerData
import com.mantra.mfs100.MFS100Event
import android.widget.Toast

class MainActivity : FlutterActivity() {

    private val CHANNEL = "fingerprint_scanner"
    private lateinit var mfs100: MFS100
    private lateinit var usbManager: UsbManager
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the USB Manager
        usbManager = getSystemService(USB_SERVICE) as UsbManager

        // Initialize the MFS100 fingerprint scanner
        mfs100 = MFS100(object : MFS100Event {
//            override fun OnPreview(fingerData: FingerData) {
//                println("Previewing fingerprint data.")
//            }
//
//            override fun OnCaptureCompleted(success: Boolean, errorCode: Int, errorMessage: String, fingerData: FingerData?) {
//                if (success) {
//                    println("Fingerprint captured successfully.")
//                } else {
//                    println("Error during fingerprint capture: $errorMessage")
//                }
//            }

            override fun OnDeviceAttached(vid: Int, pid: Int, hasPermission: Boolean) {
                println("Device attached. VID: $vid, PID: $pid, Has Permission: $hasPermission")
                if (!hasPermission) {
                    requestUsbPermission()
                }
            }

            override fun OnDeviceDetached() {
                println("Device detached.")
            }

            override fun OnHostCheckFailed(err: String?) {
                println("Host check failed: $err")
            }
        })

        // Register the USB permission broadcast receiver
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(mUsbReceiver, filter)

        // Set the application context for the fingerprint scanner
        if (mfs100.SetApplicationContext(this)) {
            Toast.makeText(this, "Scanner context set successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to set scanner context", Toast.LENGTH_SHORT).show()
        }

        // Initialize MethodChannel for Flutter communication
        flutterEngine?.dartExecutor?.binaryMessenger?.let { binaryMessenger ->
            MethodChannel(binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
                when (call.method) {
                    "scanFingerprint" -> {
                        val fingerprintResult = captureFingerprint()
                        result.success(fingerprintResult)
                    }
                    else -> {
                        result.notImplemented()
                    }
                }
            }
        }
    }

    // Request permission to access the USB device
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun requestUsbPermission() {
        val device: UsbDevice? = usbManager.deviceList.values.firstOrNull()
        device?.let {
            val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
            if (!usbManager.hasPermission(it)) {
                usbManager.requestPermission(it, permissionIntent)
            } else {
                println("Already have permission for the device.")
            }
//            val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
//            usbManager.requestPermission(it, permissionIntent)

        }
    }
   /* private fun requestUsbPermission() {
        val device: UsbDevice? = usbManager.deviceList.values.firstOrNull()
        device?.let {
            val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
            if (!usbManager.hasPermission(it)) {
                usbManager.requestPermission(it, permissionIntent)
            } else {
                println("Already have permission for the device.")
            }
        }
    }*/

    private val mUsbReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            val action = intent.action
            when (action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.let {
                                println("Permission granted for device.")
                            }
                        } else {
                            println("Permission denied for device.")
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    println("USB Device attached.")
                    requestUsbPermission()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    println("USB Device detached.")
                }
            }
        }
    }

    private fun captureFingerprint(): String {
        return try {
            val ret = mfs100.Init() // Initialize the scanner
            if (ret == 0) {
                val fingerData = FingerData()
                val captureResult = mfs100.AutoCapture(fingerData, 600, true)

                if (captureResult == 0) {
                    "Fingerprint captured successfully with quality: ${fingerData.Quality()}"
                } else {
                    "Error capturing fingerprint: ${mfs100.GetErrorMsg(captureResult)}"
                }
            } else {
                "Error initializing fingerprint scanner: ${mfs100.GetErrorMsg(ret)}"
            }
        } catch (e: Exception) {
            "Exception during fingerprint capture: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mfs100.UnInit() // Uninitialize the scanner when the activity is destroyed
        try {
            unregisterReceiver(mUsbReceiver)
        } catch (e: Exception) {
            println("Failed to unregister receiver: ${e.message}")
        }
    }
}
