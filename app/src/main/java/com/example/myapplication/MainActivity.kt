package com.example.myapplication

import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var editTextBusStopName: EditText
    private lateinit var buttonSearch: Button
    private lateinit var textViewResult: TextView
    private lateinit var listViewBusStops: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextBusStopName = findViewById(R.id.editTextBusStopName)
        buttonSearch = findViewById(R.id.buttonSearch)
        textViewResult = findViewById(R.id.textViewResult)
        listViewBusStops = findViewById(R.id.listViewBusStops)

        buttonSearch.setOnClickListener {
            val busStopName = editTextBusStopName.text.toString()
            if (busStopName.isNotEmpty()) {
                FetchBusStopListTask().execute(busStopName)
            } else {
                Toast.makeText(this, "정류장 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        listViewBusStops.setOnItemClickListener { parent, view, position, id ->
            val selectedBusStop = parent.getItemAtPosition(position) as String

            startActivity(intent)
        }
    }

    private inner class FetchBusStopListTask : AsyncTask<String, Void, List<String>>() {
        override fun doInBackground(vararg params: String?): List<String> {
            val busStopName = params[0]

            val url = URL("http://203.250.34.102:5000/search_bus_stop") // 서버 주소 입력
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            try {
                val outputStream = connection.outputStream
                val body = JSONObject().apply {
                    put("bus_stop_name", busStopName)
                }.toString().toByteArray(Charsets.UTF_8)
                outputStream.write(body)
                outputStream.close()

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonArray = JSONArray(response.toString())
                val busStopList = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val busStopName = jsonObject.getString("nodenm")
                    if (!busStopList.contains(busStopName)) {
                        busStopList.add(busStopName)
                    }
                }
                return busStopList
            } catch (e: Exception) {
                Log.e("MainActivity", "Error", e)
                return emptyList()
            } finally {
                connection.disconnect()
            }
        }

        override fun onPostExecute(result: List<String>?) {
            super.onPostExecute(result)
            if (result.isNullOrEmpty()) {
                Toast.makeText(this@MainActivity, "정류장 목록을 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            } else {
                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, result)
                listViewBusStops.adapter = adapter
            }
        }
    }

    private inner class FetchBusRouteInfoTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val selectedBusStop = params[0]

            val url = URL("http://203.250.34.102:5000/get_bus_info") // 서버 주소 입력
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            try {
                val outputStream = connection.outputStream
                val body = JSONObject().apply {
                    put("selected_index", 0)
                    put("selected_node_id", selectedBusStop)
                }.toString().toByteArray(Charsets.UTF_8)
                outputStream.write(body)
                outputStream.close()

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                return response.toString()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error", e)
                return ""
            } finally {
                connection.disconnect()
            }
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result.isNullOrEmpty()) {
                textViewResult.text = "해당 정류장에 대한 버스 노선 정보가 없습니다."
            } else {
                parseAndDisplayResult(result)
            }
        }

        private fun parseAndDisplayResult(result: String) {
            try {
                val jsonArray = JSONArray(result)
                if (jsonArray.length() > 0) {
                    val resultBuilder = StringBuilder()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        resultBuilder.append("노선번호: ${jsonObject.getString("routeno")}\n")
                        resultBuilder.append("남은 정류장 수: ${jsonObject.optString("arrprevstationcnt", "정보 없음")}\n")
                        resultBuilder.append("도착 예정 시간: ${jsonObject.optString("arrival_time", "정보 없음")}\n")
                        resultBuilder.append("혼잡도: ${jsonObject.optString("congestion_level", "정보 없음")}\n\n")
                    }
                    textViewResult.text = resultBuilder.toString()
                } else {
                    textViewResult.text = "해당 정류장에 대한 버스 노선 정보가 없습니다."
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error", e)
                textViewResult.text = "데이터를 파싱하는 동안 오류가 발생했습니다."
            }
        }
    }
}

