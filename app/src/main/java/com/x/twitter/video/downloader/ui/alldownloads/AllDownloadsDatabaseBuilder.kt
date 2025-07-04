package com.twittervideodownloader.video.gif.twitter.save.twittermonkey.ui.alldownloads

import android.content.Context
import androidx.room.Room

object AllDownloadsDatabaseBuilder {

    private var INSTANCE: AllDownloadsDatabase? = null

    fun getInstance(context: Context): AllDownloadsDatabase {

        if (INSTANCE == null) {
            INSTANCE =
            Room.databaseBuilder(context,
                AllDownloadsDatabase::class.java, "all_downloads_db"
            ).build()

        }

        return INSTANCE!!
    }
}