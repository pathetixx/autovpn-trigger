package pw.x4.autovpn.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trigger_apps")
data class TriggerAppEntity(
    @PrimaryKey val packageName: String,
    val addedAt: Long = System.currentTimeMillis(),
)
