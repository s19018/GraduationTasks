package com.websarva.wings.android.searchapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.os.HandlerCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        //https://app.rakuten.co.jp/services/api/IchibaItem/Search/20170706?&keyword=maikura&hits=10&applicationId=1086096818318021682
        private const val DEBUG_TAG = "AsyncSample"
        private const val GOODSINFO_URL =
            "https://app.rakuten.co.jp/services/api/IchibaItem/Search/20170706?"
        private const val APP_ID = "1086096818318021682"
        private const val hits = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //表示ボタンであるButtonオブジェクトを取得
        val btClick = findViewById<Button>(R.id.btClick)
        //リスナクラスのインスタンス生成
        val listener = HelloListener()
        //表示ボタンにリスナを設定
        btClick.setOnClickListener(listener)
    }

    @UiThread
    private fun receiveGoodsInfo(urlfull: String) {
        val handler = HandlerCompat.createAsync(mainLooper)
        val backgroundReceiver = GoodsInfoBackgroundReceiver(handler, urlfull)
        val executeService = Executors.newSingleThreadExecutor()
        executeService.submit(backgroundReceiver)

    }

    private inner class GoodsInfoBackgroundReceiver(handler: Handler, url: String) : Runnable {

        private val _handler = handler
        private val _url = url

        @WorkerThread
        override fun run() {
            var result = ""
            val url = URL(_url)
            val con = url.openConnection() as? HttpURLConnection
            con?.let {
                try {
                    it.connectTimeout = 1000
                    it.readTimeout = 1000
                    it.requestMethod = "GET"
                    it.connect()
                    val stream = it.inputStream
                    result = isString(stream)
                    stream.close()
                } catch (ex: SocketTimeoutException) {
                    Log.w(DEBUG_TAG, "通信タイムアウト", ex)
                }
                it.disconnect()
            }
            val postExecutor = GoodsInfoPostExecutor(result)
            _handler.post(postExecutor)
        }

        private fun isString(stream: InputStream): String {
            val sb = StringBuilder()
            val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
            var line = reader.readLine()
            while (line != null) {
                sb.append(line)
                line = reader.readLine()
            }
            reader.close()
            return sb.toString()
        }
    }

    private inner class GoodsInfoPostExecutor(result: String) : Runnable {

        private val _result = result

        //val listType = object : TypeToken<List<RowData>>() {}.type
        //val texts: Array<TextView?> = arrayOfNulls(10)
        //val texts: ArrayList<String?> = arrayListOf()
        val texts: MutableList<MutableMap<String, String>> = mutableListOf()
        //val texts = mutableListOf<TextView?>()

        @UiThread
        override fun run() {
            /*
            val rootJSON = JSONObject(_result)
            val ItemsJSONArray = rootJSON.getJSONArray("Items")
            val ItemJSON = ItemsJSONArray.getJSONObject(0)
            val ItemNameJSON = ItemJSON.getJSONObject("Item")
            val ItemName = ItemNameJSON.getString("itemName")
            val telop = "${ItemName}"

            val tvTelop = findViewById<TextView>(R.id.tvOutput)
            tvTelop.text = telop
             */

            val rootJSON = JSONObject(_result)
            val ItemsJSONArray = rootJSON.getJSONArray("Items")
            for (i in 0..9) {
                val ItemJSON = ItemsJSONArray.getJSONObject(i)
                val ItemNameJSON = ItemJSON.getJSONObject("Item")
                val ItemName = ItemNameJSON.getString("itemName")
                val ItemPrice = ItemNameJSON.getString("itemPrice")
                val telop = mutableMapOf("name" to "${ItemPrice}円", "price" to "${ItemName}")
                texts.add(telop)
            }

            val itemlist = findViewById<ListView>(R.id.item_list1)
            val from = arrayOf("name", "price")
            val to = intArrayOf(android.R.id.text1, android.R.id.text2)
            val adapter = SimpleAdapter(this@MainActivity, texts, android.R.layout.simple_list_item_2, from, to)
            itemlist.adapter = adapter
        }
    }

    private inner class HelloListener : View.OnClickListener {
        override fun onClick(view: View?) {
            val input = findViewById<EditText>(R.id.etName)
            val inputStr = input.text.toString()
            val urlFull = "$GOODSINFO_URL&keyword=${inputStr}&$hits=hits&applicationId=$APP_ID"
            receiveGoodsInfo(urlFull)
        }
    }
}