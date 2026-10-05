package com.example.documentassistant

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var etQuestion: EditText
    private lateinit var btnSearch: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvAnswer: TextView
    private lateinit var tvSources: TextView
    private lateinit var tvDocumentStatus: TextView

    private val client = OkHttpClient()

    /*
     * Android Emulator:
     * 10.0.2.2 points to the computer running the backend.
     *
     * Physical phone:
     * Replace this with your computer's IPv4 address.
     *
     * Example:
     * http://192.168.1.100:8000/search
     */
    private val serverUrl = "http://10.0.2.2:8000/search"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initializeViews()

        btnSearch.setOnClickListener {
            searchDocuments()
        }
    }

    private fun initializeViews() {

        etQuestion = findViewById(R.id.etQuestion)

        btnSearch = findViewById(R.id.btnSearch)

        progressBar = findViewById(R.id.progressBar)

        tvAnswer = findViewById(R.id.tvAnswer)

        tvSources = findViewById(R.id.tvSources)

        tvDocumentStatus = findViewById(R.id.tvDocumentStatus)
    }

    private fun searchDocuments() {

        val question = etQuestion.text.toString().trim()

        if (question.isEmpty()) {

            etQuestion.error = "Please enter a question"

            return
        }

        setLoading(true)

        tvDocumentStatus.text = "Searching documents..."

        val json = JSONObject()

        json.put("query", question)

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val requestBody = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {

            override fun onFailure(
                call: okhttp3.Call,
                e: IOException
            ) {

                runOnUiThread {

                    setLoading(false)

                    tvDocumentStatus.text = "Connection failed"

                    tvAnswer.text =
                        "Could not connect to the document search server.\n\n" +
                                "Make sure the Python backend is running."

                    tvSources.text = ""

                }
            }

            override fun onResponse(
                call: okhttp3.Call,
                response: okhttp3.Response
            ) {

                val responseBody = response.body?.string()

                runOnUiThread {

                    setLoading(false)

                    if (!response.isSuccessful || responseBody == null) {

                        tvDocumentStatus.text = "Server error"

                        tvAnswer.text =
                            "The server returned an error."

                        tvSources.text = ""

                        return@runOnUiThread
                    }

                    try {

                        val result = JSONObject(responseBody)

                        val answer =
                            result.optString(
                                "answer",
                                "No answer found."
                            )

                        tvAnswer.text = answer

                        val sourcesArray =
                            result.optJSONArray("sources")

                        if (sourcesArray != null &&
                            sourcesArray.length() > 0
                        ) {

                            val sourceText =
                                StringBuilder()

                            for (i in 0 until sourcesArray.length()) {

                                val source =
                                    sourcesArray.optString(i)

                                sourceText.append("• ")
                                    .append(source)
                                    .append("\n\n")
                            }

                            tvSources.text =
                                sourceText.toString()

                        } else {

                            tvSources.text =
                                "No relevant sources found."

                        }

                        tvDocumentStatus.text =
                            "Search completed successfully"

                    } catch (e: Exception) {

                        tvDocumentStatus.text =
                            "Invalid server response"

                        tvAnswer.text =
                            "Unable to read the server response."

                        tvSources.text = ""

                    }
                }
            }
        })
    }

    private fun setLoading(isLoading: Boolean) {

        if (isLoading) {

            progressBar.visibility = View.VISIBLE

            btnSearch.isEnabled = false

            btnSearch.text = "SEARCHING..."

        } else {

            progressBar.visibility = View.GONE

            btnSearch.isEnabled = true

            btnSearch.text = "SEARCH DOCUMENTS"
        }
    }
}