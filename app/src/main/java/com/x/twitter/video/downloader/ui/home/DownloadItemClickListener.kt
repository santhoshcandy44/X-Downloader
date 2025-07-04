package com.twittervideodownloader.video.gif.twitter.save.twittermonkey.ui.home

import com.twittervideodownloader.video.gif.twitter.save.twittermonkey.ui.home.models.DownloadFileItem
import com.twittervideodownloader.video.gif.twitter.save.twittermonkey.ui.home.models.Variant


interface DownloadItemClickListener{

    fun itemClick(url:String, downloadFileItem: DownloadFileItem, variant: Variant)
}
