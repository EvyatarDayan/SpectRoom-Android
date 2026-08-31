package com.example.spectroom.ui.screens.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.DrawableCompat

private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val MARKER_IMAGE_ID = "spectroom-map-pin"

@Composable
fun AddressMapPreview(
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lat = latitude ?: 62.0
    val lon = longitude ?: 16.0
    val zoom = if (latitude != null && longitude != null) 13.0 else 5.5
    val showMarker = latitude != null && longitude != null

    var maplibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { style ->
                    val markerDrawable = ContextCompat.getDrawable(
                        context,
                        org.maplibre.android.R.drawable.maplibre_marker_icon_default,
                    ) ?: return@setStyle
                    DrawableCompat.setTint(markerDrawable, TabSelected.toArgb())
                    style.addImage(MARKER_IMAGE_ID, markerDrawable.toBitmap())

                    map.uiSettings.apply {
                        isScrollGesturesEnabled = false
                        isZoomGesturesEnabled = false
                        isRotateGesturesEnabled = false
                        isTiltGesturesEnabled = false
                        isLogoEnabled = true
                        setAttributionMargins(0, 0, 8, 8)
                    }

                    val manager = SymbolManager(this@apply, map, style).apply {
                        iconAllowOverlap = true
                        iconIgnorePlacement = true
                    }
                    symbolManager = manager
                    maplibreMap = map
                }
            }
        }
    }

    LaunchedEffect(lat, lon, zoom, showMarker, maplibreMap, symbolManager) {
        val map = maplibreMap ?: return@LaunchedEffect
        val manager = symbolManager ?: return@LaunchedEffect

        map.cameraPosition = CameraPosition.Builder()
            .target(LatLng(lat, lon))
            .zoom(zoom)
            .build()

        manager.deleteAll()
        if (showMarker) {
            manager.create(
                SymbolOptions()
                    .withLatLng(LatLng(lat, lon))
                    .withIconImage(MARKER_IMAGE_ID)
                    .withIconSize(1.1f)
                    .withIconAnchor("bottom"),
            )
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.border.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .shadow(8.dp, RoundedCornerShape(16.dp), clip = false),
    )
}
