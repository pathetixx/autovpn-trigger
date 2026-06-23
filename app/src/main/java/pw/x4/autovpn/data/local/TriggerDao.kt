package pw.x4.autovpn.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TriggerDao {

    @Query("SELECT packageName FROM trigger_apps")
    fun observePackages(): Flow<List<String>>

    /** EXISTS дешевле, чем выгрузка строки — вызывается часто из сервиса. */
    @Query("SELECT EXISTS(SELECT 1 FROM trigger_apps WHERE packageName = :packageName)")
    suspend fun exists(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TriggerAppEntity)

    @Query("DELETE FROM trigger_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
