package de.westnordost.streetcomplete.data.user

import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.content.contentValuesOf

import javax.inject.Inject

import de.westnordost.streetcomplete.data.user.QuestStatisticsTable.Columns.QUEST_TYPE
import de.westnordost.streetcomplete.data.user.QuestStatisticsTable.Columns.SUCCEEDED
import de.westnordost.streetcomplete.data.user.QuestStatisticsTable.NAME
import de.westnordost.streetcomplete.ktx.*

/** Stores how many quests of which quest types the user solved */
class QuestStatisticsDao @Inject constructor(private val dbHelper: SQLiteOpenHelper) {
    private val db get() = dbHelper.writableDatabase

    fun getNoteAmount(): Int {
        return getAmount(NOTE)
    }

    fun getTotalAmount(): Int {
        return db.queryOne(NAME, arrayOf("total($SUCCEEDED)")) { it.getInt(0) } ?: 0
    }

    fun getAll(): Map<String, Int> {
        return db.query(NAME) {
            it.getString(QUEST_TYPE) to it.getInt(SUCCEEDED)
        }.toMap()
    }

    fun replaceAll(amounts: Map<String, Int>) {
        db.transaction {
            db.delete(NAME, null, null)
            for ((key, value) in amounts) {
                db.insert(NAME, null, contentValuesOf(
                    QUEST_TYPE to key,
                    SUCCEEDED to value
                ))
            }
        }
    }

    fun addOneNote() {
        addOne(NOTE)
    }

    fun addOne(questType: String) {
        // first ensure the row exists
        db.insertWithOnConflict(NAME, null, contentValuesOf(
            QUEST_TYPE to questType,
            SUCCEEDED to 0
        ), CONFLICT_IGNORE)

        // then increase by one
        db.execSQL("UPDATE $NAME SET $SUCCEEDED = $SUCCEEDED + 1 WHERE $QUEST_TYPE = ?", arrayOf(questType))
    }

    fun getAmount(questType: String): Int {
        return db.queryOne(NAME, arrayOf(SUCCEEDED), "$QUEST_TYPE = ?", arrayOf(questType)) {
            it.getInt(0)
        } ?: 0
    }
}

private const val NOTE = "NOTE"
