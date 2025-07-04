package com.twittervideodownloader.video.gif.twitter.save.twittermonkey.ui.alldownloads.models

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "all_downloads")
data class DownloadedFileItem(
    @PrimaryKey(autoGenerate = true) val id:Int=0,
    val thumbnailUrl:String,
    val fileProfileHttpsUrl:String,
    val name:String,
    val fullText:String,
    val mediaType:String,
    val contentType:String,
    val lastModified:Long,
    val fileSize: Long,
    val absPath:String,
    val originalUrl:String,
    val contentUri:String?
    )