package com.x.twitter.video.downloader.ui.home

import com.x.twitter.video.downloader.ui.home.models.DownloadFileItem
import com.x.twitter.video.downloader.ui.home.models.Variant


interface DownloadItemClickListener{

    fun itemClick(url:String, downloadFileItem: DownloadFileItem, variant: Variant)
}
