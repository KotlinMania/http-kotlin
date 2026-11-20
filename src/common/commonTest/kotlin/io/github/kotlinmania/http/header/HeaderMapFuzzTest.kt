// port-lint: tests tests/header_map_fuzz.rs
package io.github.kotlinmania.http.header

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class HeaderMapFuzzTest {
    @Test
    fun headerMapFuzz() {
        for (trial in 0 until 40) {
            val seed = 0x5EED_CAFE_0000L + trial.toLong()
            Fuzz.new(seed).run()
        }
    }

    private data class Weight(
        val insert: Int,
        val remove: Int,
        val append: Int,
    )

    private data class Step(
        val action: Action,
        val expect: AltMap,
    )

    private sealed class Action {
        data class Insert(
            val name: HeaderName,
            val value: HeaderValue,
            val old: HeaderValue?,
        ) : Action()

        data class Append(
            val name: HeaderName,
            val value: HeaderValue,
            val ret: Boolean,
        ) : Action()

        data class Remove(
            val name: HeaderName,
            val value: HeaderValue?,
        ) : Action()

        fun apply(map: HeaderMap<HeaderValue>) {
            when (this) {
                is Insert -> {
                    val actual = map.insert(name, value)
                    assertEquals(old, actual)
                }
                is Remove -> {
                    map.getAll(name).iter().toList()
                    val actual = map.remove(name)
                    assertEquals(value, actual)
                }
                is Append -> {
                    assertEquals(ret, map.append(name, value))
                }
            }
        }
    }

    private class AltMap(
        private val map: MutableMap<HeaderName, MutableList<HeaderValue>> = linkedMapOf(),
    ) {
        fun snapshot(): AltMap {
            val copy = linkedMapOf<HeaderName, MutableList<HeaderValue>>()
            for ((k, v) in map) {
                copy[k.clone()] = v.mapTo(mutableListOf()) { it.clone() }
            }
            return AltMap(copy)
        }

        fun genStep(
            weight: Weight,
            rng: Random,
        ): Step {
            val action = genAction(weight, rng)
            return Step(action = action, expect = snapshot())
        }

        private fun genAction(
            weight: Weight,
            rng: Random,
        ): Action {
            val sum = weight.insert + weight.remove + weight.append
            var num = rng.nextInt(sum)
            if (num < weight.insert) {
                return genInsert(rng)
            }
            num -= weight.insert
            if (num < weight.remove) {
                return genRemove(rng)
            }
            return genAppend(rng)
        }

        private fun genInsert(rng: Random): Action {
            val name = genName(4, rng)
            val valHeader = genHeaderValue(rng)
            val old = map.put(name, mutableListOf(valHeader.clone()))?.firstOrNull()
            return Action.Insert(name, valHeader, old)
        }

        private fun genRemove(rng: Random): Action {
            val name = genName(-4, rng)
            val removed = map.remove(name)?.firstOrNull()
            return Action.Remove(name, removed)
        }

        private fun genAppend(rng: Random): Action {
            val name = genName(-5, rng)
            val valHeader = genHeaderValue(rng)
            val vals = map.getOrPut(name) { mutableListOf() }
            val ret = vals.isNotEmpty()
            vals.add(valHeader.clone())
            return Action.Append(name, valHeader, ret)
        }

        private fun genName(
            weight: Int,
            rng: Random,
        ): HeaderName {
            var existing = rng.nextInt(abs(weight)) == 0
            if (weight < 0) {
                existing = !existing
            }
            return if (existing && map.isNotEmpty()) {
                val n = rng.nextInt(map.size)
                map.keys.elementAt(n).clone()
            } else {
                genHeaderName(rng)
            }
        }

        fun assertIdentical(other: HeaderMap<HeaderValue>) {
            assertEquals(map.size, other.keysLen())
            for ((key, expectedVals) in map) {
                assertEquals(expectedVals.firstOrNull(), other.get(key))
                val actual = other.getAll(key).iter().toList()
                assertEquals(expectedVals, actual)
            }
        }
    }

    private class Fuzz(
        private val steps: List<Step>,
    ) {
        fun run() {
            val map = HeaderMap.new()
            for (step in steps) {
                step.action.apply(map)
                step.expect.assertIdentical(map)
            }
        }

        companion object {
            fun new(seed: Long): Fuzz {
                val rng = Random(seed)
                val expect = AltMap()
                val num = rng.nextInt(5, 350)
                val weight =
                    Weight(
                        insert = rng.nextInt(1, 10),
                        remove = rng.nextInt(1, 10),
                        append = rng.nextInt(1, 10),
                    )
                val steps = ArrayList<Step>(num)
                repeat(num) {
                    steps.add(expect.genStep(weight, rng))
                }
                return Fuzz(steps)
            }
        }
    }

    companion object {
        private const val HEADER_ALPHABET: String = "ABCDEFGHIJKLMNOPQRSTUVabcdefghilpqrstuvwxyz----"

        private val STANDARD_HEADERS: List<HeaderName> =
            listOf(
                ACCEPT,
                ACCEPT_CHARSET,
                ACCEPT_ENCODING,
                ACCEPT_LANGUAGE,
                ACCEPT_RANGES,
                ACCESS_CONTROL_ALLOW_CREDENTIALS,
                ACCESS_CONTROL_ALLOW_HEADERS,
                ACCESS_CONTROL_ALLOW_METHODS,
                ACCESS_CONTROL_ALLOW_ORIGIN,
                ACCESS_CONTROL_EXPOSE_HEADERS,
                ACCESS_CONTROL_MAX_AGE,
                ACCESS_CONTROL_REQUEST_HEADERS,
                ACCESS_CONTROL_REQUEST_METHOD,
                AGE,
                ALLOW,
                ALT_SVC,
                AUTHORIZATION,
                CACHE_CONTROL,
                CACHE_STATUS,
                CDN_CACHE_CONTROL,
                CONNECTION,
                CONTENT_DISPOSITION,
                CONTENT_ENCODING,
                CONTENT_LANGUAGE,
                CONTENT_LENGTH,
                CONTENT_LOCATION,
                CONTENT_RANGE,
                CONTENT_SECURITY_POLICY,
                CONTENT_SECURITY_POLICY_REPORT_ONLY,
                CONTENT_TYPE,
                COOKIE,
                DNT,
                DATE,
                ETAG,
                EXPECT,
                EXPIRES,
                FORWARDED,
                FROM,
                HOST,
                IF_MATCH,
                IF_MODIFIED_SINCE,
                IF_NONE_MATCH,
                IF_RANGE,
                IF_UNMODIFIED_SINCE,
                LAST_MODIFIED,
                LINK,
                LOCATION,
                MAX_FORWARDS,
                ORIGIN,
                PRAGMA,
                PROXY_AUTHENTICATE,
                PROXY_AUTHORIZATION,
                PUBLIC_KEY_PINS,
                PUBLIC_KEY_PINS_REPORT_ONLY,
                RANGE,
                REFERER,
                REFERRER_POLICY,
                REFRESH,
                RETRY_AFTER,
                SEC_WEBSOCKET_ACCEPT,
                SEC_WEBSOCKET_EXTENSIONS,
                SEC_WEBSOCKET_KEY,
                SEC_WEBSOCKET_PROTOCOL,
                SEC_WEBSOCKET_VERSION,
                SERVER,
                SET_COOKIE,
                STRICT_TRANSPORT_SECURITY,
                TE,
                TRAILER,
                TRANSFER_ENCODING,
                UPGRADE,
                UPGRADE_INSECURE_REQUESTS,
                USER_AGENT,
                VARY,
                VIA,
                WARNING,
                WWW_AUTHENTICATE,
                X_CONTENT_TYPE_OPTIONS,
                X_DNS_PREFETCH_CONTROL,
                X_FRAME_OPTIONS,
                X_XSS_PROTECTION,
            )

        private fun genHeaderName(rng: Random): HeaderName =
            if (rng.nextInt(2) == 0) {
                STANDARD_HEADERS[rng.nextInt(STANDARD_HEADERS.size)].clone()
            } else {
                val value = genString(rng, 1, 25)
                HeaderName.fromBytes(value.encodeToByteArray()).getOrThrow()
            }

        private fun genHeaderValue(rng: Random): HeaderValue {
            val value = genString(rng, 0, 70)
            return HeaderValue.fromBytes(value.encodeToByteArray()).getOrThrow()
        }

        private fun genString(
            rng: Random,
            min: Int,
            max: Int,
        ): String {
            val sb = StringBuilder(max - min)
            repeat(max - min) {
                sb.append(HEADER_ALPHABET[rng.nextInt(HEADER_ALPHABET.length)])
            }
            return sb.toString()
        }
    }
}
