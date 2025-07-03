package com.x.twitter.video.downloader

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment


class MoreFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val root=inflater.inflate(R.layout.fragment_more, container, false)
        val shareWithFriends:CardView=root.findViewById(R.id.share_with_friends)
        val feedback:CardView=root.findViewById(R.id.feed_back)
        val rateUs:CardView=root.findViewById(R.id.rate_us)

        //val appVersion:CardView=root.findViewById(R.id.app_version)
        val appVersionText:TextView=root.findViewById(R.id.app_version_text)
        val requireContext: Context =requireContext()


        val packageManager=requireContext.packageManager
        val packageName=requireContext.applicationContext.packageName

        val pi=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){

            packageManager.getPackageInfo(packageName,PackageManager.PackageInfoFlags.of(0))

        }else{
            packageManager.getPackageInfo(packageName,0)
        }

        val versionName=pi.versionName
        appVersionText.text="Version: v$versionName"

        shareWithFriends.setOnClickListener {

            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Video Downloader for X")
                var shareMessage = "\nVideo Downloader for X (Download It from PlayStore)\n\n"
                shareMessage =
                    """
                    ${shareMessage}https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}
                    
            
                    """.trimIndent()
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: Exception) {
                Toast.makeText(requireContext,e.message,Toast.LENGTH_SHORT)
                    .show()
            }


        }

        feedback.setOnClickListener {

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("sherlobamboo@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Video Downloader for X Application Feedback")
            }

            try{
                startActivity(Intent.createChooser(emailIntent, "Send mail..."))
            }catch(e:ActivityNotFoundException){
                Toast.makeText(
                    requireContext,
                    "There is no email app installed...", Toast.LENGTH_SHORT
                ).show()
            }
        }

        rateUs.setOnClickListener {
            try{
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "market://details?id=${packageName}"
                        )
                    )
                )
            }catch(_:ActivityNotFoundException){
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://play.google.com/store/apps/setails?id=${packageName}"
                    )
                )
            }

        }


        return root
    }

}