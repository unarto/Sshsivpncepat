package com.sivpn.cepat.parser

import com.sivpn.cepat.config.DefaultValues

data class ProxyConfig(
    val host: String = "",
    val port: String = DefaultValues.DEFAULT_PROXY_PORT.toString()
)

object ProxyParser {

    /**
     * Parses full proxy string formatted as `host:port`
     */
    fun parseFullInput(input: String, defaultPort: String = DefaultValues.DEFAULT_PROXY_PORT.toString()): ProxyConfig {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ProxyConfig("", defaultPort)
        }
        
        if (trimmed.startsWith("[")) {
            val closeBracketIndex = trimmed.indexOf("]")
            if (closeBracketIndex != -1) {
                val host = trimmed.substring(1, closeBracketIndex)
                var port = defaultPort
                
                if (trimmed.length > closeBracketIndex + 1 && trimmed[closeBracketIndex + 1] == ':') {
                    val parsedPort = trimmed.substring(closeBracketIndex + 2).trim()
                    if (parsedPort.isNotEmpty()) {
                        port = parsedPort
                    }
                }
                return ProxyConfig(host, port)
            }
        }
        
        val lastColonIndex = trimmed.lastIndexOf(":")
        return if (lastColonIndex != -1 && trimmed.indexOf(":") == lastColonIndex) {
            val host = trimmed.substring(0, lastColonIndex).trim()
            val portPart = trimmed.substring(lastColonIndex + 1).trim()
            val port = if (portPart.isNotEmpty()) portPart else defaultPort
            ProxyConfig(host, port)
        } else if (lastColonIndex != -1) {
            ProxyConfig(trimmed, defaultPort)
        } else {
            ProxyConfig(trimmed, defaultPort)
        }
    }

    fun formatFullInput(config: ProxyConfig): String {
        if (config.host.isEmpty()) return ""
        val isIpv6Raw = config.host.count { it == ':' } >= 2 && !config.host.startsWith("[")
        val formattedHost = if (isIpv6Raw) "[${config.host}]" else config.host
        return "$formattedHost:${config.port}"
    }
}
