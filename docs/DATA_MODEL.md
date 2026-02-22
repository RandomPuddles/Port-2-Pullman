# Data Model

## Domain Models

### Reminder

```kotlin
data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val conditionPrompt: String,           // plain language condition
    val schedule: Schedule,
    val notificationMethod: NotificationMethod,
    val triggerOnce: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

### Schedule

```kotlin
sealed class Schedule {
    data class Interval(
        val intervalMinutes: Long          // how often to check, e.g. every 30 minutes
    ) : Schedule()

    data class OneShot(
        val triggerAtMs: Long              // specific epoch timestamp to check once
    ) : Schedule()
}
```

### NotificationMethod

```kotlin
data class NotificationMethod(
    val voiceOutput: Boolean = true,       // TTS reads the reminder title aloud
    val ringing: Boolean = false           // phone rings
)
```

---

## Room Schema

### ReminderEntity

```kotlin
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val conditionPrompt: String,
    val scheduleType: String,              // "INTERVAL" or "ONE_SHOT"
    val scheduleValue: Long,               // intervalMinutes or triggerAtMs
    val voiceOutput: Boolean,
    val ringing: Boolean,
    val triggerOnce: Boolean,
    val isActive: Boolean,
    val createdAt: Long
)
```

No type converters needed. The condition is a plain string. The schedule is flattened to two columns.

### ReminderDao

```kotlin
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isActive = 1")
    suspend fun getActiveReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: String)

    @Delete
    suspend fun delete(reminder: ReminderEntity)
}
```

### AppDatabase

```kotlin
@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}
```

---

## ReminderRepository

Maps between `ReminderEntity` (Room) and `Reminder` (domain).

```kotlin
@Singleton
class ReminderRepository @Inject constructor(
    private val dao: ReminderDao
) {
    fun getAllReminders(): Flow<List<Reminder>> =
        dao.getAllReminders().map { list -> list.map { it.toDomain() } }

    suspend fun getActiveReminders(): List<Reminder> =
        dao.getActiveReminders().map { it.toDomain() }

    suspend fun getById(id: String): Reminder? =
        dao.getById(id)?.toDomain()

    suspend fun save(reminder: Reminder) =
        dao.insert(reminder.toEntity())

    suspend fun deactivate(id: String) =
        dao.deactivate(id)

    suspend fun delete(reminder: Reminder) =
        dao.delete(reminder.toEntity())
}

// Mapping extensions
fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    title = title,
    conditionPrompt = conditionPrompt,
    schedule = when (scheduleType) {
        "INTERVAL" -> Schedule.Interval(scheduleValue)
        "ONE_SHOT"  -> Schedule.OneShot(scheduleValue)
        else        -> Schedule.Interval(60L)
    },
    notificationMethod = NotificationMethod(voiceOutput, ringing),
    triggerOnce = triggerOnce,
    isActive = isActive,
    createdAt = createdAt
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    title = title,
    conditionPrompt = conditionPrompt,
    scheduleType = when (schedule) {
        is Schedule.Interval -> "INTERVAL"
        is Schedule.OneShot  -> "ONE_SHOT"
    },
    scheduleValue = when (schedule) {
        is Schedule.Interval -> schedule.intervalMinutes
        is Schedule.OneShot  -> schedule.triggerAtMs
    },
    voiceOutput = notificationMethod.voiceOutput,
    ringing = notificationMethod.ringing,
    triggerOnce = triggerOnce,
    isActive = isActive,
    createdAt = createdAt
)
```

---

## DataStore Preferences

```kotlin
object PreferencesKeys {
    val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    val DEFAULT_INTERVAL_MINUTES = longPreferencesKey("default_interval_minutes")  // default: 60
}
```
