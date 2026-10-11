package dem.dev.timeflame.feature.main.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import dem.dev.timeflame.R
import dem.dev.timeflame.data.repository.GenesisCoin
import dem.dev.timeflame.data.repository.GenesisNftClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun GenesisNftCarousel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val client = remember { GenesisNftClient(context.applicationContext) }
    val imageLoader = remember(context) {
        ImageLoader.Builder(context.applicationContext)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    DisposableEffect(client, imageLoader) {
        onDispose { client.close(); imageLoader.shutdown() }
    }
    var total by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(0) }
    var scrolling by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(retry) {
        loading = true
        failed = false
        try {
            total = client.collectionSize()
            selected = selected.coerceIn(0, total - 1)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Text("PISTOL GENESIS NFT", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (total > 0) stringResource(R.string.genesis_nft_carousel_count, total)
                    else "Polygon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                loading -> Box(
                    Modifier.fillMaxWidth().height(190.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                failed -> Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(stringResource(R.string.genesis_nft_load_failed), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { retry++ }) { Text(stringResource(R.string.genesis_nft_retry)) }
                }
                total > 0 -> LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(count = total, key = { it }) { tokenId ->
                        GenesisCoinCard(
                            tokenId = tokenId,
                            client = client,
                            imageLoader = imageLoader,
                            selected = selected == tokenId,
                            onSelected = { selected = tokenId }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val current = listState.firstVisibleItemIndex
                        val target = if (total == 1) 0 else {
                            val draw = Random.nextInt(total - 1)
                            if (draw >= current) draw + 1 else draw
                        }
                        selected = target
                        scrolling = true
                        scope.launch {
                            try { listState.animateScrollToItem(target) }
                            finally { scrolling = false }
                        }
                    },
                    enabled = !loading && !failed && total > 0 && !scrolling,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.genesis_nft_random),
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            TextButton(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GenesisNftClient.COLLECTION_URL)))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, R.string.genesis_nft_browser_unavailable, Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) { Text(stringResource(R.string.genesis_nft_open_collection)) }
        }
    }
}

@Composable
private fun GenesisCoinCard(
    tokenId: Int,
    client: GenesisNftClient,
    imageLoader: ImageLoader,
    selected: Boolean,
    onSelected: () -> Unit
) {
    var retry by remember(tokenId) { mutableStateOf(0) }
    var imageGateway by remember(tokenId, retry) { mutableStateOf(0) }
    var imageFailed by remember(tokenId, retry) { mutableStateOf(false) }
    var imageLoading by remember(tokenId, retry) { mutableStateOf(true) }
    val coin by produceState<GenesisCoin?>(null, tokenId, retry) {
        value = null
        try {
            value = client.coin(tokenId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            imageFailed = true
            imageLoading = false
        }
    }
    Card(
        onClick = onSelected,
        modifier = Modifier.width(164.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(144.dp), contentAlignment = Alignment.Center) {
                coin?.let { nft ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(GenesisNftClient.mediaUrl(nft.imageUri, imageGateway))
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = nft.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(144.dp),
                        onSuccess = { imageLoading = false; imageFailed = false },
                        onError = {
                            if (nft.imageUri.startsWith("ipfs://") &&
                                imageGateway < GenesisNftClient.IPFS_GATEWAYS.lastIndex) {
                                imageGateway++
                            } else { imageLoading = false; imageFailed = true }
                        }
                    )
                }
                if (imageLoading && !imageFailed) CircularProgressIndicator(Modifier.size(20.dp))
                if (imageFailed) TextButton(onClick = { retry++ }) {
                    Text(stringResource(R.string.genesis_nft_retry))
                }
            }
            Text(
                coin?.rarity ?: "—",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stringResource(R.string.genesis_nft_token_id, coin?.tokenId ?: (tokenId + 1)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
