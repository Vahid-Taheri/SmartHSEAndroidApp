package com.aranuma.smarthse

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import no.nordicsemi.android.support.v18.scanner.*

class MainActivity : AppCompatActivity() {

    private lateinit var scanner: BluetoothLeScannerCompat
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var devicesListView: ListView
    private lateinit var scanButton: Button
    private val scannedDevices: ArrayList<ScanResult> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devicesListView = findViewById(R.id.list_devices)
        scanButton = findViewById(R.id.send_button)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        devicesListView.adapter = adapter

        devicesListView.setOnItemClickListener{ parent, view, position, id ->
            val result = scannedDevices[position]
            val deviceAddress = result.device.address

            val intent = Intent(this, SecondActivity::class.java)
            intent.putExtra("deviceAddress", deviceAddress)
            startActivity(intent)
        }

        scanner = BluetoothLeScannerCompat.getScanner()


        scanButton.setOnClickListener{
            startScanning()
        }
    }

    private fun startScanning() {
        // Check for location permission required for scanning on Android 6.0 and above
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSION_LOCATION
            )
        } else {
            adapter.clear()
            val settings = ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setUseHardwareBatchingIfSupported(true)
                .build()

            val filters: MutableList<ScanFilter> = mutableListOf()
            // Add any filters if needed

            scanner.startScan(filters, settings, scanCallback)
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            val existingIndex = scannedDevices.indexOfFirst { it.device.address == result.device.address }
            if (!result.device.name.isNullOrEmpty()) {
                if(existingIndex != -1){
                    scannedDevices[existingIndex] = result
                }
                else {
                    scannedDevices.add(result)
                }
                adapter.clear()
                scannedDevices.forEach{
                    val device = it.device
                    val deviceAddress = device.address
                    val deviceName = device.name
                    val deviceRssi = it.rssi
                    val deviceInfo = "$deviceName\n$deviceAddress\t$deviceRssi dBm"
                    adapter.add(deviceInfo)
                    adapter.notifyDataSetChanged()
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            // Handle scan failure
        }
    }

    override fun onStop() {
        super.onStop()
        scanner.stopScan(scanCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                // Permission denied, handle accordingly
            }
        }
    }

    companion object {
        private const val REQUEST_PERMISSION_LOCATION = 123
    }
}
