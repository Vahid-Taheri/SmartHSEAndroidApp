package com.aranuma.smarthse

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import no.nordicsemi.android.support.v18.scanner.*


class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION_LOCATION:Int =1
    private val REQUEST_BLUETOOTH_CONNECT:Int = 2
    private val REQUEST_BLUETOOTH_SCAN:Int = 3

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var scanner: BluetoothLeScannerCompat
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var devicesListView: ListView
    private lateinit var btTurnOnView: ConstraintLayout
    private lateinit var btScanView: ConstraintLayout
    private lateinit var btTurnOnButton: Button
    private lateinit var scanButton: Button
    private val scannedDevices: ArrayList<ScanResult> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        btTurnOnView = findViewById(R.id.bt_turn_on_view)
        btScanView = findViewById(R.id.bt_scan_view)
        devicesListView = findViewById(R.id.list_devices)
        btTurnOnButton = findViewById(R.id.bt_turn_on_btn)
        scanButton = findViewById(R.id.send_button)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        devicesListView.adapter = adapter

        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothAdapter = bluetoothManager.adapter
        scanner = BluetoothLeScannerCompat.getScanner()

        checkBluetooth()

        btTurnOnButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_CONNECT
                )
            } else {
                turnOnBluetooth()
            }
        }
        scanButton.setOnClickListener{
            startScanning()
        }

        devicesListView.setOnItemClickListener{ parent, view, position, id ->
            val result = scannedDevices[position]
            val deviceAddress = result.device.address

            val intent = Intent(this, SecondActivity::class.java)
            intent.putExtra("deviceAddress", deviceAddress)
            startActivity(intent)
        }
    }

    private fun checkBluetooth(){

        if (bluetoothAdapter.isEnabled) {
            btTurnOnView.visibility = View.GONE
            btScanView.visibility = View.VISIBLE
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
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                REQUEST_BLUETOOTH_SCAN
            )
        }
        else {
            adapter.clear()
            val settings = ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setUseHardwareBatchingIfSupported(true)
                .build()

            val filters: MutableList<ScanFilter> = mutableListOf()
            // Add any filters if needed

            scanner.startScan(filters, settings, scanCallback)
            scanButton.text = getString(R.string.stop_scan)
            scanButton.setOnClickListener{
                stopScanning(this)
            }
        }
    }

    private fun stopScanning(context:Context) {
        scanner.stopScan(scanCallback)
        scanButton.text = getString(R.string.scan)
        scanButton.setOnClickListener{
            startScanning()
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
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_PERMISSION_LOCATION || requestCode == REQUEST_BLUETOOTH_SCAN)
                startScanning()
            else if (requestCode == REQUEST_BLUETOOTH_CONNECT)
                turnOnBluetooth()

        } else {
            // Permission denied, handle accordingly
        }
    }

    private fun turnOnBluetooth() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        resultLauncher.launch(intent)
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            // val data: Intent? = result.data
            checkBluetooth()
        }
    }
}
