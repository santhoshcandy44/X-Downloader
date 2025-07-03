plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    id ("kotlin-android")
}

android {
    namespace = "com.x.twitter.video.downloader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.x.twitter.video.downloader"
        minSdk = 21
        targetSdk = 35
        versionCode = 8
        versionName = "1.2.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String" ,"BASE_URL","\"https://v2-x-api.25122022.xyz\"")
        buildConfigField("String" ,"REFERER","\"com.x.twitter.video.downloader.referer\"")
    }

    buildTypes {


        release {
            isMinifyEnabled =true
            isShrinkResources =true
            multiDexEnabled =false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        debug{
            isMinifyEnabled=false
            proguardFiles (getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig=true
        viewBinding=true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    //implementation 'androidx.navigation:navigation-fragment-ktx:2.5.3'
    //implementation 'androidx.navigation:navigation-ui-ktx:2.5.3'
    implementation("com.google.firebase:firebase-common-ktx:21.0.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.android.volley:volley:1.2.1")

    implementation("com.github.bumptech.glide:glide:4.14.2")

    implementation("com.google.android.flexbox:flexbox:3.0.0")

    implementation("com.github.ybq:Android-SpinKit:1.4.0")

    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // def media2_version = "1.2.1"

    // Interacting with MediaSessions
    //   implementation "androidx.media2:media2-session:$media2_version"
    // optional - UI widgets for VideoView and MediaControlView
    //implementation "androidx.media2:media2-widget:$media2_version"
    // optional - Implementation of a SessionPlayer
    //implementation "androidx.media2:media2-player:$media2_version"

    implementation("androidx.annotation:annotation:1.8.0")
    //implementation 'com.facebook.android:audience-network-sdk:6.13.7'
    implementation("com.google.android.gms:play-services-ads:23.2.0")
    implementation("com.google.ads.mediation:facebook:6.17.0.0")

    //facebook ads
    implementation("com.facebook.android:facebook-android-sdk:[8,9)")

    implementation("com.google.android.ump:user-messaging-platform:3.0.0")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // Add the dependencies for the Firebase Cloud Messaging and Analytics libraries
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation ("com.google.firebase:firebase-messaging")
    implementation ("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-config")


    val lifecycle_version = "2.8.3"
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    ksp("androidx.lifecycle:lifecycle-compiler:$lifecycle_version")

    implementation ("androidx.activity:activity-ktx:1.7.2")

    implementation ("com.google.android.play:app-update:2.1.0")
    implementation ("com.google.android.play:app-update-ktx:2.1.0")
    implementation ("org.jetbrains.kotlin:kotlin-script-runtime:1.8.0")


    val room_version = "2.6.1"

    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    // optional - Kotlin extensions and Coroutines support for Room
    implementation ("androidx.room:room-ktx:$room_version")


}