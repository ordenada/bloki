package com.bloki.adblocker.vpn

/**
 * Parses and constructs DNS packets from raw IP/UDP byte arrays.
 */
object DnsPacketParser {

    private const val IP_HEADER_MIN = 20
    private const val UDP_HEADER = 8
    private const val DNS_PORT = 53

    data class DnsQuery(
        val ipHeader: ByteArray,
        val udpHeader: ByteArray,
        val dnsPayload: ByteArray,
        val transactionId: Int,
        val questionDomain: String,
        val questionType: Int,
        val questionClass: Int,
        val questionRaw: ByteArray
    )

    /**
     * Parse a raw IP packet. Returns null if not a UDP DNS query on port 53.
     */
    fun parse(packet: ByteArray, length: Int): DnsQuery? {
        if (length < IP_HEADER_MIN + UDP_HEADER + 12) return null

        // IPv4 check
        val version = (packet[0].toInt() shr 4) and 0xF
        if (version != 4) return null

        val ipHeaderLen = (packet[0].toInt() and 0xF) * 4
        if (length < ipHeaderLen + UDP_HEADER + 12) return null

        // Protocol: UDP = 17
        if (packet[9].toInt() and 0xFF != 17) return null

        // Destination port
        val dstPort = ((packet[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or
                (packet[ipHeaderLen + 3].toInt() and 0xFF)
        if (dstPort != DNS_PORT) return null

        val dnsOffset = ipHeaderLen + UDP_HEADER
        val dnsLen = length - dnsOffset

        val transactionId = ((packet[dnsOffset].toInt() and 0xFF) shl 8) or
                (packet[dnsOffset + 1].toInt() and 0xFF)

        // Question count should be >= 1
        val qdCount = ((packet[dnsOffset + 4].toInt() and 0xFF) shl 8) or
                (packet[dnsOffset + 5].toInt() and 0xFF)
        if (qdCount < 1) return null

        // Parse the first question domain
        val domainResult = readDomainName(packet, dnsOffset + 12, dnsOffset, dnsLen) ?: return null
        val domain = domainResult.first
        val afterDomain = domainResult.second

        if (afterDomain + 4 > length) return null
        val qType = ((packet[afterDomain].toInt() and 0xFF) shl 8) or
                (packet[afterDomain + 1].toInt() and 0xFF)
        val qClass = ((packet[afterDomain + 2].toInt() and 0xFF) shl 8) or
                (packet[afterDomain + 3].toInt() and 0xFF)

        val questionRaw = packet.copyOfRange(dnsOffset + 12, afterDomain + 4)

        return DnsQuery(
            ipHeader = packet.copyOfRange(0, ipHeaderLen),
            udpHeader = packet.copyOfRange(ipHeaderLen, ipHeaderLen + UDP_HEADER),
            dnsPayload = packet.copyOfRange(dnsOffset, length),
            transactionId = transactionId,
            questionDomain = domain,
            questionType = qType,
            questionClass = qClass,
            questionRaw = questionRaw
        )
    }

    /**
     * Read a DNS domain name handling compression pointers.
     * Returns (domain, offset after name) or null on error.
     */
    private fun readDomainName(
        data: ByteArray,
        startOffset: Int,
        dnsBase: Int,
        dnsLen: Int,
        depth: Int = 0
    ): Pair<String, Int>? {
        if (depth > 10) return null
        val parts = mutableListOf<String>()
        var offset = startOffset
        var jumped = false
        var jumpReturn = 0

        while (offset < data.size) {
            val len = data[offset].toInt() and 0xFF
            if (len == 0) {
                if (!jumped) jumpReturn = offset + 1
                break
            }
            if ((len and 0xC0) == 0xC0) {
                // Compression pointer
                if (offset + 1 >= data.size) return null
                val pointer = ((len and 0x3F) shl 8) or (data[offset + 1].toInt() and 0xFF)
                if (!jumped) jumpReturn = offset + 2
                offset = dnsBase + pointer
                jumped = true
                continue
            }
            offset++
            if (offset + len > data.size) return null
            parts.add(String(data, offset, len, Charsets.US_ASCII))
            offset += len
        }

        return Pair(parts.joinToString(".").lowercase(), if (jumped) jumpReturn else offset + 1)
    }

    /**
     * Build an NXDOMAIN response for a query.
     */
    fun buildNxDomainResponse(query: DnsQuery): ByteArray {
        // DNS response: copy transaction ID, set flags to NXDOMAIN (0x8183),
        // qdcount=1, ancount=0, nscount=0, arcount=0, then question section
        val dnsResponse = ByteArray(12 + query.questionRaw.size)
        // Transaction ID
        dnsResponse[0] = ((query.transactionId shr 8) and 0xFF).toByte()
        dnsResponse[1] = (query.transactionId and 0xFF).toByte()
        // Flags: QR=1, OPCODE=0, AA=0, TC=0, RD=1, RA=1, RCODE=3 (NXDOMAIN)
        dnsResponse[2] = 0x81.toByte()
        dnsResponse[3] = 0x83.toByte()
        // QDCOUNT = 1
        dnsResponse[4] = 0
        dnsResponse[5] = 1
        // ANCOUNT, NSCOUNT, ARCOUNT = 0
        // Copy question section
        System.arraycopy(query.questionRaw, 0, dnsResponse, 12, query.questionRaw.size)

        return wrapInUdpIp(query, dnsResponse)
    }

    /**
     * Wrap a DNS response payload in a UDP/IP packet, swapping src/dst.
     */
    fun wrapInUdpIp(query: DnsQuery, dnsPayload: ByteArray): ByteArray {
        val ipHeaderLen = query.ipHeader.size
        val totalLen = ipHeaderLen + UDP_HEADER + dnsPayload.size
        val packet = ByteArray(totalLen)

        // Copy and modify IP header
        System.arraycopy(query.ipHeader, 0, packet, 0, ipHeaderLen)
        // Swap src and dst IP (offsets 12-15 = src, 16-19 = dst)
        for (i in 0..3) {
            val tmp = packet[12 + i]
            packet[12 + i] = packet[16 + i]
            packet[16 + i] = tmp
        }
        // Total length
        packet[2] = ((totalLen shr 8) and 0xFF).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        // Protocol UDP
        packet[9] = 17
        // Zero checksum for recalculation
        packet[10] = 0
        packet[11] = 0
        val ipChecksum = ipChecksum(packet, ipHeaderLen)
        packet[10] = ((ipChecksum shr 8) and 0xFF).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        // UDP header: swap src/dst ports
        val srcPort = ((query.udpHeader[0].toInt() and 0xFF) shl 8) or
                (query.udpHeader[1].toInt() and 0xFF)
        val dstPort = ((query.udpHeader[2].toInt() and 0xFF) shl 8) or
                (query.udpHeader[3].toInt() and 0xFF)
        val udpLen = UDP_HEADER + dnsPayload.size
        packet[ipHeaderLen] = ((dstPort shr 8) and 0xFF).toByte()
        packet[ipHeaderLen + 1] = (dstPort and 0xFF).toByte()
        packet[ipHeaderLen + 2] = ((srcPort shr 8) and 0xFF).toByte()
        packet[ipHeaderLen + 3] = (srcPort and 0xFF).toByte()
        packet[ipHeaderLen + 4] = ((udpLen shr 8) and 0xFF).toByte()
        packet[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()
        // UDP checksum = 0 (optional for IPv4)
        packet[ipHeaderLen + 6] = 0
        packet[ipHeaderLen + 7] = 0

        System.arraycopy(dnsPayload, 0, packet, ipHeaderLen + UDP_HEADER, dnsPayload.size)
        return packet
    }

    private fun ipChecksum(header: ByteArray, length: Int): Int {
        var sum = 0L
        var i = 0
        while (i < length) {
            sum += ((header[i].toInt() and 0xFF) shl 8) or (header[i + 1].toInt() and 0xFF)
            i += 2
        }
        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.toInt().inv()) and 0xFFFF
    }
}
