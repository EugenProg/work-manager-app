package kz.just_code.workmanagerapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import kz.just_code.workmanagerapp.databinding.ActivityMainBinding
import java.time.Duration
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val adapter = ImagesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.imagesView.layoutManager = LinearLayoutManager(this)
        binding.imagesView.adapter = adapter

        binding.startBtn.setOnClickListener {
            if (binding.inputView.text.isNullOrBlank()) return@setOnClickListener

            val work = createLoadImageWorker()
            WorkManager.getInstance(this).enqueue(work)

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(work.id).observe(this) {
                Log.e("WorkManager", ">>> ${it.state.name}")
                if (it.state.isFinished) {
                    it.outputData
                        .getString(WorkerKey.SAVED_IMAGE_PATH.name)?.let {

                        }
                    showImages()
                }
            }
        }


        showImages()
    }

    private fun showImages() {
        val preferences = getSharedPreferences("Images list", Context.MODE_PRIVATE)
        val list = Gson().fromJson(preferences.getString(WorkerKey.PHOTO_URL.name, "[]"), Array<String>::class.java)
        adapter.submitList(list.toList())
    }

    private fun createLoadImageWorker(): OneTimeWorkRequest {
        val inputData = Data.Builder()
            .putString(WorkerKey.IMAGE_URL.name, binding.inputView.text.toString())
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        return OneTimeWorkRequestBuilder<LoadImageWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()
    }
}