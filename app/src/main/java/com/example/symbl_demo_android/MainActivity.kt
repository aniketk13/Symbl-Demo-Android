package com.example.symbl_demo_android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.symbl_demo_android.databinding.ActivityMainBinding
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var accessToken:String
    private lateinit var progressBar: ProgressBar
    private lateinit var mediaUrl:String
    private lateinit var conversationId:String
    private lateinit var jobId:String
    private var transcription:String=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progressBar=binding.progressBar2
        generateAccessToken()
    binding.copy.isEnabled=false
        binding.submit.setOnClickListener {
           mediaUrl=binding.acceptUrl.text.toString()
            binding.acceptUrl.hideKeyboard()
            progressBar.visibility=View.VISIBLE
            processVideo()
        }
        binding.copy.setOnClickListener {
            copy_to_clipboard()
        }
    }

    private fun generateAccessToken() {
        val parameters = JSONObject()
        parameters.put("type", "application")
        parameters.put("appId", "706f657063505776757476457764344d7434674553644c53747a4d5757795156")
        parameters.put(
            "appSecret",
            "39594b715047596d51705a79704879786e526e52344a613676794659766142633859596e4c597975706263684c73483577684b67334a44313348784668436f57"
        )

        val queue = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(
            Request.Method.POST, "https://api.symbl.ai/oauth2/token:generate", parameters,
            {
                accessToken = it.getString("accessToken")
                Log.i("accessToken", accessToken)
                progressBar.visibility= View.GONE
            }, {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
            })
        queue.add(req)
    }

    private fun processVideo() {
        Log.i("in","hello")
        val body = JSONObject()
        body.put("url", mediaUrl)
        body.put("name", "General")
        val queue = Volley.newRequestQueue(this)
        val req = object : JsonObjectRequest(
            Method.POST, "https://api.symbl.ai/v1/process/video/url", body,
            {

                //getting the conversation id
                conversationId = it.getString("conversationId")
                jobId = it.getString("jobId")

                Log.i("VideoProcess", conversationId)
                Log.i("VideoProcess", jobId)

                getStatusSymblResponse()
            }, {
                Toast.makeText(this, "Error in video", Toast.LENGTH_SHORT).show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Authorization", "Bearer $accessToken")
                headers.put("Content-Type", "application/json")
                return headers
            }
        }
        queue.add(req)
    }
    //webhook implemented for Symbl.ai
    private fun getStatusSymblResponse() {
        val queue = Volley.newRequestQueue(this)
        val request = object : JsonObjectRequest(
            Method.GET, "https://api.symbl.ai/v1/job/$jobId", null, {
                val status = it.getString("status")
                if (status == "completed") {
                    getResponseFromSymbl()
                } else {
                    val handler = Handler(Looper.getMainLooper())
                    Handler().postDelayed({
                        getStatusSymblResponse()
                    }, 3000)
                }
            }, {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Authorization", "Bearer $accessToken")
                return headers
            }
        }
        queue.add(request)
    }
    private fun getResponseFromSymbl() {
        val queue = Volley.newRequestQueue(this)
        val req = object : JsonObjectRequest(
            Method.GET,"https://api.symbl.ai/v1/conversations/$conversationId/messages", null,
            {
                Log.i("VideoProcess", "Response generated")
                Log.i("VideoProcess", it.toString())
//                summary=it.getJSONArray("messages").getJSONObject(0).getString("text")
                textOutputSymbl(it)
            }, {
                Toast.makeText(this, "Error in video", Toast.LENGTH_SHORT).show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Authorization", "Bearer $accessToken")
                headers.put("Content-Type","application/json")
                return headers
            }
        }
        queue.add(req)
    }
    private fun textOutputSymbl(response: JSONObject?) {
        Log.i("response", response.toString())
        val messages = response?.getJSONArray("messages")
        if (messages != null) {
            for (i in 0 until messages.length()) {
                val obj = messages.getJSONObject(i)
                val text = obj.getString("text")
                transcription = "$transcription$text "
                binding.copy.isEnabled=true
            }
        }
        Log.i("Final Body Symbl", transcription)
        progressBar.visibility=View.GONE
        binding.showResult.text=transcription
    }
    private fun copy_to_clipboard() {
        val clipboard =
            ContextCompat.getSystemService(
                this,
                ClipboardManager::class.java
            ) as ClipboardManager
        val clip = ClipData.newPlainText("label", transcription)
        clipboard!!.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to Clipboard", Toast.LENGTH_SHORT).show()
    }
    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}