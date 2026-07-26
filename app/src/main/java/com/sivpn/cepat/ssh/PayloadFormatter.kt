package com.sivpn.cepat.ssh
import com.sivpn.cepat.vpn.LogManager

import android.util.Base64
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger

/**
 * Production-ready, thread-safe Payload Formatter for Android VPN Applications.
 *
 * Expands placeholder tags and escape sequences in HTTP Injector, eProxy, TLS Tunnel,
 * HA Tunnel, and WebSocket SSH payloads into properly formatted HTTP requests or split chunks.
 *
 * Target: Android API 24+ (Android 7.0 Nougat and above).
 */
object PayloadFormatter {

    /** Default Android Chrome User-Agent header value used when user-agent is omitted or empty. */
    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private val secureRandom = SecureRandom()
    private val rotateCounter = AtomicInteger(0)

    /**
     * Formats an SSH/HTTP Injector payload string by resolving all placeholder tags and escape sequences.
     *
     * @param payload The raw payload string containing tag placeholders (e.g. `[host_port]`, `[crlf]`, `[ua]`).
     * @param sshHost Target SSH host or proxy address (IPv4, IPv6, or domain). Must not be blank.
     * @param sshPort Target SSH port number (valid range: 1..65535, defaults to 22 if out of range).
     * @param userAgent Custom User-Agent header value; defaults to [DEFAULT_USER_AGENT] if blank.
     * @return Fully expanded and formatted payload string ready for transmission.
     * @throws IllegalArgumentException if [sshHost] is empty or blank.
     */
    fun formatPayload(
        payload: String,
        sshHost: String,
        sshPort: Int,
        userAgent: String = DEFAULT_USER_AGENT
    ): String {
        if (payload.isBlank()) return ""

        val validHost = sshHost.trim()
        require(validHost.isNotEmpty()) { "Target SSH host address must not be blank" }

        val validPort = if (sshPort in 1..65535) sshPort else 22
        val validUa = if (userAgent.isBlank()) DEFAULT_USER_AGENT else userAgent.trim()

        return processSinglePayload(payload, validHost, validPort, validUa)
    }

    /**
     * Parses a payload containing split tags (`[split]`, `[instant_split]`, `[delay_split]`)
     * into a list of formatted string chunks for sequential socket transmission.
     *
     * @param payload The raw payload string containing split tags and placeholders.
     * @param sshHost Target SSH host or proxy address (IPv4, IPv6, or domain). Must not be blank.
     * @param sshPort Target SSH port number (valid range: 1..65535).
     * @param userAgent Custom User-Agent header value.
     * @return List of formatted payload string chunks.
     * @throws IllegalArgumentException if [sshHost] is empty or blank.
     */
    fun parsePayloadChunks(
        payload: String,
        sshHost: String,
        sshPort: Int,
        userAgent: String = DEFAULT_USER_AGENT
    ): List<String> {
        if (payload.isBlank()) return emptyList()

        val validHost = sshHost.trim()
        require(validHost.isNotEmpty()) { "Target SSH host address must not be blank" }

        val validPort = if (sshPort in 1..65535) sshPort else 22
        val validUa = if (userAgent.isBlank()) DEFAULT_USER_AGENT else userAgent.trim()

        return parsePayloadChunkDetails(payload, validHost, validPort, validUa).map { it.content }
    }

    /**
     * Advanced chunk parser that returns structured [PayloadChunk] objects containing
     * both the formatted chunk content and the specific [SplitType] requested at the chunk boundary.
     *
     * @param payload The raw payload string containing tag placeholders.
     * @param sshHost Target SSH host or proxy address. Must not be blank.
     * @param sshPort Target SSH port number.
     * @param userAgent Custom User-Agent header value.
     * @return List of [PayloadChunk] items containing content and split boundary metadata.
     * @throws IllegalArgumentException if [sshHost] is empty or blank.
     */
    fun parsePayloadChunkDetails(
        payload: String,
        sshHost: String,
        sshPort: Int,
        userAgent: String = DEFAULT_USER_AGENT
    ): List<PayloadChunk> {
        if (payload.isBlank()) return emptyList()

        val validHost = sshHost.trim()
        require(validHost.isNotEmpty()) { "Target SSH host address must not be blank" }

        val validPort = if (sshPort in 1..65535) sshPort else 22
        val validUa = if (userAgent.isBlank()) DEFAULT_USER_AGENT else userAgent.trim()

        return processChunkedPayload(payload, validHost, validPort, validUa)
    }

    /**
     * Internal single-pass formatter for unchunked payload formatting.
     */
    private fun processSinglePayload(
        payload: String,
        sshHost: String,
        sshPort: Int,
        userAgent: String
    ): String {
        val hostPort = formatHostPort(sshHost, sshPort)
        val hostNoPort = sshHost
        val protocol = "HTTP/1.1"

        val sb = StringBuilder(payload.length + 128)
        var i = 0
        val len = payload.length

        while (i < len) {
            val ch = payload[i]

            // Check for tag start '['
            if (ch == '[') {
                val closingIndex = payload.indexOf(']', i)
                if (closingIndex != -1) {
                    val tagContent = payload.substring(i + 1, closingIndex)
                    val tagLower = tagContent.lowercase()

                    val replacement = resolveTagReplacement(
                        tagContent = tagContent,
                        tagLower = tagLower,
                        hostPort = hostPort,
                        hostNoPort = hostNoPort,
                        sshPort = sshPort,
                        protocol = protocol,
                        userAgent = userAgent,
                        isChunked = false
                    )

                    if (replacement != null) {
                        sb.append(replacement)
                        i = closingIndex + 1
                        continue
                    }
                }
            }

            // Check for escape sequence starting with '\'
            if (ch == '\\') {
                val (escapedText, consumed) = resolveEscapeSequence(payload, i, len)
                if (consumed > 0) {
                    sb.append(escapedText)
                    i += consumed
                    continue
                }
            }

            sb.append(ch)
            i++
        }

        return sb.toString()
    }

    /**
     * Internal single-pass formatter for chunked payload parsing.
     */
    private fun processChunkedPayload(
        payload: String,
        sshHost: String,
        sshPort: Int,
        userAgent: String
    ): List<PayloadChunk> {
        val hostPort = formatHostPort(sshHost, sshPort)
        val hostNoPort = sshHost
        val protocol = "HTTP/1.1"

        val chunks = mutableListOf<PayloadChunk>()
        val currentSb = StringBuilder(payload.length + 128)
        var i = 0
        val len = payload.length

        while (i < len) {
            val ch = payload[i]

            // Check for tag start '['
            if (ch == '[') {
                val closingIndex = payload.indexOf(']', i)
                if (closingIndex != -1) {
                    val tagContent = payload.substring(i + 1, closingIndex)
                    val tagLower = tagContent.lowercase()

                    val splitType = when (tagLower) {
                        "split" -> SplitType.NORMAL_SPLIT
                        "instant_split" -> SplitType.INSTANT_SPLIT
                        "delay_split" -> SplitType.DELAY_SPLIT
                        else -> null
                    }

                    if (splitType != null) {
                        if (currentSb.isNotEmpty()) {
                            chunks.add(PayloadChunk(currentSb.toString(), splitType))
                            currentSb.clear()
                        } else if (chunks.isNotEmpty()) {
                            chunks[chunks.lastIndex] = chunks.last().copy(splitType = splitType)
                        }
                        i = closingIndex + 1
                        continue
                    }

                    val replacement = resolveTagReplacement(
                        tagContent = tagContent,
                        tagLower = tagLower,
                        hostPort = hostPort,
                        hostNoPort = hostNoPort,
                        sshPort = sshPort,
                        protocol = protocol,
                        userAgent = userAgent,
                        isChunked = true
                    )

                    if (replacement != null) {
                        currentSb.append(replacement)
                        i = closingIndex + 1
                        continue
                    }
                }
            }

            // Check for escape sequence starting with '\'
            if (ch == '\\') {
                val (escapedText, consumed) = resolveEscapeSequence(payload, i, len)
                if (consumed > 0) {
                    currentSb.append(escapedText)
                    i += consumed
                    continue
                }
            }

            currentSb.append(ch)
            i++
        }

        if (currentSb.isNotEmpty()) {
            chunks.add(PayloadChunk(currentSb.toString(), SplitType.NORMAL_SPLIT))
        }

        return chunks
    }

    /**
     * Resolves tag replacements for standard and dynamic payload tags.
     */
    private fun resolveTagReplacement(
        tagContent: String,
        tagLower: String,
        hostPort: String,
        hostNoPort: String,
        sshPort: Int,
        protocol: String,
        userAgent: String,
        isChunked: Boolean
    ): String? {
        return when {
            // Host / Port tags
            tagLower == "host_port" -> hostPort
            tagLower == "host" || tagLower == "real_host" || tagLower == "host_no_port" -> hostNoPort
            tagLower == "port" -> sshPort.toString()

            // Protocol & HTTP tags
            tagLower == "protocol" || tagLower == "http_version" -> protocol
            tagLower == "method" -> "CONNECT"
            tagLower == "path" -> "/"

            // User Agent tags
            tagLower == "ua" || tagLower == "user-agent" -> userAgent

            // Compound tags
            tagLower == "raw" -> "CONNECT $hostPort $protocol\r\n\r\n"
            tagLower == "netdata" -> "CONNECT $hostPort $protocol\r\n"

            // WebSocket tags
            tagLower == "websocket_key" -> generateWsKey()
            tagLower == "websocket_version" -> "13"
            tagLower == "websocket_extensions" -> "permessage-deflate; client_max_window_bits"

            // Line break tags
            tagLower == "crlf" -> "\r\n"
            tagLower == "lfcr" -> "\n\r"
            tagLower == "cr" -> "\r"
            tagLower == "lf" -> "\n"

            // Split tags (handled during unchunked mode)
            tagLower == "split" || tagLower == "instant_split" || tagLower == "delay_split" -> {
                if (isChunked) "" else "\r\n"
            }

            // Dynamic tags: [random] or [random=a,b,c]
            tagLower == "random" -> generateRandomString(6)
            tagLower.startsWith("random=") -> {
                val optionsStr = tagContent.substring(7)
                val options = optionsStr.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (options.isNotEmpty()) {
                    options[secureRandom.nextInt(options.size)]
                } else ""
            }

            // Dynamic tags: [rotate] or [rotate=a,b,c]
            tagLower == "rotate" -> generateRandomString(6)
            tagLower.startsWith("rotate=") -> {
                val optionsStr = tagContent.substring(7)
                val options = optionsStr.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (options.isNotEmpty()) {
                    val count = rotateCounter.getAndIncrement()
                    val idx = Math.floorMod(count, options.size)
                    options[idx]
                } else ""
            }

            // Unrecognized tag: keep untouched
            else -> null
        }
    }

    /**
     * Resolves literal escape sequences (\r\n, \n\r, \r, \n, \t, \\).
     */
    private fun resolveEscapeSequence(payload: String, index: Int, len: Int): Pair<String, Int> {
        // 1. Check for 4-character escape sequence \r\n
        if (index + 3 < len &&
            payload[index + 1] == 'r' &&
            payload[index + 2] == '\\' &&
            payload[index + 3] == 'n'
        ) {
            return Pair("\r\n", 4)
        }

        // 2. Check for 4-character escape sequence \n\r
        if (index + 3 < len &&
            payload[index + 1] == 'n' &&
            payload[index + 2] == '\\' &&
            payload[index + 3] == 'r'
        ) {
            return Pair("\n\r", 4)
        }

        // 3. Check for 2-character escape sequence
        if (index + 1 < len) {
            return when (payload[index + 1]) {
                'r' -> Pair("\r", 2)
                'n' -> Pair("\n", 2)
                't' -> Pair("\t", 2)
                '\\' -> Pair("\\", 2)
                else -> Pair("", 0)
            }
        }

        return Pair("", 0)
    }

    /**
     * Formats host and port into RFC-compliant host:port format with IPv6 bracket support.
     */
    private fun formatHostPort(host: String, port: Int): String {
        val trimmed = host.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return "$trimmed:$port"
        }
        val isIpv6Raw = trimmed.count { it == ':' } >= 2
        return if (isIpv6Raw) {
            "[$trimmed]:$port"
        } else {
            "$trimmed:$port"
        }
    }

    /**
     * Generates a 16-byte random Sec-WebSocket-Key encoded in Base64 (RFC 6455)
     * using Android 24+ compatible android.util.Base64.
     */
    private fun generateWsKey(): String {
        val randomBytes = ByteArray(16)
        secureRandom.nextBytes(randomBytes)
        return Base64.encodeToString(randomBytes, Base64.NO_WRAP)
    }

    /**
     * Generates a random alphanumeric string of specified length.
     */
    private fun generateRandomString(length: Int): String {
        val charPool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val sb = StringBuilder(length)
        for (k in 0 until length) {
            sb.append(charPool[secureRandom.nextInt(charPool.length)])
        }
        return sb.toString()
    }
}

/**
 * Represents the type of split operation requested by payload split tags.
 */
enum class SplitType {
    /** Standard socket split tag ([split]). */
    NORMAL_SPLIT,

    /** Instant socket split tag ([instant_split]). */
    INSTANT_SPLIT,

    /** Delayed socket split tag ([delay_split]). */
    DELAY_SPLIT
}

/**
 * Encapsulates a payload string chunk along with its designated [SplitType].
 *
 * @property content Formatted string content of the payload chunk.
 * @property splitType Split behavior requested at the chunk boundary.
 */
data class PayloadChunk(
    val content: String,
    val splitType: SplitType
)
