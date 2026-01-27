package com.bloki.adblocker.blocklist

import com.bloki.adblocker.data.BlocklistSource

object BlocklistSources {
    val defaults = listOf(
        BlocklistSource(
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
            name = "Steven Black Unified"
        ),
        BlocklistSource(
            url = "https://adaway.org/hosts.txt",
            name = "AdAway Default"
        ),
        BlocklistSource(
            url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext",
            name = "Peter Lowe's Ad Servers"
        )
    )
}
