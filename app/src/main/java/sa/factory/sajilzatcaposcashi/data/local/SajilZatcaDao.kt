package sa.factory.sajilzatcaposcashi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SajilZatcaDao {
    @Query("SELECT * FROM pos_cashier_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<SajilZatcaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SajilZatcaEntity>)

    @Query("DELETE FROM pos_cashier_items_cache")
    suspend fun clearAll()
}
