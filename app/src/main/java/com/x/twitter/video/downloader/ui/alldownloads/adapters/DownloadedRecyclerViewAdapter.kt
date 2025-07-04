package com.x.twitter.video.downloader.ui.alldownloads.adapters

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.x.twitter.video.downloader.*
import com.x.twitter.video.downloader.ui.alldownloads.AllDownloadsDatabaseBuilder
import com.x.twitter.video.downloader.ui.alldownloads.AllDownloadsFragment
import com.x.twitter.video.downloader.ui.alldownloads.models.DownloadedFileItem
import com.x.twitter.video.downloader.ui.home.NetworkConnectionInterceptor
import com.x.twitter.video.downloader.ui.home.NoConnectivityException
import com.x.twitter.video.downloader.ui.home.PermissionGrantedListener
import com.x.twitter.video.downloader.utils.generateUniqueFileName
import com.x.twitter.video.downloader.utils.humanReadableByteCountBin
import com.x.twitter.video.downloader.utils.toast
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.io.*
import java.net.ConnectException
import java.net.URL
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*


class DownloadedRecyclerViewAdapter(
    private val app: Application,
    private val fragment: AllDownloadsFragment,
    private val activity: Activity,
    private val context: Context, private val items: ArrayList<DownloadedFileItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "TAG_123"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return VH(
            (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                    as LayoutInflater).inflate(R.layout.downloaded_item, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        po: Int
    ) {

        val vh = holder as VH
        val position = vh.bindingAdapterPosition
        val item = items[position]
        val downloadedThumbnail = vh.itemView.findViewById<ImageView>(R.id.downloaded_thumbnail)
        val downloadedProfile = vh.itemView.findViewById<ImageView>(R.id.downloaded_profile)
        val downloadedName = vh.itemView.findViewById<TextView>(R.id.downloaded_name)
        val downloadedFullText = vh.itemView.findViewById<TextView>(R.id.downloaded_full_text)
        val downloadedMediaType = vh.itemView.findViewById<ImageView>(R.id.downloaded_media_type)
        val downloadedCreatedDate = vh.itemView.findViewById<TextView>(R.id.downloaded_created_date)
        val downloadedFileSize = vh.itemView.findViewById<TextView>(R.id.downloaded_file_size)
        val moreOptions = vh.itemView.findViewById<ImageView>(R.id.more_option)

        val file = File(items[position].absPath)
        val isExists = File(items[position].absPath).exists()
        moreOptions.setOnClickListener {
            val isExists2 = File(items[position].absPath).exists()
            if (isExists2) {

                val popUpMenu = PopupMenu(
                    context, moreOptions
                )
                popUpMenu.inflate(R.menu.more_options)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    popUpMenu.setForceShowIcon(true)
                }
                popUpMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.more_option_share -> {

                            MediaScannerConnection
                                .scanFile(
                                    context,
                                   arrayOf(items[position].absPath),
                                    null
                                ) { path, uri ->

                                    val shareIntent = Intent(
                                        Intent.ACTION_SEND
                                    ).apply {
                                         if (items[position].mediaType == "video") {
                                                type = "video/*"
                                         }else{
                                                type = "video/*"
                                         }

                                        putExtra(Intent.EXTRA_STREAM, uri)

                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent, "Share via..."
                                        )
                                    )
                                }

                            true
                        }


                        R.id.more_option_delete -> {


                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                                if(File(items[position].absPath).exists()
                                ){
                                    item.contentUri?.let {
                                        context.contentResolver.delete(
                                            Uri.parse(it), null
                                        )
                                        context.contentResolver.notifyChange(
                                            Uri.parse(it), null
                                        )
                                    }

                                    deleteFile(item.id)


                                }else{
                                    notifyItemChanged(position)

                                }



                            } else {

                                if (fragment.checkPermission(context)) {


                                    if(File(items[position].absPath).exists()){
                                        file.delete()
                                        MediaScannerConnection.scanFile(
                                            context,
                                            arrayOf(file.absolutePath), null, null
                                        )

                                        deleteFile(item.id)
                                    }else{
                                        notifyItemChanged(position)
                                    }


                                } else {


                                    toast(
                                        context, "Storage Permission Needed..."
                                    )

                                    fragment.registerPermissionGrantedListener(object :
                                        PermissionGrantedListener {
                                        override fun onPermissionGranted() {
                                            toast(
                                                context,
                                                "Successfully Storage Permission Granted..."
                                            )

                                            if (file.exists())
                                                file.delete()
                                            MediaScannerConnection.scanFile(
                                                context,
                                                arrayOf(file.absolutePath), null, null
                                            )

                                            deleteFile(item.id)


                                        }

                                    })
                                    fragment.requestPermission()

                                }

                            }



                            true
                        }

                        else -> true
                    }


                }
                popUpMenu.show()


            } else {
                Toast.makeText(
                    context, "File might be deleted, so remove or re-download it",
                    Toast.LENGTH_SHORT
                ).show()


                val popUpMenu = PopupMenu(
                    context, moreOptions
                )
                popUpMenu.inflate(R.menu.more_options_action)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    popUpMenu.setForceShowIcon(true)
                }
                popUpMenu.setOnMenuItemClickListener {
                    when (it.itemId) {

                        R.id.more_option_remove-> {
                            deleteFile(item.id)
                            true
                        }

                        else -> true
                    }


                }
                popUpMenu.show()
                notifyItemChanged(position)


            }


        }

        val downloadedAlphaContainer =
            vh.itemView.findViewById<FrameLayout>(R.id.downloaded_alpha_container)
        val downloadedCloudImage = vh.itemView.findViewById<ImageView>(R.id.cloud_download)
        val downloadedProgressBarContainer =
            vh.itemView.findViewById<FrameLayout>(R.id.downloaded_progress_bar_container)

        if (downloadedAlphaContainer.visibility == View.VISIBLE) {
            downloadedAlphaContainer.alpha = 1.0f
            downloadedAlphaContainer.visibility = View.GONE
        }

        if (downloadedCloudImage.visibility == View.VISIBLE) {
            downloadedCloudImage.visibility = View.GONE
        }


        vh.itemView.setOnClickListener {
            val fileExists = File(items[position].absPath).exists()

            if (fileExists) {

                val intent = Intent(context, PlayerActivity::class.java)
                intent.data = Uri.parse(items[position].absPath)
                fragment.playerActivityLauncher.launch(intent)
                val app = activity.application as Application
                if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {
                    if (app.aicpProtector()) {
                        fragment.allDownloadsPlayerActivityFinishedLoadInterstitialAd()

                    }
                }
            } else {
                Toast.makeText(
                    context, "File might be deleted, re-download it", Toast.LENGTH_SHORT
                ).show()
                notifyItemChanged(position)
            }
        }

        if (!isExists) {
            Log.d("TAG_123", "Not Exist" + items[position].toString())


            downloadCall(
                app,
                activity,
                false,
                vh.itemView,
                downloadedAlphaContainer,
                downloadedProgressBarContainer, downloadedCloudImage, position
            )
        }




        Glide.with(context)
            .load(items[position].thumbnailUrl)
            .centerCrop()
            .into(downloadedThumbnail)

        Glide.with(context)
            .load(items[position].fileProfileHttpsUrl)
            .circleCrop()
            .circleCrop()
            .into(downloadedProfile)

        downloadedName.text = items[position].name

        downloadedFullText.text = items[position].fullText

        if (items[position].mediaType == "video") {
            Glide.with(context)
                .load(R.drawable.ic_type_video)
                .circleCrop()
                .into(downloadedMediaType)
        } else {
            Glide.with(context)
                .load(R.drawable.ic_type_gif)
                .circleCrop()
                .into(downloadedMediaType)
        }

        val lastModifiedDate = items[position].lastModified

        val resultLastModifiedDate = SimpleDateFormat("dd/MM/yyyy").format(
            Date(
                lastModifiedDate
            )
        )

        downloadedCreatedDate.text = resultLastModifiedDate
        downloadedFileSize.text = humanReadableByteCountBin(items[position].fileSize)


    }


    private fun downloadCall(
        app: Application,
        activity: Activity,
        isRefreshed: Boolean,
        itemView: View, downloadedAlphaContainer: FrameLayout,
        downloadedProgressBarContainer: FrameLayout,
        downloadedCloudImage: ImageView, position: Int
    ) {

        if (downloadedAlphaContainer.visibility == View.GONE) {
            downloadedAlphaContainer.alpha = 1.0f
            downloadedAlphaContainer.visibility = View.VISIBLE
        }

        if (downloadedCloudImage.visibility == View.GONE) {
            downloadedCloudImage.visibility = View.VISIBLE
        }


        downloadedCloudImage.setOnClickListener {
            val loadingDialog = getLoadingProgressBarDialog()
            if (!loadingDialog.isShowing) {
                loadingDialog.show()
            }

            if (fragment.checkPermission(context)) {


                val requireContext = context

                 fragment.lifecycleScope.launch(Dispatchers.IO) {

                    try {


                        val downloadedUrl = URL(items[position].originalUrl)

                        val oktHttpClient = OkHttpClient.Builder()
                            .apply {
                                addInterceptor(NetworkConnectionInterceptor(requireContext))
                            }

                        // Adding NetworkConnectionInterceptor with okHttpClientBuilder.

                        val baseURL = downloadedUrl.protocol + "://" + downloadedUrl.host
                        val retrofit = Retrofit.Builder()
                            .baseUrl(baseURL)
                            .client(oktHttpClient.build())
                            .build()

                        val retrofitInterface =
                            retrofit.create(TwitterMonkeyAPI::class.java)

                        val request = retrofitInterface
                            .downloadFile(
                                items[position].contentType,
                                downloadedUrl.path
                            )


                        val response = request.execute()


                        val isDownloaded: Boolean

                       launch(Dispatchers.Main){
                            if (loadingDialog.isShowing) {
                                loadingDialog.dismiss()
                            }
                        }


                        isDownloaded = downloadFile(
                            activity,
                            context,
                            items[position].fileSize,
                            items[position],
                            itemView, response.body()!!
                        )



                        if (!isDownloaded) {
                            launch(Dispatchers.Main) {

                                if (downloadedAlphaContainer.visibility == View.GONE) {
                                    downloadedAlphaContainer.visibility = View.VISIBLE
                                }
                                downloadedAlphaContainer.alpha = 1.0f

                                if (downloadedProgressBarContainer.visibility == View.VISIBLE) {
                                    downloadedProgressBarContainer.visibility = View.GONE

                                }

                                if (downloadedCloudImage.visibility == View.GONE) {
                                    downloadedCloudImage.visibility = View.VISIBLE
                                }

                                downloadedCloudImage.visibility = View.GONE

                            }

                            downloadCall(
                                app,
                                activity,
                                true,
                                itemView, downloadedAlphaContainer,
                                downloadedProgressBarContainer,
                                downloadedCloudImage,
                                position
                            )

                        } else {
                            launch(Dispatchers.Main) {
                                if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {

                                    MobileAds.initialize(
                                        requireContext
                                    ) { }
                                    val adRequest: AdRequest = AdRequest.Builder().build()

                                    InterstitialAd.load(requireContext,
                                        requireContext.getString(
                                            R.string.ADMOB_ALL_DOWNLOADS_INTERSTITIAL_VIDEO_PLAYER
                                        ),
                                        adRequest,
                                        object : InterstitialAdLoadCallback() {
                                            override fun onAdLoaded(interstitialAd: InterstitialAd) {

                                                mInterstitialAd = interstitialAd


                                                mInterstitialAd!!.show(activity)
                                                mInterstitialAd!!.fullScreenContentCallback =
                                                    object : FullScreenContentCallback() {
                                                        override fun onAdClicked() {
                                                            Log.d(TAG, "Ad was clicked.")
                                                        }

                                                        override fun onAdDismissedFullScreenContent() {
                                                            Log.d(
                                                                TAG,
                                                                "Ad dismissed fullscreen content."
                                                            )
                                                            mInterstitialAd = null
                                                        }

                                                        override fun onAdFailedToShowFullScreenContent(
                                                            p0: com.google.android.gms.ads.AdError
                                                        ) {
                                                            Log.e(
                                                                TAG,
                                                                "Ad failed to show fullscreen content."
                                                            )
                                                            mInterstitialAd = null
                                                        }

                                                        override fun onAdImpression() {
                                                            Log.d(TAG, "Ad recorded an impression.")
                                                        }

                                                        override fun onAdShowedFullScreenContent() {
                                                            Log.d(
                                                                TAG,
                                                                "Ad showed fullscreen content."
                                                            )
                                                        }
                                                    }


                                                Log.i(TAG, "onAdLoaded")
                                            }

                                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                                Log.d(TAG, loadAdError.toString())
                                                mInterstitialAd = null
                                            }
                                        })
                                }
                            }


                        }

                    } catch (e: Exception) {

                        launch(Dispatchers.Main) {
                            if (loadingDialog.isShowing) {
                                loadingDialog.dismiss()
                            }
                            if (isRefreshed) {
                                if (downloadedCloudImage.visibility == View.GONE) {
                                    downloadedCloudImage.visibility = View.VISIBLE
                                }

                            }
                        }

                        if (e is UnknownHostException || e is ConnectException || e is NoConnectivityException) {
                            launch(Dispatchers.Main) {
                                val alertDialog = AlertDialog.Builder(
                                    context
                                )

                                alertDialog.setTitle("Network Error")
                                alertDialog.setMessage("Check your network or internet data connection...")
                                alertDialog.setCancelable(true)
                                alertDialog.setPositiveButton(
                                    "Dismiss",
                                ) { dialog, which -> dialog!!.dismiss() }
                                alertDialog.show()
                            }
                        }

                    }
                }
            } else {


                toast(
                    context, "Storage Permission Needed..."
                )

                fragment.registerPermissionGrantedListener(
                    object : PermissionGrantedListener {
                        override fun onPermissionGranted() {
                            downloadedCloudImage.visibility = View.GONE
                            downloadCall(
                                app,
                                activity,
                                true,
                                itemView, downloadedAlphaContainer,
                                downloadedProgressBarContainer, downloadedCloudImage,
                                position
                            )
                        }

                    }

                )


                fragment.requestPermission()
            }

        }
    }


    private fun downloadFile(
        activity: Activity,
        requireContext: Context,
        fileSize: Long,
        item: DownloadedFileItem,
        itemView: View,
        body: ResponseBody,
    ): Boolean {

        val output: OutputStream
        val fileLastModifiedDate: Long
        var outputFileUri: String? = null
        var savedUri: Uri? = null



        try {

            activity.runOnUiThread {

                val downloadedCloudImage = itemView.findViewById<ImageView>(
                    R.id.cloud_download
                )
                if (downloadedCloudImage.visibility == View.VISIBLE) {
                    downloadedCloudImage.visibility = View.GONE
                }
                val downloadedProgressBarContainer = itemView.findViewById<FrameLayout>(
                    R.id.downloaded_progress_bar_container
                )

                if (downloadedProgressBarContainer.visibility == View.GONE) {

                    downloadedProgressBarContainer.visibility = View.VISIBLE
                }

                itemView.findViewById<TextView>(
                    R.id.downloaded_file_size
                ).text = humanReadableByteCountBin(fileSize)

            }




            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {

                val folderName = "X Monkey"
                val file = File("${Environment.getExternalStorageDirectory()}/$folderName")

                if (!file.exists()) {
                    createFolder()
                }

                val fileFormatList = item.contentType.split("/")


                var uniqueFileName = generateUniqueFileName(fileFormatList[fileFormatList.size - 1])
                var outputFile =
                    File("${Environment.getExternalStorageDirectory()}/$folderName/$uniqueFileName")

                while (outputFile.exists()) {
                    uniqueFileName = generateUniqueFileName(fileFormatList[fileFormatList.size - 1])
                    outputFile =
                        File("${Environment.getExternalStorageDirectory()}/$folderName/$uniqueFileName")

                }
                output= FileOutputStream(outputFile)
                outputFileUri = outputFile.absolutePath


            } else {

                val resolver = requireContext.contentResolver
                val values = ContentValues()

                val ext: String = if (item.mediaType == "video") {
                    val fileFormatList = item.contentType.split("/")
                    fileFormatList[fileFormatList.size - 1]


                } else {
                    val fileFormatList = item.contentType.split("/")
                    fileFormatList[fileFormatList.size - 1]
                }
                val folderName = "X Monkey"
                var uniqueFileName = generateUniqueFileName(ext)



                if (item.mediaType == "video") {
                    var outputFile =
                        File("${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_MOVIES}/$folderName/$uniqueFileName")

                    while (outputFile.exists()) {
                        uniqueFileName = generateUniqueFileName(ext)
                        outputFile =
                            File("${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_MOVIES}/$folderName/$uniqueFileName")

                    }
                    values.put(MediaStore.Video
                        .Media
                        .DISPLAY_NAME, uniqueFileName)
                    values.put(MediaStore.Video
                        .Media
                        .MIME_TYPE, "video/mp4")
                    values.put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/$folderName/"
                    )



                } else {

                    var outputFile =
                        File("${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_DOWNLOADS}/$folderName/$uniqueFileName")

                    while (outputFile.exists()) {
                        uniqueFileName = generateUniqueFileName(ext)
                        outputFile =
                            File("${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_DOWNLOADS}/$folderName/$uniqueFileName")

                    }
                    values.put(MediaStore.Downloads.MIME_TYPE, "video/mp4")
                    values.put(MediaStore.Downloads.DISPLAY_NAME, uniqueFileName)
                    values.put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/$folderName/"
                    )

                }


                val contentUri: Uri = if (item.mediaType == "video") {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI

                } else {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI

                    // MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                }

                savedUri = resolver.insert(contentUri, values)!!
                output=resolver.openOutputStream(savedUri)!!


                val cursor: Cursor?
                val proj = arrayOf(MediaStore.Video.Media.DATA)
                cursor =context.contentResolver.query(savedUri, proj, null, null, null)
                val columnIndex =
                    cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                cursor.moveToFirst()
                outputFileUri = cursor.getString(columnIndex)
                cursor.close()

            }


            var count: Int
            val data = ByteArray(1024)

            val bis: InputStream = BufferedInputStream(body.byteStream(), 1024)


            var total: Long = 0
            while (bis.read(data).also { count = it } != -1) {
                total += count.toLong()

                val progress = (total * 100 / fileSize).toInt()

                activity.runOnUiThread {
                    val progressBar = itemView.findViewById<ProgressBar>(
                        R.id.downloaded_progress_bar
                    )
                    progressBar.progress = progress
                    val progress_text = itemView.findViewById<TextView>(
                        R.id.downloaded_progress_percentage
                    )
                    progress_text.text = "$progress%"
                }



                if (progress % 10 == 0) {

                    val alphaContainer = itemView.findViewById<FrameLayout>(
                        R.id.downloaded_alpha_container
                    )
                    activity.runOnUiThread {
                        alphaContainer.alpha = (100 - progress).toFloat().div(100)

                    }
                }

                output.write(data, 0, count)
            }
            output.flush()
            output.close()
            bis.close()



            activity.runOnUiThread {
                val downloadedProgressBarContainer = itemView.findViewById<FrameLayout>(
                    R.id.downloaded_progress_bar_container
                )
                if (downloadedProgressBarContainer.visibility == View.VISIBLE) {
                    downloadedProgressBarContainer.visibility = View.GONE
                }
            }


            fileLastModifiedDate = File(outputFileUri!!).lastModified()

            activity.runOnUiThread {

                val resultLastModifiedDate = SimpleDateFormat("dd/MM/yyyy").format(
                    Date(
                        fileLastModifiedDate
                    )
                )

                val downloading_media_created_date = itemView.findViewById<TextView>(
                    R.id.downloaded_created_date
                )


                downloading_media_created_date.text = resultLastModifiedDate

                val progressBar = itemView.findViewById<ProgressBar>(
                    R.id.downloaded_progress_bar
                )

                val progress_text = itemView.findViewById<TextView>(
                    R.id.downloaded_progress_percentage
                )

                val alphaContainer = itemView.findViewById<FrameLayout>(
                    R.id.downloaded_alpha_container
                )

                alphaContainer.visibility = View.GONE
                progressBar.progress = 0
                progress_text.text = "0%"

            }

            MediaScannerConnection.scanFile(
                requireContext,
                arrayOf(outputFileUri), null, null
            )


            val allDownloadsDao =
                AllDownloadsDatabaseBuilder.getInstance(
                    activity.applicationContext
                ).allDownloadsDao()


            allDownloadsDao.updateById(
                item.id,
                thumbnailUrl = item.thumbnailUrl,
                fileProfileHttpsUrl = item.fileProfileHttpsUrl,
                name = item.name,
                fullText = item.fullText,
                mediaType = item.mediaType,
                contentType = item.contentType,
                lastModified = fileLastModifiedDate,
                fileSize = item.fileSize,
                absPath = outputFileUri,
                originalUrl = item.originalUrl,
                contentUri = savedUri?.toString()
            )


            return true
        } catch (e: IOException) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                savedUri?.let {
                    requireContext.contentResolver.delete(
                        savedUri, null
                    )
                    requireContext.contentResolver.notifyChange(
                        savedUri, null
                    )
                }


            } else {
                outputFileUri?.let {
                    if (File(outputFileUri).exists())
                        File(outputFileUri).delete()
                    MediaScannerConnection.scanFile(
                        requireContext,
                        arrayOf(outputFileUri), null, null
                    )
                }
            }

            return false
        }

    }


    private fun createFolder(): Boolean {
        //folder name
        val folderName = "X Monkey"

        //create folder using name we just input
        val file = File("${Environment.getExternalStorageDirectory()}/$folderName")
        //create folder

        val folderCreated = file.mkdirs()

        return folderCreated
    }

    override fun getItemCount(): Int {
        return items.size
    }


    private fun getLoadingProgressBarDialog(): AlertDialog {
        return AlertDialog.Builder(activity).apply {
            setView(
                activity.layoutInflater.inflate(
                    R.layout.loading_progress_dialog, null
                )
            )

        }.create().apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }


    }


    fun deleteFile(deleteId: Int) {


        fragment.lifecycleScope.launch {

            val allDownloadsDao = AllDownloadsDatabaseBuilder.getInstance(
                context.applicationContext
            ).allDownloadsDao()


            launch(Dispatchers.IO)
            {
                allDownloadsDao.deleteById(
                    deleteId
                )

                launch(Dispatchers.Main) {

                    Toast.makeText(
                        context, "Successfully deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }


        }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView)

    fun updateItems(){
        val tempItems=ArrayList<DownloadedFileItem>()
        for(item in items){
            val isExists = File(item.absPath).exists()

            if(isExists){
                tempItems.add(item)
            }
        }
        items.clear()
        items.addAll(tempItems)
        notifyDataSetChanged()

        if(items.isEmpty()){
            if (fragment.allDownloadsProgressBar.visibility == View.VISIBLE) {
                fragment.allDownloadsProgressBar.visibility = View.GONE
            }

            if (fragment.downloadedRecyclerView.visibility == View.VISIBLE) {
                fragment.downloadedRecyclerView.visibility = View.GONE
            }
            if (fragment.noMediaLayout.visibility == View.GONE) {
                fragment.noMediaLayout.visibility = View.VISIBLE
            }
        }
    }

}

