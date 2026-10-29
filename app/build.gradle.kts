plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.drpsphca.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.drpsphca.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 26102901
        versionName = "phcaapp-stable-26.10.29.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val wordpressApiUrl = providers.gradleProperty("WORDPRESS_API_URL").getOrElse("https://drpsphca.com/wp-json/")
        val wordpressApiKey = providers.gradleProperty("WORDPRESS_API_KEY").getOrElse("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjEsIm5hbWUiOiJkcnBzcGhjYSIsImlhdCI6MTc2OTQyOTMzNCwiZXhwIjoxOTI3MTA5MzM0fQ.-nKP0IFtPUgyvWb2hIsuFep424Xx2CYYCpITHR_GJoA")
        val tvalApiUrl = providers.gradleProperty("TVAL_API_URL").getOrElse("https://tval.drpsphca.com/wp-json/")
        val tvalApiKey = providers.gradleProperty("TVAL_API_KEY").getOrElse("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjYsIm5hbWUiOiJhcHBzeXN0dmFsIiwiaWF0IjoxNzg0ODczMjc5LCJleHAiOjE5NDI1NTMyNzl9.Gava56ERdrkmd9OE8pdFKlzcNnEzR6s8-HRduVUZWQA")
        val appxApiUrl = providers.gradleProperty("APPX_API_URL").getOrElse("https://app.drpsphca.com/wp-json/")
        val appxApiKey = providers.gradleProperty("APPX_API_KEY").getOrElse("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjUsIm5hbWUiOiJhcHBzeXNtYWluIiwiaWF0IjoxNzkxNTI0NDgwLCJleHAiOjE5NDkyMDQ0ODB9.btMGc-STMitsqthX6rQ_sTV5nM6DISI7vKZXuKWW8gM")

        buildConfigField("String", "WORDPRESS_API_URL", "\"$wordpressApiUrl\"")
        buildConfigField("String", "WORDPRESS_API_KEY", "\"$wordpressApiKey\"")
        buildConfigField("String", "TVAL_API_URL", "\"$tvalApiUrl\"")
        buildConfigField("String", "TVAL_API_KEY", "\"$tvalApiKey\"")
        buildConfigField("String", "APPX_API_URL", "\"$appxApiUrl\"")
        buildConfigField("String", "APPX_API_KEY", "\"$appxApiKey\"")
    }

    flavorDimensions.add("distribution")
    productFlavors {
        register("huawei") {
            dimension = "distribution"
            buildConfigField("String", "ADS_UNIT_ID", "\"b27ccpb8fh\"")
        }
        register("googlePlay") {
            dimension = "distribution"
            buildConfigField("String", "ADS_UNIT_ID", "\"ca-app-pub-5724714825846987/5308478635\"")
        }
        register("appTest") {
            dimension = "distribution"
            applicationId = "com.drpsphca.app.test"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Added a comment to trigger sync
}

dependencies {
    "huaweiImplementation"(libs.huawei.ads)
    "huaweiImplementation"(libs.huawei.appservice)
    "googlePlayImplementation"(libs.google.ads)
    "googlePlayImplementation"(libs.google.app.update)
    "googlePlayImplementation"(libs.google.app.update.ktx)
    implementation(libs.google.review)
    implementation(libs.google.review.ktx)
    implementation(libs.androidx.fragment)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.preview)
    implementation(libs.glancepreviewtool)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(project(":shared"))
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
}
