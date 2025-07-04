package com.twittervideodownloader.video.gif.twitter.save.twittermonkey.ui.alldownloads

import androidx.room.Database
import androidx.room.RoomDatabase
import com.twittervideodownloader.video.gif.twitter.save.twittermonkey.ui.alldownloads.models.DownloadedFileItem


@Database(entities = [DownloadedFileItem::class], version = 1)
abstract class AllDownloadsDatabase : RoomDatabase() {
    abstract fun allDownloadsDao(): AllDownloadsDao
}
