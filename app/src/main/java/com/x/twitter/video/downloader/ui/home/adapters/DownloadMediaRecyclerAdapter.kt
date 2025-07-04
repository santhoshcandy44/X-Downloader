package com.x.twitter.video.downloader.ui.home.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexboxLayout
import com.x.twitter.video.downloader.R
import com.x.twitter.video.downloader.TwitterMonkeyAPI
import com.x.twitter.video.downloader.ui.home.DownloadItemClickListener
import com.x.twitter.video.downloader.ui.home.NetworkConnectionInterceptor
import com.x.twitter.video.downloader.ui.home.NoConnectivityException
import com.x.twitter.video.downloader.ui.home.models.DownloadFileItem
import com.x.twitter.video.downloader.utils.humanReadableByteCountBin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import java.net.ConnectException
import java.net.URL
import java.util.ArrayList

class DownloadMediaRecyclerAdapter(private val downloadDialog: AlertDialog,private val context: Context, private val downloadFileItems: ArrayList<DownloadFileItem>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var downloadItemClickListener: DownloadItemClickListener


    fun setDownloadItemListener(downloadItemClickListener: DownloadItemClickListener){
        this.downloadItemClickListener=downloadItemClickListener
    }

    inner class VH(itemView: View): RecyclerView.ViewHolder(itemView)
    inner class VH1(itemView: View): RecyclerView.ViewHolder(itemView)




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType==VIEW_TYPE_HEADER){
            VH(
                LayoutInflater.from(context).inflate(
                R.layout.download_item_header_view,parent,false
            ))
        }else{
            VH1(
                LayoutInflater.from(context).inflate(
                R.layout.media_download_item,parent,false
            ))
        }
    }

    private val VIEW_TYPE_HEADER:Int=0
    private val VIEW_TYPE_ITEM:Int=1

    override fun getItemViewType(position: Int): Int {
        return if(position==0){
            VIEW_TYPE_HEADER
        }else{
            VIEW_TYPE_ITEM
        }

    }



    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {


        if(holder is VH1){

            val item= downloadFileItems[position-1]
            val dlMediaThumbnail=holder.itemView.findViewById<ImageView>(R.id.media_download_thumbnail)
            val thumbnail=item.thumbnail

            Glide.with(dlMediaThumbnail)
                .load(thumbnail)
                .centerCrop()
                .into(dlMediaThumbnail)

            val mediaType=holder.itemView.findViewById<ImageView>(R.id.media_download_media_type)
            if(item.mediaType=="video"){
                Glide.with(mediaType)
                    .load(R.drawable.ic_type_video)
                    .centerCrop()
                    .into(mediaType)
            }else if(item.mediaType=="animated_gif"){
                Glide.with(mediaType)
                    .load(R.drawable.ic_type_gif)
                    .centerCrop()
                    .into(mediaType)
            }

            val fullText=holder.itemView.findViewById<TextView>(R.id.media_download_full_text)
            fullText.text=item.fullText


            val profile=holder.itemView.findViewById<ImageView>(R.id.media_download_profile)

            Glide.with(profile)
                .load(item.profileImageUrlHttps)
                .circleCrop()
                .into(profile)

            val name=holder.itemView.findViewById<TextView>(R.id.media_download_name)
            name.text=item.name

            val dlMediaGridLayout=holder.itemView.findViewById<FlexboxLayout>(R.id.media_download_options_layout)

            if(dlMediaGridLayout.childCount>0){
                dlMediaGridLayout.removeAllViews()
            }
            if(item.mediaType=="video"){
                for (variant in item.variants){

                    val dlMediaResolutionView= LayoutInflater.from(holder.itemView.context).inflate(
                        R.layout.media_download_variant_item_video,dlMediaGridLayout,false
                    )
                    dlMediaResolutionView.setOnClickListener {
                        val downloadURL = variant.url
                        downloadItemClickListener.itemClick(downloadURL,item,variant)

                    }

                    val oktHttpClient = OkHttpClient.Builder()
                        .apply {
                            addInterceptor(NetworkConnectionInterceptor(context))

                        }
                    // Adding NetworkConnectionInterceptor with okHttpClientBuilder.

                    val downloadURL = URL(variant.url)
                    val baseURL = downloadURL.protocol + "://" + downloadURL.host
                    val retrofit = Retrofit.Builder()
                        .baseUrl(baseURL)
                        .client(oktHttpClient.build())
                        .build()


                    val retrofitInterface =
                        retrofit.create(TwitterMonkeyAPI::class.java)


                    val request: Call<ResponseBody> = retrofitInterface
                        .downloadFile(variant.contentType,downloadURL.path)


                    if(variant.fileSize<=0){
                        GlobalScope.launch {

                            try{
                                val response=request.execute()
                                val contentLength=response.body()!!.contentLength()

                                launch(Dispatchers.Main) {

                                    val mediaSize=dlMediaResolutionView.findViewById<TextView>(R.id.video_variant_media_size)
                                    mediaSize.text= humanReadableByteCountBin(contentLength)

                                }

                            }catch(e:Exception){

                                if( e is ConnectException || e is NoConnectivityException){
                                    launch(Dispatchers.Main) {

                                        if(downloadDialog.isShowing){
                                            downloadDialog.dismiss()
                                        }

                                        val alertDialog= AlertDialog.Builder(context
                                        )

                                        alertDialog.setTitle("Network Error")
                                        alertDialog.setMessage("Check your network or internet data connection...")
                                        alertDialog.setCancelable(true)
                                        alertDialog.setPositiveButton("Dismiss",object: DialogInterface.OnClickListener{

                                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                                dialog!!.dismiss()
                                            }
                                        })
                                        alertDialog.show()
                                    }

                                }
                            }

                        }
                    }else{
                        val mediaSize=dlMediaResolutionView.findViewById<TextView>(R.id.video_variant_media_size)
                        mediaSize.text= humanReadableByteCountBin(variant.fileSize)
                    }





                    val quality=dlMediaResolutionView.findViewById<TextView>(R.id.video_variant_quality)
                    quality.text=variant.quality
                    val resolution=dlMediaResolutionView.findViewById<TextView>(R.id.video_variant_media_resolution)
                    resolution.text=variant.resolution
                    dlMediaGridLayout.addView(dlMediaResolutionView)


                }

            }else{
                for (variant in item.variants){
                    val dlMediaResolutionView= LayoutInflater.from(holder.itemView.context).inflate(
                        R.layout.media_download_variant_item_gif,dlMediaGridLayout,false
                    )
                    dlMediaResolutionView.setOnClickListener {
                        val downloadURL = variant.url
                        downloadItemClickListener.itemClick(downloadURL,item,variant)

                    }
                    val mediaSize=dlMediaResolutionView.findViewById<TextView>(R.id.gif_variant_media_size)

                    mediaSize.text= humanReadableByteCountBin(variant.fileSize)
                    dlMediaGridLayout.addView(dlMediaResolutionView)

                }

            }

        }

    }



    override fun getItemCount(): Int {

        return if (downloadFileItems.size>0){
            downloadFileItems.size+1
        }else{
            0
        }
    }


}
