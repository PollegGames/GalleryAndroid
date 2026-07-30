package com.polleg.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.polleg.gallery.gallery.platform.MediaPermissionGateway
import com.polleg.gallery.gallery.platform.NativeMediaOpener
import com.polleg.gallery.gallery.ui.GalleryAction
import com.polleg.gallery.gallery.ui.GalleryRoute
import com.polleg.gallery.gallery.ui.GalleryViewModel
import com.polleg.gallery.gallery.ui.theme.GalleryTheme

class MainActivity : ComponentActivity() {
    private val permissionGateway by lazy { MediaPermissionGateway(this) }
    private val mediaOpener by lazy { NativeMediaOpener(this) }
    private val galleryViewModel: GalleryViewModel by viewModels {
        GalleryViewModel.factory((application as GalleryApplication).container)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        galleryViewModel.onAction(
            GalleryAction.PermissionStatusChanged(permissionGateway.currentStatus()),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GalleryTheme {
                GalleryRoute(
                    viewModel = galleryViewModel,
                    onRequestPermissions = {
                        permissionLauncher.launch(permissionGateway.permissionsToRequest())
                    },
                    onOpenMedia = mediaOpener::open,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        galleryViewModel.onAction(
            GalleryAction.PermissionStatusChanged(permissionGateway.currentStatus()),
        )
    }
}
