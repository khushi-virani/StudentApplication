package com.example.studentapplication

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StudentApp : Application(), Configuration.Provider {

    // Hilt provides this automatically
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    //Required for HiltWorker to work
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}