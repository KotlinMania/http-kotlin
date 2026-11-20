// port-lint: source header/map.rs
package io.github.kotlinmania.http.header

import kotlin.random.Random

private const val MAX_SIZE: Int = 1 shl 15
private const val NONE_INDEX: Int = 0xFFFF
private const val DISPLACEMENT_THRESHOLD: Int = 128
private const val FORWARD_SHIFT_THRESHOLD: Int = 512
private const val LOAD_FACTOR_THRESHOLD: Float = 0.2f
private const val HASH_MASK: ULong = 0x7FFFUL
private const val INITIAL_RAW_CAPACITY: Int = 8

private val EMPTY_POSITIONS: Array<Pos> = emptyArray()

/** A key-value entry yielded by [Iter], [IterMut], or [OccupiedEntry.removeEntry]. */
data class HeaderEntry<T : Any>(
    val name: HeaderName,
    val value: T,
)

/** An entry yielded by [IntoIter] or [Drain], where `name` is non-null for the first value of a key. */
data class HeaderItem<T : Any>(
    val name: HeaderName?,
    val value: T,
)

/** The removed key and drained values returned by [OccupiedEntry.removeEntryMult]. */
data class RemovedHeaderValues<T : Any>(
    val name: HeaderName,
    val values: ValueDrain<T>,
)

/** Lower and optional upper bounds returned by iterator `sizeHint()`. */
data class SizeHint(
    val lower: Int,
    val upper: Int?,
)

/**
 * A specialized multimap for HTTP header names and values.
 *
 * `HeaderMap` stores entries compactly using Robin Hood open addressing for header names
 * and an auxiliary doubly-linked storage table for multiple values per key. A single
 * `HeaderMap` holds at most 32,768 keys and switches to a keyed hash function if excessive
 * probe collisions occur.
 */
class HeaderMap<T : Any> private constructor(
    private var mask: Int,
    private var indices: Array<Pos>,
    internal val entries: MutableList<Bucket<T>>,
    internal val extraValues: MutableList<ExtraValue<T>>,
    private var danger: Danger,
) : Iterable<HeaderItem<T>> {
    /** Creates an empty [HeaderMap] without allocating table storage. */
    constructor() : this(
        mask = 0,
        indices = EMPTY_POSITIONS,
        entries = mutableListOf(),
        extraValues = mutableListOf(),
        danger = Danger.Green,
    )

    override fun iterator(): Iterator<HeaderItem<T>> = intoIter().iterator()

    /** Returns an independent copy of this map and its internal table layout. */
    fun clone(): HeaderMap<T> {
        val clonedEntries =
            entries
                .mapTo(ArrayList(entries.size)) { bucket ->
                    Bucket(
                        hash = bucket.hash,
                        key = bucket.key.clone(),
                        value = cloneValueIfPossible(bucket.value),
                        links = bucket.links?.let { Links(it.next, it.tail) },
                    )
                }
        val clonedExtra =
            extraValues
                .mapTo(ArrayList(extraValues.size)) { extra ->
                    ExtraValue(
                        value = cloneValueIfPossible(extra.value),
                        prev = extra.prev,
                        next = extra.next,
                    )
                }
        return HeaderMap(
            mask = mask,
            indices = indices.copyOf(),
            entries = clonedEntries,
            extraValues = clonedExtra,
            danger = danger,
        )
    }

    /**
     * Returns the total number of header **values** stored in the map.
     *
     * This may exceed [keysLen] when keys have multiple associated values.
     */
    fun len(): Int = entries.size + extraValues.size

    /** Returns the number of distinct header keys stored in the map. */
    fun keysLen(): Int = entries.size

    /** Returns true if the map contains no elements. */
    fun isEmpty(): Boolean = entries.isEmpty()

    /** Clears all key-value pairs while retaining allocated hash table capacity. */
    fun clear() {
        entries.clear()
        extraValues.clear()
        danger = Danger.Green
        for (idx in indices.indices) {
            indices[idx] = Pos.NONE
        }
    }

    /** Returns the approximate number of distinct headers holdable before reallocation. */
    fun capacity(): Int = usableCapacity(indices.size)

    /**
     * Reserves capacity for at least [additional] more header keys.
     *
     * Throws [IllegalStateException] if the resulting capacity exceeds `MAX_SIZE`.
     */
    fun reserve(additional: Int) {
        tryReserve(additional).getOrElse { err ->
            error(err.message ?: "size overflows MAX_SIZE")
        }
    }

    /**
     * Reserves capacity for at least [additional] more header keys, returning [MaxSizeReached]
     * if the requested capacity exceeds `MAX_SIZE`.
     */
    fun tryReserve(additional: Int): Result<Unit> {
        if (additional < 0) {
            return Result.failure(MaxSizeReached.new())
        }
        val capLong = entries.size.toLong() + additional.toLong()
        if (capLong > Int.MAX_VALUE.toLong()) {
            return Result.failure(MaxSizeReached.new())
        }
        val rawCap = toRawCapacity(capLong.toInt()).getOrElse { return Result.failure(it) }
        if (rawCap > indices.size) {
            val nextPow2 = checkedNextPowerOfTwo(rawCap) ?: return Result.failure(MaxSizeReached.new())
            if (nextPow2 > MAX_SIZE) {
                return Result.failure(MaxSizeReached.new())
            }
            if (entries.isEmpty()) {
                mask = nextPow2 - 1
                indices = Array(nextPow2) { Pos.NONE }
            } else {
                tryGrow(nextPow2).getOrElse { return Result.failure(it) }
            }
        }
        return Result.success(Unit)
    }

    /** Returns the first value associated with [key], or null if absent. */
    operator fun get(key: HeaderName): T? = findName(key)?.let { entries[it.second].value }

    /** Returns the first value associated with [key], or null if absent or invalid. */
    operator fun get(key: String): T? = findStr(key)?.let { entries[it.second].value }

    /** Returns the first value associated with [key], or null if absent or invalid. */
    operator fun get(key: AsHeaderName): T? =
        when (key) {
            is AsHeaderName.Name -> get(key.headerName)
            is AsHeaderName.Text -> get(key.text)
        }

    /** Returns the first value for [key], throwing if not present. */
    fun index(key: HeaderName): T = get(key) ?: error("no entry found for key \"${key.asStr()}\"")

    /** Returns the first value for [key], throwing if not present. */
    fun index(key: String): T = get(key) ?: error("no entry found for key \"$key\"")

    /** Returns the first value for [key], throwing if not present. */
    fun index(key: AsHeaderName): T = get(key) ?: error("no entry found for key \"${key.asStr()}\"")

    /** Returns a mutable reference to the first value associated with [key], or null if absent. */
    fun getMut(key: HeaderName): T? = get(key)

    /** Returns a mutable reference to the first value associated with [key], or null if absent. */
    fun getMut(key: String): T? = get(key)

    /** Returns a mutable reference to the first value associated with [key], or null if absent. */
    fun getMut(key: AsHeaderName): T? = get(key)

    /** Updates the first value associated with [key] using [transform] when present. */
    fun updateValue(
        key: HeaderName,
        transform: (T) -> T,
    ): Boolean {
        val found = findName(key) ?: return false
        entries[found.second].value = transform(entries[found.second].value)
        return true
    }

    /** Updates the first value associated with [key] using [transform] when present. */
    fun updateValue(
        key: String,
        transform: (T) -> T,
    ): Boolean {
        val found = findStr(key) ?: return false
        entries[found.second].value = transform(entries[found.second].value)
        return true
    }

    /** Returns a view of all values associated with [key]. */
    fun getAll(key: HeaderName): GetAll<T> = GetAll(this, findName(key)?.second)

    /** Returns a view of all values associated with [key]. */
    fun getAll(key: String): GetAll<T> = GetAll(this, findStr(key)?.second)

    /** Returns a view of all values associated with [key]. */
    fun getAll(key: AsHeaderName): GetAll<T> =
        when (key) {
            is AsHeaderName.Name -> getAll(key.headerName)
            is AsHeaderName.Text -> getAll(key.text)
        }

    /** Returns true if the map contains a value for [key]. */
    fun containsKey(key: HeaderName): Boolean = findName(key) != null

    /** Returns true if the map contains a value for [key]. */
    fun containsKey(key: String): Boolean = findStr(key) != null

    /** Returns true if the map contains a value for [key]. */
    fun containsKey(key: AsHeaderName): Boolean =
        when (key) {
            is AsHeaderName.Name -> containsKey(key.headerName)
            is AsHeaderName.Text -> containsKey(key.text)
        }

    /** Returns an iterator visiting all `(key, value)` pairs in arbitrary order. */
    fun iter(): Iter<T> =
        Iter(
            map = this,
            entry = 0,
            cursor = if (entries.isEmpty()) null else Cursor.Head,
        )

    /** Returns a mutable iterator visiting all `(key, value)` pairs. */
    fun iterMut(): IterMut<T> =
        IterMut(
            map = this,
            entry = 0,
            cursor = if (entries.isEmpty()) null else Cursor.Head,
        )

    /** Returns a consuming iterator yielding `(HeaderName?, T)` pairs. */
    fun intoIter(): IntoIter<T> =
        IntoIter(
            entries = entries,
            extraValues = extraValues,
        )

    /** Returns an iterator visiting each distinct header name once. */
    fun keys(): Keys<T> = Keys(entries)

    /** Returns an iterator visiting every stored value. */
    fun values(): Values<T> = Values(iter())

    /** Returns a mutable iterator visiting every stored value. */
    fun valuesMut(): ValuesMut<T> = ValuesMut(iterMut())

    /**
     * Clears the map, returning all `(HeaderName?, T)` entries as a drain iterator while
     * retaining allocated table storage.
     */
    fun drain(): Drain<T> {
        for (i in indices.indices) {
            indices[i] = Pos.NONE
        }
        val drainedEntries = ArrayList(entries)
        val drainedExtra = ArrayList(extraValues)
        entries.clear()
        extraValues.clear()
        return Drain(
            drainedEntries = drainedEntries,
            drainedExtraValues = drainedExtra,
        )
    }

    internal fun valueIter(idx: Int?): ValueIter<T> =
        if (idx != null) {
            val back = entries[idx].links?.let { Cursor.Values(it.tail) } ?: Cursor.Head
            ValueIter(
                map = this,
                index = idx,
                front = Cursor.Head,
                back = back,
            )
        } else {
            ValueIter(
                map = this,
                index = Int.MAX_VALUE,
                front = null,
                back = null,
            )
        }

    internal fun valueIterMut(idx: Int): ValueIterMut<T> {
        val back = entries[idx].links?.let { Cursor.Values(it.tail) } ?: Cursor.Head
        return ValueIterMut(
            map = this,
            index = idx,
            front = Cursor.Head,
            back = back,
        )
    }

    /** Returns the [Entry] corresponding to [key] for in-place manipulation. */
    fun entry(key: HeaderName): Entry<T> =
        tryEntryName(key).getOrElse { err ->
            error(err.message ?: "size overflows MAX_SIZE")
        }

    /**
     * Returns the [Entry] corresponding to [key] for in-place manipulation.
     *
     * Throws [IllegalArgumentException] if [key] is not a valid header name, or
     * [IllegalStateException] if the map exceeds `MAX_SIZE`.
     */
    fun entry(key: String): Entry<T> =
        HdrName.fromStatic(key) { hdr ->
            tryEntryHdr(hdr).getOrElse { err ->
                error(err.message ?: "size overflows MAX_SIZE")
            }
        }

    /** Returns the [Entry] corresponding to [key] for in-place manipulation. */
    fun entry(key: IntoHeaderName): Entry<T> =
        when (key) {
            is IntoHeaderName.Name -> entry(key.headerName)
            is IntoHeaderName.Static -> entry(key.text)
        }

    /** Attempts to return the [Entry] for [key]. */
    fun tryEntry(key: HeaderName): Result<Entry<T>> = tryEntryName(key)

    /** Attempts to return the [Entry] for [key], failing if invalid or over `MAX_SIZE`. */
    fun tryEntry(key: String): Result<Entry<T>> {
        val parsed =
            HdrName.fromBytes(key.encodeToByteArray()) { hdr ->
                tryEntryHdr(hdr)
            }
        return parsed.fold(
            onSuccess = { innerResult ->
                innerResult.recoverCatching {
                    throw IllegalArgumentException(InvalidHeaderName.new().message)
                }
            },
            onFailure = { Result.failure(IllegalArgumentException(InvalidHeaderName.new().message)) },
        )
    }

    /** Attempts to return the [Entry] for [key], failing if invalid or over `MAX_SIZE`. */
    fun tryEntry(key: AsHeaderName): Result<Entry<T>> =
        when (key) {
            is AsHeaderName.Name -> tryEntry(key.headerName)
            is AsHeaderName.Text -> tryEntry(key.text)
        }

    /** Inserts [key] and [value], replacing and returning the previous first value if present. */
    fun insert(
        key: HeaderName,
        value: T,
    ): T? =
        tryInsert(key, value).getOrElse { err ->
            error(err.message ?: "size overflows MAX_SIZE")
        }

    /** Inserts [key] and [value], replacing and returning the previous first value if present. */
    fun insert(
        key: String,
        value: T,
    ): T? =
        tryInsert(key, value).getOrElse { err ->
            error(err.message ?: "size overflows MAX_SIZE")
        }

    /** Inserts [key] and [value], replacing and returning the previous first value if present. */
    fun insert(
        key: IntoHeaderName,
        value: T,
    ): T? =
        tryInsert(key, value).getOrElse { err ->
            error(err.message ?: "size overflows MAX_SIZE")
        }

    /** Inserts [key] and [value], returning [MaxSizeReached] on overflow. */
    fun tryInsert(
        key: HeaderName,
        value: T,
    ): Result<T?> = tryInsertName(key, value)

    /** Inserts [key] and [value], returning [MaxSizeReached] on overflow. */
    fun tryInsert(
        key: String,
        value: T,
    ): Result<T?> = HdrName.fromStatic(key) { hdr -> tryInsertHdr(hdr, value) }

    /** Inserts [key] and [value], returning [MaxSizeReached] on overflow. */
    fun tryInsert(
        key: IntoHeaderName,
        value: T,
    ): Result<T?> =
        when (key) {
            is IntoHeaderName.Name -> tryInsert(key.headerName, value)
            is IntoHeaderName.Static -> tryInsert(key.text, value)
        }

    /** Appends [value] to the value list for [key], returning whether [key] already existed. */
    fun append(
        key: HeaderName,
        value: T,
    ): Boolean =
        tryAppend(key, value).getOrElse { err ->
            error(err.message ?: "size overflows MAX_SIZE")
        }

    /** Appends [value] to the value list for [key], returning whether [key] already existed. */
    fun append(
        key: String,
        value: T,
    ): Boolean =
        tryAppend(key, value).getOrElse { err ->
            error(err.message ?: "size overflows MAX_SIZE")
        }

    /** Appends [value] to the value list for [key], returning whether [key] already existed. */
    fun append(
        key: IntoHeaderName,
        value: T,
    ): Boolean =
        tryAppend(key, value).getOrElse { err ->
            error(err.message ?: "size overflows MAX_SIZE")
        }

    /** Appends [value] to [key], returning [MaxSizeReached] on overflow. */
    fun tryAppend(
        key: HeaderName,
        value: T,
    ): Result<Boolean> = tryAppendName(key, value)

    /** Appends [value] to [key], returning [MaxSizeReached] on overflow. */
    fun tryAppend(
        key: String,
        value: T,
    ): Result<Boolean> = HdrName.fromStatic(key) { hdr -> tryAppendHdr(hdr, value) }

    /** Appends [value] to [key], returning [MaxSizeReached] on overflow. */
    fun tryAppend(
        key: IntoHeaderName,
        value: T,
    ): Result<Boolean> =
        when (key) {
            is IntoHeaderName.Name -> tryAppend(key.headerName, value)
            is IntoHeaderName.Static -> tryAppend(key.text, value)
        }

    /** Removes [key] and all associated values, returning the first removed value if present. */
    fun remove(key: HeaderName): T? = removeFoundSlot(findName(key))

    /** Removes [key] and all associated values, returning the first removed value if present. */
    fun remove(key: String): T? = removeFoundSlot(findStr(key))

    /** Removes [key] and all associated values, returning the first removed value if present. */
    fun remove(key: AsHeaderName): T? =
        when (key) {
            is AsHeaderName.Name -> remove(key.headerName)
            is AsHeaderName.Text -> remove(key.text)
        }

    /** Extends this map using the `(HeaderName?, T)` stream protocol of [IntoIter]/[Drain]. */
    fun extend(items: Iterable<HeaderItem<T>>) {
        val iterator = items.iterator()
        if (!iterator.hasNext()) {
            return
        }
        val first = iterator.next()
        var currentKey = checkNotNull(first.name) { "expected a header name, but got null" }
        var currentVal = first.value

        outer@ while (true) {
            val occupied =
                when (val e = entry(currentKey)) {
                    is Entry.Occupied -> {
                        e.entry.insert(currentVal)
                        e.entry
                    }
                    is Entry.Vacant -> e.entry.insertEntry(currentVal)
                }

            while (true) {
                if (!iterator.hasNext()) {
                    return
                }
                val next = iterator.next()
                val nextKey = next.name
                if (nextKey != null) {
                    currentKey = nextKey
                    currentVal = next.value
                    continue@outer
                } else {
                    occupied.append(next.value)
                }
            }
        }
    }

    /** Extends this map by appending each [HeaderEntry]. */
    fun extendEntries(items: Iterable<HeaderEntry<T>>) {
        for ((k, v) in items) {
            append(k, v)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeaderMap<*>) return false
        if (len() != other.len() || keysLen() != other.keysLen()) return false
        for (key in keys()) {
            if (getAll(key).toList() != other.getAll(key).toList()) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var acc = len()
        for (key in keys()) {
            acc = acc xor (key.hashCode() * 31 + getAll(key).toList().hashCode())
        }
        return acc
    }

    /** Formats a debug map string matching `{"name": value, ...}`. */
    fun debugString(): String {
        val parts =
            iter().map { (k, v) ->
                val vStr = if (v is HeaderValue) v.debugString() else v.toString()
                "\"${k.asStr()}\": $vStr"
            }
        return "{${parts.joinToString(", ")}}"
    }

    override fun toString(): String = debugString()

    internal fun insertOccupied(
        index: Int,
        value: T,
    ): T {
        entries[index].links?.let { links ->
            removeAllExtraValues(links.next)
        }
        val entry = entries[index]
        val old = entry.value
        entry.value = value
        return old
    }

    internal fun insertOccupiedMult(
        index: Int,
        value: T,
    ): ValueDrain<T> {
        val entry = entries[index]
        val old = entry.value
        entry.value = value
        val head = entry.links?.next
        val extras = head?.let { drainAllExtraValues(entries, extraValues, it) }
        return ValueDrain(first = old, extras = extras)
    }

    internal fun removeFound(
        probe: Int,
        found: Int,
    ): Bucket<T> {
        indices[probe] = Pos.NONE
        val entry = entries.swapRemove(found)

        if (found < entries.size) {
            val moved = entries[found]
            var scanProbe = desiredPos(mask, moved.hash)
            val indicesLen = indices.size
            while (true) {
                if (scanProbe >= indicesLen) {
                    scanProbe = 0
                }
                val pos = indices[scanProbe]
                if (pos.isSome() && pos.index >= entries.size) {
                    indices[scanProbe] = Pos.new(found, moved.hash)
                    break
                }
                scanProbe += 1
            }

            moved.links?.let { links ->
                extraValues[links.next].prev = Link.Entry(found)
                extraValues[links.tail].next = Link.Entry(found)
            }
        }

        if (entries.isNotEmpty()) {
            var lastProbe = probe
            var shiftProbe = probe + 1
            val indicesLen = indices.size
            while (true) {
                if (shiftProbe >= indicesLen) {
                    shiftProbe = 0
                }
                val candidate = indices[shiftProbe]
                if (candidate.isSome() && probeDistance(mask, candidate.hash, shiftProbe) > 0) {
                    indices[lastProbe] = candidate
                    indices[shiftProbe] = Pos.NONE
                    lastProbe = shiftProbe
                    shiftProbe += 1
                } else {
                    break
                }
            }
        }

        return entry
    }

    internal fun removeAllExtraValues(headIndex: Int) {
        var head = headIndex
        while (true) {
            val extra = removeExtraValue(entries, extraValues, head)
            when (val nxt = extra.next) {
                is Link.Extra -> head = nxt.index
                is Link.Entry -> break
            }
        }
    }

    internal fun tryInsertPhaseTwo(
        key: HeaderName,
        value: T,
        hash: Int,
        probe: Int,
        isDanger: Boolean,
    ): Result<Int> {
        val index = entries.size
        tryInsertEntry(hash, key, value).getOrElse { return Result.failure(it) }
        val displaced = doInsertPhaseTwo(indices, probe, Pos.new(index, hash))
        if (isDanger || displaced >= DISPLACEMENT_THRESHOLD) {
            if (danger is Danger.Green) {
                danger = Danger.Yellow
            }
        }
        return Result.success(index)
    }

    private fun removeFoundSlot(slot: Pair<Int, Int>?): T? {
        if (slot == null) {
            return null
        }
        val (probe, idx) = slot
        entries[idx].links?.let { removeAllExtraValues(it.next) }
        return removeFound(probe, idx).value
    }

    private fun findName(key: HeaderName): Pair<Int, Int>? {
        if (entries.isEmpty()) return null
        return findSlot(hashHeaderName(danger, key)) { it == key }
    }

    private fun findStr(key: String): Pair<Int, Int>? {
        if (entries.isEmpty()) return null
        return HdrName.fromBytes(key.encodeToByteArray()) { hdr -> findHdr(hdr) }.getOrNull()
    }

    private fun findHdr(key: HdrName): Pair<Int, Int>? {
        if (entries.isEmpty()) return null
        return findSlot(hashHdrName(danger, key)) { it.eq(key) }
    }

    private inline fun findSlot(
        hash: Int,
        matchesKey: (HeaderName) -> Boolean,
    ): Pair<Int, Int>? {
        var probe = desiredPos(mask, hash)
        var dist = 0
        val len = indices.size
        while (true) {
            if (probe >= len) {
                probe = 0
            }
            val slot = indices[probe]
            if (slot.isNone()) {
                return null
            }
            if (dist > probeDistance(mask, slot.hash, probe)) {
                return null
            }
            if (slot.hash == hash && matchesKey(entries[slot.index].key)) {
                return Pair(probe, slot.index)
            }
            dist += 1
            probe += 1
        }
    }

    private fun tryEntryName(key: HeaderName): Result<Entry<T>> {
        tryReserveOne().getOrElse { return Result.failure(it) }
        val hash = hashHeaderName(danger, key)
        var result: Entry<T>? = null
        insertPhaseOne(
            hash = hash,
            matchesKey = { it == key },
            onVacant = { probe, isDanger ->
                result = Entry.Vacant(VacantEntry(this, key, hash, probe, isDanger))
            },
            onOccupied = { probe, pos ->
                result = Entry.Occupied(OccupiedEntry(this, probe, pos))
            },
        )
        return Result.success(checkNotNull(result))
    }

    private fun tryEntryHdr(key: HdrName): Result<Entry<T>> {
        tryReserveOne().getOrElse { return Result.failure(it) }
        val hash = hashHdrName(danger, key)
        var result: Entry<T>? = null
        insertPhaseOne(
            hash = hash,
            matchesKey = { it.eq(key) },
            onVacant = { probe, isDanger ->
                result = Entry.Vacant(VacantEntry(this, HeaderName.from(key), hash, probe, isDanger))
            },
            onOccupied = { probe, pos ->
                result = Entry.Occupied(OccupiedEntry(this, probe, pos))
            },
        )
        return Result.success(checkNotNull(result))
    }

    private fun tryInsertName(
        key: HeaderName,
        value: T,
    ): Result<T?> {
        tryReserveOne().getOrElse { return Result.failure(it) }
        val hash = hashHeaderName(danger, key)
        return performInsert(hash, { it == key }, { key }, value)
    }

    private fun tryInsertHdr(
        key: HdrName,
        value: T,
    ): Result<T?> {
        tryReserveOne().getOrElse { return Result.failure(it) }
        val hash = hashHdrName(danger, key)
        return performInsert(hash, { it.eq(key) }, { HeaderName.from(key) }, value)
    }

    private inline fun performInsert(
        hash: Int,
        matchesKey: (HeaderName) -> Boolean,
        materializeKey: () -> HeaderName,
        value: T,
    ): Result<T?> {
        var previous: T? = null
        var overflow: MaxSizeReached? = null
        insertPhaseOne(
            hash = hash,
            matchesKey = matchesKey,
            onVacant = { probe, isDanger ->
                if (!isDanger && indices[probe].isNone()) {
                    val index = entries.size
                    tryInsertEntry(hash, materializeKey(), value)
                        .onSuccess { indices[probe] = Pos.new(index, hash) }
                        .onFailure { overflow = it as MaxSizeReached }
                } else {
                    tryInsertPhaseTwo(materializeKey(), value, hash, probe, isDanger)
                        .onFailure { overflow = it as MaxSizeReached }
                }
            },
            onOccupied = { _, pos ->
                previous = insertOccupied(pos, value)
            },
        )
        overflow?.let { return Result.failure(it) }
        return Result.success(previous)
    }

    private fun tryAppendName(
        key: HeaderName,
        value: T,
    ): Result<Boolean> {
        tryReserveOne().getOrElse { return Result.failure(it) }
        val hash = hashHeaderName(danger, key)
        return performAppend(hash, { it == key }, { key }, value)
    }

    private fun tryAppendHdr(
        key: HdrName,
        value: T,
    ): Result<Boolean> {
        tryReserveOne().getOrElse { return Result.failure(it) }
        val hash = hashHdrName(danger, key)
        return performAppend(hash, { it.eq(key) }, { HeaderName.from(key) }, value)
    }

    private inline fun performAppend(
        hash: Int,
        matchesKey: (HeaderName) -> Boolean,
        materializeKey: () -> HeaderName,
        value: T,
    ): Result<Boolean> {
        var existed = false
        var overflow: MaxSizeReached? = null
        insertPhaseOne(
            hash = hash,
            matchesKey = matchesKey,
            onVacant = { probe, isDanger ->
                if (!isDanger && indices[probe].isNone()) {
                    val index = entries.size
                    tryInsertEntry(hash, materializeKey(), value)
                        .onSuccess { indices[probe] = Pos.new(index, hash) }
                        .onFailure { overflow = it as MaxSizeReached }
                } else {
                    tryInsertPhaseTwo(materializeKey(), value, hash, probe, isDanger)
                        .onFailure { overflow = it as MaxSizeReached }
                }
                existed = false
            },
            onOccupied = { _, pos ->
                appendValue(pos, entries[pos], extraValues, value)
                existed = true
            },
        )
        overflow?.let { return Result.failure(it) }
        return Result.success(existed)
    }

    private inline fun insertPhaseOne(
        hash: Int,
        matchesKey: (HeaderName) -> Boolean,
        onVacant: (probe: Int, isDanger: Boolean) -> Unit,
        onOccupied: (probe: Int, pos: Int) -> Unit,
    ) {
        var probe = desiredPos(mask, hash)
        var dist = 0
        val len = indices.size
        while (true) {
            if (probe >= len) {
                probe = 0
            }
            val slot = indices[probe]
            if (slot.isSome()) {
                val theirDist = probeDistance(mask, slot.hash, probe)
                if (theirDist < dist) {
                    val isDanger = dist >= FORWARD_SHIFT_THRESHOLD && danger !is Danger.Red
                    onVacant(probe, isDanger)
                    return
                } else if (slot.hash == hash && matchesKey(entries[slot.index].key)) {
                    onOccupied(probe, slot.index)
                    return
                }
            } else {
                val isDanger = dist >= FORWARD_SHIFT_THRESHOLD && danger !is Danger.Red
                onVacant(probe, isDanger)
                return
            }
            dist += 1
            probe += 1
        }
    }

    private fun tryInsertEntry(
        hash: Int,
        key: HeaderName,
        value: T,
    ): Result<Unit> {
        if (entries.size >= MAX_SIZE) {
            return Result.failure(MaxSizeReached.new())
        }
        entries.add(Bucket(hash = hash, key = key, value = value, links = null))
        return Result.success(Unit)
    }

    private fun rebuild() {
        for (index in entries.indices) {
            val entry = entries[index]
            val hash = hashHeaderName(danger, entry.key)
            entry.hash = hash
            var probe = desiredPos(mask, hash)
            var dist = 0
            val len = indices.size
            var placedDirect = false
            while (true) {
                if (probe >= len) {
                    probe = 0
                }
                val slot = indices[probe]
                if (slot.isSome()) {
                    val theirDist = probeDistance(mask, slot.hash, probe)
                    if (theirDist < dist) {
                        break
                    }
                } else {
                    indices[probe] = Pos.new(index, hash)
                    placedDirect = true
                    break
                }
                dist += 1
                probe += 1
            }
            if (!placedDirect) {
                doInsertPhaseTwo(indices, probe, Pos.new(index, hash))
            }
        }
    }

    private fun reinsertEntryInOrder(pos: Pos) {
        if (pos.isNone()) return
        var probe = desiredPos(mask, pos.hash)
        val len = indices.size
        while (true) {
            if (probe >= len) {
                probe = 0
            }
            if (indices[probe].isNone()) {
                indices[probe] = pos
                return
            }
            probe += 1
        }
    }

    private fun tryReserveOne(): Result<Unit> {
        val len = entries.size
        if (danger is Danger.Yellow) {
            val loadFactor = len.toFloat() / indices.size.toFloat()
            if (loadFactor >= LOAD_FACTOR_THRESHOLD) {
                danger = Danger.Green
                val newCap = indices.size * 2
                tryGrow(newCap).getOrElse { return Result.failure(it) }
            } else {
                danger = Danger.Red(Random.nextLong().toULong(), Random.nextLong().toULong())
                for (i in indices.indices) {
                    indices[i] = Pos.NONE
                }
                rebuild()
            }
        } else if (len == capacity()) {
            if (len == 0) {
                mask = INITIAL_RAW_CAPACITY - 1
                indices = Array(INITIAL_RAW_CAPACITY) { Pos.NONE }
            } else {
                tryGrow(indices.size shl 1).getOrElse { return Result.failure(it) }
            }
        }
        return Result.success(Unit)
    }

    private fun tryGrow(newRawCap: Int): Result<Unit> {
        if (newRawCap > MAX_SIZE) {
            return Result.failure(MaxSizeReached.new())
        }

        var firstIdeal = 0
        for (i in indices.indices) {
            val pos = indices[i]
            if (pos.isSome() && probeDistance(mask, pos.hash, i) == 0) {
                firstIdeal = i
                break
            }
        }

        val oldIndices = indices
        indices = Array(newRawCap) { Pos.NONE }
        mask = newRawCap - 1

        for (i in firstIdeal until oldIndices.size) {
            reinsertEntryInOrder(oldIndices[i])
        }
        for (i in 0 until firstIdeal) {
            reinsertEntryInOrder(oldIndices[i])
        }

        return Result.success(Unit)
    }

    companion object {
        /** Creates an empty [HeaderMap] specialized for [HeaderValue] (Swift-friendly). */
        fun new(): HeaderMap<HeaderValue> = HeaderMap()

        /** Creates an empty [HeaderMap] specialized for [HeaderValue]. */
        fun default(): HeaderMap<HeaderValue> = HeaderMap()

        /** Creates an empty [HeaderMap] for [HeaderValue] pre-sized for [capacity]. */
        fun withCapacity(capacity: Int): HeaderMap<HeaderValue> = withCapacityTyped(capacity)

        /** Creates an empty [HeaderMap] for [HeaderValue] pre-sized for [capacity], or [MaxSizeReached]. */
        fun tryWithCapacityValues(capacity: Int): Result<HeaderMap<HeaderValue>> = tryWithCapacity(capacity)

        /** Creates an empty generic [HeaderMap]. */
        fun <T : Any> newTyped(): HeaderMap<T> = HeaderMap()

        /** Creates an empty generic [HeaderMap] pre-sized for [capacity]. */
        fun <T : Any> withCapacityTyped(capacity: Int): HeaderMap<T> =
            tryWithCapacity<T>(capacity).getOrElse { err ->
                error(err.message ?: "size overflows MAX_SIZE")
            }

        /** Creates an empty generic [HeaderMap] pre-sized for [capacity], or returns [MaxSizeReached]. */
        fun <T : Any> tryWithCapacity(capacity: Int): Result<HeaderMap<T>> {
            if (capacity == 0) {
                return Result.success(HeaderMap())
            }
            if (capacity < 0) {
                return Result.failure(MaxSizeReached.new())
            }
            val rawCap = toRawCapacity(capacity).getOrElse { return Result.failure(it) }
            val nextPow2 = checkedNextPowerOfTwo(rawCap) ?: return Result.failure(MaxSizeReached.new())
            if (nextPow2 > MAX_SIZE) {
                return Result.failure(MaxSizeReached.new())
            }
            return Result.success(
                HeaderMap(
                    mask = nextPow2 - 1,
                    indices = Array(nextPow2) { Pos.NONE },
                    entries = ArrayList(usableCapacity(nextPow2)),
                    extraValues = mutableListOf(),
                    danger = Danger.Green,
                ),
            )
        }

        /** Builds a [HeaderMap] from an iterable of [HeaderItem] matching [IntoIter]/[Drain]. */
        fun <T : Any> fromIterator(iter: Iterable<HeaderItem<T>>): HeaderMap<T> =
            HeaderMap<T>().apply { extend(iter) }

        /** Builds a [HeaderMap] from an iterable of [HeaderEntry] items, appending duplicate keys. */
        fun <T : Any> fromEntries(iter: Iterable<HeaderEntry<T>>): HeaderMap<T> =
            HeaderMap<T>().apply { extendEntries(iter) }
    }
}

/** Marker wrapper identifying values usable as lookup keys in [HeaderMap]. */
sealed class AsHeaderName {
    data class Name(
        val headerName: HeaderName,
    ) : AsHeaderName()

    data class Text(
        val text: String,
    ) : AsHeaderName()

    internal fun asStr(): String =
        when (this) {
            is Name -> headerName.asStr()
            is Text -> text
        }

    companion object {
        fun of(name: HeaderName): AsHeaderName = Name(name)

        fun of(name: String): AsHeaderName = Text(name)
    }
}

/** Marker wrapper identifying values usable as insert keys in [HeaderMap]. */
sealed class IntoHeaderName {
    data class Name(
        val headerName: HeaderName,
    ) : IntoHeaderName()

    data class Static(
        val text: String,
    ) : IntoHeaderName()

    companion object {
        fun of(name: HeaderName): IntoHeaderName = Name(name)

        fun of(name: String): IntoHeaderName = Static(name)
    }
}

/** A view into a single location in a [HeaderMap], which may be vacant or occupied. */
sealed class Entry<T : Any> {
    /** An occupied entry. */
    class Occupied<T : Any>(
        val entry: OccupiedEntry<T>,
    ) : Entry<T>()

    /** A vacant entry. */
    class Vacant<T : Any>(
        val entry: VacantEntry<T>,
    ) : Entry<T>()

    /** Ensures a value is present by inserting [default] when vacant, returning the first value. */
    fun orInsert(default: T): T =
        orTryInsert(default).getOrElse { err ->
            error(err.message ?: "size overflows MAX_SIZE")
        }

    /** Ensures a value is present by inserting [default] when vacant, returning [MaxSizeReached] on overflow. */
    fun orTryInsert(default: T): Result<T> =
        when (this) {
            is Occupied -> Result.success(entry.intoMut())
            is Vacant -> entry.tryInsert(default)
        }

    /** Ensures a value is present by calling [default] when vacant, returning the first value. */
    fun orInsertWith(default: () -> T): T =
        orTryInsertWith(default).getOrElse { err ->
            error(err.message ?: "size overflows MAX_SIZE")
        }

    /** Ensures a value is present by calling [default] when vacant, returning [MaxSizeReached] on overflow. */
    fun orTryInsertWith(default: () -> T): Result<T> =
        when (this) {
            is Occupied -> Result.success(entry.intoMut())
            is Vacant -> entry.tryInsert(default())
        }

    /** Inserts [default] if vacant or transforms the existing value when occupied. */
    fun orUpdate(
        default: T,
        transform: (T) -> T,
    ): T =
        when (this) {
            is Occupied -> entry.update(transform)
            is Vacant -> entry.insert(transform(default))
        }

    /** Returns the entry's header name. */
    fun key(): HeaderName =
        when (this) {
            is Vacant -> entry.key()
            is Occupied -> entry.key()
        }
}

/** A view into a single empty slot in a [HeaderMap]. */
class VacantEntry<T : Any> internal constructor(
    private val map: HeaderMap<T>,
    private val keyValue: HeaderName,
    private val hash: Int,
    private val probe: Int,
    private val isDanger: Boolean,
) {
    /** Returns the entry's header name. */
    fun key(): HeaderName = keyValue

    /** Takes ownership of the entry's header name. */
    fun intoKey(): HeaderName = keyValue

    /** Inserts [value] into the map and returns it. */
    fun insert(value: T): T =
        tryInsert(value).getOrElse { err ->
            error(err.message ?: "size overflows MAX_SIZE")
        }

    /** Attempts to insert [value] into the map, failing with [MaxSizeReached] on overflow. */
    fun tryInsert(value: T): Result<T> {
        val index =
            map
                .tryInsertPhaseTwo(keyValue, value, hash, probe, isDanger)
                .getOrElse { return Result.failure(it) }
        return Result.success(map.entries[index].value)
    }

    /** Inserts [value] and returns an [OccupiedEntry] for further manipulation. */
    fun insertEntry(value: T): OccupiedEntry<T> =
        tryInsertEntry(value).getOrElse { err ->
            error(err.message ?: "size overflows MAX_SIZE")
        }

    /** Attempts to insert [value] and return an [OccupiedEntry]. */
    fun tryInsertEntry(value: T): Result<OccupiedEntry<T>> {
        val index =
            map
                .tryInsertPhaseTwo(keyValue, value, hash, probe, isDanger)
                .getOrElse { return Result.failure(it) }
        return Result.success(OccupiedEntry(map, probe, index))
    }
}

/** A view into a single occupied location in a [HeaderMap]. */
class OccupiedEntry<T : Any> internal constructor(
    private val map: HeaderMap<T>,
    private val probe: Int,
    private val index: Int,
) : Iterable<T> {
    override fun iterator(): Iterator<T> = intoIter().iterator()

    /** Returns the entry's header name. */
    fun key(): HeaderName = map.entries[index].key

    /** Returns the first value stored in this entry. */
    fun get(): T = map.entries[index].value

    /** Returns a mutable reference to the first value stored in this entry. */
    fun getMut(): T = map.entries[index].value

    /** Converts this entry into the first stored value. */
    fun intoMut(): T = map.entries[index].value

    /** Replaces the first stored value in place while preserving any appended values. */
    fun set(value: T) {
        map.entries[index].value = value
    }

    /** Transforms and replaces the first stored value in place, returning the updated value. */
    fun update(transform: (T) -> T): T {
        val updated = transform(map.entries[index].value)
        map.entries[index].value = updated
        return updated
    }

    /** Replaces all values associated with this entry with [value], returning the previous first value. */
    fun insert(value: T): T = map.insertOccupied(index, value)

    /** Replaces all values with [value], returning a [ValueDrain] over all previous values. */
    fun insertMult(value: T): ValueDrain<T> = map.insertOccupiedMult(index, value)

    /** Appends [value] to the end of this entry's value list. */
    fun append(value: T) {
        appendValue(index, map.entries[index], map.extraValues, value)
    }

    /** Removes the entry from the map, returning the first value. */
    fun remove(): T = removeEntry().value

    /** Removes the entry from the map, returning its key and first value. */
    fun removeEntry(): HeaderEntry<T> {
        map.entries[index].links?.let { links ->
            map.removeAllExtraValues(links.next)
        }
        val bucket = map.removeFound(probe, index)
        return HeaderEntry(bucket.key, bucket.value)
    }

    /** Removes the entry and all associated values, returning `(name, ValueDrain)`. */
    fun removeEntryMult(): RemovedHeaderValues<T> {
        val head = map.entries[index].links?.next
        val extras = head?.let { drainAllExtraValues(map.entries, map.extraValues, it) }
        val bucket = map.removeFound(probe, index)
        return RemovedHeaderValues(bucket.key, ValueDrain(first = bucket.value, extras = extras))
    }

    /** Returns an iterator over all values associated with this entry. */
    fun iter(): ValueIter<T> = map.valueIter(index)

    /** Returns a mutable iterator over all values associated with this entry. */
    fun iterMut(): ValueIterMut<T> = map.valueIterMut(index)

    /** Returns a mutable iterator over all values associated with this entry. */
    fun intoIter(): ValueIterMut<T> = map.valueIterMut(index)
}

/** A view over all values associated with a single header key. */
class GetAll<T : Any> internal constructor(
    private val map: HeaderMap<T>,
    private val index: Int?,
) : Iterable<T> {
    override fun iterator(): Iterator<T> = intoIter().iterator()

    /** Returns a fresh [ValueIter] visiting the values in insertion order. */
    fun iter(): ValueIter<T> = GetAll(map, index).intoIter()

    /** Returns a consuming [ValueIter] visiting the values in insertion order. */
    fun intoIter(): ValueIter<T> = map.valueIter(index)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetAll<*>) return false
        return toList() == other.toList()
    }

    override fun hashCode(): Int = toList().hashCode()
}

/** Entry iterator yielding [HeaderEntry] items. */
class Iter<T : Any> internal constructor(
    private val map: HeaderMap<T>,
    private var entry: Int,
    private var cursor: Cursor?,
) : Iterable<HeaderEntry<T>> {
    override fun iterator(): Iterator<HeaderEntry<T>> =
        object : AbstractIterator<HeaderEntry<T>>() {
            override fun computeNext() {
                val item = this@Iter.next()
                if (item == null) done() else setNext(item)
            }
        }

    /** Advances the iterator and returns the next [HeaderEntry], or null when exhausted. */
    fun next(): HeaderEntry<T>? {
        if (cursor == null) {
            if (entry + 1 >= map.entries.size) {
                return null
            }
            entry += 1
            cursor = Cursor.Head
        }
        val bucket = map.entries[entry]
        return when (val current = cursor) {
            is Cursor.Head -> {
                cursor = bucket.links?.let { Cursor.Values(it.next) }
                HeaderEntry(bucket.key, bucket.value)
            }
            is Cursor.Values -> {
                val extra = map.extraValues[current.index]
                cursor =
                    when (val nxt = extra.next) {
                        is Link.Entry -> null
                        is Link.Extra -> Cursor.Values(nxt.index)
                    }
                HeaderEntry(bucket.key, extra.value)
            }
            null -> null
        }
    }

    /** Returns the `(lower, upper)` bounds on remaining element count. */
    fun sizeHint(): SizeHint = SizeHint(map.entries.size - entry, null)
}

/** Mutable entry iterator yielding [HeaderEntry] items. */
class IterMut<T : Any> internal constructor(
    private val map: HeaderMap<T>,
    private var entry: Int,
    private var cursor: Cursor?,
) : Iterable<HeaderEntry<T>> {
    private var lastSlotEntry: Int? = null
    private var lastSlotExtra: Int? = null

    override fun iterator(): Iterator<HeaderEntry<T>> =
        object : AbstractIterator<HeaderEntry<T>>() {
            override fun computeNext() {
                val item = this@IterMut.next()
                if (item == null) done() else setNext(item)
            }
        }

    /** Advances the iterator and returns the next [HeaderEntry], or null when exhausted. */
    fun next(): HeaderEntry<T>? {
        if (cursor == null) {
            if (entry + 1 >= map.entries.size) {
                return null
            }
            entry += 1
            cursor = Cursor.Head
        }
        val bucket = map.entries[entry]
        return when (val current = cursor) {
            is Cursor.Head -> {
                lastSlotEntry = entry
                lastSlotExtra = null
                cursor = bucket.links?.let { Cursor.Values(it.next) }
                HeaderEntry(bucket.key, bucket.value)
            }
            is Cursor.Values -> {
                lastSlotEntry = null
                lastSlotExtra = current.index
                val extra = map.extraValues[current.index]
                cursor =
                    when (val nxt = extra.next) {
                        is Link.Entry -> null
                        is Link.Extra -> Cursor.Values(nxt.index)
                    }
                HeaderEntry(bucket.key, extra.value)
            }
            null -> null
        }
    }

    /** Replaces the value most recently yielded by [next] inside the underlying [HeaderMap]. */
    fun replaceCurrent(value: T) {
        lastSlotEntry?.let { map.entries[it].value = value }
        lastSlotExtra?.let { map.extraValues[it].value = value }
    }

    /** Returns the `(lower, upper)` bounds on remaining element count. */
    fun sizeHint(): SizeHint = SizeHint(map.entries.size - entry, null)
}

/** Consuming iterator over [HeaderItem] entries from [HeaderMap]. */
class IntoIter<T : Any> internal constructor(
    private val entries: MutableList<Bucket<T>>,
    private val extraValues: MutableList<ExtraValue<T>>,
) : Iterable<HeaderItem<T>> {
    private var entryIdx: Int = 0
    private var nextExtra: Int? = null

    override fun iterator(): Iterator<HeaderItem<T>> =
        object : AbstractIterator<HeaderItem<T>>() {
            override fun computeNext() {
                val item = this@IntoIter.next()
                if (item == null) done() else setNext(item)
            }
        }

    /** Advances and returns the next [HeaderItem], or null when exhausted. */
    fun next(): HeaderItem<T>? {
        val extraIdx = nextExtra
        if (extraIdx != null) {
            val extra = extraValues[extraIdx]
            nextExtra =
                when (val nxt = extra.next) {
                    is Link.Entry -> null
                    is Link.Extra -> nxt.index
                }
            return HeaderItem(null, extra.value)
        }
        if (entryIdx < entries.size) {
            val bucket = entries[entryIdx++]
            nextExtra = bucket.links?.next
            return HeaderItem(bucket.key, bucket.value)
        }
        return null
    }

    /** Returns the `(lower, upper)` bounds on remaining elements. */
    fun sizeHint(): SizeHint = SizeHint((entries.size - entryIdx).coerceAtLeast(0), null)
}

/** Iterator over distinct header names in a [HeaderMap]. */
class Keys<T : Any> internal constructor(
    private val entries: List<Bucket<T>>,
) : Iterable<HeaderName> {
    private var idx: Int = 0

    override fun iterator(): Iterator<HeaderName> =
        object : AbstractIterator<HeaderName>() {
            override fun computeNext() {
                val item = this@Keys.next()
                if (item == null) done() else setNext(item)
            }
        }

    /** Returns the next distinct [HeaderName], or null when exhausted. */
    fun next(): HeaderName? = if (idx < entries.size) entries[idx++].key else null

    /** Returns exact `(remaining, remaining)` bounds. */
    fun sizeHint(): SizeHint {
        val remaining = (entries.size - idx).coerceAtLeast(0)
        return SizeHint(remaining, remaining)
    }

    /** Advances by [n] items and returns the key at that position, or null. */
    fun nth(n: Int): HeaderName? {
        if (n < 0 || idx + n >= entries.size) {
            idx = entries.size
            return null
        }
        val key = entries[idx + n].key
        idx += n + 1
        return key
    }

    /** Consumes the iterator and returns the remaining key count. */
    fun count(): Int {
        val remaining = (entries.size - idx).coerceAtLeast(0)
        idx = entries.size
        return remaining
    }

    /** Consumes the iterator and returns the final key, if any remain. */
    fun last(): HeaderName? {
        if (idx >= entries.size) return null
        val key = entries[entries.lastIndex].key
        idx = entries.size
        return key
    }
}

/** Iterator over all header values stored in a [HeaderMap]. */
class Values<T : Any> internal constructor(
    private val inner: Iter<T>,
) : Iterable<T> {
    override fun iterator(): Iterator<T> =
        object : AbstractIterator<T>() {
            override fun computeNext() {
                val item = this@Values.next()
                if (item == null) done() else setNext(item)
            }
        }

    /** Returns the next value, or null when exhausted. */
    fun next(): T? = inner.next()?.value

    /** Returns the underlying `(lower, upper)` size hint. */
    fun sizeHint(): SizeHint = inner.sizeHint()
}

/** Mutable iterator over all header values stored in a [HeaderMap]. */
class ValuesMut<T : Any> internal constructor(
    private val inner: IterMut<T>,
) : Iterable<T> {
    override fun iterator(): Iterator<T> =
        object : AbstractIterator<T>() {
            override fun computeNext() {
                val item = this@ValuesMut.next()
                if (item == null) done() else setNext(item)
            }
        }

    /** Returns the next value, or null when exhausted. */
    fun next(): T? = inner.next()?.value

    /** Replaces the value most recently yielded by [next] in place. */
    fun replaceCurrent(value: T) {
        inner.replaceCurrent(value)
    }

    /** Returns the underlying `(lower, upper)` size hint. */
    fun sizeHint(): SizeHint = inner.sizeHint()
}

/** Draining iterator removing and yielding all [HeaderItem] entries from a [HeaderMap]. */
class Drain<T : Any> internal constructor(
    private val drainedEntries: MutableList<Bucket<T>>,
    private val drainedExtraValues: MutableList<ExtraValue<T>>,
) : Iterable<HeaderItem<T>> {
    private var idx: Int = 0
    private var nextExtra: Int? = null

    override fun iterator(): Iterator<HeaderItem<T>> =
        object : AbstractIterator<HeaderItem<T>>() {
            override fun computeNext() {
                val item = this@Drain.next()
                if (item == null) done() else setNext(item)
            }
        }

    /** Advances and yields the next [HeaderItem], or null when exhausted. */
    fun next(): HeaderItem<T>? {
        val extraIdx = nextExtra
        if (extraIdx != null) {
            val extra = removeExtraValue(drainedEntries, drainedExtraValues, extraIdx)
            nextExtra =
                when (val nxt = extra.next) {
                    is Link.Extra -> nxt.index
                    is Link.Entry -> null
                }
            return HeaderItem(null, extra.value)
        }
        if (idx == drainedEntries.size) {
            return null
        }
        val bucket = drainedEntries[idx]
        idx += 1
        nextExtra = bucket.links?.next
        return HeaderItem(bucket.key, bucket.value)
    }

    /** Returns `(remainingKeys, remainingTotalValues)` size hints. */
    fun sizeHint(): SizeHint {
        val lower = drainedEntries.size - idx
        val upper = drainedExtraValues.size + lower
        return SizeHint(lower, upper)
    }
}

/** Double-ended iterator over all values associated with a single header entry. */
class ValueIter<T : Any> internal constructor(
    private val map: HeaderMap<T>,
    private val index: Int,
    private var front: Cursor?,
    private var back: Cursor?,
) : Iterable<T> {
    override fun iterator(): Iterator<T> =
        object : AbstractIterator<T>() {
            override fun computeNext() {
                val item = this@ValueIter.next()
                if (item == null) done() else setNext(item)
            }
        }

    /** Advances from the front and returns the next value, or null when exhausted. */
    fun next(): T? =
        when (val f = front) {
            is Cursor.Head -> {
                val entry = map.entries[index]
                if (back == Cursor.Head) {
                    front = null
                    back = null
                } else {
                    val links = entry.links ?: error("unreachable")
                    front = Cursor.Values(links.next)
                }
                entry.value
            }
            is Cursor.Values -> {
                val extra = map.extraValues[f.index]
                if (front == back) {
                    front = null
                    back = null
                } else {
                    front =
                        when (val nxt = extra.next) {
                            is Link.Entry -> null
                            is Link.Extra -> Cursor.Values(nxt.index)
                        }
                }
                extra.value
            }
            null -> null
        }

    /** Advances from the back and returns the trailing value, or null when exhausted. */
    fun nextBack(): T? =
        when (val b = back) {
            is Cursor.Head -> {
                front = null
                back = null
                map.entries[index].value
            }
            is Cursor.Values -> {
                val extra = map.extraValues[b.index]
                if (front == back) {
                    front = null
                    back = null
                } else {
                    back =
                        when (val prv = extra.prev) {
                            is Link.Entry -> Cursor.Head
                            is Link.Extra -> Cursor.Values(prv.index)
                        }
                }
                extra.value
            }
            null -> null
        }

    /** Returns `(lower, upper)` bounds on remaining element count. */
    fun sizeHint(): SizeHint =
        when {
            front == Cursor.Head && back == Cursor.Head -> SizeHint(1, 1)
            front != null -> SizeHint(1, null)
            else -> SizeHint(0, 0)
        }
}

/** Mutable double-ended iterator over all values associated with a single header entry. */
class ValueIterMut<T : Any> internal constructor(
    private val map: HeaderMap<T>,
    private val index: Int,
    private var front: Cursor?,
    private var back: Cursor?,
) : Iterable<T> {
    private var lastWasHead: Boolean = false
    private var lastExtraIndex: Int? = null

    override fun iterator(): Iterator<T> =
        object : AbstractIterator<T>() {
            override fun computeNext() {
                val item = this@ValueIterMut.next()
                if (item == null) done() else setNext(item)
            }
        }

    /** Advances from the front and returns the next value, or null when exhausted. */
    fun next(): T? =
        when (val f = front) {
            is Cursor.Head -> {
                lastWasHead = true
                lastExtraIndex = null
                val entry = map.entries[index]
                if (back == Cursor.Head) {
                    front = null
                    back = null
                } else {
                    val links = entry.links ?: error("unreachable")
                    front = Cursor.Values(links.next)
                }
                entry.value
            }
            is Cursor.Values -> {
                lastWasHead = false
                lastExtraIndex = f.index
                val extra = map.extraValues[f.index]
                if (front == back) {
                    front = null
                    back = null
                } else {
                    front =
                        when (val nxt = extra.next) {
                            is Link.Entry -> null
                            is Link.Extra -> Cursor.Values(nxt.index)
                        }
                }
                extra.value
            }
            null -> null
        }

    /** Advances from the back and returns the trailing value, or null when exhausted. */
    fun nextBack(): T? =
        when (val b = back) {
            is Cursor.Head -> {
                lastWasHead = true
                lastExtraIndex = null
                front = null
                back = null
                map.entries[index].value
            }
            is Cursor.Values -> {
                lastWasHead = false
                lastExtraIndex = b.index
                val extra = map.extraValues[b.index]
                if (front == back) {
                    front = null
                    back = null
                } else {
                    back =
                        when (val prv = extra.prev) {
                            is Link.Entry -> Cursor.Head
                            is Link.Extra -> Cursor.Values(prv.index)
                        }
                }
                extra.value
            }
            null -> null
        }

    /** Replaces the value most recently yielded by [next] or [nextBack] in place. */
    fun replaceCurrent(value: T) {
        if (lastWasHead) {
            map.entries[index].value = value
        } else {
            lastExtraIndex?.let { map.extraValues[it].value = value }
        }
    }

    /** Returns `(lower, upper)` bounds on remaining element count. */
    fun sizeHint(): SizeHint =
        when {
            front == Cursor.Head && back == Cursor.Head -> SizeHint(1, 1)
            front != null -> SizeHint(1, null)
            else -> SizeHint(0, 0)
        }
}

/** Drain iterator yielding all removed values from a single occupied entry. */
class ValueDrain<T : Any> internal constructor(
    private var first: T?,
    private val extras: List<T>?,
) : Iterable<T> {
    private var extraIdx: Int = 0

    override fun iterator(): Iterator<T> =
        object : AbstractIterator<T>() {
            override fun computeNext() {
                val item = this@ValueDrain.next()
                if (item == null) done() else setNext(item)
            }
        }

    /** Advances and returns the next drained value, or null when exhausted. */
    fun next(): T? {
        first?.let { head ->
            first = null
            return head
        }
        val list = extras ?: return null
        if (extraIdx < list.size) {
            return list[extraIdx++]
        }
        return null
    }

    /** Returns exact `(remaining, remaining)` count bounds. */
    fun sizeHint(): SizeHint {
        val remExtras = extras?.let { (it.size - extraIdx).coerceAtLeast(0) } ?: 0
        val total = (if (first != null) 1 else 0) + remExtras
        return SizeHint(total, total)
    }
}

/** Error returned when the maximum capacity of [HeaderMap] is exceeded. */
class MaxSizeReached private constructor() : IllegalStateException("max number of headers exceeded") {
    /** Returns the Rust Debug representation `"MaxSizeReached"`. */
    fun debugString(): String = "MaxSizeReached"

    override fun toString(): String = "max size reached"

    companion object {
        internal fun new(): MaxSizeReached = MaxSizeReached()
    }
}

internal sealed class Cursor {
    data object Head : Cursor()

    data class Values(
        val index: Int,
    ) : Cursor()
}

internal data class Pos(
    val index: Int,
    val hash: Int,
) {
    fun isSome(): Boolean = index != NONE_INDEX

    fun isNone(): Boolean = index == NONE_INDEX

    companion object {
        val NONE: Pos = Pos(index = NONE_INDEX, hash = 0)

        fun new(
            index: Int,
            hash: Int,
        ): Pos {
            check(index in 0 until MAX_SIZE)
            return Pos(index = index, hash = hash)
        }
    }
}

internal class Bucket<T : Any>(
    var hash: Int,
    val key: HeaderName,
    var value: T,
    var links: Links?,
)

internal class Links(
    var next: Int,
    var tail: Int,
)

internal class ExtraValue<T : Any>(
    var value: T,
    var prev: Link,
    var next: Link,
)

internal sealed class Link {
    data class Entry(
        val index: Int,
    ) : Link()

    data class Extra(
        val index: Int,
    ) : Link()
}

internal sealed class Danger {
    data object Green : Danger()

    data object Yellow : Danger()

    data class Red(
        val k0: ULong,
        val k1: ULong,
    ) : Danger()
}

private fun <E> MutableList<E>.swapRemove(idx: Int): E {
    val lastIdx = lastIndex
    val removed = this[idx]
    val last = removeAt(lastIdx)
    if (idx != lastIdx) {
        this[idx] = last
    }
    return removed
}

private fun <T : Any> removeExtraValue(
    entries: MutableList<Bucket<T>>,
    extraValues: MutableList<ExtraValue<T>>,
    idx: Int,
): ExtraValue<T> {
    val original = extraValues[idx]
    val prev = original.prev
    val next = original.next

    when (prev) {
        is Link.Entry -> {
            when (next) {
                is Link.Entry -> {
                    check(prev.index == next.index)
                    entries[prev.index].links = null
                }
                is Link.Extra -> {
                    checkNotNull(entries[prev.index].links).next = next.index
                    extraValues[next.index].prev = Link.Entry(prev.index)
                }
            }
        }
        is Link.Extra -> {
            when (next) {
                is Link.Entry -> {
                    checkNotNull(entries[next.index].links).tail = prev.index
                    extraValues[prev.index].next = Link.Entry(next.index)
                }
                is Link.Extra -> {
                    extraValues[prev.index].next = Link.Extra(next.index)
                    extraValues[next.index].prev = Link.Extra(prev.index)
                }
            }
        }
    }

    val removed = extraValues.swapRemove(idx)
    val oldIdx = extraValues.size

    if (removed.prev == Link.Extra(oldIdx)) {
        removed.prev = Link.Extra(idx)
    }
    if (removed.next == Link.Extra(oldIdx)) {
        removed.next = Link.Extra(idx)
    }

    if (idx != oldIdx) {
        val moved = extraValues[idx]
        when (val movedPrev = moved.prev) {
            is Link.Entry -> checkNotNull(entries[movedPrev.index].links).next = idx
            is Link.Extra -> extraValues[movedPrev.index].next = Link.Extra(idx)
        }
        when (val movedNext = moved.next) {
            is Link.Entry -> checkNotNull(entries[movedNext.index].links).tail = idx
            is Link.Extra -> extraValues[movedNext.index].prev = Link.Extra(idx)
        }
    }

    return removed
}

private fun <T : Any> drainAllExtraValues(
    entries: MutableList<Bucket<T>>,
    extraValues: MutableList<ExtraValue<T>>,
    headIndex: Int,
): List<T> {
    var head = headIndex
    val drained = mutableListOf<T>()
    while (true) {
        val extra = removeExtraValue(entries, extraValues, head)
        drained.add(extra.value)
        when (val nxt = extra.next) {
            is Link.Extra -> head = nxt.index
            is Link.Entry -> break
        }
    }
    return drained
}

private fun doInsertPhaseTwo(
    indices: Array<Pos>,
    startProbe: Int,
    initialPos: Pos,
): Int {
    var probe = startProbe
    var oldPos = initialPos
    var numDisplaced = 0
    val len = indices.size
    check(len > 0)
    while (true) {
        if (probe >= len) {
            probe = 0
        }
        val current = indices[probe]
        if (current.isNone()) {
            indices[probe] = oldPos
            break
        } else {
            numDisplaced += 1
            indices[probe] = oldPos
            oldPos = current
        }
        probe += 1
    }
    return numDisplaced
}

private fun <T : Any> appendValue(
    entryIdx: Int,
    entry: Bucket<T>,
    extra: MutableList<ExtraValue<T>>,
    value: T,
) {
    val links = entry.links
    if (links != null) {
        val idx = extra.size
        extra.add(
            ExtraValue(
                value = value,
                prev = Link.Extra(links.tail),
                next = Link.Entry(entryIdx),
            ),
        )
        extra[links.tail].next = Link.Extra(idx)
        links.tail = idx
    } else {
        val idx = extra.size
        extra.add(
            ExtraValue(
                value = value,
                prev = Link.Entry(entryIdx),
                next = Link.Entry(entryIdx),
            ),
        )
        entry.links = Links(next = idx, tail = idx)
    }
}

private fun usableCapacity(cap: Int): Int = cap - cap / 4

private fun toRawCapacity(n: Int): Result<Int> {
    if (n < 0) return Result.failure(MaxSizeReached.new())
    val raw = n.toLong() + (n / 3).toLong()
    if (raw > Int.MAX_VALUE.toLong()) return Result.failure(MaxSizeReached.new())
    return Result.success(raw.toInt())
}

private fun checkedNextPowerOfTwo(n: Int): Int? {
    if (n <= 0) return 1
    if (n > (1 shl 30)) return null
    var v = n - 1
    v = v or (v ushr 1)
    v = v or (v ushr 2)
    v = v or (v ushr 4)
    v = v or (v ushr 8)
    v = v or (v ushr 16)
    return v + 1
}

private fun desiredPos(
    mask: Int,
    hash: Int,
): Int = hash and mask

private fun probeDistance(
    mask: Int,
    hash: Int,
    current: Int,
): Int = (current - desiredPos(mask, hash)) and mask

private fun hashHeaderName(
    danger: Danger,
    key: HeaderName,
): Int = hashRepr(danger, key.inner, customBytes = { it.value.asStr().encodeToByteArray() }, isLower = { true })

private fun hashHdrName(
    danger: Danger,
    key: HdrName,
): Int = hashRepr(danger, key.inner, customBytes = { it.buf }, isLower = { it.lower })

private inline fun <C> hashRepr(
    danger: Danger,
    repr: HeaderNameRepr<C>,
    customBytes: (C) -> ByteArray,
    isLower: (C) -> Boolean,
): Int {
    val state0: ULong
    val k1: ULong
    val isRed: Boolean
    if (danger is Danger.Red) {
        state0 = 0xcbf29ce484222325UL xor danger.k0
        k1 = danger.k1
        isRed = true
    } else {
        state0 = 0xcbf29ce484222325UL
        k1 = 0UL
        isRed = false
    }

    var state = state0
    when (repr) {
        is HeaderNameRepr.Standard -> {
            state = mixByte(state, 0.toByte(), k1)
            val ord = repr.header.ordinal
            state = mixByte(state, (ord and 0xFF).toByte(), k1)
            state = mixByte(state, ((ord ushr 8) and 0xFF).toByte(), k1)
        }
        is HeaderNameRepr.Custom -> {
            state = mixByte(state, 1.toByte(), k1)
            val bytes = customBytes(repr.value)
            val alreadyLower = isLower(repr.value)
            for (b in bytes) {
                val normalized = if (alreadyLower) b else asciiLowerByte(b)
                state = mixByte(state, normalized, k1)
            }
        }
    }

    if (isRed) {
        state = state xor (state shr 33)
        state *= 0xff51afd7ed558ccdUL
        state = state xor (state shr 33)
        state *= 0xc4ceb9fe1a85ec53UL
        state = state xor (state shr 33)
    }

    return (state and HASH_MASK).toInt()
}

private fun mixByte(
    state: ULong,
    b: Byte,
    salt: ULong,
): ULong = (state xor ((b.toInt() and 0xFF).toULong() + salt)) * 0x100000001b3UL

private fun asciiLowerByte(b: Byte): Byte =
    if (b in 'A'.code.toByte()..'Z'.code.toByte()) {
        (b + ('a'.code - 'A'.code)).toByte()
    } else {
        b
    }

@Suppress("UNCHECKED_CAST")
private fun <T : Any> cloneValueIfPossible(value: T): T =
    if (value is HeaderValue) {
        value.clone() as T
    } else {
        value
    }
