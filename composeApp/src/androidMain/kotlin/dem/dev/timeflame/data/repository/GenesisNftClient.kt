package dem.dev.timeflame.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.math.BigInteger
import java.util.LinkedHashMap

data class GenesisCoin(val tokenId: Int, val name: String, val imageUri: String)

/** Read-only collection browsing. No wallet, signing, payment or mint calls. */
class GenesisNftClient {
    companion object {
        const val CONTRACT = "0x7A5eD4eCe6A085c532F52517957aFf220B48609B"
        const val COLLECTION_URL = "https://thirdweb.com/polygon/$CONTRACT"
        private val RPC_URLS = listOf(
            "https://polygon-bor-rpc.publicnode.com",
            "https://polygon.drpc.org"
        )
        val IPFS_GATEWAYS = listOf(
            "https://gateway.pinata.cloud/ipfs/",
            "https://ipfs.io/ipfs/",
            "https://dweb.link/ipfs/"
        )

        fun mediaUrl(uri: String, gatewayIndex: Int = 0): String {
            if (uri.startsWith("ipfs://")) {
                val path = uri.removePrefix("ipfs://").removePrefix("ipfs/")
                require(path.isNotBlank())
                return IPFS_GATEWAYS[gatewayIndex.coerceIn(IPFS_GATEWAYS.indices)] + path
            }
            require(uri.startsWith("https://")) { "Unsupported NFT media URL" }
            return uri
        }

        /** Decode the offset, byte length and UTF-8 bytes of a Solidity string. */
        internal fun decodeString(encoded: String): String {
            val hex = encoded.removePrefix("0x")
            require(hex.length >= 128 && hex.length % 2 == 0)
            val byteOffset = BigInteger(hex.take(64), 16).intValueExact()
            require(byteOffset >= 32 && byteOffset <= (hex.length - 64) / 2)
            val offset = byteOffset * 2
            val byteLength = BigInteger(hex.substring(offset, offset + 64), 16).intValueExact()
            require(byteLength in 1..8192)
            val start = offset + 64
            require(byteLength <= (hex.length - start) / 2)
            return ByteArray(byteLength) { i ->
                hex.substring(start + i * 2, start + i * 2 + 2).toInt(16).toByte()
            }.toString(Charsets.UTF_8)
        }
    }

    private val http = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 12000
            connectTimeoutMillis = 8000
            socketTimeoutMillis = 12000
        }
    }
    private val permits = Semaphore(3)
    private val cache = object : LinkedHashMap<Int, GenesisCoin>(96, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, GenesisCoin>?): Boolean = size > 96
    }

    fun close() = http.close()

    suspend fun collectionSize(): Int {
        // nextTokenIdToMint(): includes the collection's published lazy-minted NFTs.
        val result = rpc("0x3b1475a7")
        return BigInteger(result.removePrefix("0x"), 16).intValueExact().also {
            require(it in 1..200000) { "Collection has no published NFTs" }
        }
    }

    suspend fun coin(tokenId: Int): GenesisCoin {
        synchronized(cache) { cache[tokenId] }?.let { return it }
        return permits.withPermit {
            synchronized(cache) { cache[tokenId] }?.let { return@withPermit it }
            require(tokenId >= 0)
            // tokenURI(uint256); use actual on-chain URI, never guess metadata paths.
            val uri = decodeString(rpc("0xc87b56dd" + tokenId.toString(16).padStart(64, '0')))
            val metadata = metadata(uri)
            val image = metadata["image"]?.jsonPrimitive?.contentOrNull
                ?: metadata["image_url"]?.jsonPrimitive?.contentOrNull
                ?: error("NFT image is missing")
            mediaUrl(image) // Validate before handing the URI to the image loader.
            GenesisCoin(
                tokenId,
                metadata["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: "PISTOL #$tokenId",
                image
            ).also { synchronized(cache) { cache[tokenId] = it } }
        }
    }

    private suspend fun rpc(data: String): String {
        val payload = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", "eth_call")
            putJsonArray("params") {
                add(buildJsonObject { put("to", CONTRACT); put("data", data) })
                add(kotlinx.serialization.json.JsonPrimitive("latest"))
            }
        }.toString()
        var failure: Exception? = null
        for (url in RPC_URLS) {
            try {
                val response = http.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
                check(response.status.value == 200)
                val result = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                check(result["error"] == null) { "Contract read failed" }
                return result["result"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.startsWith("0x") && it.length > 2 }
                    ?: error("Empty contract response")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = e
            }
        }
        throw failure ?: IllegalStateException("Polygon RPC unavailable")
    }

    private suspend fun metadata(uri: String): JsonObject {
        var failure: Exception? = null
        val attempts = if (uri.startsWith("ipfs://")) IPFS_GATEWAYS.size else 1
        repeat(attempts) { gateway ->
            try {
                val response = http.get(mediaUrl(uri, gateway))
                check(response.status.value == 200)
                val text = response.bodyAsText()
                require(text.length <= 256000)
                return Json.parseToJsonElement(text).jsonObject
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = e
            }
        }
        throw failure ?: IllegalStateException("NFT metadata unavailable")
    }
}
