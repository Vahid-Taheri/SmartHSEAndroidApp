package com.aranuma.smarthse

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gzgamut.demo.helper.ParserHelper
import com.gzgamut.demo.helper.SDK
import com.gzgamut.demo.model.SDKCallBack
import org.json.JSONObject
import java.util.Calendar

class SecondActivity : AppCompatActivity() {

    private var deviceAddress: String? = null
    private var transparentDataValue = false

    private lateinit var versionTextView: TextView
    private lateinit var batteryTextView: TextView
    private lateinit var disconnectBtn: Button
    private lateinit var syncTimeBtn: Button
    private lateinit var getTemperatureBtn: Button
    private lateinit var setTransparentData: Button
    private lateinit var sdk: SDK
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        deviceAddress = intent.getStringExtra("deviceAddress")

        versionTextView = findViewById(R.id.version_textview)
        batteryTextView = findViewById(R.id.battery_percent_textview)
        disconnectBtn = findViewById(R.id.disconnect_bt_device_btn)
        syncTimeBtn = findViewById(R.id.sync_data_time_btn)
        getTemperatureBtn = findViewById(R.id.get_temperature)
        setTransparentData = findViewById(R.id.set_transparent_btn)

        sdk = SDK(applicationContext, 1, callBack)

        sdk.connect(deviceAddress)

        disconnectBtn.setOnClickListener {
            sdk.disconnectDevice(false)
        }

        syncTimeBtn.setOnClickListener {
            sdk.setDateTime(Calendar.getInstance(), 0, 0)
            syncTimeBtn.text = getText(R.string.date_time_synced)
            syncTimeBtn.isEnabled = false
            syncTimeBtn.setBackgroundColor(resources.getColor(R.color.UFOGreen100))
        }

        getTemperatureBtn.setOnClickListener {
            sdk.write(ParserHelper.getTemperatureDataValue());
        }

        setTransparentData.setOnClickListener {
            transparentDataValue = !transparentDataValue
            sdk.setTransparentData(!transparentDataValue)

            val value = ParserHelper.getTransparentValue(
                if (transparentDataValue)
                    1
                else
                    0
            )
            sdk.write(value)
        }
    }

    private val callBack: SDKCallBack = object : SDKCallBack() {
        override fun onDeviceFound(device: FoundDevice, rssi: Int, scanRecord: ByteArray) {
            if (device.mac.equals(deviceAddress, ignoreCase = true)) {
                sdk.stopScan()
                sdk.connect(device.mac)
            }
        }

        override fun onConnectionStateChange(state: Int) {
            val intent = Intent(this@SecondActivity, MainActivity::class.java)
            Log.e(TAG, "Connection status changed")
            if (state == STATE_CONNECT_SUCCESS) {
                sdk.openDescriptor() // After connecting successfully, you must call this otherwise you will not be able to receive data.。

            } else if (state == STATE_DISCONNECT) {
                startActivity(intent)
            } else if (state == STATE_CONNECT_FAIL) {
                startActivity(intent)
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onDescriptorWrite(status: Int) {
            if (status == STATUS_SUCCESS) {
                Log.e(TAG, "Successful connection")
                runOnUiThread {
                    Toast.makeText(this@SecondActivity, "Successful connection", Toast.LENGTH_LONG)
                        .show()
                    versionTextView.text = sdk.version.getString("result")
                    batteryTextView.text = "Battery: ${sdk.battery.getString("result")}%"
                }
            }
        }

        override fun onSDKDeviceResponse(value: JSONObject, type: Int) {
            Log.e(TAG, "type:$type   data:$value")
            runOnUiThread {
                Toast.makeText(
                    this@SecondActivity,
                    "type:$type   data:$value",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
