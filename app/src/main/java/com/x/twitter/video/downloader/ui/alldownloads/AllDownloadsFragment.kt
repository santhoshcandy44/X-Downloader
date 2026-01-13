package com.x.twitter.video.downloader.ui.alldownloads

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdListener
import com.facebook.ads.AdSize
import com.facebook.ads.AdView
import com.facebook.ads.InterstitialAd
import com.facebook.ads.InterstitialAdListener
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

    private var mInterstitialAd: InterstitialAd? = null
    val TAG = "TAG_123"


    fun allDownloadsPlayerActivityFinishedLoadInterstitialAd() {
        val ad1 = InterstitialAd(
            requireContext(),
            "245848558610696_245851035277115"
        )

        // Load the ad
        ad1.loadAd(
            ad1.buildLoadAdConfig().withAdListener(object : InterstitialAdListener {

                    override fun onAdLoaded(ad: Ad) {
                        mInterstitialAd = ad1
                        Log.i(TAG, "Facebook Interstitial onAdLoaded")
                    }

                    override fun onError(ad: Ad, adError: AdError) {
                        Log.e(
                            TAG,
                            "Facebook Interstitial failed: ${adError.errorMessage}"
                        )
                        mInterstitialAd = null
                    }

                    override fun onInterstitialDisplayed(ad: Ad) {
                        Log.d(TAG, "Facebook Interstitial displayed")
                    }

                    override fun onInterstitialDismissed(ad: Ad) {
                        mInterstitialAd = null
                        Log.d(TAG, "Facebook Interstitial dismissed")
                    }

                    override fun onAdClicked(ad: Ad) {
                        app.increaseAdClickCount()
                        Log.d(TAG, "Facebook Interstitial clicked")
                    }

                    override fun onLoggingImpression(ad: Ad) {
                        Log.d(TAG, "Facebook Interstitial impression logged")
                    }
                }).build()
        )
    }

    val playerActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                Handler(Looper.getMainLooper()).postDelayed({
                    mInterstitialAd?.show()
                }, 100)
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
            if (downloadedFileItems.isNotEmpty()) {

                if (noMediaLayout.visibility == View.VISIBLE) {
                    noMediaLayout.visibility = View.GONE
                }
                if (downloadedRecyclerViewWrapper.visibility == View.GONE) {
                    downloadedRecyclerViewWrapper.visibility = View.VISIBLE
                }

                if (app.aicpProtector()) {
                    if (frame.visibility == View.GONE) {
                        frame.visibility = View.VISIBLE
                    }
                    loadBanner(requireContext(), root)
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
            } catch (_: Exception) {
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
    private fun loadBanner(context: Context, root: View) {

        val adView = AdView(
            context,
            "245848558610696_680906295104918",
            AdSize.RECTANGLE_HEIGHT_250
        )

        // Find the Ad Container (from root view)
        val adContainer = root.findViewById<LinearLayout>(R.id.banner_container)
        adView.buildLoadAdConfig()
            .withAdListener(object : AdListener {

                override fun onAdLoaded(ad: Ad) {
                    app.increaseAdClickCount()
                    Log.d("FB_AD", "RECTANGLE loaded")
                }

                override fun onAdClicked(p0: Ad?) {

                }

                override fun onLoggingImpression(p0: Ad?) {

                }

                override fun onError(ad: Ad, adError: AdError) {
                    Log.e("FB_AD", "ERROR: ${adError.errorMessage}")
                }
            })
        // Add the ad view to layout
        adContainer.removeAllViews()
        adContainer.addView(adView)

        // Load the ad
        adView.loadAd()
    }


    private var downloadedFileItems: ArrayList<DownloadedFileItem> = ArrayList()

   lateinit var downloadedRecyclerView: RecyclerView
   lateinit var downloadedRecyclerViewWrapper: LinearLayout

   lateinit var noMediaLayout: LinearLayout


    lateinit var app: Application

    lateinit var frame: LinearLayout

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
                    if (frame.visibility == View.VISIBLE) {
                        frame.visibility = View.GONE
                    }
                }
            }


        frame = root.findViewById(R.id.banner_container)
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

                    if (frame.visibility == View.VISIBLE) {
                        frame.visibility = View.GONE
                    }
                } else {
                    if (noMediaLayout.visibility == View.VISIBLE) {
                        noMediaLayout.visibility = View.GONE
                    }
                    if (downloadedRecyclerView.visibility == View.GONE) {
                        downloadedRecyclerView.visibility = View.VISIBLE
                    }

                    if (app.aicpProtector()) {
                        if (frame.visibility == View.GONE) {
                            frame.visibility = View.VISIBLE
                        }
                        loadBanner(requireContext(), root)
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

