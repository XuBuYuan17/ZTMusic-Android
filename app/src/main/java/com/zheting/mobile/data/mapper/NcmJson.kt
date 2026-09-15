package com.zheting.mobile.data.mapper

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 网易云宽松 JSON 的收窄工具（映射表见 docs/api-contract.md §3）。
 * 所有取值缺省回退，不抛异常；id 统一转 String。
 */
object NcmJson {
    fun asObj(e: JsonElement?): JsonObject? = e as? JsonObject

    fun asArray(e: JsonElement?): JsonArray? = e as? JsonArray

    fun str(obj: JsonObject?, key: String): String =
        (obj?.get(key) as? JsonPrimitive)?.let { if (it.isString) it.content else it.contentOrNull } ?: ""

    fun long(obj: JsonObject?, key: String): Long =
        (obj?.get(key) as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L

    fun int(obj: JsonObject?, key: String): Int =
        (obj?.get(key) as? JsonPrimitive)?.content?.toIntOrNull() ?: 0

    fun bool(obj: JsonObject?, key: String): Boolean =
        (obj?.get(key) as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false

    /** id 可为数字或字符串，统一转 String；缺失返回 null（丢弃无 id 元素）。 */
    fun id(obj: JsonObject?): String? {
        val v = obj?.get("id") as? JsonPrimitive ?: return null
        val s = (if (v.isString) v.content else v.contentOrNull) ?: return null
        return s.trim().ifEmpty { null }
    }
}