package com.zheting.mobile

import com.zheting.mobile.core.network.AccountResultDto
import com.zheting.mobile.data.mapper.toAuthUser
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionDtoMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun decode(raw: String): AccountResultDto = json.decodeFromString(raw)

    @Test
    fun decode_loginStatus_fullProfile() {
        val dto = decode(
            """
            {
              "code": 200,
              "data": {
                "code": 200,
                "account": { "id": 10001 },
                "profile": { "userId": 10001, "nickname": "海", "avatarUrl": "https://picsum/1.jpg" }
              }
            }
            """.trimIndent(),
        )
        val user = dto.toAuthUser()
        assertEquals("10001", user?.userId)
        assertEquals("海", user?.nickname)
        assertEquals("https://picsum/1.jpg", user?.avatarUrl)
        assertEquals(200, dto.actualCode)
    }

    @Test
    fun decode_userAccount_topLevelProfileWins() {
        // /user/account 可能把 profile 放在顶层
        val dto = decode(
            """
            {
              "code": 200,
              "profile": { "userId": 7, "nickname": "top" },
              "data": { "profile": { "userId": 8, "nickname": "nested" } }
            }
            """.trimIndent(),
        )
        val user = dto.toAuthUser()
        assertEquals("7", user?.userId)
        assertEquals("top", user?.nickname)
    }

    @Test
    fun decode_fallbackToAccountIdAndNickname() {
        val dto = decode(
            """
            {
              "code": 200,
              "account": { "id": 42 }
            }
            """.trimIndent(),
        )
        val user = dto.toAuthUser()
        assertEquals("42", user?.userId)
        assertEquals("用户", user?.nickname)
    }

    @Test
    fun decode_noAccountMeansNull() {
        val dto = decode("""{ "code": -1, "message": "登录已失效" }""")
        assertNull(dto.toAuthUser())
    }

    @Test
    fun actualCode_prefersTopLevel() {
        val dto = decode("""{ "code": 301, "data": { "code": 200 } }""")
        assertEquals(301, dto.actualCode)
    }

    @Test
    fun isAnonymous_flagFromEitherLevel() {
        assert(
            decode("""{ "code": 200, "data": { "account": { "anonimousUser": true } } }""")
                .isAnonymous,
        )
        assert(
            decode("""{ "code": 200, "account": { "id": 1, "anonimousUser": true } }""")
                .isAnonymous,
        )
    }
}