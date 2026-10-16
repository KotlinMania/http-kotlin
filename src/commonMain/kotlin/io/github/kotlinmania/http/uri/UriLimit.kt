package io.github.kotlinmania.http.uri

/** The largest URI length, reserving one offset value as a sentinel. */
internal const val maxUriLength: Int = (1 shl 16) - 2
