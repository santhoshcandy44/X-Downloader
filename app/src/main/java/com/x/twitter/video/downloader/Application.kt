package com.x.twitter.video.downloader


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.facebook.ads.AudienceNetworkAds
import com.google.android.gms.ads.MobileAds
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


/*


class AdMetaInfo : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        intent?.let {
            if (intent.action.equals("ad_meta_info_clear")) {

                context?.let {
                    val sharedPreferences =
                        context.getSharedPreferences("app_meta", Context.MODE_PRIVATE)
                    sharedPreferences.edit().apply {

                        putBoolean("day_started", true)
                        //putInt("ad_click_count", 0)
                        commit()
                    }

                    val isAdAlarmScheduledAlarmManager =
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        isAdAlarmScheduledAlarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            Calendar.getInstance().apply {
                                timeInMillis = Calendar.getInstance().apply {
                                    add(Calendar.DATE, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                }.timeInMillis
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }.timeInMillis,
                            PendingIntent.getBroadcast(
                                context,
                                0,
                                Intent(context, AdMetaInfo::class.java).apply {
                                    action = "ad_meta_info_clear"
                                },
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                            )
                        )

                    }

                }

            }

        }
    }

}
 */

class Application : BaseApplication() {


    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        if (AD_TYPE == AdType.ADMOB) {
            if (aicpProtector()) {
                loadAppOpenAdmobAd()
            }
        }
    }



    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(this@Application)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this@Application)

        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Default){

                MobileAds.initialize(this@Application) { }

                if (appOpenAdManager == null) {
                    appOpenAdManager = AppOpenAdManager()
                }

                val adMeta = getSharedPreferences("ad_meta", Context.MODE_PRIVATE)
                adClickCount = adMeta.getInt("ad_click_count", 0)
                banEndTime = adMeta.getLong("ban_end_time", 0)
                val adType = adMeta.getString("ad_type", null)
                val adMetaEditor = adMeta.edit()


                val appMeta = getSharedPreferences("app_meta", Context.MODE_PRIVATE)
                val appMetaEditor = appMeta.edit()

                UPDATE_TYPE = appMeta.getInt("update_type", 1)


                AD_TYPE = if (adType == null) {
                    //default ad type
                    AdType.ADMOB
                } else {
                    AdType.valueOf(adType)
                }

                val appRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

                appRemoteConfig.setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
                        .build()
                )

                appRemoteConfig.setDefaultsAsync(
                    mapOf(
                        "ad_type" to "none",
                        "update_type" to "1"
                    )
                )

                appRemoteConfig.fetchAndActivate()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            if (it.result) {

                                when (appRemoteConfig.getString("update_type")) {
                                    "0" -> {
                                        appMetaEditor.putInt("update_type", 0)
                                        appMetaEditor.commit()
                                        UPDATE_TYPE = 0
                                    }
                                    "1" -> {
                                        appMetaEditor.putInt("update_type", 1)
                                        appMetaEditor.commit()
                                        UPDATE_TYPE = 1

                                    }
                                    else -> {
                                        appMetaEditor.putInt("update_type", 1)
                                        appMetaEditor.commit()
                                        UPDATE_TYPE = 1
                                    }
                                }


                                when (appRemoteConfig.getString("ad_type")) {

                                    "ADMOB" -> {
                                        adMetaEditor.putString("ad_type", AdType.ADMOB.toString())
                                        adMetaEditor.commit()
                                        AD_TYPE = AdType.ADMOB

                                    }
                                    else -> {
                                        adMetaEditor.putString("ad_type", null)
                                        adMetaEditor.commit()
                                        AD_TYPE = null
                                    }
                                }

                            }
                        }
                    }
            }

        }


    }

}