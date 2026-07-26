package com.sivpn.cepat.parser

import com.sivpn.cepat.config.DefaultValues

data class SshCredentials(
    val host: String = "",
    val port: String = DefaultValues.DEFAULT_SSH_PORT.toString(),
    val username: String = "",
    val password: String = ""
)

object SshParser {

    /**
     * Parses full SSH string formatted as `host:port@username:password` or `host:port`
     */
    fun parseFullInput(input: String, defaultPort: String = DefaultValues.DEFAULT_SSH_PORT.toString()): SshCredentials {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return SshCredentials()
        }

        return try {
            if (trimmed.contains("@")) {
                val parts = trimmed.split("@")
                val hostPortPart = parts[0].trim()
                val userPassPart = parts.getOrNull(1)?.trim() ?: ""

                val (host, port) = parseHostPort(hostPortPart, defaultPort)

                var username = ""
                var password = ""
                if (userPassPart.contains(":")) {
                    val userPassParts = userPassPart.split(":", limit = 2)
                    username = userPassParts[0].trim()
                    password = userPassParts.getOrNull(1)?.trim() ?: ""
                } else {
                    username = userPassPart
                }

                SshCredentials(
                    host = host,
                    port = port,
                    username = username,
                    password = password
                )
            } else {
                val (host, port) = parseHostPort(trimmed, defaultPort)
                SshCredentials(host = host, port = port)
            }
        } catch (e: Exception) {
            SshCredentials(host = trimmed)
        }
    }

    private fun parseHostPort(hostPort: String, defaultPort: String): Pair<String, String> {
        val trimmed = hostPort.trim()
        
        // Handle IPv6 enclosed in brackets, e.g., [2001:db8::1]:22 or [2001:db8::1]
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
                return Pair(host, port)
            }
        }
        
        // Handle IPv4, domain name, or raw IPv6
        val lastColonIndex = trimmed.lastIndexOf(":")
        return if (lastColonIndex != -1 && trimmed.indexOf(":") == lastColonIndex) { 
            // Only one colon implies host:port
            val host = trimmed.substring(0, lastColonIndex).trim()
            val portPart = trimmed.substring(lastColonIndex + 1).trim()
            val port = if (portPart.isNotEmpty()) portPart else defaultPort
            Pair(host, port)
        } else if (lastColonIndex != -1) {
            // Multiple colons without brackets means raw IPv6 without port
            Pair(trimmed, defaultPort)
        } else {
            // No colons
            Pair(trimmed, defaultPort)
        }
    }

    fun formatFullInput(creds: SshCredentials): String {
        if (creds.host.isEmpty()) return ""
        val isIpv6Raw = creds.host.count { it == ':' } >= 2 && !creds.host.startsWith("[")
        val formattedHost = if (isIpv6Raw) "[${creds.host}]" else creds.host
        val hostPort = "$formattedHost:${creds.port}"
        return if (creds.username.isNotEmpty() || creds.password.isNotEmpty()) {
            "$hostPort@${creds.username}:${creds.password}"
        } else {
            hostPort
        }
    }
}
