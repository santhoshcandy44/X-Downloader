package com.x.twitter.video.downloader

import com.x.twitter.video.downloader.ui.home.models.DownloadFileItem
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


interface TwitterMonkeyAPI{

    @GET("/x-dl-api.php")
    fun getData(@Query(value="url",encoded=false) url: String): Call<List<DownloadFileItem>>


    @Headers("Accept-Encoding:*")
    @Streaming
    @GET
    fun downloadFile(@Header("Content-Type") contentType:String , @Url file_url:String): Call<ResponseBody>
}


