package com.x.twitter.video.downloader.ui.alldownloads

import androidx.room.Database
import androidx.room.RoomDatabase
import com.x.twitter.video.downloader.ui.alldownloads.models.DownloadedFileItem


@Database(entities = [DownloadedFileItem::class], version = 1)
abstract class AllDownloadsDatabase : RoomDatabase() {
    abstract fun allDownloadsDao(): AllDownloadsDao
}
