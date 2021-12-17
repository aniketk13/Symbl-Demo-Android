package com.example.symbl_demo_android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
    private lateinit var accessToken: String
    private lateinit var progressBar: ProgressBar
    private lateinit var mediaUrl: String
    private lateinit var conversationId: String
    private lateinit var jobId: String
    private var transcription: String = ""
    lateinit var ai: ApplicationInfo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ai=this.packageManager.getApplicationInfo(this.packageName,PackageManager.GET_META_DATA)
        progressBar = binding.progressBar2

//        generating access token to work on symbl.ai models
        generateAccessToken()
        binding.copy.isEnabled = false      //disabling copy button until the result is generated
        binding.submit.setOnClickListener {
            mediaUrl = binding.acceptUrl.text.toString()        //extracting the entered url on button click
            binding.acceptUrl.hideKeyboard()
            progressBar.visibility = View.VISIBLE       //starting the progress bar to begin with the operation
            processVideo()      //calling processVideo() to generate the conversation id for the api request
        }
        binding.copy.setOnClickListener {
            copy_to_clipboard()         //copy the transcription to keyboard on button click
        }
    }


    private fun generateAccessToken() {         //generating the access token for symbl.ai
        val parameters = JSONObject()
        parameters.put("type", "application")
        parameters.put("appId", "${ai.metaData["SymblAppId"]}")
        parameters.put(
            "appSecret",
            "${ai.metaData["SymblAppSecret"]}"
        )

        val queue = Volley.newRequestQueue(this)        //writing the volley request to initiate the api call
        val req = JsonObjectRequest(
            Request.Method.POST, "https://api.symbl.ai/oauth2/token:generate", parameters,
            {
                accessToken = it.getString("accessToken")
                Log.i("accessToken", accessToken)
                progressBar.visibility = View.GONE          //stopping progress bar as the access token has been generated
            }, {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
            })
        queue.add(req)
    }

    private fun processVideo() {            //processing the video and generating its conversation as well as job id
        Log.i("in", "hello")
        val body = JSONObject()         //making the json body which is to be passed in the api call
        body.put("url", mediaUrl)
        body.put("name", "General")

        val queue = Volley.newRequestQueue(this)        //writing the api request for processing the video
        val req = object : JsonObjectRequest(
            Method.POST, "https://api.symbl.ai/v1/process/video/url", body,
            {

                //getting the conversation id
                conversationId = it.getString("conversationId")
                jobId = it.getString("jobId")

                Log.i("VideoProcess", conversationId)
                Log.i("VideoProcess", jobId)

                getStatusSymblResponse()            //calling getStatusSymblResponse as webhook to check if the job has been completed or not
            }, {
                Toast.makeText(this, "Error in video", Toast.LENGTH_SHORT).show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {         //passing the required headers along with the api call
                val headers = HashMap<String, String>()
                headers.put("Authorization", "Bearer $accessToken")
                headers.put("Content-Type", "application/json")
                return headers
            }
        }
        queue.add(req)
    }

    //webhook implemented for Symbl.ai
    private fun getStatusSymblResponse() {          //this function acts as a webhook to check if the job has been completed or not

        val queue = Volley.newRequestQueue(this)        //writing api request for job status check
        val request = object : JsonObjectRequest(
            Method.GET, "https://api.symbl.ai/v1/job/$jobId", null, {
                val status = it.getString("status")
                if (status == "completed") {
                    getResponseFromSymbl()          //if the job is completed proceed to getResponseFromSymbl() to extract the response
                } else {
                    val handler = Handler(Looper.getMainLooper())
                    Handler().postDelayed({
                        getStatusSymblResponse()        //if the job is not completed, call the api again after 3 sec delay
                    }, 3000)
                }
            }, {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {         //passing necessary headers along with the api call
                val headers = HashMap<String, String>()
                headers.put("Authorization", "Bearer $accessToken")
                return headers
            }
        }
        queue.add(request)
    }

    private fun getResponseFromSymbl() {        //extracting the response from api call

        val queue = Volley.newRequestQueue(this)        //wriitng the api request to extract the response body
        val req = object : JsonObjectRequest(
            Method.GET, "https://api.symbl.ai/v1/conversations/$conversationId/messages", null,
            {
                Log.i("VideoProcess", "Response generated")
                Log.i("VideoProcess", it.toString())
                textOutputSymbl(it)     //passing the response body to segregate our transcription separately
            }, {
                Toast.makeText(this, "Error in video", Toast.LENGTH_SHORT).show()
            }) {
            override fun getHeaders(): MutableMap<String, String> {         //passing the necessary headers along with the api call
                val headers = HashMap<String, String>()
                headers.put("Authorization", "Bearer $accessToken")
                headers.put("Content-Type", "application/json")
                return headers
            }
        }
        queue.add(req)
    }

    private fun textOutputSymbl(response: JSONObject?) {        //function to generate the complete transcription of the video url provided
        Log.i("response", response.toString())
        val messages = response?.getJSONArray("messages")
        if (messages != null) {
            for (i in 0 until messages.length()) {
                val obj = messages.getJSONObject(i)
                val text = obj.getString("text")
                transcription = "$transcription$text "      //concatenating all the dialogues in the video
                binding.copy.isEnabled = true
            }
        }
        Log.i("Final Body Symbl", transcription)
        progressBar.visibility = View.GONE          //stopping the progress bar as the task has been completed
        binding.showResult.text = transcription         //showing the transcription to window
    }

    private fun copy_to_clipboard() {           //function to copy the given transcription to clipboard
        val clipboard =
            ContextCompat.getSystemService(
                this,
                ClipboardManager::class.java
            ) as ClipboardManager
        val clip = ClipData.newPlainText("label", transcription)        //copying the generated transcription
        clipboard!!.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to Clipboard", Toast.LENGTH_SHORT).show()
    }

    fun View.hideKeyboard() {       //function to close the keyboard after submit button has been pressed to initiate the job
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}