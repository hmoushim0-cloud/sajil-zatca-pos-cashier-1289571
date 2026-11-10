package sa.factory.sajilzatcaposcashi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SajilZatcaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pos_cashierDao(): SajilZatcaDao
}
