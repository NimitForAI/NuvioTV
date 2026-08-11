package com.nuvio.tv.core.network

import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.Inet4Address
import java.net.InetAddress

/**
 * Custom DNS that reorders resolved addresses to place IPv4 (Inet4Address)
 * before IPv6 (Inet6Address). This avoids 60s timeout delays on networks
 * with broken IPv6 routing (issue #651).
 */
class IPv4FirstDns(private val delegate: Dns = Dns.SYSTEM) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = delegate.lookup(hostname)
        return addresses.sortedBy { if (it is Inet4Address) 0 else 1 }
    }
}

/**
 * Shared OkHttp defaults for networks with broken/dead-end IPv6 routing.
 *
 * `fastFallback(true)` enables OkHttp's Happy Eyeballs (RFC 8305): IPv4 and
 * IPv6 connection attempts are raced concurrently and the first to succeed
 * wins. On a network that advertises IPv6 but can't route it, a dead IPv6
 * attempt costs the fallback stagger (~250ms) instead of the full ~60s
 * connect timeout — matching the behavior of clients like Stremio.
 *
 * `IPv4FirstDns` is retained as a belt-and-suspenders ordering hint for the
 * case where only one family is usable.
 */
fun OkHttpClient.Builder.nuvioNetworkDefaults(): OkHttpClient.Builder =
    this.dns(IPv4FirstDns()).fastFallback(true)
