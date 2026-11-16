/*
 * 本ソフトウェアは無保証で提供されるフリーソフトウェアです。
 * 商用・非商用を問わず、複製・改変・再配布が自由に許可されます。
 * 本ソフトウェアの使用によって生じた直接・間接の損害について、誰も責任を負いません。
 */

package jp.ruma.freesoft.kotlinlesson.callConfirmation.util

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import jp.ruma.freesoft.kotlinlesson.callConfirmation.Const
import jp.ruma.freesoft.kotlinlesson.callConfirmation.enum.LockMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException

/**
 * 設定値を保持する Data Store
 *  ダイレクトブート状態に対応するため、常に Device Protected Storage (DE Storage) を使用する。
 *
 *  AI Agentによるアシストコーディング
 */

class AppPreference(private val context: Context) {
    companion object {
        // Preference filename
        internal const val DATASTORE_NAME = "user_settings"

        // Preference Key
        private val PREFERENCE_KEY_VERSION = stringPreferencesKey("version")
        private val PREFERENCE_KEY_LOCK_MODE = intPreferencesKey("lock_mode")
        private val PREFERENCE_KEY_KEEP_ALIVE = booleanPreferencesKey("keep_alive")
        private val PREFERENCE_KEY_OUTSIDE_DISMISS = booleanPreferencesKey("outside_dismiss")
    }

    // プライベートレジストリをメンバ変数に持たせ、カスタムgetterを定義しておく.
    private val dataStore: DataStore<Preferences>
        get() = DeDataStoreRegistry.getInstance(context)

    /**
     * 例外発生時に安全に空の Preferences を返却する Flow
     */
    private val safeDataFlow: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(Const.TAG, "Error reading DataStore preferences: ${exception.message}", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    /**
     * バージョン情報を同期読み出しする（デフォルト値: ""）
     */
    fun getVersionSync(): String = try {
        runBlocking {
            safeDataFlow.map { preferences ->
                preferences[PREFERENCE_KEY_VERSION] ?: ""
            }.first()
        }
    } catch (e: Exception) {
        Log.e(Const.TAG, "Error in getVersionSync", e)
        ""
    }

    /**
     * バージョン情報を保存する (PREFERENCE_KEY_VERSION に PREFERENCE_VAL_VERSION を格納)
     */
    suspend fun saveVersion(version: String = Const.PREFERENCE_VAL_VERSION) {
        try {
            dataStore.edit { preferences ->
                preferences[PREFERENCE_KEY_VERSION] = version
            }
        } catch (e: Exception) {
            Log.e(Const.TAG, "Error saving version to DataStore", e)
        }
    }

    /**
     * ロックモードを取得する Flow（デフォルト値: CALL_DIRECT）
     */
    val lockModeFlow: Flow<Int> = safeDataFlow.map { preferences ->
        preferences[PREFERENCE_KEY_LOCK_MODE] ?: LockMode.CALL_DIRECT.rawValue
    }

    /**
     * ロックモード Enum を取得する Flow
     */
    val lockModeEnumFlow: Flow<LockMode> = lockModeFlow.map { LockMode.fromInt(it) }

    /**
     * ロックモードを同期読み出しする（デフォルト値: CALL_DIRECT）
     */
    fun getLockModeSync(): Int = try {
        runBlocking {
            lockModeFlow.first()
        }
    } catch (e: Exception) {
        Log.e(Const.TAG, "Error in getLockModeSync", e)
        LockMode.CALL_DIRECT.rawValue
    }

    /**
     * ロックモード Enum を同期読み出しする
     */
    fun getLockModeEnumSync(): LockMode = LockMode.fromInt(getLockModeSync())

    /**
     * ロックモードを保存する
     */
    suspend fun saveLockMode(mode: Int) {
        try {
            dataStore.edit { preferences ->
                preferences[PREFERENCE_KEY_LOCK_MODE] = mode
            }
        } catch (e: Exception) {
            Log.e(Const.TAG, "Error saving lockMode to DataStore", e)
        }
    }

    /**
     * ロックモード Enum を保存する
     */
    suspend fun saveLockMode(mode: LockMode) {
        saveLockMode(mode.rawValue)
    }

    /**
     * 常駐監視サービスの設定を取得する Flow（デフォルト値: false）
     */
    val keepAliveFlow: Flow<Boolean> = safeDataFlow.map { preferences ->
        preferences[PREFERENCE_KEY_KEEP_ALIVE] ?: false
    }

    /**
     * 常駐監視サービスの設定を同期読み出しする（デフォルト値: false）
     */
    fun isKeepAliveSync(): Boolean = try {
        runBlocking {
            keepAliveFlow.first()
        }
    } catch (e: Exception) {
        Log.e(Const.TAG, "Error in isKeepAliveSync", e)
        false
    }

    /**
     * 常駐監視サービスの設定を保存する
     */
    suspend fun saveKeepAlive(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PREFERENCE_KEY_KEEP_ALIVE] = enabled
            }
        } catch (e: Exception) {
            Log.e(Const.TAG, "Error saving keepAlive to DataStore", e)
        }
    }

    /**
     * ダイアログの枠外ダブルタップで閉じる Flow（デフォルト値: true）
     */
    val outsideDismissFlow: Flow<Boolean> = safeDataFlow.map { preferences ->
        preferences[PREFERENCE_KEY_OUTSIDE_DISMISS] ?: true
    }

    /**
     * ダイアログの枠外ダブルタップで閉じる（デフォルト値: true）
     */
    fun isOutsideDismissSync(): Boolean = try {
        runBlocking {
            outsideDismissFlow.first()
        }
    } catch (e: Exception) {
        Log.e(Const.TAG, "Error in isOutsideDismissSync", e)
        true
    }

    /**
     * ダイアログの枠外ダブルタップで閉じるを保存する
     */
    suspend fun saveOutsideDismiss(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PREFERENCE_KEY_OUTSIDE_DISMISS] = enabled
            }
        } catch (e: Exception) {
            Log.e(Const.TAG, "Error saving outsideDismiss to DataStore", e)
        }
    }
}

// -------------------------------------------------------------------
// コンテキストからアクセスするための拡張関数宣言.
fun Context.getAppPreference(): AppPreference = AppPreference(this)

// インスタンスの重複生成を防ぐためのプライベートレジストリ.
private object DeDataStoreRegistry {
    @Volatile
    private var instance: DataStore<Preferences>? = null

    fun getInstance(context: Context): DataStore<Preferences> = instance ?: synchronized(this) {
        val appContext = context.applicationContext ?: context
        val deContext = if (appContext.isDeviceProtectedStorage) {
            appContext
        } else {
            appContext.createDeviceProtectedStorageContext()
        }
        instance ?: PreferenceDataStoreFactory.create(
            produceFile = {
                // deviceProtectedContext.preferencesDataStoreFile(DATASTORE_NAME)　は、DEに対して使えない.
                // Android Jetpack DataStore の Context.preferencesDataStoreFile の実装は、
                // 内部で部でapplicationContext を参照しているので、CE領域にパスが変わって mkdir() が 126で失敗する.
                //
                // public fun Context.dataStoreFile(fileName: String): File {
                //    return File(applicationContext.filesDir, "datastore/$fileName")
                // }
                File(deContext.filesDir, "datastore/${AppPreference.DATASTORE_NAME}.preferences_pb")
            },
        ).also { instance = it }
    }
}
