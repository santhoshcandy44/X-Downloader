package com.x.twitter.video.downloader.ui.home

import com.x.twitter.video.downloader.BaseApplication
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.*
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.github.ybq.android.spinkit.SpinKitView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.x.twitter.video.downloader.*
import com.x.twitter.video.downloader.BuildConfig
import com.x.twitter.video.downloader.R
import com.x.twitter.video.downloader.databinding.FragmentHomeBinding
import com.x.twitter.video.downloader.ui.alldownloads.AllDownloadsDatabaseBuilder
import com.x.twitter.video.downloader.ui.alldownloads.models.DownloadedFileItem
import com.x.twitter.video.downloader.ui.home.adapters.DownloadMediaRecyclerAdapter
import com.x.twitter.video.downloader.ui.home.models.DownloadFileItem
import com.x.twitter.video.downloader.ui.home.models.ErrorData
import com.x.twitter.video.downloader.ui.home.models.Variant
import com.x.twitter.video.downloader.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.net.ConnectException
import java.net.URL
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri

interface PermissionGrantedListener {
    fun onPermissionGranted()
}

interface HandleResponse<T> {
    fun onResponse(p1: T)
    fun onError(p1: Throwable)
}


class HomeFragment : Fragment(),


    HandleResponse<Response<List<DownloadFileItem>>> {


    @SuppressLint("NotifyDataSetChanged")
    fun handleMedia(context: Context, jsonArray: JSONArray) {

        if (jsonArray.length() > 0) {

            val mediaItems = Gson().fromJson<List<DownloadFileItem>>(
                jsonArray.toString(),
                object : TypeToken<List<DownloadFileItem>>() {}.type
            )


            if (loadingDialog.isShowing) {
                loadingDialog.dismiss()
            }
            downloadFileItems=ArrayList()
            downloadFileItems.clear()
            downloadFileItems.addAll(
                mediaItems
            )

            createDownloadDialog(downloadFileItems,context)

            adapter.notifyDataSetChanged()

            downloadDialog.show()


            if (!downloadBtn.isEnabled) {
                downloadBtn.isEnabled = true
            }
            if (indicator.isVisible) {
                indicator.visibility = View.GONE
            }

            if (dlBtnText.isGone) {
                dlBtnText.visibility = View.VISIBLE
            }


        } else {
            Toast.makeText(context, "No Media Founded", Toast.LENGTH_SHORT)
                .show()
        }
    }



   fun showError(message:String){
       errorCon.findViewById<TextView>(
           R.id.error_message
       ).text = message
       expand(errorCon)
   }

   fun handleError(p0:VolleyError){

       if (loadingDialog.isShowing) {
           loadingDialog.dismiss()
       }
       if (activity != null) {


           when (p0) {

               is NetworkError-> {
                   showError("Check your internet connection")
                   Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT)
                       .show()
               }

               is NoConnectionError->{
                   showError("Server connection failed")
                   Toast.makeText(context, "Server connection failed", Toast.LENGTH_SHORT)
                       .show()
               }

               is TimeoutError -> {
                   showError("Server is busy, retry")
                   Toast.makeText(context, "Timeout Error", Toast.LENGTH_SHORT)
                       .show()
               }
               else -> {
                   if (p0.networkResponse != null && p0.networkResponse.data != null) {
                       if (p0.networkResponse.statusCode == 401) {

                           Log.d("TAG_123",String(p0.networkResponse.data))

                           val errorData =
                               Gson().fromJson<ErrorData>(
                                   String(p0.networkResponse.data), ErrorData::class.java
                               ) as ErrorData

                           showError(errorData.message)


                       }
                   }

               }
           }




           if (!downloadBtn.isEnabled) {
               downloadBtn.isEnabled = true
           }
           if (indicator.isVisible) {
               indicator.visibility = View.GONE

           }
           if (dlBtnText.isGone) {
               dlBtnText.visibility = View.VISIBLE
           }
       }

   }

    override fun onResponse(p1: Response<List<DownloadFileItem>>) {

        if (activity != null) {

            if (p1.isSuccessful) {

                if (loadingDialog.isShowing) {
                    loadingDialog.dismiss()
                }

                if (p1.code() == 200) {


                    val body = p1.body()
                    downloadFileItems=ArrayList()

                    if (body != null) {
                        downloadFileItems.clear()
                        downloadFileItems.addAll(
                            body
                        )
                        createDownloadDialog(downloadFileItems,

                            requireContext())
                        adapter.notifyDataSetChanged()
                        downloadDialog.show()


                        if (!downloadBtn.isEnabled) {
                            downloadBtn.isEnabled = true
                        }
                        if (indicator.isVisible) {
                            indicator.visibility = View.GONE
                        }

                        if (dlBtnText.isGone) {
                            dlBtnText.visibility = View.VISIBLE
                        }
                    } else {

                        errorCon.findViewById<TextView>(
                            R.id.error_message
                        ).text = "Something went wrong try again..."
                        if (errorCon.isVisible) {
                            collapse(errorCon)
                        }
                        expand(errorCon)
                    }

                }


            } else {


                if (loadingDialog.isShowing) {
                    loadingDialog.dismiss()
                }
                if (p1.code() == 401) {

                    if (!downloadBtn.isEnabled) {
                        downloadBtn.isEnabled = true
                    }
                    if (indicator.isVisible) {
                        indicator.visibility = View.GONE

                    }

                    if (dlBtnText.isGone) {
                        dlBtnText.visibility = View.VISIBLE
                    }


                    val errorJsonData = p1.errorBody()


                    val errorMessage = if (errorJsonData != null) {
                        val gson = Gson()
                        val errorData = gson.fromJson(
                            errorJsonData.string(),
                            ErrorData::class.java
                        )

                        errorData.message

                    } else {

                        "Internal Error and We Will Catch It As Soon As Possible !!!"
                    }


                    errorCon.findViewById<TextView>(
                        R.id.error_message
                    ).text = errorMessage
                    if (errorCon.isVisible) {
                        collapse(errorCon)
                    }
                    expand(errorCon)
                }

            }

        }

    }

    override fun onError(p1: Throwable) {

        if (activity != null) {

            if (loadingDialog.isShowing) {
                loadingDialog.dismiss()
            }



            if (p1 is UnknownHostException || p1 is ConnectException || p1 is NoConnectivityException) {
                errorCon.findViewById<TextView>(
                    R.id.error_message
                ).text = "Check your network or internet data connection"
                expand(errorCon)

            }

            if (!downloadBtn.isEnabled) {
                downloadBtn.isEnabled = true
            }
            if (indicator.isVisible) {
                indicator.visibility = View.GONE

            }
            if (dlBtnText.isGone) {
                dlBtnText.visibility = View.VISIBLE
            }
        }

    }

    private var handleResponse: HandleResponse<Response<List<DownloadFileItem>>>? = null

    private var pgl: PermissionGrantedListener? = null

    fun registerPermissionGrantedListener(pgl: PermissionGrantedListener) {
        this.pgl = pgl
    }

    private var playerActivityFinishedInterstitialAd: InterstitialAd? = null
    private var downloadCompletedInterstitialAd: InterstitialAd? = null

    private val TAG = "TAG_123"


    private fun checkPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android is 11(R) or above
            true
        } else {
            //Android is below 11(R)
            val write = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val read = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }


    private fun playerActivityLauncherLoadInterstitialAd() {


        if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {

            if (app.aicpProtector()) {
                MobileAds.initialize(
                    requireContext()
                ) { }
                val adRequest: AdRequest =AdRequest.Builder().build()


                InterstitialAd.load(requireContext(),

resources.getString(R.string.ADMOB_HOME_INTERSTITIAL_VIDEO_PLAYER),
                    adRequest,
                    object : InterstitialAdLoadCallback() {

                        override fun onAdLoaded(interstitialAd: InterstitialAd) {

                            playerActivityFinishedInterstitialAd = interstitialAd
                            Log.i(TAG, "onAdLoaded")
                        }


                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            Log.d(TAG, loadAdError.toString())
                            playerActivityFinishedInterstitialAd = null
                        }
                    })
            }


        }
    }


    private val playerActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {

                    Handler(Looper.getMainLooper()).postDelayed({
                        playerActivityFinishedInterstitialAd?.let {
                            playerActivityFinishedInterstitialAd!!.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdClicked() {

                                        app.increaseAdClickCount()

                                        Log.d(TAG, "Ad was clicked.")
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        Log.d(TAG, "Ad dismissed fullscreen content.")
                                        playerActivityFinishedInterstitialAd = null
                                    }

                                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                        Log.e(TAG, "Ad failed to show fullscreen content.")
                                        playerActivityFinishedInterstitialAd = null
                                    }

                                    override fun onAdImpression() {
                                        Log.d(TAG, "Ad recorded an impression.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        Log.d(TAG, "Ad showed fullscreen content.")
                                    }
                                }
                            playerActivityFinishedInterstitialAd!!.show(requireActivity())


                        }
                    }, 100)


                }

            }

        }


    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android is 11(R) or above

        } else {
            //Android is below 11(R)
            requestStoragePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val write = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
        val read = permissions[Manifest.permission.READ_EXTERNAL_STORAGE]


        if (write!! && read!!) {
            //External Storage Permission granted
            pgl?.let {
                it.apply {
                    onPermissionGranted()
                }
            }
        } else {
            //External Storage Permission denied...
            toast(requireContext(), "Storage Permission denied...")
        }
    }




    private fun createFolder(): Boolean {
        //folder name
        val folderName = "X Monkey"

        //create folder using name we just input
        val file = File("${Environment.getExternalStorageDirectory()}/$folderName")
        //create folder

        return file.mkdirs()
    }


    fun hideInputMethod() {
        val inputMethodManager = requireContext().getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager

        if (inputMethodManager.isAcceptingText) {
            inputMethodManager.hideSoftInputFromWindow(
                linkInput.windowToken, 0
            )
        }
    }

    private val downloadButtonClickListener: View.OnClickListener = View.OnClickListener {

        hideInputMethod()

        loadingDialog = getLoadingProgressBarDialog()

        if (!loadingDialog.isShowing) {
            loadingDialog.show()
        }

        if (errorCon.isVisible) {
            collapse(errorCon)
        }

        if (downloadBtn.isEnabled) {
            downloadBtn.isEnabled = false
        }
        if (indicator.isGone) {
            indicator.visibility = View.VISIBLE
        }

        if (dlBtnText.isVisible) {
            dlBtnText.visibility = View.GONE
        }

/*
        volleyGet(
            requireContext(), "${
                BuildConfig.BASE_URL
            }/x-dl-api.php?url=${linkInput.text}"
        )


*/



         val oktHttpClient = OkHttpClient.Builder()
            .connectTimeout(60*3, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(NetworkConnectionInterceptor(requireContext()))


        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(oktHttpClient.build())
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().setDateFormat("yyyy-MM-dd")
                        .create()
                )
            )
            .build()
                val call = retrofit.create(TwitterMonkeyAPI::class.java)

        call.getData(linkInput.text.toString())
            .enqueue(
                object : retrofit2.Callback<List<DownloadFileItem>> {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onResponse(
                        p0: Call<List<DownloadFileItem>>,
                        p1: Response<List<DownloadFileItem>>
                    ) {
                        handleResponse!!.onResponse(
                            p1
                        )

                    }

                    override fun onFailure(p0: Call<List<DownloadFileItem>>, p1: Throwable) {

                        handleResponse!!.onError(p1)

                    }

                }
            )





    }


    var mediaRequest: JsonArrayRequest? = null

    fun volleyGet(context: Context, endPoint: String) {

        mediaRequest =  object:JsonArrayRequest(
            Request.Method.GET,
            endPoint,
            null,
            {
                handleMedia(context, it)
            },
            {

             handleError(it)

            }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers=HashMap<String, String>()
                headers["referer"]="android-app://${BuildConfig.REFERER}"
                return headers
            }
        }

        mediaRequest!!.retryPolicy = object:RetryPolicy{
            override fun getCurrentRetryCount(): Int {
                return DefaultRetryPolicy.DEFAULT_MAX_RETRIES
            }

            override fun getCurrentTimeout(): Int {
                return 60000*2
            }

            override fun retry(p0: VolleyError?) {

                if(p0!=null) throw p0
            }
        }

        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(mediaRequest)
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaRequest?.cancel()
    }

    private lateinit var downloadBtn: FrameLayout
    private lateinit var errorCon: RelativeLayout
    private lateinit var linkInput: EditText
    private lateinit var indicator: SpinKitView
    private lateinit var dlBtnText: TextView
    private lateinit var downloadingContainerWrapper: LinearLayout
    private lateinit var dlMediaRecyclerView: RecyclerView
    private lateinit var downloadFileItems: ArrayList<DownloadFileItem>
    private lateinit var linkInputAction: ImageView

    lateinit var homeFragmentViewModel: HomeFragmentViewModel
    lateinit var app: Application
    private lateinit var adapter: DownloadMediaRecyclerAdapter


    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    lateinit var downloadDialog: AlertDialog
    lateinit var loadingDialog: AlertDialog

    private lateinit var root: View


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        root = binding.root

        handleResponse = this


        downloadBtn = root.findViewById(R.id.download_btn)
        errorCon = root.findViewById(R.id.error_con)
        linkInput = root.findViewById(R.id.link_input)
        linkInputAction = root.findViewById(R.id.link_input_action_button)

        indicator = root.findViewById(R.id.loading)
        dlBtnText = root.findViewById(R.id.dl_btn_txt)
        downloadingContainerWrapper = root.findViewById(R.id.downloading_container_wrapper)


        app = requireActivity().application as Application

        homeFragmentViewModel = ViewModelProvider(this)[HomeFragmentViewModel::class.java]


        if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {
            if (app.aicpProtector()) {
                loadNativeAdAdmob(
                    requireContext()
                )
            }
        }



        (requireActivity() as MainActivity).homeFragmentAdHideListener = object : AdHideListener {

            override fun adShow() {
                adTypeChanged = true
            }

            override fun adDismiss() {
                val frame =
                    root.findViewById<CardView>(R.id.fragment_home_admob_native_ad_native_ad_frame)

                if (frame.isVisible) {
                    frame.visibility = View.GONE
                }
            }
        }


        homeFragmentViewModel.isDownloadCompleted.observe(requireActivity()) {

            if (it) {
                if (homeFragmentViewModel.isInterstitialAdLoaded.value == true

                    && homeFragmentViewModel.isDownloadCompleted.value == true
                ) {

                    if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {

                        downloadCompletedInterstitialAd?.let {
                            downloadCompletedInterstitialAd!!.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdClicked() {

                                        app.increaseAdClickCount()

                                        Log.d(TAG, "Ad was clicked.")
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        Log.d(TAG, "Ad dismissed fullscreen content.")
                                        downloadCompletedInterstitialAd = null
                                    }

                                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                        Log.e(TAG, "Ad failed to show fullscreen content.")
                                        downloadCompletedInterstitialAd = null
                                    }

                                    override fun onAdImpression() {
                                        Log.d(TAG, "Ad recorded an impression.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        Log.d(TAG, "Ad showed fullscreen content.")
                                    }
                                }
                            downloadCompletedInterstitialAd!!.show(requireActivity())

                        }
                    }

                }
            }
        }

        homeFragmentViewModel.isInterstitialAdLoaded.observe(requireActivity()) {
            if (it) {
                if (homeFragmentViewModel.isInterstitialAdLoaded.value == true

                    && homeFragmentViewModel.isDownloadCompleted.value == true
                ) {

                    if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {

                        downloadCompletedInterstitialAd?.let {

                            downloadCompletedInterstitialAd!!.show(requireActivity())
                            downloadCompletedInterstitialAd!!.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdClicked() {
                                        app.increaseAdClickCount()
                                        Log.d(TAG, "Ad was clicked.")
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        Log.d(TAG, "Ad dismissed fullscreen content.")
                                        downloadCompletedInterstitialAd = null
                                    }

                                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                        Log.e(TAG, "Ad failed to show fullscreen content.")
                                        downloadCompletedInterstitialAd = null
                                    }

                                    override fun onAdImpression() {
                                        Log.d(TAG, "Ad recorded an impression.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        Log.d(TAG, "Ad showed fullscreen content.")
                                    }
                                }

                        }
                    }
                }
            }
        }


        val data = requireActivity().intent.getStringExtra(Intent.EXTRA_TEXT)
        data?.let {
            val dataUri = data.toUri()
            if (dataUri.host.equals("twitter.com") || dataUri.host.equals("x.com")) {
                linkInput.setText(dataUri.toString())
            }
        }

        if (savedInstanceState != null) {
            linkInput.setText(savedInstanceState.getString("link_input_value"))
        }


        if (linkInput.text.isEmpty()) {
            linkInputAction.setImageResource(
                R.drawable.ic_paste
            )
            linkInputAction.setOnClickListener {
                val clipBoardManager = requireContext().getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

                if (clipBoardManager.hasPrimaryClip()) {
                    val clip = clipBoardManager.primaryClip
                    val clipText = clip!!.getItemAt(0).text
                    linkInput.setText(clipText)
                    linkInput.setSelection(0)

                }
            }
        }

        if (linkInput.text.isNotEmpty()) {
            linkInputAction.setImageResource(
                R.drawable.ic_clear
            )
            linkInputAction.setOnClickListener {
                linkInput.text.clear()


            }
        }


        downloadBtn.setOnClickListener(downloadButtonClickListener)



        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("link_input_value", linkInput.text.toString())
    }



    fun createDownloadDialog(downloadFileItems:ArrayList<DownloadFileItem>,context:Context){

        val downloadView =
            (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                    as LayoutInflater)
            .inflate(R.layout.download_dialog, null)

        val downloadDialogBuilder = AlertDialog.Builder(context)

        dlMediaRecyclerView = downloadView.findViewById(R.id.download_media_recycler_view)
        dlMediaRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        downloadDialogBuilder.setView(downloadView)
        downloadDialogBuilder.setCancelable(true)

        val downloadDialogClose =
            downloadView.findViewById<ImageView>(R.id.download_dialog_close)

        downloadDialog = downloadDialogBuilder.create()

        adapter =
            DownloadMediaRecyclerAdapter(downloadDialog,
                context, downloadFileItems)

        downloadDialog.window?.setBackgroundDrawable(
            android.graphics.Color.TRANSPARENT.toDrawable()
        )

        downloadDialogClose.setOnClickListener {
            if (downloadDialog.isShowing) {
                downloadDialog.dismiss()
            }
        }

        adapter = DownloadMediaRecyclerAdapter(downloadDialog, requireContext(), downloadFileItems)
        adapter.setDownloadItemListener(object : DownloadItemClickListener {

            override fun itemClick(
                url: String,
                downloadFileItem: DownloadFileItem,
                variant: Variant
            ) {

                if (downloadDialog.isShowing) {
                    downloadDialog.dismiss()
                }

                loadingDialog = getLoadingProgressBarDialog()

                if (!loadingDialog.isShowing) {
                    loadingDialog.show()
                }

                homeFragmentViewModel.updateDownloadCompleted(false)
                homeFragmentViewModel.updateInterstitialAdLoaded(false)


                if (checkPermission(context)) {

                    val downloadingItemView = (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                        R.layout.downloading_item, null
                    ) as LinearLayout

                    downloadVariant(
                        context,
                        variant, downloadingItemView, false,
                        app, url, downloadFileItem, loadingDialog
                    )


                } else {

                    if (loadingDialog.isShowing) {
                        loadingDialog.dismiss()
                    }

                    toast(
                        requireContext(), "Storage Permission Needed..."
                    )

                    registerPermissionGrantedListener(object : PermissionGrantedListener {
                        override fun onPermissionGranted() {
                            toast(
                                requireContext(), "Successfully Storage Permission Granted..."
                            )

                            val downloadingItemView = layoutInflater.inflate(
                                R.layout.downloading_item, null
                            ) as LinearLayout

                            downloadVariant(
                                context,
                                variant, downloadingItemView, false,
                                app, url, downloadFileItem, loadingDialog
                            )
                        }

                    })
                    requestPermission()
                }


            }

        })

        dlMediaRecyclerView.adapter = adapter
    }




    private fun downloadFile(
        app: Application,
        context: Context,
        originalUrl: String,
        item: DownloadFileItem,
        variant: Variant,
        byteStream: InputStream,
        progressBar: ProgressBar,
        downloadingProgressBarContainer: FrameLayout,
        file_size: Long,
        progress_text: TextView,
        alpha_container: FrameLayout,
        downloading_media_file_size: TextView,
        downloading_media_created_date: TextView, li: LinearLayout
    ): Boolean {

        val output: OutputStream
        val fileLastModifiedDate: Long
        var outputFileUri: String? = null
        var savedUri: Uri? = null

        try {

            requireActivity().runOnUiThread {
                if (downloadingProgressBarContainer.isGone) {
                    downloadingProgressBarContainer.visibility = View.VISIBLE
                }
                progressBar.progress = 0
                progress_text.text = "0%"

                if (alpha_container.isGone) {
                    alpha_container.visibility = View.VISIBLE
                }
                alpha_container.alpha = 1.0f

                downloading_media_file_size.text = humanReadableByteCountBin(file_size)
            }



            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {

                val folderName = "X Monkey"

                val file = File("${Environment.getExternalStorageDirectory()}/$folderName")

                if (!file.exists()) {
                    createFolder()
                }

                val ext: String

                if (item.mediaType == "video") {
                    val fileFormatList = variant.contentType.split("/")
                    ext = fileFormatList[fileFormatList.size - 1]


                } else {
                    val fileFormatList = variant.contentType.split("/")
                    ext = fileFormatList[fileFormatList.size - 1]

                }


                var uniqueFileName = generateUniqueFileName(ext)
                var outputFile =
                    File("${Environment.getExternalStorageDirectory()}/$folderName/$uniqueFileName")

                while (outputFile.exists()) {
                    uniqueFileName = generateUniqueFileName(ext)
                    outputFile =
                        File("${Environment.getExternalStorageDirectory()}/$folderName/$uniqueFileName")
                }


                outputFileUri = outputFile.absolutePath

                output=FileOutputStream(outputFileUri)



            } else {

                val resolver = context.contentResolver
                val values = ContentValues()



                val ext: String
                if (item.mediaType == "video") {
                    val fileFormatList = variant.contentType.split("/")
                    ext = fileFormatList[fileFormatList.size - 1]
                    Log.d("TAG_123", fileFormatList.toString())


                } else {
                    val fileFormatList = variant.contentType.split("/")
                    ext = fileFormatList[fileFormatList.size - 1]
                    Log.d("TAG_123", fileFormatList.toString())

                }
                val folderName = "X Monkey"
                Log.d("TAG_123", ext)

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
                    values.put(MediaStore.Downloads.DISPLAY_NAME, uniqueFileName)
                    values.put(MediaStore.Downloads.MIME_TYPE, "video/mp4")
                    values.put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/$folderName/"
                    )

                }

                val contentUri: Uri = if (item.mediaType == "video") {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI

                } else {
                    // MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI

                }


                savedUri = resolver.insert(contentUri, values)!!



                if (item.mediaType == "animated_gif") {
                    output=resolver.openOutputStream(savedUri)!!

                } else {

                    output=resolver.openOutputStream(savedUri)!!
                }


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

            val bis: InputStream = BufferedInputStream(byteStream, 1024)


            var total: Long = 0
            while (bis.read(data).also { count = it } != -1) {
                total += count.toLong()

                val progress = (total * 100 / file_size).toInt()


                requireActivity().runOnUiThread {
                    progressBar.progress = progress
                    progress_text.text = "$progress%"
                    if (progress % 10 == 0) {
                        alpha_container.alpha = (100 - progress).toFloat().div(100)
                    }

                }
                output.write(data, 0, count)
            }
            output.flush()
            output.close()
            bis.close()



            fileLastModifiedDate = File(outputFileUri!!).lastModified()

            val resultLastModifiedDate = SimpleDateFormat("dd/MM/yyyy").format(
                Date(
                    fileLastModifiedDate
                )
            )

            requireActivity().runOnUiThread {
                downloading_media_created_date.text = resultLastModifiedDate
            }

            if (item.mediaType == "animated_gif") {

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFileUri.toString()),
                    null, null
                )
            } else {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFileUri.toString()),
                    null, null
                )
            }


            requireActivity().runOnUiThread {

                if (downloadingProgressBarContainer.isVisible) {
                    downloadingProgressBarContainer.visibility = View.GONE
                }
                progressBar.progress = 0
                progress_text.text = "0%"

                if (alpha_container.isVisible) {
                    alpha_container.visibility = View.GONE
                }
                alpha_container.alpha = 1.0f

                li.setOnClickListener {
                    val loadingDialog = getLoadingProgressBarDialog()

                    if (!File(outputFileUri).exists()) {

                        toast(context, "File might be deleted, re-download it")

                        if (alpha_container.isGone) {
                            alpha_container.visibility = View.VISIBLE
                        }
                        val reDownload =
                            li.findViewById<ImageView>(R.id.downloading_refresh_download)
                        if (reDownload.isGone) {
                            reDownload.visibility = View.VISIBLE
                        }
                        reDownload.setOnClickListener {
                            if (checkPermission(context)) {
                                reDownload.visibility = View.GONE
                                downloadVariant(
                                    context,
                                    variant,
                                    li, true, app, originalUrl, item, loadingDialog
                                )
                            } else {

                                toast(
                                    context, "Storage Permission Needed..."
                                )

                                registerPermissionGrantedListener(
                                    object : PermissionGrantedListener {
                                        override fun onPermissionGranted() {
                                            reDownload.visibility = View.GONE
                                            downloadVariant(
                                                context,
                                                variant,
                                                li,
                                                true,
                                                app,
                                                originalUrl,
                                                item,
                                                loadingDialog
                                            )

                                        }

                                    }

                                )

                                requestPermission()
                            }

                        }


                    } else {
                        val intent = Intent(context, PlayerActivity::class.java)
                        intent.data = outputFileUri.toUri()
                        playerActivityLauncher.launch(intent)
                        playerActivityLauncherLoadInterstitialAd()
                    }

                }
            }


            AllDownloadsDatabaseBuilder.getInstance(
                requireActivity().applicationContext
            ).allDownloadsDao().apply {

                insert(
                    DownloadedFileItem(
                        thumbnailUrl = item.thumbnail,
                        fileProfileHttpsUrl = item.profileImageUrlHttps,
                        name = item.name,
                        fullText = item.fullText,
                        mediaType = item.mediaType,
                        contentType = variant.contentType,
                        lastModified = fileLastModifiedDate,
                        fileSize = file_size,
                        absPath = outputFileUri,
                        originalUrl = originalUrl,
                        contentUri = savedUri?.toString()
                    )
                )

            }



            return true
        } catch (e: IOException) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                savedUri?.let {
                    context.contentResolver.delete(
                        savedUri, null
                    )
                    context.contentResolver.notifyChange(
                        savedUri, null
                    )
                }


            } else {
                outputFileUri?.let {
                    if (File(outputFileUri).exists())
                        File(outputFileUri).delete()
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(outputFileUri), null, null
                    )
                }
            }

            return false
        }


    }



    fun downloadVariant(
        context: Context,
        variant: Variant,
        downloadingItemView: LinearLayout,
        isRefreshed: Boolean,
        app: Application,
        url: String,
        downloadFileItem: DownloadFileItem, loadingDialog: AlertDialog
    ) {


        val downloadingThumbnail = downloadingItemView.findViewById<ImageView>(
            R.id.downloading_thumbnail
        )
        val alphaContainer = downloadingItemView.findViewById<FrameLayout>(
            R.id.downloading_alpha_container
        )
        val downloadingProfile = downloadingItemView.findViewById<ImageView>(
            R.id.downloading_profile
        )
        val downloadingName = downloadingItemView.findViewById<TextView>(
            R.id.downloading_name
        )
        val downloadingFulText = downloadingItemView.findViewById<TextView>(
            R.id.downloading_full_text
        )
        val downloadingProgressBarContainer = downloadingItemView.findViewById<FrameLayout>(
            R.id.downloading_progress_bar_container
        )
        val downloadingRefresh = downloadingItemView.findViewById<ImageView>(
            R.id.downloading_refresh_download
        )

        Glide.with(context)
            .load(downloadFileItem.thumbnail)
            .centerCrop()
            .into(downloadingThumbnail)

        val downloadingMediaType = downloadingItemView.findViewById<ImageView>(
            R.id.downloading_media_type
        )
        val downloadingMediaCreatedDate = downloadingItemView.findViewById<TextView>(
            R.id.downloading_media_created_date
        )
        val downloadingMediaFileSize = downloadingItemView.findViewById<TextView>(
            R.id.downloading_media_file_size
        )

        if (downloadFileItem.mediaType == "video") {
            Glide.with(context)
                .load(R.drawable.ic_type_video)
                .centerCrop()
                .into(downloadingMediaType)
        } else if (downloadFileItem.mediaType == "animated_gif") {
            Glide.with(context)
                .load(R.drawable.ic_type_gif)
                .centerCrop()
                .into(downloadingMediaType)
        }

        val downloadingProgressBar = downloadingItemView.findViewById<ProgressBar>(
            R.id.downloading_progress_bar
        )

        val downloadingMediaProgress = downloadingItemView.findViewById<TextView>(
            R.id.progress_percentage
        )

        Glide.with(context)
            .load(downloadFileItem.profileImageUrlHttps)
            .circleCrop()
            .into(downloadingProfile)
        downloadingFulText.text = downloadFileItem.fullText
        downloadingName.text = downloadFileItem.name


        val oktHttpClient = OkHttpClient.Builder()
            .apply {
                addInterceptor(NetworkConnectionInterceptor(context))
            }

        // Adding NetworkConnectionInterceptor with okHttpClientBuilder.

        val downloadURL = URL(url)
        val baseURL = downloadURL.protocol + "://" + downloadURL.host
        val retrofit = Retrofit.Builder()
            .baseUrl(baseURL)
            .client(oktHttpClient.build())
            .build()


        val retrofitInterface =
            retrofit.create(TwitterMonkeyAPI::class.java)


        val request: Call<ResponseBody> = retrofitInterface
            .downloadFile(variant.contentType, downloadURL.path)




        lifecycleScope.launch(Dispatchers.IO) {

            try {
               launch(Dispatchers.Main) {
                    if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {

                        if (app.aicpProtector()){
                            MobileAds.initialize(context) { }

                            val adRequest: AdRequest =
                                AdRequest.Builder().build()

                            InterstitialAd.load(context,

                                resources.getString(R.string.ADMOB_HOME_INTERSTITIAL),adRequest,
                                object : InterstitialAdLoadCallback() {
                                    override fun onAdLoaded(interstitialAd: InterstitialAd) {

                                        downloadCompletedInterstitialAd = interstitialAd
                                        homeFragmentViewModel.updateInterstitialAdLoaded(
                                            true
                                        )
                                        Log.i(TAG, "onAdLoaded")
                                    }

                                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                        Log.d(TAG, loadAdError.toString())
                                        downloadCompletedInterstitialAd = null
                                    }
                                })

                        }

                    }
                }

                val response = request.execute()
                val byteStream = response.body()!!.byteStream()

                launch(Dispatchers.Main) {

                    if (loadingDialog.isShowing) {
                        loadingDialog.dismiss()
                    }
                    if (!isRefreshed) {
                        downloadingContainerWrapper.addView(downloadingItemView, 1)
                    }
                    if (downloadingContainerWrapper.isGone) {
                        downloadingContainerWrapper.visibility = View.VISIBLE
                    }
                }

                val isDownloaded: Boolean



                isDownloaded = downloadFile(
                    app,
                    context,
                    url,
                    downloadFileItem,
                    variant,
                    byteStream,
                    downloadingProgressBar,
                    downloadingProgressBarContainer,
                    variant.fileSize,
                    downloadingMediaProgress,
                    alphaContainer,
                    downloadingMediaFileSize,
                    downloadingMediaCreatedDate,
                    downloadingItemView
                )

                if (!isDownloaded) {


                    launch(Dispatchers.Main) {

                        if (alphaContainer.isGone) {
                            alphaContainer.visibility = View.VISIBLE
                        }
                        alphaContainer.alpha = 1.0f

                        if (downloadingProgressBarContainer.isVisible) {
                            downloadingProgressBarContainer.visibility = View.GONE

                        }

                        if (downloadingRefresh.isGone) {
                            downloadingRefresh.visibility = View.VISIBLE
                        }

                        downloadingRefresh.setOnClickListener {
                            downloadingRefresh.visibility = View.GONE
                            if (!loadingDialog.isShowing) {
                                loadingDialog.show()
                            }
                            downloadVariant(
                                context,
                                variant,
                                downloadingItemView,
                                true,
                                app,
                                url,
                                downloadFileItem, loadingDialog
                            )
                        }
                    }

                } else {
                    launch(Dispatchers.Main) {
                        if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {
                            homeFragmentViewModel.updateDownloadCompleted(true)

                        }
                    }

                }

            } catch (e: Exception) {
                launch(Dispatchers.Main) {

                    if (loadingDialog.isShowing) {
                        loadingDialog.dismiss()
                    }
                    if (isRefreshed) {
                        if (downloadingRefresh.isGone) {
                            downloadingRefresh.visibility = View.VISIBLE
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
                            "Dismiss"
                        ) { dialog, _ -> dialog!!.dismiss() }
                        alertDialog.show()
                    }

                }


            }
        }


    }








    private val editTextListener = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            if (adTypeChanged) {
                if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {
                    if (app.aicpProtector()) {
                        loadNativeAdAdmob(requireContext())
                    }

                }
            }
            adTypeChanged = false
        }

        override fun afterTextChanged(s: Editable?) {

            if (this@HomeFragment.linkInput.text.isEmpty()) {

                linkInputAction.setImageResource(
                    R.drawable.ic_paste
                )
                linkInputAction.setOnClickListener {

                    val clipBoardManager = requireContext().getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager

                    if (clipBoardManager.hasPrimaryClip()) {
                        val clip = clipBoardManager.primaryClip
                        val clipText = clip!!.getItemAt(0).text
                        linkInput.setText(clipText)
                        linkInput.setSelection(0)

                    }
                }
            }

            if (this@HomeFragment.linkInput.text.isNotEmpty()) {
                linkInputAction.setImageResource(
                    R.drawable.ic_clear
                )
                linkInputAction.setOnClickListener {
                    linkInput.text.clear()
                }
            }


            if (!downloadBtn.isEnabled) {
                downloadBtn.isEnabled = true
            }
            if (indicator.isVisible) {
                indicator.visibility = View.GONE
            }

            if (dlBtnText.isGone) {
                dlBtnText.visibility = View.VISIBLE
            }

            if (errorCon.isVisible) {
                collapse(errorCon)
            }
        }


    }

    override fun onResume() {
        super.onResume()
        linkInput.addTextChangedListener(editTextListener)
    }

    override fun onPause() {
        super.onPause()
        linkInput.removeTextChangedListener(editTextListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    var adTypeChanged: Boolean = false

    override fun onStart() {
        super.onStart()
        view?.let {
            if (adTypeChanged) {
                if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {
                    if (isAdded) {

                        if (app.aicpProtector()) {
                            loadNativeAdAdmob(requireContext())
                        }
                    }
                }
            }
            adTypeChanged = false
        }
    }

    private fun loadNativeAdAdmob(context: Context) {

        MobileAds.initialize(context)

        val adLoader = AdLoader.Builder(
            context,

            resources.getString(
                R.string.ADMOB_HOME_NATIVE
            )
        )
            .forNativeAd { nativeAd ->

                val frame =
                    root.findViewById<CardView>(R.id.fragment_home_admob_native_ad_native_ad_frame)

                val nativeAdView = (LayoutInflater.from(
                    context
                ).inflate(
                    R.layout.admob_native_ad_layout,
                    null
                )) as com.google.android.gms.ads.nativead.NativeAdView


                nativeAdView.headlineView =
                    nativeAdView.findViewById<TextView>(R.id.admob_native_ad_headline)
                nativeAdView.advertiserView =
                    nativeAdView.findViewById<TextView>(R.id.admob_native_ad_advertiser)
                nativeAdView.bodyView =
                    nativeAdView.findViewById<TextView>(R.id.admob_native_ad_body_text)
                nativeAdView.starRatingView =
                    nativeAdView.findViewById<RatingBar>(R.id.admob_native_ad_star_rating)
                nativeAdView.mediaView =
                    nativeAdView.findViewById<MediaView>(R.id.admob_native_ad_media_view)
                nativeAdView.callToActionView =
                    nativeAdView.findViewById<RelativeLayout>(R.id.ad_add_call_to_action)
                nativeAdView.iconView =
                    nativeAdView.findViewById<ImageView>(R.id.admob_native_ad_icon)



                (nativeAdView.headlineView as TextView).text = nativeAd.headline

                if (nativeAd.headline == null) {
                    (nativeAdView.headlineView as TextView).visibility = View.GONE
                } else {
                    (nativeAdView.headlineView as TextView).text = nativeAd.body
                    (nativeAdView.headlineView as TextView).visibility = View.VISIBLE
                }

                if (nativeAd.body == null) {
                    (nativeAdView.bodyView as TextView).visibility = View.GONE
                } else {
                    (nativeAdView.bodyView as TextView).text = nativeAd.body
                    (nativeAdView.bodyView as TextView).visibility = View.VISIBLE
                }

                if (nativeAd.advertiser == null) {
                    (nativeAdView.advertiserView as TextView).visibility = View.GONE
                } else {
                    (nativeAdView.advertiserView as TextView).text = nativeAd.advertiser
                    (nativeAdView.advertiserView as TextView).visibility = View.VISIBLE

                }

                if (nativeAd.starRating == null) {
                    (nativeAdView.starRatingView as RatingBar).visibility = View.GONE
                } else {
                    (nativeAdView.starRatingView as RatingBar).rating =
                        nativeAd.starRating!!.toFloat()
                    (nativeAdView.starRatingView as RatingBar).visibility = View.VISIBLE

                }

                if (nativeAd.icon == null) {
                    (nativeAdView.iconView as ImageView).visibility = View.GONE
                } else {
                    (nativeAdView.iconView as ImageView).setImageDrawable(nativeAd.icon!!.drawable)
                    (nativeAdView.iconView as ImageView).visibility = View.VISIBLE

                }

                if (nativeAd.callToAction == null) {

                    (nativeAdView.callToActionView as RelativeLayout).visibility = View.GONE

                } else {
                    (nativeAdView.findViewById(R.id.admob_call_to_action_text) as TextView).text =
                        nativeAd.callToAction
                    (nativeAdView.callToActionView as RelativeLayout).visibility =
                        View.VISIBLE
                }
                if (nativeAd.mediaContent == null) {
                    (nativeAdView.mediaView as MediaView)
                        .visibility = View.GONE
                } else {
                    val mediaView =
                        (nativeAdView.mediaView as MediaView)

                    mediaView.mediaContent = nativeAd.mediaContent

                    mediaView.mediaContent?.let {

                        if (it.hasVideoContent()) {

                            it.videoController.apply {

                                if (!isMuted) {
                                    mute(true)
                                }
                                play()
                            }

                        }

                    }




                    mediaView.setImageScaleType(
                        ImageView.ScaleType.CENTER_CROP
                    )
                    nativeAdView.setOnHierarchyChangeListener(
                        object : ViewGroup.OnHierarchyChangeListener {
                            override fun onChildViewAdded(parent: View?, child: View?) {

                                if (child is ImageView) {
                                    child.adjustViewBounds = true
                                    child.scaleType = ImageView.ScaleType.CENTER_CROP
                                }
                            }

                            override fun onChildViewRemoved(parent: View?, child: View?) {

                            }

                        }
                    )
                    mediaView.visibility = View.VISIBLE
                }
                nativeAdView.setNativeAd(nativeAd)
                frame.removeAllViews()
                frame.addView(nativeAdView)
            }
            .withAdListener(object : AdListener() {

                override fun onAdClicked() {
                    super.onAdClicked()
                    app.increaseAdClickCount()

                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.i(TAG, "Native ad loaded")
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    Log.i(TAG, "Native ad failed to load {${p0.message}}")

                }

            })
            .build()

        adLoader.loadAd(
            AdRequest.Builder()
                .build()
        )

    }


    private fun getLoadingProgressBarDialog(): AlertDialog {

        return AlertDialog.Builder(requireContext()).apply {
            setView(
                layoutInflater.inflate(
                    R.layout.loading_progress_dialog, null
                )
            )

        }.create().apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }


    }


}


class HomeFragmentViewModel : ViewModel() {

    var isDownloadCompleted = MutableLiveData<Boolean>()
    var isInterstitialAdLoaded = MutableLiveData<Boolean>()

    fun updateDownloadCompleted(isCompleted: Boolean) {
        isDownloadCompleted.value = isCompleted
    }


    fun updateInterstitialAdLoaded(isLoaded: Boolean) {
        isInterstitialAdLoaded.value = isLoaded
    }

}