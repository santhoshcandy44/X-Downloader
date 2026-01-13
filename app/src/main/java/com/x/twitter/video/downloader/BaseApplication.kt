package com.x.twitter.video.downloader

import android.app.Application
import android.content.Context

open class BaseApplication : Application(){

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

    var UPDATE_TYPE: Int = 1
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
}