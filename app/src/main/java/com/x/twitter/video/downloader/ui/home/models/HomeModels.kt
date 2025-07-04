package com.twittervideodownloader.video.gif.twitter.save.twittermonkey.ui.home.models

import com.google.gson.annotations.SerializedName
import java.util.ArrayList


data class ErrorData(@SerializedName("error")
                     val error:Boolean,
                     @SerializedName("message")
                     val message:String)

data class DownloadFileItem(
    @SerializedName("thumbnail") val thumbnail:String,
    @SerializedName("media_type") val mediaType:String,
    @SerializedName("name") val name:String,
    @SerializedName("full_text") val fullText:String,
    @SerializedName("profile_image_url_https") val profileImageUrlHttps:String,
    @SerializedName("variants") val variants: ArrayList<Variant>
)

data class Variant(
    @SerializedName("bitrate") val bitrate:Long,
    @SerializedName("file_size") val fileSize:Long,
    @SerializedName("content_type") val contentType:String,
    @SerializedName("url") val url:String,
    @SerializedName("resolution") val resolution:String,
    @SerializedName("quality") val quality:String)


