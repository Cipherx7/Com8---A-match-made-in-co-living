package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CoM8Catalog
import com.example.ui.components.LeafletMapView
import com.example.ui.components.MapPinData
import com.example.ui.components.MatchScoreBadge
import com.example.ui.viewmodel.ScoredCandidate

@Composable
fun MapExploreScreen(
    candidates: List<ScoredCandidate>,
    preferredCities: List<String>,
    selectedCity: String,
    selectedCandidateId: String?,
    onCitySelected: (String) -> Unit,
    onSelectCandidate: (String) -> Unit,
    onInspectCandidate: (ScoredCandidate) -> Unit,
    onLikeCandidate: (ScoredCandidate) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCandidate = candidates.firstOrNull { it.listing.id == selectedCandidateId }
        ?: candidates.firstOrNull()
    val mapPins = candidates.map { MapPinData(it.listing, it.matchScore) }
    val cityFilters = if (preferredCities.isNotEmpty()) {
        listOf("All Cities") + preferredCities
    } else {
        CoM8Catalog.supportedCities
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (cityFilters.size > 2) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(cityFilters) { city ->
                    FilterChip(
                        selected = selectedCity == city,
                        onClick = { onCitySelected(city) },
                        label = { Text(if (city == "All Cities") "Preferred Cities" else city) }
                    )
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isWide = maxWidth >= 720.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LeafletMapView(
                        pins = mapPins,
                        selectedListingId = activeCandidate?.listing?.id,
                        onMarkerClick = onSelectCandidate,
                        zoomLevel = 13,
                        modifier = Modifier
                            .weight(1.35f)
                            .fillMaxHeight()
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(candidates, key = { it.listing.id }) { item ->
                            MapPinSummaryCard(
                                candidate = item,
                                isSelected = item.listing.id == activeCandidate?.listing?.id,
                                onClick = { onSelectCandidate(item.listing.id) },
                                onInspect = { onInspectCandidate(item) },
                                onLike = { onLikeCandidate(item) }
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LeafletMapView(
                        pins = mapPins,
                        selectedListingId = activeCandidate?.listing?.id,
                        onMarkerClick = onSelectCandidate,
                        zoomLevel = 13,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    if (activeCandidate != null) {
                        MapPinSummaryCard(
                            candidate = activeCandidate,
                            isSelected = true,
                            onClick = { onInspectCandidate(activeCandidate) },
                            onInspect = { onInspectCandidate(activeCandidate) },
                            onLike = { onLikeCandidate(activeCandidate) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPinSummaryCard(
    candidate: ScoredCandidate,
    isSelected: Boolean,
    onClick: () -> Unit,
    onInspect: () -> Unit,
    onLike: () -> Unit
) {
    val l = candidate.listing
    val photoRes = if (l.photoResId != 0) l.photoResId else R.drawable.img_flatmate_ananya_1794592209987
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = photoRes),
                        contentDescription = l.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${l.name}, ${l.age}",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${l.locality}, ${l.city} · ₹${l.monthlyRent}/mo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                MatchScoreBadge(score = candidate.matchScore)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onInspect,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Why Matched")
                }
                Button(
                    onClick = onLike,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("map_save_match_${l.id}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Match & Chat")
                }
            }
        }
    }
}
