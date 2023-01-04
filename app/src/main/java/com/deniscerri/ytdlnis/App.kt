package com.deniscerri.ytdlnis

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.deniscerri.ytdlnis.util.NotificationUtil
import com.google.android.material.color.DynamicColors
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        notificationUtil = NotificationUtil(this)
        createNotificationChannels()
        PreferenceManager.setDefaultValues(
            this,
            "root_preferences",
            MODE_PRIVATE,
            R.xml.root_preferences,
            false
        )
        configureRxJavaErrorHandler()
        Completable.fromAction { initLibraries() }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : DisposableCompletableObserver() {
                override fun onComplete() {
                    // it worked
                }

                override fun onError(e: Throwable) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "failed to initialize youtubedl-android", e)
                    Toast.makeText(
                        applicationContext,
                        "initialization failed: " + e.localizedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun configureRxJavaErrorHandler() {
        var err = Throwable()
        RxJavaPlugins.setErrorHandler { e: Throwable ->
            if (e is UndeliverableException) {
                // As UndeliverableException is a wrapper, get the cause of it to get the "real" exception
                err = e.cause!!
            }
            if (e is InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return@setErrorHandler
            }
            Log.e(TAG, "Undeliverable exception received, not sure what to do", err)
        }
    }

    @Throws(YoutubeDLException::class)
    private fun initLibraries() {
        YoutubeDL.getInstance().init(this)
        FFmpeg.getInstance().init(this)
        Aria2c.getInstance().init(this)
    }

    private fun createNotificationChannels() {
        notificationUtil.createNotificationChannel()
    }

    companion object {
        private const val TAG = "App"
        @SuppressLint("StaticFieldLeak")
        lateinit var notificationUtil: NotificationUtil
    }
}