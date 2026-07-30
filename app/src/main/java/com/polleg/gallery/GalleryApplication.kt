package com.polleg.gallery

import android.app.Application

class GalleryApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContainer(this)
    }
}
