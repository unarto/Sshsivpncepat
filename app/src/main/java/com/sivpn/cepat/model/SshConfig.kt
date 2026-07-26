package com.sivpn.cepat.model

data class SshConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val payload: String
)
