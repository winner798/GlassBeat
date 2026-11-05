package io.github.glassbeat.data

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.themeData by preferencesDataStore(name = "appearance", produceMigrations = { listOf(SharedPreferencesMigration(it, "theme")) })
data class ThemeSettings(
    val light: Boolean = false, val accent: Long = 0xFFB8A4FF,
    val opacity: Float = .48f, val blur: Float = 18f, val motion: Boolean = true,
    val lowPower: Boolean = false, val background: String = "", val font: String = ""
)
class ThemeStore(private val context: Context) {
    private object Keys {
        val light = booleanPreferencesKey("light"); val accent = longPreferencesKey("accent")
        val opacity = floatPreferencesKey("opacity"); val blur = floatPreferencesKey("blur")
        val motion = booleanPreferencesKey("motion"); val lowPower = booleanPreferencesKey("lowPower")
        val background = stringPreferencesKey("background"); val font = stringPreferencesKey("font")
    }
    val settings = context.themeData.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { p ->
        ThemeSettings(p[Keys.light] ?: false, p[Keys.accent] ?: 0xFFB8A4FF,
            (p[Keys.opacity] ?: .48f).coerceIn(.15f, .95f), (p[Keys.blur] ?: 18f).coerceIn(0f, 40f),
            p[Keys.motion] ?: true, p[Keys.lowPower] ?: false, p[Keys.background] ?: "", p[Keys.font] ?: "")
    }
    suspend fun save(t: ThemeSettings) { context.themeData.edit {
        it[Keys.light] = t.light; it[Keys.accent] = t.accent; it[Keys.opacity] = t.opacity
        it[Keys.blur] = t.blur; it[Keys.motion] = t.motion; it[Keys.lowPower] = t.lowPower
        it[Keys.background] = t.background; it[Keys.font] = t.font
    } }
}
