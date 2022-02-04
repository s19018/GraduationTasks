package com.websarva.wings.android.searchapp

import android.R.layout.*
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
import java.io.StreamCorruptedException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        //https://app.rakuten.co.jp/services/api/IchibaItem/Search/20170706?&keyword=maikura&hits=10&applicationId=1086096818318021682
        //https://shopping.yahooapis.jp/ShoppingWebService/V3/itemSearch?appid=dj00aiZpPTVmQXQ1Q2FuVFk0WiZzPWNvbnN1bWVyc2VjcmV0Jng9MTE-&results=10&query=maikura
        /*
        private const val DEBUG_TAG = "AsyncSample"
        private const val GOODSINFO_URL =
            "https://shopping.yahooapis.jp/ShoppingWebService/V3/itemSearch"
        private const val APP_ID = "dj00aiZpPTVmQXQ1Q2FuVFk0WiZzPWNvbnN1bWVyc2VjcmV0Jng9MTE-"
        private const val result = 10

        */
        private const val DEBUG_TAG = "AsyncSample"
        private const val GOODSINFO_URL_RAKUTEN =
            "https://app.rakuten.co.jp/services/api/IchibaItem/Search/20170706?"
        private const val APP_ID_RAKUTEN = "1086096818318021682"
        private const val GOODSINFO_URL_YAHOO =
            "https://shopping.yahooapis.jp/ShoppingWebService/V3/itemSearch"
        private const val APP_ID_YAHOO = "dj00aiZpPTVmQXQ1Q2FuVFk0WiZzPWNvbnN1bWVyc2VjcmV0Jng9MTE-"
        private const val results = 10
        private const val hits = 10

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btClick = findViewById<Button>(R.id.btClick)

        val listener = HelloListener()

        btClick.setOnClickListener(listener)
    }

    @UiThread
    private fun receiveGoodsInfo(rakutenurlfull: String, yahoourlfull: String) {
        val handler = HandlerCompat.createAsync(mainLooper)
        val backgroundReceiver = GoodsInfoBackgroundReceiver(handler, rakutenurlfull, yahoourlfull)
        val executeService = Executors.newSingleThreadExecutor()
        executeService.submit(backgroundReceiver)

    }

    private inner class GoodsInfoBackgroundReceiver(handler: Handler, rakutenurlfull: String, yahoourlfull: String) : Runnable {

        private val _handler = handler
        private val _rakutenurlfull = rakutenurlfull
        private val _yahoourlfull = yahoourlfull

        @WorkerThread
        override fun run() {
            var rakutenresult = ""
            var yahooresult = ""
            val rakutenurl = URL(_rakutenurlfull)
            val yahoourlfull = URL(_yahoourlfull)
            val rakutencon = rakutenurl.openConnection() as? HttpURLConnection
            rakutencon?.let {
                try {
                    it.connectTimeout = 1000
                    it.readTimeout = 1000
                    it.requestMethod = "GET"
                    it.connect()
                    val stream = it.inputStream
                    rakutenresult = isString(stream)
                    stream.close()
                } catch (ex: SocketTimeoutException) {
                    Log.w(DEBUG_TAG, "通信タイムアウト", ex)
                }
            }

                val yahoocon = yahoourlfull.openConnection() as? HttpURLConnection
                yahoocon?.let {
                    try {
                        it.connectTimeout = 1000
                        it.readTimeout = 1000
                        it.requestMethod = "GET"
                        it.connect()
                        val stream = it.inputStream
                        yahooresult = isString(stream)
                        stream.close()
                    } catch (ex: SocketTimeoutException) {
                        Log.w(DEBUG_TAG, "通信タイムアウト", ex)
                    }

                it.disconnect()
            }
            val postExecutor = GoodsInfoPostExecutor(rakutenresult, yahooresult)
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

    private inner class GoodsInfoPostExecutor(rakutenresult: String, yahooresult: String) : Runnable {

        private val _rakutenurlfull = rakutenresult
        private val _yahoourlfull = yahooresult

        val texts: MutableList<MutableMap<String, String>> = mutableListOf()
        val texts2: MutableList<MutableMap<String, String>> = mutableListOf()

        @UiThread
        override fun run() {
            val rakutenrootJSON = JSONObject(_rakutenurlfull)
            val yahoorootJSON = JSONObject(_yahoourlfull)

            val ItemsJSONArray1 = rakutenrootJSON.getJSONArray("Items")
            for (i in 0..9) {
                val ItemJSON = ItemsJSONArray1.getJSONObject(i)
                val ItemNameJSON = ItemJSON.getJSONObject("Item")
                val ItemName = ItemNameJSON.getString("itemName")
                val ItemPrice = ItemNameJSON.getString("itemPrice")
                val telop = mutableMapOf("name" to "${ItemPrice}円", "price" to "${ItemName}")
                texts.add(telop)
            }

            val itemlist1 = findViewById<ListView>(R.id.item_list1)
            val from1 = arrayOf("name", "price")
            val to1 = intArrayOf(android.R.id.text1, android.R.id.text2)
            val adapter1 = SimpleAdapter(this@MainActivity, texts, android.R.layout.simple_list_item_2, from1, to1)
            itemlist1.adapter = adapter1

            val ItemsJSONArray = yahoorootJSON.getJSONArray("hits")
            for (i in 0..9) {
                val ItemJSON = ItemsJSONArray.getJSONObject(i)
                val ItemName = ItemJSON.getString("name")
                val ItemPrice = ItemJSON.getString("price")
                val telop = mutableMapOf("price" to "${ItemPrice}円", "name" to "${ItemName}")
                texts2.add(telop)
            }

            val itemlist = findViewById<ListView>(R.id.item_list2)
            val from = arrayOf("price", "name")
            val to = intArrayOf(android.R.id.text1, android.R.id.text2)

            val adapter = SimpleAdapter(this@MainActivity, texts2, android.R.layout.simple_list_item_2, from, to)
            itemlist.adapter = adapter

        }
    }

    private inner class HelloListener : View.OnClickListener {
        override fun onClick(view: View?) {
            val input = findViewById<EditText>(R.id.etName)
            val inputStr = input.text.toString()
            val yahoourlFull = "$GOODSINFO_URL_YAHOO?appid=$APP_ID_YAHOO&results=$results&query=${inputStr}"
            val rakutenurlFull = "$GOODSINFO_URL_RAKUTEN&keyword=${inputStr}&$hits=hits&applicationId=$APP_ID_RAKUTEN"
            receiveGoodsInfo(rakutenurlFull, yahoourlFull)
        }
    }
}