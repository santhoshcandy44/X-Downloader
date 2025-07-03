package com.x.twitter.video.downloader


import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.MobileAds
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationBarView
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.*
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.x.twitter.video.downloader.databinding.ActivityMainBinding
import com.x.twitter.video.downloader.ui.alldownloads.AllDownloadsFragment
import com.x.twitter.video.downloader.ui.home.HomeFragment
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import com.facebook.appevents.AppEventsLogger;

class MainActivity : AppCompatActivity(),
    NavigationBarView.OnItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener {


    /**
     * This function assumes logger is an instance of AppEventsLogger and has been
     * created using AppEventsLogger.newLogger() call.
     */


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {


        sharedPreferences?.let {

            if (key.equals("ad_type")) {
                val adType = adMeta.getString("ad_type", null)

                (application as Application).AD_TYPE = if (adType == null) {
                    null
                } else {
                    BaseApplication.AdType.valueOf(adType)
                }

                homeFragmentAdHideListener?.let {

                    homeFragmentAdHideListener!!.adShow()
                }

                allDownloadsFragmentAdHideListener?.let {
                    allDownloadsFragmentAdHideListener!!.adShow()
                }

            }

            if (key.equals("ad_click_count")) {
                val adClickCount = sharedPreferences
                    .getInt("ad_click_count", 0)

                val app = application as Application


                if (adClickCount > app.MAX_AD_CLICK_COUNT) {


                    homeFragmentAdHideListener?.let {
                        homeFragmentAdHideListener!!.adDismiss()
                    }

                    allDownloadsFragmentAdHideListener?.let {
                        allDownloadsFragmentAdHideListener!!.adDismiss()
                    }

                    val editor = sharedPreferences.edit()
                    editor.remove("ad_click_count")
                    editor.putLong(
                        "ban_end_time", System.currentTimeMillis()
                                + (24 * 3600 * 1000)
                    )
                    editor.commit()
                }
            }

        }

    }

    var homeFragmentAdHideListener: AdHideListener? = null
    var allDownloadsFragmentAdHideListener: AdHideListener? = null

    private val homeFragment: HomeFragment = HomeFragment()
    private val allDownloadsFragment: AllDownloadsFragment = AllDownloadsFragment()
    private val moreFragment: MoreFragment = MoreFragment()
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!


    private val FLEXIBLE_UPDATE_REQ_CODE = 132
    private val IMMEDIATE_UPDATE_REQ_CODE = 133

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var completedAppUpdateInfo: AppUpdateInfo
    private lateinit var adMeta: SharedPreferences
    private lateinit var appMeta: SharedPreferences
    var updateType = -1

    private lateinit var consentInformation: ConsentInformation
    // Use an atomic boolean to initialize the Google Mobile Ads SDK and load ads once.
    private var isMobileAdsInitializeCalled = AtomicBoolean(false)

    private var TAG="TAG$123"
    private fun requestConsentForm(){


        // Create a ConsentRequestParameters object.
        val params = ConsentRequestParameters

            .Builder()
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this@MainActivity,
                    ConsentForm.OnConsentFormDismissedListener {
                            loadAndShowError ->
                        // Consent gathering failed.
                        Log.w(TAG, String.format("%s: %s",
                            loadAndShowError?.errorCode,
                            loadAndShowError?.message))

                        // Consent has been gathered.
                        if (consentInformation.canRequestAds()) {
                            initializeMobileAdsSdk()
                        }
                    }
                )
            },
            {
                    requestConsentError ->
                // Consent gathering failed.
                Log.w(TAG, String.format("%s: %s",
                    requestConsentError.errorCode,
                    requestConsentError.message))
            })

        // Check if you can initialize the Google Mobile Ads SDK in parallel
        // while checking for new consent information. Consent obtained in
        // the previous session can be used to request ads.
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk()
        }

    }
    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Initialize the Google Mobile Ads SDK.
        MobileAds.initialize(this)

        // TODO: Request an ad.
        // InterstitialAd.load(...)
    }

    lateinit var appTitleView:LinearLayout
    lateinit var allDownloadsView:LinearLayout
    lateinit var moreView:LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)


        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        _binding = ActivityMainBinding.inflate(layoutInflater)

        ViewCompat.setOnApplyWindowInsetsListener(binding.statusBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }


        setContentView(binding.root)
        appTitleView=findViewById(R.id.app_title)
        allDownloadsView=findViewById(R.id.all_downloads_bar)
        moreView=findViewById(R.id.more_bar)
        requestConsentForm()


        updateType = (application as Application).UPDATE_TYPE

        adMeta = getSharedPreferences("ad_meta", Context.MODE_PRIVATE)
        adMeta.registerOnSharedPreferenceChangeListener(this)

        appMeta = getSharedPreferences("app_meta", Context.MODE_PRIVATE)

        val isFirstTime = appMeta.getBoolean("app_launch", true)
        appMeta.edit().apply {
            putInt(
                "tip_launch_count", appMeta.getInt(
                    "tip_launch_count", 0
                ) + 1
            )
            commit()
        }

        val totalLaunchCount = appMeta.getInt("tip_launch_count", 1)
        val quickTipOneNeverShow = appMeta.getBoolean("qt1_never_show", false)

        if (updateType == 0) {
            appUpdateManager = AppUpdateManagerFactory.create(this)
            val updateInfo = appUpdateManager.appUpdateInfo
            updateInfo.addOnSuccessListener(this) {

                completedAppUpdateInfo = it

                when (it.updateAvailability()) {

                    UpdateAvailability.UPDATE_AVAILABLE -> {

                        if (it.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                            try {
                                appUpdateManager.startUpdateFlowForResult(
                                    it, AppUpdateType.IMMEDIATE, this, IMMEDIATE_UPDATE_REQ_CODE
                                )
                            } catch (e: IntentSender.SendIntentException) {
                                e.printStackTrace()
                            }
                        }
                    }

                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        if (it.installStatus == InstallStatus.DOWNLOADED) {

                            updateDownloadedDialog(appUpdateManager)
                                .show()

                        }
                    }


                    UpdateAvailability.UPDATE_NOT_AVAILABLE -> {

                    }

                    UpdateAvailability.UNKNOWN -> {

                    }

                }

            }


        } else {
            val lastTimeUpdate = appMeta.getLong("last_time_update", 0)


            if (lastTimeUpdate <= 0) {
                appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
                invokeUpdateFlow(appUpdateManager)


            } else {


                if (System.currentTimeMillis() - lastTimeUpdate > if (BuildConfig.DEBUG) TimeUnit.MINUTES.toMillis(
                        1
                    ) else TimeUnit.DAYS.toMillis(4)
                ) {
                    appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
                    invokeUpdateFlow(appUpdateManager)
                } else {
                    appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
                    invokeUpdateFlow(appUpdateManager, invokeFlow = false)
                }
            }

        }





        if (isFirstTime) {
            //first time
        } else {

            if (totalLaunchCount.rem(10) == 0) {

                if (!quickTipOneNeverShow) {

                    QuickTipOneDialog(

                    )
                        .show(
                            supportFragmentManager, "qt1"
                        )


                }
                appMeta.edit().apply {
                    remove("tip_launch_count")
                    commit()
                }

            }
        }

        if (savedInstanceState != null) {

            when (savedInstanceState.getInt("bottom_nav")) {
                R.id.navigation_home -> {
                    appTitleView.visibility=View.VISIBLE
                    allDownloadsView.visibility=View.GONE
                    moreView.visibility=View.GONE

                    //  supportActionBar!!.title = "Video Downloader for X"

                }
                R.id.navigation_all_downloads -> {
                    appTitleView.visibility=View.GONE
                    allDownloadsView.visibility=View.VISIBLE
                    moreView.visibility=View.GONE
               //     supportActionBar!!.title = "All Downloads"

                }
                else -> {
                    appTitleView.visibility=View.GONE
                    allDownloadsView.visibility=View.GONE
                    moreView.visibility=View.VISIBLE
                 //   supportActionBar!!.title = "More"

                }
            }
        } else {

           // supportActionBar!!.title = "Video Downloader for X"

            appTitleView.visibility=View.VISIBLE
            allDownloadsView.visibility=View.GONE
            moreView.visibility=View.GONE

            fragment = homeFragment
            supportFragmentManager.beginTransaction()
                .add(R.id.nav_host, fragment, "0")
                .commit()

        }


        binding.navView.setOnItemSelectedListener(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.navView.selectedItemId == R.id.navigation_home) {
                    finish()

                } else {
                    binding.navView.selectedItemId = R.id.navigation_home

                }
            }

        })


    }


    override fun onResume() {
        super.onResume()

        if (updateType == 0) {
            val updateInfo = appUpdateManager.appUpdateInfo
            updateInfo.addOnSuccessListener(this) {

                completedAppUpdateInfo = it

                when (it.updateAvailability()) {

                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {

                        if (it.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                            try {
                                appUpdateManager.startUpdateFlowForResult(
                                    it, AppUpdateType.IMMEDIATE, this, IMMEDIATE_UPDATE_REQ_CODE
                                )
                            } catch (e: IntentSender.SendIntentException) {
                                e.printStackTrace()
                            }
                        }
                    }

                }
            }
        }
    }

    private fun invokeUpdateFlow(appUpdateManager: AppUpdateManager, invokeFlow: Boolean = true) {

        val updateInfo = appUpdateManager.appUpdateInfo
        updateInfo.addOnSuccessListener(this) {

            completedAppUpdateInfo = it

            when (it.updateAvailability()) {

                UpdateAvailability.UPDATE_AVAILABLE -> {

                    if (it.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {

                        try {
                            if (!invokeFlow) {

                                if (binding.updateContainer.visibility == View.GONE) {
                                    binding.updateContainer.visibility = View.VISIBLE
                                }
                                binding.updateBtn.setOnClickListener {
                                    binding.updateContainer.visibility = View.GONE
                                    invokeUpdateFlow(appUpdateManager)
                                }

                            } else {
                                appUpdateManager.startUpdateFlowForResult(
                                    it, AppUpdateType.FLEXIBLE, this, FLEXIBLE_UPDATE_REQ_CODE
                                )
                                appMeta.edit().apply {
                                    putLong("last_time_update", System.currentTimeMillis())
                                    commit()
                                }
                            }

                        } catch (e: IntentSender.SendIntentException) {
                            e.printStackTrace()
                        }
                    }
                }

                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    if (it.installStatus == InstallStatus.DOWNLOADED) {

                        updateDownloadedDialog(appUpdateManager)
                            .show()

                    }
                }


                UpdateAvailability.UPDATE_NOT_AVAILABLE -> {

                }

                UpdateAvailability.UNKNOWN -> {

                }

            }


        }
        appUpdateManager.registerListener(installStateUpdateListener)

    }

    private fun updateDownloadedDialog(appUpdateManager: AppUpdateManager): AlertDialog {

        return AlertDialog.Builder(this)
            .apply {
                setTitle("Update Downloaded & Install Now")
                setMessage("Successfully app update downloaded and install it now.")
                    .setPositiveButton(
                        "Install"
                    ) { dialog, which ->
                        dialog.dismiss()
                        appUpdateManager.completeUpdate()

                    }
                    .setNegativeButton(
                        "Not Now"
                    ) { dialog, which ->
                        dialog.dismiss()

                        if (binding.installContainer.visibility == View.GONE) {
                            binding.installContainer.visibility = View.VISIBLE
                        }
                        binding.installBtn.setOnClickListener {
                            binding.installContainer.visibility = View.GONE
                            appUpdateManager.completeUpdate()
                        }

                    }

                setOnDismissListener {

                    if (completedAppUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        if (completedAppUpdateInfo.installStatus
                            == InstallStatus.DOWNLOADED
                        ) {
                            if (binding.installContainer.visibility == View.GONE) {
                                binding.installContainer.visibility = View.VISIBLE
                            }
                            binding.installBtn.setOnClickListener {
                                binding.installContainer.visibility = View.GONE
                                appUpdateManager.completeUpdate()
                            }
                        }

                    }
                }
            }.create()
    }

    private val installStateUpdateListener = InstallStateUpdatedListener { appUpdateInfo ->
        val totalBytesToDownload=appUpdateInfo.totalBytesToDownload()

        when (appUpdateInfo.installStatus) {
            InstallStatus.PENDING -> {

                if(totalBytesToDownload>0){
                    if (binding.updateContainer.visibility == View.VISIBLE) {
                        binding.updateContainer.visibility = View.GONE
                    }

                    if (binding.updateDownloadProgressContainer.visibility == View.GONE) {
                        binding.updateDownloadProgressContainer.visibility = View.VISIBLE
                    }
                    if (binding.installContainer.visibility == View.VISIBLE) {
                        binding.installContainer.visibility = View.GONE
                    }
                    binding.updateDownloadProgressBar.progress = 0
                    binding.updateDownloadProgressText.text = "0%"
                }



            }
            InstallStatus.DOWNLOADING -> {

                if (totalBytesToDownload > 0) {

                    if (binding.updateContainer.visibility == View.VISIBLE) {
                        binding.updateContainer.visibility = View.GONE
                    }

                    if (binding.updateDownloadProgressContainer.visibility == View.GONE) {
                        binding.updateDownloadProgressContainer.visibility = View.VISIBLE
                    }
                    if (binding.installContainer.visibility == View.VISIBLE) {
                        binding.installContainer.visibility = View.GONE
                    }
                    val percentage =
                        (appUpdateInfo.bytesDownloaded * 100 / totalBytesToDownload).toInt()

                    binding.updateDownloadProgressBar.progress = percentage.toInt()
                    binding.updateDownloadProgressText.text = "${percentage.toInt()}%"

                }

            }

            InstallStatus.DOWNLOADED -> {

                binding.updateDownloadProgressBar.progress = 100
                binding.updateDownloadProgressText.text = "$100%"

                if (binding.updateContainer.visibility == View.VISIBLE) {
                    binding.updateContainer.visibility = View.GONE
                }

                if (binding.updateDownloadProgressContainer.visibility == View.VISIBLE) {
                    binding.updateDownloadProgressContainer.visibility = View.GONE
                }
                binding.updateDownloadProgressBar.progress = 0
                binding.updateDownloadProgressText.text = "0%"

                if (binding.installContainer.visibility == View.VISIBLE) {
                    binding.installContainer.visibility = View.GONE
                }


                updateDownloadedDialog(appUpdateManager).show()
            }
            InstallStatus.INSTALLING -> {
                if (binding.installContainer.visibility == View.VISIBLE) {
                    binding.installContainer.visibility = View.GONE
                }
                if (binding.updateDownloadProgressContainer.visibility == View.VISIBLE) {
                    binding.updateDownloadProgressContainer.visibility = View.GONE
                }
                binding.updateDownloadProgressBar.progress = 0
                binding.updateDownloadProgressText.text = "0%"


            }
            InstallStatus.INSTALLED -> {

                if (binding.updateContainer.visibility == View.VISIBLE) {
                    binding.updateContainer.visibility = View.GONE
                }

                if (binding.updateDownloadProgressContainer.visibility == View.VISIBLE) {
                    binding.updateDownloadProgressContainer.visibility = View.GONE
                }

                binding.updateDownloadProgressBar.progress = 0
                binding.updateDownloadProgressText.text = "0%"


                if (binding.installContainer.visibility == View.VISIBLE) {
                    binding.installContainer.visibility = View.GONE
                }

                Toast.makeText(
                    this, "Update Installed", Toast.LENGTH_SHORT
                ).show()
            }
            InstallStatus.CANCELED -> {

                if (binding.updateDownloadProgressContainer.visibility == View.VISIBLE) {
                    binding.updateDownloadProgressContainer.visibility = View.GONE
                }
                binding.updateDownloadProgressBar.progress = 0
                binding.updateDownloadProgressText.text = "0%"

                if (completedAppUpdateInfo.updateAvailability()
                    == UpdateAvailability.UPDATE_AVAILABLE
                ) {
                    if (binding.updateContainer.visibility == View.GONE) {
                        binding.updateContainer.visibility = View.VISIBLE
                    }
                    binding.updateBtn.setOnClickListener {
                        binding.updateContainer.visibility = View.GONE
                        invokeUpdateFlow(appUpdateManager)

                    }
                }


            }
            InstallStatus.FAILED -> {
                val installErrorCode = appUpdateInfo.installErrorCode

                if (binding.installContainer.visibility == View.VISIBLE) {
                    binding.installContainer.visibility = View.GONE
                }

                if (binding.updateDownloadProgressContainer.visibility == View.VISIBLE) {
                    binding.updateDownloadProgressContainer.visibility = View.GONE
                }
                binding.updateDownloadProgressBar.progress = 0
                binding.updateDownloadProgressText.text = "0%"

                if (completedAppUpdateInfo.updateAvailability()
                    == UpdateAvailability.UPDATE_AVAILABLE
                ) {
                    if (binding.updateContainer.visibility == View.GONE) {
                        binding.updateContainer.visibility = View.VISIBLE
                    }
                    binding.updateBtn.setOnClickListener {
                        binding.updateContainer.visibility = View.GONE
                        invokeUpdateFlow(appUpdateManager)

                    }
                }

            }
            InstallStatus.UNKNOWN -> {
                Toast.makeText(
                    this, "unknown", Toast.LENGTH_SHORT
                ).show()
            }

            InstallStatus.REQUIRES_UI_INTENT -> {

            }
        }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMMEDIATE_UPDATE_REQ_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    Toast.makeText(this, "App update started", Toast.LENGTH_SHORT)
                        .show()
                }
                RESULT_CANCELED -> {
                    Toast.makeText(this, "Without update can't use app", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            }
        }

        if (requestCode == FLEXIBLE_UPDATE_REQ_CODE) {
            if (resultCode == RESULT_OK) {

            } else {

                if (completedAppUpdateInfo.updateAvailability()
                    == UpdateAvailability.UPDATE_AVAILABLE
                ) {
                    if (binding.updateContainer.visibility == View.GONE) {
                        binding.updateContainer.visibility = View.VISIBLE
                    }

                    binding.updateBtn.setOnClickListener {
                        binding.updateContainer.visibility = View.GONE
                        invokeUpdateFlow(appUpdateManager)

                    }
                }
            }
        }
    }

    lateinit var fragment: Fragment

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        val fm = supportFragmentManager

        if(item.itemId== R.id.navigation_home){
           // supportActionBar!!.title = "Video Downloader for X"

            appTitleView.visibility=View.VISIBLE
            allDownloadsView.visibility=View.GONE
            moreView.visibility=View.GONE

            if (fm.findFragmentByTag("0") != null) {
                fragment = fm.findFragmentByTag("0")!!
                fm.beginTransaction().show(
                    fragment
                ).commit()


            } else {

                fragment = homeFragment
                fm.beginTransaction().add(
                    R.id.nav_host,
                    fragment, "0"
                ).commit()
            }


            if (fm.findFragmentByTag("1") != null) {
                fragment = fm.findFragmentByTag("1")!!
                fm.beginTransaction().hide(
                    fragment
                ).commit()


            }

            if (fm.findFragmentByTag("2") != null) {
                fragment = fm.findFragmentByTag("2")!!
                fm.beginTransaction().hide(
                    fragment
                ).commit()


            }

            return true
        }

        if(item.itemId==R.id.navigation_all_downloads){
           // supportActionBar!!.title = "All Downloads"

            appTitleView.visibility=View.GONE
            allDownloadsView.visibility=View.VISIBLE
            moreView.visibility=View.GONE
            if (fm.findFragmentByTag("1") != null) {

                fragment = fm.findFragmentByTag("1")!!
                fm.beginTransaction().show(
                    fragment
                ).commit()

            } else {

                fragment = allDownloadsFragment
                fm.beginTransaction().add(
                    R.id.nav_host,
                    fragment, "1"
                ).commit()
            }


            if (fm.findFragmentByTag("0") != null) {
                fragment = fm.findFragmentByTag("0")!!
                fm.beginTransaction().hide(
                    fragment
                ).commit()

            }




            if (fm.findFragmentByTag("2") != null) {
                fragment = fm.findFragmentByTag("2")!!
                fm.beginTransaction().hide(
                    fragment
                ).commit()

            }


            return true
        }

        if(item.itemId==R.id.navigation_more){
         //   supportActionBar!!.title = "More"
            appTitleView.visibility=View.GONE
            allDownloadsView.visibility=View.GONE
            moreView.visibility=View.VISIBLE

            if (fm.findFragmentByTag("2") != null) {
                fragment = fm.findFragmentByTag("2")!!
                fm.beginTransaction().show(
                    fragment
                ).commit()

            } else {

                fragment = moreFragment
                fm.beginTransaction().add(
                    R.id.nav_host,
                    fragment, "2"
                ).commit()
            }


            if (fm.findFragmentByTag("0") != null) {
                fragment = fm.findFragmentByTag("0")!!
                fm.beginTransaction().hide(
                    fragment
                ).commit()

            }


            if (fm.findFragmentByTag("1") != null) {
                fragment = fm.findFragmentByTag("1")!!
                fm.beginTransaction().hide(
                    fragment
                ).commit()

            }


            return true
        }

        return true
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("bottom_nav", binding.navView.selectedItemId)
    }

    override fun onDestroy() {
        super.onDestroy()

        appMeta.edit().apply {

            if (appMeta.getBoolean(
                    "app_launch", true
                )
            ) {
                putBoolean(
                    "app_launch", false
                )
            }


            commit()
        }

        if (updateType == 1) {
            if (this::appUpdateManager.isInitialized) {
                appUpdateManager.unregisterListener(installStateUpdateListener)

            }

        }

        val fm = supportFragmentManager
        if (fm.findFragmentByTag("0") != null) {
            fm.beginTransaction().remove(
                fm.findFragmentByTag("0")!!

            ).commit()

        }

        if (fm.findFragmentByTag("1") != null) {
            fm.beginTransaction().remove(
                fm.findFragmentByTag("1")!!

            ).commit()

        }

        if (fm.findFragmentByTag("3") != null) {
            fm.beginTransaction().remove(
                fm.findFragmentByTag("3")!!

            ).commit()

        }
        _binding = null

    }


}



class QuickTipOneDialog() : DialogFragment() {
    lateinit var appMeta: SharedPreferences
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        appMeta = requireActivity().getSharedPreferences("app_meta", Context.MODE_PRIVATE)
        return quickTipOneDialog()
    }


    private fun quickTipOneDialog(): AlertDialog {
        val view = layoutInflater.inflate(
            R.layout.qt_one, null
        ) as ScrollView
        val quickTipOneDialog = AlertDialog.Builder(requireActivity()).apply {


            setView(
                view
            )


        }.create()

        view.findViewById<MaterialButton>(R.id.qt1_don_t_show_again).setOnClickListener {

            appMeta.edit().apply {
                putBoolean("qt1_never_show", true)
                commit()
            }
            quickTipOneDialog.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.qt1_dismiss).setOnClickListener {

            quickTipOneDialog.dismiss()
        }

        view.findViewById<ImageView>(R.id.qt1_dialog_close).setOnClickListener {

            quickTipOneDialog.dismiss()
        }


        return quickTipOneDialog


    }

}


