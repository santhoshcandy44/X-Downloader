package com.x.twitter.video.downloader

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import java.io.File


class PlayerActivity : AppCompatActivity() {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var styledPlayerView:PlayerView

    private val exoPlayerListener= object: Player.Listener{

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            if(playbackState==Player.STATE_IDLE){
                    Log.d("TAG_123","IDLE")
                styledPlayerView.keepScreenOn=false

            }
            if(playbackState==Player.STATE_BUFFERING){
                Log.d("TAG_123","IDLE")
                styledPlayerView.keepScreenOn=true

            }
            if(playbackState==Player.STATE_READY){
                Log.d("TAG_123","READY")
                styledPlayerView.keepScreenOn=true
                exoPlayer.playWhenReady=true

            }

            if(playbackState== Player.STATE_ENDED){
                Log.d("TAG_123","ENDED")
                styledPlayerView.keepScreenOn=false

                exoPlayer.playWhenReady=false
                exoPlayer.stop()
                exoPlayer.seekTo(0)

            }

        }

        @SuppressLint("IntentReset")
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)

            Toast.makeText(this@PlayerActivity,"Can't play, open with other",Toast.LENGTH_SHORT)
                .show()



            MediaScannerConnection
                .scanFile(
                    this@PlayerActivity,
                    arrayOf(intentData.path),
                    null
                ) { path, uri ->

                    val shareIntent = Intent(
                        Intent.ACTION_VIEW
                    ).apply {
                        type = "video/*"
                        data = uri
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    }

                    if(shareIntent.resolveActivity(packageManager)!=null){

                        startActivity(
                            Intent.createChooser(
                                shareIntent, "Open with"
                            )
                        )

                    }else{
                        Toast.makeText(this@PlayerActivity,"No app to play the video",Toast.LENGTH_SHORT)
                            .show()
                    }

                }





            Log.d("TAG_123","Play Error"+error.message)

        }
    }

    private lateinit var intentData: Uri

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun  onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_NO
        )

        setContentView(R.layout.activity_player)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResult(RESULT_OK)
                finish()
            }
        })

        supportActionBar!!.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        intentData=intent.data!!



        styledPlayerView=findViewById(R.id.player_view)
        styledPlayerView.setShowSubtitleButton(false)

        exoPlayer=ExoPlayer.Builder(applicationContext)
            .build()

        Log.d("TAG_123",intentData.toString())



        val df= DefaultDataSource.Factory(this)

        val pms= ProgressiveMediaSource.Factory(df)
        val pmsds=pms.createMediaSource(
            MediaItem.fromUri(
           intentData
            )
        )
        exoPlayer.setMediaSource(pmsds)
        styledPlayerView.player=exoPlayer
        styledPlayerView.requestFocus()
        exoPlayer.addListener(exoPlayerListener)

        exoPlayer.seekTo(0)
        exoPlayer.prepare()

        val audioManager=getSystemService(
            AUDIO_SERVICE
        ) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val result=audioManager.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_UNKNOWN)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener {

                        when(it){
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT->{
                                exoPlayer.pause()

                            }
                            AudioManager.AUDIOFOCUS_GAIN->{
                                exoPlayer.play()

                            }
                            AudioManager.AUDIOFOCUS_LOSS->{
                                exoPlayer.pause()

                            }
                        }



                    }.build()
            )
            if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            {

                exoPlayer.play()

            }
        } else {
           val result= audioManager.requestAudioFocus(
                { focusChange ->

                    when(focusChange){
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT->{
                            exoPlayer.pause()
                        }
                        AudioManager.AUDIOFOCUS_GAIN->{
                            exoPlayer.play()

                        }
                        AudioManager.AUDIOFOCUS_LOSS->{
                            exoPlayer.pause()

                        }
                    }

                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            {

                exoPlayer.play()

            }
        }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId==android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
       exoPlayer.playWhenReady=false

    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
        exoPlayer.removeListener(exoPlayerListener)
    }
}

