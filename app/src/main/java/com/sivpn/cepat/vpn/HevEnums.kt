package com.sivpn.cepat.vpn

enum class HevUdpMode(val value: String) {
    UDP("udp"),
    UDPGW("udpgw"),
    NONE("none");

    companion object {
        fun fromString(str: String): HevUdpMode = entries.find { it.value.equals(str, ignoreCase = true) } ?: UDP
    }
}

enum class HevLogLevel(val value: String) {
    DEBUG("debug"),
    INFO("info"),
    WARN("warn"),
    ERROR("error");

    companion object {
        fun fromString(str: String): HevLogLevel = entries.find { it.value.equals(str, ignoreCase = true) } ?: WARN
    }
}
