package com.twittervideodownloader.video.gif.twitter.save.twittermonkey.ui.alldownloads

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.twittervideodownloader.video.gif.twitter.save.twittermonkey.ui.alldownloads.models.DownloadedFileItem


@Dao
interface AllDownloadsDao {

    @Query("SELECT * FROM all_downloads ORDER BY id DESC")
    fun getAll(): LiveData<List<DownloadedFileItem>>

    @Insert
    fun insert(downloadedFileItem: DownloadedFileItem)


    @Query("DELETE FROM all_downloads WHERE id = :userId")
    fun deleteById(userId: Int)


    @Query(
        "UPDATE all_downloads SET " +
                "thumbnailUrl= :thumbnailUrl, " +
                "fileProfileHttpsUrl= :fileProfileHttpsUrl, " +
                "name= :name, " +
                "fullText= :fullText, " +
                "mediaType= :mediaType, " +
                "contentType= :contentType, " +
                "lastModified= :lastModified, " +
                "fileSize= :fileSize, " +
                "absPath= :absPath, " +
                "originalUrl= :originalUrl, " +
                "contentUri= :contentUri " +
                "WHERE id = :userId"
    )
    fun updateById(
        userId: Int,
        thumbnailUrl: String,
        fileProfileHttpsUrl: String,
        name: String,
        fullText: String,
        mediaType: String,
        contentType: String,
        lastModified: Long,
        fileSize: Long,
        absPath: String,
        originalUrl: String,
        contentUri: String?
    )
}