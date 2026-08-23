// port-lint: source extensions.rs
package io.github.kotlinmania.http

import kotlin.reflect.KClass

private typealias AnyMap = MutableMap<KClass<*>, AnyClone>

/**
 * A type map of protocol extensions.
 *
 * `Extensions` can be used by `Request` and `Response` to store
 * extra data derived from the underlying protocol.
 */
class Extensions private constructor(
    // If extensions are never used, no need to carry around an empty HashMap.
    // That's 3 words. Instead, this is only 1 word.
    private var map: AnyMap?,
) {
    companion object {
        /** Create an empty `Extensions`. */
        fun new(): Extensions = Extensions(null)

        fun default(): Extensions = new()
    }

    /**
     * Insert a type into this `Extensions`.
     *
     * If a extension of this type already existed, it will
     * be returned and replaced with the new one.
     *
     * # Example
     *
     * ```
     * val ext = Extensions.new()
     * check(ext.insert(5) == null)
     * check(ext.insert(4.toUByte()) == null)
     * check(ext.insert(9) == 5)
     * ```
     */
    inline fun <reified T : Any> insert(value: T): T? = insert(value) { it }

    inline fun <reified T : Any> insert(
        value: T,
        noinline clone: (T) -> T,
    ): T? {
        val erasedClone: (Any) -> Any = { v -> clone(v as T) }
        return insert(T::class, T::class.simpleName ?: T::class.toString(), value, erasedClone) as? T
    }

    fun insert(
        type: KClass<*>,
        typeName: String,
        value: Any,
        clone: (Any) -> Any,
    ): Any? {
        val extensions = map ?: mutableMapOf<KClass<*>, AnyClone>().also { map = it }
        return extensions.put(type, AnyClone(value, typeName, clone))?.intoAny()
    }

    /**
     * Get a reference to a type previously inserted on this `Extensions`.
     *
     * # Example
     *
     * ```
     * val ext = Extensions.new()
     * check(ext.get<Int>() == null)
     * ext.insert(5)
     *
     * check(ext.get<Int>() == 5)
     * ```
     */
    inline fun <reified T : Any> get(): T? = get(T::class) as? T

    fun get(type: KClass<*>): Any? = map?.get(type)?.asAny()

    /**
     * Get a mutable reference to a type previously inserted on this `Extensions`.
     *
     * # Example
     *
     * ```
     * val ext = Extensions.new()
     * ext.insert("Hello")
     * val value = ext.getMut<String>() + " World"
     * ext.insert(value)
     *
     * check(ext.get<String>() == "Hello World")
     * ```
     */
    inline fun <reified T : Any> getMut(): T? = getMut(T::class) as? T

    fun getMut(type: KClass<*>): Any? = map?.get(type)?.asAnyMut()

    /**
     * Get a mutable reference to a type, inserting `value` if not already present on this
     * `Extensions`.
     *
     * # Example
     *
     * ```
     * val ext = Extensions.new()
     * ext.insert(ext.getOrInsert(1) + 2)
     *
     * check(ext.get<Int>() == 3)
     * ```
     */
    inline fun <reified T : Any> getOrInsert(value: T): T = getOrInsertWith { value }

    /**
     * Get a mutable reference to a type, inserting the value created by `f` if not already present
     * on this `Extensions`.
     *
     * # Example
     *
     * ```
     * val ext = Extensions.new()
     * ext.insert(ext.getOrInsertWith { 1 } + 2)
     *
     * check(ext.get<Int>() == 3)
     * ```
     */
    inline fun <reified T : Any> getOrInsertWith(noinline f: () -> T): T {
        val current = get<T>()
        if (current != null) {
            return current
        }

        val created = f()
        insert(created)
        return created
    }

    /**
     * Get a mutable reference to a type, inserting the type's default value if not already present
     * on this `Extensions`.
     *
     * # Example
     *
     * ```
     * val ext = Extensions.new()
     * ext.insert(ext.getOrInsertDefault { 0 } + 2)
     *
     * check(ext.get<Int>() == 2)
     * ```
     */
    inline fun <reified T : Any> getOrInsertDefault(noinline default: () -> T): T = getOrInsertWith(default)

    /**
     * Remove a type from this `Extensions`.
     *
     * If a extension of this type existed, it will be returned.
     *
     * # Example
     *
     * ```
     * val ext = Extensions.new()
     * ext.insert(5)
     * check(ext.remove<Int>() == 5)
     * check(ext.get<Int>() == null)
     * ```
     */
    inline fun <reified T : Any> remove(): T? = remove(T::class) as? T

    fun remove(type: KClass<*>): Any? = map?.remove(type)?.intoAny()

    /**
     * Clear the `Extensions` of all inserted extensions.
     *
     * # Example
     *
     * ```
     * val ext = Extensions.new()
     * ext.insert(5)
     * ext.clear()
     *
     * check(ext.get<Int>() == null)
     * ```
     */
    fun clear() {
        map?.clear()
    }

    /**
     * Check whether the extension set is empty or not.
     *
     * # Example
     *
     * ```
     * val ext = Extensions.new()
     * check(ext.isEmpty())
     * ext.insert(5)
     * check(!ext.isEmpty())
     * ```
     */
    fun isEmpty(): Boolean = map?.isEmpty() ?: true

    /**
     * Get the number of extensions available.
     *
     * # Example
     *
     * ```
     * val ext = Extensions.new()
     * check(ext.len() == 0)
     * ext.insert(5)
     * check(ext.len() == 1)
     * ```
     */
    fun len(): Int = map?.size ?: 0

    /**
     * Extends `self` with another `Extensions`.
     *
     * If an instance of a specific type exists in both, the one in `self` is overwritten with the
     * one from `other`.
     *
     * # Example
     *
     * ```
     * val extA = Extensions.new()
     * extA.insert(8.toUByte())
     * extA.insert(16.toUShort())
     *
     * val extB = Extensions.new()
     * extB.insert(4.toUByte())
     * extB.insert("hello")
     *
     * extA.extend(extB)
     * check(extA.len() == 3)
     * check(extA.get<UByte>() == 4.toUByte())
     * check(extA.get<UShort>() == 16.toUShort())
     * check(extA.get<String>() == "hello")
     * ```
     */
    fun extend(other: Extensions) {
        val otherMap = other.map
        if (otherMap != null) {
            val extensions = map
            if (extensions != null) {
                extensions.putAll(otherMap)
            } else {
                map = otherMap.toMutableMap()
            }
        }
    }

    fun clone(): Extensions = Extensions(map?.mapValues { entry -> entry.value.cloneBox() }?.toMutableMap())

    fun debugString(): String {
        val extensions = map ?: return "{}"
        return extensions.values.joinToString(prefix = "{", postfix = "}") { value ->
            TypeName(value.typeName()).toString()
        }
    }

    fun fmt(): String = debugString()

    fun fmt(formatter: StringBuilder): StringBuilder {
        formatter.append(debugString())
        return formatter
    }

    override fun toString(): String = debugString()
}

private data class TypeName(
    private val value: String,
) {
    override fun toString(): String = value
}

private class AnyClone(
    private val value: Any,
    private val typeName: String,
    private val clone: (Any) -> Any,
) {
    fun cloneBox(): AnyClone = AnyClone(clone(value), typeName, clone)

    fun asAny(): Any = value

    fun asAnyMut(): Any = value

    fun intoAny(): Any = value

    fun typeName(): String = typeName
}
