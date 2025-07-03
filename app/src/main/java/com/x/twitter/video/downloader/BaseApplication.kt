package com.x.twitter.video.downloader

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.*


open class BaseApplication : Application(), Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {



    fun increaseAdClickCount() {
        val sharedPreferences = getSharedPreferences("ad_meta", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putInt(
                "ad_click_count", sharedPreferences.getInt(
                    "ad_click_count", 0
                ) + 1
            )
            commit()
        }
    }

    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    enum class AdType {
        ADMOB
    }

    class AppOpenAdManager {
        private var loadTime: Long = 0
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false


        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        private fun isAdAvailable(): Boolean {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        }



        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener
        ) {
            if (isShowingAd) {

                Log.d("TAG_123", "The app open ad is already showing.")
                return
            }

            if (!isAdAvailable()) {
                Log.d("TAG_123", "The app open ad is not ready yet.")
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
                return
            }


            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdClicked() {
                    super.onAdClicked()
                    val app =
                        ((activity.application) as com.x.twitter.video.downloader.Application)
                    app.increaseAdClickCount()


                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d("TAG_123", "Ad dismissed fullscreen content.")
                    appOpenAd = null
                    isShowingAd = false

                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {

                    Log.d("TAG_123", adError.message)
                    appOpenAd = null
                    isShowingAd = false

                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d("123", "Ad showed fullscreen content.")
                }
            }
            isShowingAd = true
            appOpenAd?.show(activity)
        }


        private fun loadAd(context: Context) {

            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request: AdRequest = AdRequest.Builder().build()
            AppOpenAd.load(
                context,context.resources.
                getString(R.string.ADMOB_APP_OPEN), request,

                object : AppOpenAd.AppOpenAdLoadCallback() {

                    override fun onAdLoaded(ad: AppOpenAd) {
                        // Called when an app open ad has loaded.
                        Log.d("TAG_123", "Ad was loaded.")
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time

                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Called when an app open ad has failed to load.
                        Log.d("TAG_123", loadAdError.message)
                        isLoadingAd = false
                    }
                })
        }


    }


    var currentActivity: Activity? = null

    var AD_TYPE: AdType? = null

    var UPDATE_TYPE: Int = 1

    var appOpenAdManager: AppOpenAdManager? = null


    var adClickCount: Int = 0

    //var dayStarted:Boolean=true
    var MAX_AD_CLICK_COUNT = 4
    var banEndTime: Long = 0

    fun aicpProtector(): Boolean {
        val sp = getSharedPreferences(
            "ad_meta",
            Context.MODE_PRIVATE
        )

        val c0 = sp.getInt("ad_click_count", 0)
        val c1 = sp.getLong("ban_end_time", 0)

        return (c0 <= MAX_AD_CLICK_COUNT
                && System.currentTimeMillis() > c1)

    }

    fun loadAppOpenAdmobAd() {



        currentActivity?.let {

              if (currentActivity is MainActivity) {

                appOpenAdManager!!.showAdIfAvailable(it,

                    object : OnShowAdCompleteListener {
                        override fun onShowAdComplete() {
                            Log.d("TAG_123", "App Open Loaded")
                        }

                    }
                )
        }
        }

    }


    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {

        appOpenAdManager?.let {
            if (!appOpenAdManager!!.isShowingAd) {
                currentActivity = activity
            }
        }

    }

    override fun onActivityResumed(activity: Activity) {

    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }


}