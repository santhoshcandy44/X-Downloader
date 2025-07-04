package com.x.twitter.video.downloader.ui.alldownloads

import com.x.twitter.video.downloader.BaseApplication
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.x.twitter.video.downloader.*
import com.x.twitter.video.downloader.R
import com.x.twitter.video.downloader.databinding.FragmentAllDownloadsBinding
import com.x.twitter.video.downloader.ui.alldownloads.adapters.DownloadedRecyclerViewAdapter
import com.x.twitter.video.downloader.ui.alldownloads.models.DownloadedFileItem
import com.x.twitter.video.downloader.utils.toast
import java.io.*
import java.util.*
import com.x.twitter.video.downloader.ui.home.PermissionGrantedListener

class AllDownloadsFragment : Fragment() {

    var pgl: PermissionGrantedListener? = null

    fun registerPermissionGrantedListener(pgl: PermissionGrantedListener) {
        this.pgl = pgl
    }

    var mInterstitialAd: InterstitialAd? = null
    val TAG = "TAG_123"


    fun allDownloadsPlayerActivityFinishedLoadInterstitialAd() {
        MobileAds.initialize(
            requireContext()
        ) { }
        val adRequest: AdRequest = AdRequest.Builder().build()

        AdManagerInterstitialAd.load(
            requireContext(),
            resources.getString(R.string.ADMOB_ALL_DOWNLOADS_INTERSTITIAL_VIDEO_PLAYER),

            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {

                    mInterstitialAd = interstitialAd
                    Log.i(TAG, "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(TAG, loadAdError.toString())
                    mInterstitialAd = null
                }
            })
    }


    val playerActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val app = requireActivity().application as Application
            if (it.resultCode == Activity.RESULT_OK) {
                if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        mInterstitialAd?.let {
                            mInterstitialAd!!.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdClicked() {
                                        app.increaseAdClickCount()

                                        Log.d(TAG, "Ad was clicked.")
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        Log.d(TAG, "Ad dismissed fullscreen content.")
                                        mInterstitialAd = null
                                    }

                                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                                        Log.e(TAG, "Ad failed to show fullscreen content.")
                                        mInterstitialAd = null
                                    }

                                    override fun onAdImpression() {
                                        Log.d(TAG, "Ad recorded an impression.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        Log.d(TAG, "Ad showed fullscreen content.")
                                    }
                                }
                            mInterstitialAd!!.show(requireActivity())


                        }
                    }, 100)

                }

            }

        }

    private var _binding: FragmentAllDownloadsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var root: View
    private lateinit var adapter: DownloadedRecyclerViewAdapter
    var adTypeChanged: Boolean = false

    @SuppressLint("NotifyDataSetChanged")
    override fun onStart() {
        super.onStart()
        if (adTypeChanged) {
            if (downloadedFileItems.size > 0) {

                if (noMediaLayout.visibility == View.VISIBLE) {
                    noMediaLayout.visibility = View.GONE
                }
                if (downloadedRecyclerViewWrapper.visibility == View.GONE) {
                    downloadedRecyclerViewWrapper.visibility = View.VISIBLE
                }

                if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {

                    if (app.aicpProtector()) {
                        if (frame.visibility == View.GONE) {
                            frame.visibility = View.VISIBLE
                        }
                        loadNativeAdAdmob(requireContext(),root)
                    }


                }
                adapter.notifyDataSetChanged()
            }

        }
        adTypeChanged = false

    }

    fun checkPermission(requireContext: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android is 11(R) or above
            true
        } else {
            //Android is below 11(R)
            val write = ContextCompat.checkSelfPermission(
                requireContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val read = ContextCompat.checkSelfPermission(
                requireContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android is 11(R) or above
            try {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                storageActivityResultLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                storageActivityResultLauncher.launch(intent)
            }
        } else {
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


    private val storageActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            //here we will handle the result of our intent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //Android is 11(R) or above
                if (Environment.isExternalStorageManager()) {
                    //Manage External Storage Permission is granted
                } else {
                    //Manage External Storage Permission is denied....
                toast(requireContext(), "Manage External Storage Permission is denied....")
                }
            } else {
                //Android is below 11(R)
            }
        }


    @SuppressLint("InflateParams")
    private fun loadNativeAdAdmob(context:Context, root: View) {

        MobileAds.initialize(context)

        val adLoader = AdLoader.Builder(
            context,
           resources.getString(R.string.ADMOB_TOP_MEDIUM_NATIVE)
        ).forNativeAd { nativeAd ->

                val frame = root.findViewById<CardView>(R.id.all_downloads_admob_native_ad)

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
                nativeAdView.mediaView = nativeAdView.findViewById(R.id.admob_native_ad_media_view)
                nativeAdView.callToActionView =
                    nativeAdView.findViewById<RelativeLayout>(R.id.ad_add_call_to_action)
                nativeAdView.iconView =
                    nativeAdView.findViewById<ImageView>(R.id.admob_native_ad_icon)

                if ((nativeAdView.mediaView as com.google.android.gms.ads.nativead.MediaView).visibility
                    == View.VISIBLE
                ) {
                    (nativeAdView.mediaView
                            as com.google.android.gms.ads.nativead.MediaView)
                        .visibility = View.GONE
                }

                /*
                 (nativeAdView.mediaView as com.google.android.gms.ads.nativead.MediaView)
                     .mediaContent=nativeAd.mediaContent
                 */

                (nativeAdView.headlineView as TextView).setText(nativeAd.headline)

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
                    nativeAdView.findViewById<TextView>(R.id.admob_call_to_action_text).text =
                        nativeAd.callToAction
                    (nativeAdView.callToActionView as RelativeLayout).visibility = View.VISIBLE
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
                    Log.i(TAG, "All Downloads Native ad loaded")
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    Log.i(TAG, "All Downloads Native ad failed to load {${p0.message}}")

                }

            })
            .build()


        adLoader.loadAd(AdRequest.Builder().build())

    }


    private var downloadedFileItems: ArrayList<DownloadedFileItem> = ArrayList()

   lateinit var downloadedRecyclerView: RecyclerView
   lateinit var downloadedRecyclerViewWrapper: LinearLayout

   lateinit var noMediaLayout: LinearLayout


    lateinit var app: Application

    lateinit var frame: CardView

    lateinit var allDownloadsProgressBar: ProgressBar

    lateinit var liveDataAllDownloadedFileItem:LiveData<List<DownloadedFileItem>>


    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAllDownloadsBinding.inflate(inflater, container, false)
        root = binding.root

        app = requireActivity().application as Application

        (requireActivity() as MainActivity).allDownloadsFragmentAdHideListener =
            object : AdHideListener {

                override fun adShow() {

                    adTypeChanged = true

                }

                override fun adDismiss() {

                    if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {


                        if (frame.visibility == View.VISIBLE) {
                            frame.visibility = View.GONE
                        }

                    }

                }
            }


        frame = root.findViewById(R.id.all_downloads_admob_native_ad)
        noMediaLayout = root.findViewById(R.id.no_media_layout)
        downloadedRecyclerView = root.findViewById(R.id.downloaded_recycler_view)
        downloadedRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.VERTICAL, false
        )

        downloadedRecyclerViewWrapper = root.findViewById(R.id.downloaded_recycler_view_container)

        allDownloadsProgressBar = root.findViewById(
            R.id.all_downloads_progress_bar
        )


        val allDownloadsDao = AllDownloadsDatabaseBuilder.getInstance(
            requireActivity().applicationContext
        ).allDownloadsDao()

        liveDataAllDownloadedFileItem = allDownloadsDao.getAll()





        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        liveDataAllDownloadedFileItem.observe(
            viewLifecycleOwner
        ) {

            if (it == null) {
                if (allDownloadsProgressBar.visibility == View.VISIBLE) {
                    allDownloadsProgressBar.visibility = View.GONE
                }

                if (downloadedRecyclerView.visibility == View.VISIBLE) {
                    downloadedRecyclerView.visibility = View.GONE
                }
                if (noMediaLayout.visibility == View.GONE) {
                    noMediaLayout.visibility = View.VISIBLE
                }
            } else {
                downloadedFileItems.clear()
                for(item in it){
                    val isExists = File(item.absPath).exists()

                    if(isExists){
                        downloadedFileItems.add(item)
                    }
                }

                if (allDownloadsProgressBar.visibility == View.VISIBLE) {
                    allDownloadsProgressBar.visibility = View.GONE
                }

                if (downloadedFileItems.isEmpty()) {

                    if (downloadedRecyclerView.visibility == View.VISIBLE) {
                        downloadedRecyclerView.visibility = View.GONE
                    }
                    if (noMediaLayout.visibility == View.GONE) {
                        noMediaLayout.visibility = View.VISIBLE
                    }


                    if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {

                        if (frame.visibility == View.VISIBLE) {
                            frame.visibility = View.GONE
                        }
                    }


                } else {
                    if (noMediaLayout.visibility == View.VISIBLE) {
                        noMediaLayout.visibility = View.GONE
                    }
                    if (downloadedRecyclerView.visibility == View.GONE) {
                        downloadedRecyclerView.visibility = View.VISIBLE
                    }


                    if (app.AD_TYPE == BaseApplication.AdType.ADMOB) {
                        if (app.aicpProtector()) {
                            if (frame.visibility == View.GONE) {
                                frame.visibility = View.VISIBLE
                            }
                            loadNativeAdAdmob(requireContext(),root)
                        }
                    }

                }



                if (!this::adapter.isInitialized) {
                    adapter = DownloadedRecyclerViewAdapter(
                        app,
                        this@AllDownloadsFragment,
                        requireActivity(),
                        requireContext(),
                        downloadedFileItems
                    )
                    downloadedRecyclerView.layoutManager = LinearLayoutManager(
                        requireContext(), LinearLayoutManager.VERTICAL, false
                    )
                    downloadedRecyclerView.adapter = adapter


                } else {
                    adapter.updateItems()
                }

            }


        }

    }

    var isPause=false
    override fun onPause() {
        super.onPause()
        isPause=true
    }

    override fun onResume() {
        super.onResume()
        if(isPause){
            adapter.updateItems()
            isPause=false
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}

