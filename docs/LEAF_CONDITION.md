# LeafCondition

Leaf conditions are the terminal nodes of the condition tree. They perform the actual real-world checks. Each one knows how to evaluate itself and provide its own Compose UI for the condition builder.

---

## Abstract Base — LeafCondition

```kotlin
abstract class LeafCondition : Condition {
    override val id: String = UUID.randomUUID().toString()

    // Each subclass provides its own Compose UI for the condition builder tile.
    // The composable must call [onConditionReady] when the user has finished
    // configuring the condition. The passed LeafCondition is the fully configured
    // instance ready to be added to the reminder.
    @Composable
    abstract fun BuildUI(onConditionReady: (LeafCondition) -> Unit)
}
```

---

## SystemLeafCondition — Android OS-based conditions

For conditions that read from Android system services. No network calls.

```kotlin
abstract class SystemLeafCondition(
    @ApplicationContext protected val context: Context
) : LeafCondition()
```

All system leaf conditions take `Context` as a constructor parameter, injected via Hilt.

### Shared comparison helper

The base class provides a `compare()` helper used by all structured system conditions:

```kotlin
protected fun compare(actual: Any, operator: Operator, expected: String): Boolean {
    return when (operator) {
        Operator.EQUALS       -> actual.toString().equals(expected, ignoreCase = true)
        Operator.NOT_EQUALS   -> !actual.toString().equals(expected, ignoreCase = true)
        Operator.CONTAINS     -> actual.toString().contains(expected, ignoreCase = true)
        Operator.NOT_CONTAINS -> !actual.toString().contains(expected, ignoreCase = true)
        Operator.GT           -> actual.toString().toDoubleOrNull()!! > expected.toDouble()
        Operator.LT           -> actual.toString().toDoubleOrNull()!! < expected.toDouble()
        Operator.GTE          -> actual.toString().toDoubleOrNull()!! >= expected.toDouble()
        Operator.LTE          -> actual.toString().toDoubleOrNull()!! <= expected.toDouble()
    }
}
```

---

## Concrete System Conditions

### BatteryCondition

Checks the device battery level.

```kotlin
@JsonClass(generateAdapter = true)
data class BatteryCondition(
    override val id: String = UUID.randomUUID().toString(),
    val operator: Operator,
    val threshold: Int,           // percentage 0–100
    @ApplicationContext val context: Context
) : SystemLeafCondition(context) {

    override val label: String
        get() = "Battery ${operator.symbol} $threshold%"

    override suspend fun getCondition(): Boolean {
        val bm = context.getSystemService(BatteryManager::class.java)
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return compare(level, operator, threshold.toString())
    }

    @Composable
    override fun BuildUI(onConditionReady: (LeafCondition) -> Unit) {
        // UI: operator dropdown (GT, LT, GTE, LTE, EQUALS)
        //     threshold slider or number input (0–100)
        //     "Add" button → calls onConditionReady(copy(operator=..., threshold=...))
    }
}
```

**Permissions required:** none — `BatteryManager` is available without permissions.

---

### NetworkCondition

Checks the current network connectivity state.

```kotlin
@JsonClass(generateAdapter = true)
data class NetworkCondition(
    override val id: String = UUID.randomUUID().toString(),
    val expectedState: NetworkState,
    @ApplicationContext val context: Context
) : SystemLeafCondition(context) {

    enum class NetworkState { CONNECTED, DISCONNECTED, WIFI, MOBILE_DATA }

    override val label: String
        get() = "Network is ${expectedState.name.lowercase()}"

    override suspend fun getCondition(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return expectedState == NetworkState.DISCONNECTED
        val caps = cm.getNetworkCapabilities(network) ?: return false

        return when (expectedState) {
            NetworkState.CONNECTED    -> true
            NetworkState.DISCONNECTED -> false
            NetworkState.WIFI         -> caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            NetworkState.MOBILE_DATA  -> caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }
    }

    @Composable
    override fun BuildUI(onConditionReady: (LeafCondition) -> Unit) {
        // UI: dropdown with NetworkState options
        //     "Add" button → onConditionReady(copy(expectedState=...))
    }
}
```

**Permissions required:** `ACCESS_NETWORK_STATE`

---

### LocationCondition

Checks whether the device is within a radius of a given location.

```kotlin
@JsonClass(generateAdapter = true)
data class LocationCondition(
    override val id: String = UUID.randomUUID().toString(),
    val targetLat: Double,
    val targetLng: Double,
    val radiusMeters: Float,
    val mode: LocationMode,         // INSIDE or OUTSIDE the radius
    @ApplicationContext val context: Context
) : SystemLeafCondition(context) {

    enum class LocationMode { INSIDE, OUTSIDE }

    override val label: String
        get() = "Location ${mode.name.lowercase()} ${radiusMeters.toInt()}m radius"

    override suspend fun getCondition(): Boolean {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = suspendCancellableCoroutine<Location?> { cont ->
            client.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        } ?: return false

        val results = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, targetLat, targetLng, results)
        val distanceMeters = results[0]

        return when (mode) {
            LocationMode.INSIDE  -> distanceMeters <= radiusMeters
            LocationMode.OUTSIDE -> distanceMeters > radiusMeters
        }
    }

    @Composable
    override fun BuildUI(onConditionReady: (LeafCondition) -> Unit) {
        // UI: map picker or lat/lng text inputs
        //     radius slider (e.g. 100m – 50km)
        //     INSIDE / OUTSIDE toggle
        //     "Add" button → onConditionReady(copy(...))
    }
}
```

**Permissions required:** `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`

---

### CalendarCondition

Checks whether the user has an event in their calendar within a time window.

```kotlin
@JsonClass(generateAdapter = true)
data class CalendarCondition(
    override val id: String = UUID.randomUUID().toString(),
    val mode: CalendarMode,
    val windowMinutes: Int = 60       // how far ahead to look
) : SystemLeafCondition(context) {

    enum class CalendarMode { HAS_EVENT, IS_FREE }

    override val label: String
        get() = "Calendar: ${mode.name.replace('_', ' ').lowercase()} in next ${windowMinutes}min"

    override suspend fun getCondition(): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val windowEnd = now + (windowMinutes * 60 * 1000L)

        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(CalendarContract.Events._ID)
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val args = arrayOf(now.toString(), windowEnd.toString())

        val cursor = context.contentResolver.query(uri, projection, selection, args, null)
        val hasEvent = (cursor?.count ?: 0) > 0
        cursor?.close()

        return@withContext when (mode) {
            CalendarMode.HAS_EVENT -> hasEvent
            CalendarMode.IS_FREE   -> !hasEvent
        }
    }

    @Composable
    override fun BuildUI(onConditionReady: (LeafCondition) -> Unit) {
        // UI: HAS_EVENT / IS_FREE toggle
        //     window slider (15min, 30min, 1hr, 2hr, 4hr)
        //     "Add" button → onConditionReady(copy(...))
    }
}
```

**Permissions required:** `READ_CALENDAR`

---

### TimeCondition

Checks whether the current time is within a given range or matches a day of week.

```kotlin
@JsonClass(generateAdapter = true)
data class TimeCondition(
    override val id: String = UUID.randomUUID().toString(),
    val mode: TimeMode,
    val fromHour: Int = 0,
    val fromMinute: Int = 0,
    val toHour: Int = 23,
    val toMinute: Int = 59,
    val daysOfWeek: List<Int> = listOf(1,2,3,4,5,6,7)  // 1=Mon … 7=Sun
) : SystemLeafCondition(context) {

    enum class TimeMode { IN_RANGE, OUT_OF_RANGE, DAY_OF_WEEK }

    override val label: String
        get() = when (mode) {
            TimeMode.IN_RANGE     -> "Time between $fromHour:${fromMinute.toString().padStart(2,'0')} – $toHour:${toMinute.toString().padStart(2,'0')}"
            TimeMode.OUT_OF_RANGE -> "Time outside $fromHour:${fromMinute.toString().padStart(2,'0')} – $toHour:${toMinute.toString().padStart(2,'0')}"
            TimeMode.DAY_OF_WEEK  -> "Day of week in ${daysOfWeek}"
        }

    override suspend fun getCondition(): Boolean {
        val cal = Calendar.getInstance()
        return when (mode) {
            TimeMode.IN_RANGE, TimeMode.OUT_OF_RANGE -> {
                val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                val fromMinutes = fromHour * 60 + fromMinute
                val toMinutes = toHour * 60 + toMinute
                val inRange = currentMinutes in fromMinutes..toMinutes
                if (mode == TimeMode.IN_RANGE) inRange else !inRange
            }
            TimeMode.DAY_OF_WEEK -> {
                val today = cal.get(Calendar.DAY_OF_WEEK)  // 1=Sun in Java Calendar
                val adjusted = if (today == 1) 7 else today - 1   // convert to 1=Mon
                adjusted in daysOfWeek
            }
        }
    }

    @Composable
    override fun BuildUI(onConditionReady: (LeafCondition) -> Unit) {
        // UI: mode selector
        //     time range pickers (from / to)
        //     day-of-week checkboxes
        //     "Add" button
    }
}
```

**Permissions required:** none

---

## ApiLeafCondition — HTTP-based conditions

For conditions that require a network call to an external API.

```kotlin
abstract class ApiLeafCondition(
    protected val httpClient: HttpClient
) : LeafCondition() {

    protected fun compare(actual: Any, operator: Operator, expected: String): Boolean {
        // same implementation as SystemLeafCondition.compare()
    }

    protected fun resolveField(data: Map<String, Any>, path: String): Any? {
        val parts = path.split(".")
        var current: Any? = data
        for (part in parts) {
            current = (current as? Map<*, *>)?.get(part) ?: return null
        }
        return current
    }

    // Subclasses implement this to fetch and return parsed API response as Map
    abstract suspend fun fetchData(): Map<String, Any>

    // Default getCondition() — subclasses can override if they need custom logic
    override suspend fun getCondition(): Boolean {
        return try {
            val data = fetchData()
            evaluate(data)
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "fetchData failed: ${e.message}")
            false   // network failure = condition not met, never fires erroneously
        }
    }

    // Subclasses implement this to apply their field/operator/value logic
    protected abstract fun evaluate(data: Map<String, Any>): Boolean
}
```

---

### WeatherCondition

Checks weather data from Open-Meteo (free, no API key required).

```kotlin
@JsonClass(generateAdapter = true)
data class WeatherCondition(
    override val id: String = UUID.randomUUID().toString(),
    val field: WeatherField,
    val operator: Operator,
    val value: String,
    val latitude: Double,
    val longitude: Double,
    val httpClient: HttpClient
) : ApiLeafCondition(httpClient) {

    enum class WeatherField(val path: String, val displayName: String) {
        TEMPERATURE("current.temperature_2m", "Temperature (°C)"),
        RAIN("current.rain", "Rain (mm)"),
        WIND_SPEED("current.wind_speed_10m", "Wind Speed (km/h)"),
        HUMIDITY("current.relative_humidity_2m", "Humidity (%)"),
        WEATHER_CODE("current.weather_code", "Weather Code")
    }

    override val label: String
        get() = "Weather: ${field.displayName} ${operator.symbol} $value"

    override suspend fun fetchData(): Map<String, Any> {
        val response: String = httpClient.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("current", "temperature_2m,rain,wind_speed_10m,relative_humidity_2m,weather_code")
        }.bodyAsText()
        return Json.decodeFromString(response)
    }

    override fun evaluate(data: Map<String, Any>): Boolean {
        val actual = resolveField(data, field.path) ?: return false
        return compare(actual, operator, value)
    }

    @Composable
    override fun BuildUI(onConditionReady: (LeafCondition) -> Unit) {
        // UI: WeatherField dropdown
        //     Operator dropdown (valid operators per field type)
        //     Value input (number for most fields)
        //     Location — use device GPS or let user type city
        //     "Add" button → onConditionReady(copy(...))
    }
}
```

**Permissions required:** `INTERNET`, `ACCESS_FINE_LOCATION` (for auto-detecting coordinates)

**Note:** Open-Meteo requires no API key and is free for non-commercial use. Docs: https://open-meteo.com/en/docs

---

## Adding a New LeafCondition

Follow these steps to add a new condition type:

1. Decide which base class to extend:
   - Uses Android system service → extend `SystemLeafCondition`
   - Makes an HTTP call → extend `ApiLeafCondition`
   - Neither (e.g. a local file check) → extend `LeafCondition` directly

2. Implement the required members: `id`, `label`, `getCondition()`, `BuildUI()`

3. Add a `type` discriminator string to the Moshi `PolymorphicJsonAdapterFactory` in `ConditionTypeConverter`

4. Add a `@Provides` method in the Hilt module if the class needs injected dependencies

5. Register the new type in `ConditionTypeRegistry` (a simple list used to populate the UI dropdown of available condition types)

```kotlin
// ConditionTypeRegistry.kt
object ConditionTypeRegistry {
    val availableTypes: List<ConditionTypeInfo> = listOf(
        ConditionTypeInfo("battery",  "Battery",        "Device battery level"),
        ConditionTypeInfo("network",  "Network",        "Wi-Fi or mobile data state"),
        ConditionTypeInfo("location", "Location",       "Device location"),
        ConditionTypeInfo("calendar", "Calendar",       "Calendar events"),
        ConditionTypeInfo("time",     "Time",           "Time of day or day of week"),
        ConditionTypeInfo("weather",  "Weather",        "Current weather conditions"),
        ConditionTypeInfo("custom",   "Custom (AI)",    "Describe any condition in plain text")
    )
}

data class ConditionTypeInfo(
    val typeKey: String,
    val displayName: String,
    val description: String
)
```
