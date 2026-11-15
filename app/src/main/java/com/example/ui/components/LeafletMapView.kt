package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.FlatmateListing
import org.json.JSONArray
import org.json.JSONObject

data class MapPinData(
    val listing: FlatmateListing,
    val matchScore: Int
)

private class CoM8MapBridge(
    private val onPinSelected: (String) -> Unit
) {
    @JavascriptInterface
    fun selectPin(listingId: String) {
        onPinSelected(listingId)
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun LeafletMapView(
    pins: List<MapPinData>,
    selectedListingId: String?,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    zoomLevel: Int = 13,
    interactive: Boolean = true
) {
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageLoaded by remember { mutableStateOf(false) }

    val pinsJson = remember(pins, selectedListingId) {
        val arr = JSONArray()
        pins.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.listing.id)
                    put("name", item.listing.name)
                    put("locality", item.listing.locality)
                    put("city", item.listing.city)
                    put("rent", item.listing.monthlyRent)
                    put("lat", item.listing.latitude)
                    put("lng", item.listing.longitude)
                    put("score", item.matchScore)
                    put("selected", item.listing.id == selectedListingId)
                }
            )
        }
        arr.toString()
    }

    LaunchedEffect(pinsJson, pageLoaded, zoomLevel, interactive) {
        val wv = webViewRef
        if (wv != null && pageLoaded) {
            val safeJson = JSONObject.quote(pinsJson)
            wv.evaluateJavascript(
                "window.renderCoM8Pins && window.renderCoM8Pins($safeJson, $zoomLevel, $interactive);",
                null
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEEF2F8))
            .testTag("leaflet_map_view")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(AndroidColor.parseColor("#EEF2F8"))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.userAgentString = "CoM8AndroidApp/1.0 (https://com8.app)"
                    webChromeClient = WebChromeClient()
                    if (!interactive) {
                        isClickable = false
                        isFocusable = false
                        setOnTouchListener { _, _ -> false }
                    } else {
                        setOnTouchListener { v, _ ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                            false
                        }
                    }
                    addJavascriptInterface(
                        CoM8MapBridge { id ->
                            post { currentOnMarkerClick(id) }
                        },
                        "CoM8Bridge"
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            pageLoaded = true
                            val safeJson = JSONObject.quote(pinsJson)
                            view?.evaluateJavascript(
                                "window.renderCoM8Pins && window.renderCoM8Pins($safeJson, $zoomLevel, $interactive);",
                                null
                            )
                        }
                    }
                    loadDataWithBaseURL(
                        "https://unpkg.com/",
                        LEAFLET_MAP_HTML,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    webViewRef = this
                }
            },
            update = { view ->
                webViewRef = view
            }
        )
    }
}

/**
 * Compact embedded map box used inside swipe cards so users immediately see the flat's neighborhood
 * and OpenStreetMap location without interfering with smooth horizontal card swipe gestures.
 */
@Composable
fun CardMiniMapBox(
    listing: FlatmateListing,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFEAF0FA))
            .testTag("card_mini_map_box")
    ) {
        // Instant vector street-map backdrop + live non-blocking Leaflet mini-map overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft park & water accents
            drawCircle(
                color = Color(0xFFD6F3E4),
                radius = h * 0.55f,
                center = Offset(w * 0.18f, h * 0.25f)
            )
            drawCircle(
                color = Color(0xFFDCE8FC),
                radius = h * 0.65f,
                center = Offset(w * 0.85f, h * 0.8f)
            )

            // Subtle city block grid lines
            val gridColor = Color(0xFFD5DFEE)
            for (i in 1..4) {
                val y = h * (i / 5f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 3f)
            }
            for (i in 1..5) {
                val x = w * (i / 6f)
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 3f)
            }

            // Arterial avenue curve
            val roadPath = Path().apply {
                moveTo(0f, h * 0.72f)
                cubicTo(w * 0.35f, h * 0.62f, w * 0.55f, h * 0.35f, w, h * 0.24f)
            }
            drawPath(
                path = roadPath,
                color = Color.White,
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )

            // Neighborhood radius ring around center pin
            drawCircle(
                color = Color(0xFF2D4BFF).copy(alpha = 0.14f),
                radius = h * 0.32f,
                center = Offset(w * 0.5f, h * 0.46f)
            )
        }

        LeafletMapView(
            pins = listOf(MapPinData(listing, 95)),
            selectedListingId = listing.id,
            onMarkerClick = {},
            zoomLevel = 14,
            interactive = false,
            modifier = Modifier.fillMaxSize()
        )

        // Crisp floating location pill at bottom-left of mini map
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.94f),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Column {
                    Text(
                        text = "${listing.locality}, ${listing.city}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private val LEAFLET_MAP_HTML = """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    html, body, #map {
      width: 100%;
      height: 100%;
      background: transparent;
      font-family: system-ui, -apple-system, sans-serif;
    }
    .com8-pin {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 5px 10px;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 700;
      color: #111827;
      background: #FFFFFF;
      border: 2px solid #2D4BFF;
      box-shadow: 0 4px 12px rgba(17, 24, 39, 0.16);
      white-space: nowrap;
    }
    .com8-pin.active {
      background: #2D4BFF;
      color: #FFFFFF;
      border-color: #FFFFFF;
      transform: scale(1.06);
      z-index: 999;
    }
    .com8-score {
      color: #10B981;
      font-weight: 800;
    }
    .com8-pin.active .com8-score {
      color: #A7F3D0;
    }
    .leaflet-control-attribution {
      font-size: 9px !important;
      background: rgba(255,255,255,0.75) !important;
    }
  </style>
</head>
<body>
  <div id="map"></div>
  <script>
    var map = L.map('map', {
      zoomControl: false,
      attributionControl: true
    }).setView([18.5590, 73.7868], 13);

    var primaryTiles = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap'
    });
    var fallbackTiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap'
    });
    primaryTiles.on('tileerror', function() {
      if (!map.hasLayer(fallbackTiles)) {
        fallbackTiles.addTo(map);
      }
    });
    primaryTiles.addTo(map);

    var markerLayer = L.layerGroup().addTo(map);
    var radiusCircle = null;

    if (window.ResizeObserver) {
      new ResizeObserver(function() {
        map.invalidateSize();
      }).observe(document.getElementById('map'));
    }

    window.renderCoM8Pins = function(jsonStr, zoom, interactive) {
      try {
        map.invalidateSize();
        if (interactive === false) {
          map.dragging.disable();
          map.touchZoom.disable();
          map.doubleClickZoom.disable();
          map.scrollWheelZoom.disable();
        } else {
          map.dragging.enable();
          map.touchZoom.enable();
          map.doubleClickZoom.enable();
        }

        var pins = JSON.parse(jsonStr);
        markerLayer.clearLayers();
        if (radiusCircle) {
          map.removeLayer(radiusCircle);
          radiusCircle = null;
        }
        if (!pins || pins.length === 0) return;

        var activePin = null;
        var bounds = [];

        pins.forEach(function(p) {
          bounds.push([p.lat, p.lng]);
          if (p.selected) activePin = p;

          var rentK = (p.rent / 1000).toFixed(1).replace(/\.0$/, '') + 'k';
          var html = '<div class="com8-pin ' + (p.selected ? 'active' : '') + '">' +
                     '<span>&#8377;' + rentK + '</span>' +
                     '<span class="com8-score">' + p.score + '%</span>' +
                     '</div>';

          var icon = L.divIcon({
            className: '',
            html: html,
            iconSize: [92, 28],
            iconAnchor: [46, 14]
          });

          var marker = L.marker([p.lat, p.lng], { icon: icon }).addTo(markerLayer);
          if (interactive !== false) {
            marker.bindPopup('<b>' + p.name + '</b><br/>' + p.locality + ', ' + p.city + '<br/>&#8377;' + p.rent + '/mo');
            marker.on('click', function() {
              if (window.CoM8Bridge && window.CoM8Bridge.selectPin) {
                window.CoM8Bridge.selectPin(p.id);
              }
            });
          }
        });

        setTimeout(function() {
          map.invalidateSize();
          if (activePin) {
            map.setView([activePin.lat, activePin.lng], zoom || 14, { animate: false });
            radiusCircle = L.circle([activePin.lat, activePin.lng], {
              color: '#2D4BFF',
              fillColor: '#2D4BFF',
              fillOpacity: 0.12,
              weight: 1.5,
              radius: 550
            }).addTo(map);
          } else if (bounds.length === 1) {
            map.setView(bounds[0], zoom || 13, { animate: false });
          } else if (bounds.length > 1) {
            map.fitBounds(bounds, { padding: [32, 32], maxZoom: 13 });
          }
        }, 60);
      } catch (e) {
        console.error('renderCoM8Pins error', e);
      }
    };
  </script>
</body>
</html>
""".trimIndent()
