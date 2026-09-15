package com.zheting.mobile.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * API 配置与会话的持久化。key 名与原项目（本地存储的 localStorage key）保持一致：
 * api_base / api_cookie，便于迁移旧会话。
 *
 * DataStore 的读写是异步的；网络拦截器只读内存缓存，写操作走这里落盘。
 */
private val Context.apiDataStore: DataStore<Preferences> by preferencesDataStore(name = "zheting_prefs")

object PreferenceKeys {
    val apiBase: Preferences.Key<String> = stringPreferencesKey("api_base")
    val apiCookie: Preferences.Key<String> = stringPreferencesKey("api_cookie")
}

class PreferenceStore(private val context: Context) {
    private val store: DataStore<Preferences> get() = context.apiDataStore

    fun apiBase(): Flow<String> = store.data.map { it[PreferenceKeys.apiBase].orEmpty() }

    fun apiCookie(): Flow<String> = store.data.map { it[PreferenceKeys.apiCookie].orEmpty() }

    suspend fun setApiBase(base: String) {
        store.edit { it[PreferenceKeys.apiBase] = base }
    }

    suspend fun setApiCookie(cookie: String) {
        store.edit { it[PreferenceKeys.apiCookie] = cookie }
    }
}