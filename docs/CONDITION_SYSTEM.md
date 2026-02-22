# Condition System

This document covers the implementation of the entire condition architecture. It is the core of the application — everything else depends on it. Read this fully before writing any code.

---

## Design Pattern — Boolean Expression Tree

This system is a **Boolean Expression Tree** — a well-established pattern in computer science used in compilers, calculators, and query engines. It is the boolean equivalent of an arithmetic expression tree where `+`, `-`, `*`, `/` are operators and numbers are operands.

| Arithmetic Expression Tree | This System |
|---|---|
| Internal node | `CompositeCondition` |
| Operator (`+`, `*`, ...) | `LogicalOperator` (AND, OR) |
| Leaf node | `LeafCondition` |
| Operand (number/variable) | Concrete condition (Weather, Battery...) |
| `evaluate(): Int` | `getCondition(): Boolean` |

Just as `(3 + 5) * (2 - 1)` forms a tree where `*` is the root with `+` and `-` as children, a condition like `(weatherClear AND batteryOk) OR locationIsHome` forms a tree where `OR` is the root with two `AND` composites as children.

**The key insight:** adding a new condition type (a new `ConcreteLeafCondition`) never requires any changes to `CompositeCondition` or the evaluation logic — exactly like adding a new number to an arithmetic expression tree doesn't change how `+` works.

---

## Overview

Conditions form a recursive tree. Every node in the tree implements the `Condition` interface. There are two kinds of nodes:

- **Leaf nodes** — terminal nodes that evaluate a single real-world check (battery level, weather, custom AI prompt, etc.)
- **Composite nodes** — internal nodes that combine children with AND / OR logic

The tree is evaluated by calling `getCondition()` on the root. It recursively resolves down to each leaf, which performs the actual check and returns a Boolean. The result bubbles back up through the composites.

```
CompositeCondition [AND]
├── BatteryCondition       (battery > 20%)         → true
├── NetworkCondition       (wifi == connected)      → true
└── CompositeCondition [OR]
    ├── WeatherCondition   (temp >= 70°F)           → false
    └── CustomCondition    ("is it sunny outside?") → true
                                                    → true
Final result: true AND true AND (false OR true) = true
```

---

## Interface

```kotlin
sealed interface Condition {
    val id: String
    val label: String           // human-readable label shown in UI tile
    suspend fun getCondition(): Boolean
}
```

`label` is what gets rendered in the `ConditionTile` in the UI. Each concrete class should produce a meaningful default, e.g. `"Battery > 20%"` or `"Weather: temp >= 70°F"`.

---

## Package Structure

```
domain/condition/
├── Condition.kt                    # sealed interface
├── CompositeCondition.kt           # AND / OR composite
├── leaf/
│   ├── LeafCondition.kt            # abstract base
│   ├── system/
│   │   ├── SystemLeafCondition.kt  # abstract base for Android system APIs
│   │   ├── BatteryCondition.kt
│   │   ├── LocationCondition.kt
│   │   ├── NetworkCondition.kt
│   │   ├── CalendarCondition.kt
│   │   └── TimeCondition.kt
│   ├── api/
│   │   ├── ApiLeafCondition.kt     # abstract base for HTTP-based conditions
│   │   └── WeatherCondition.kt
│   └── custom/
│       └── CustomCondition.kt      # Gemini + Google Search
└── Operator.kt
```

---

## Operator

```kotlin
enum class Operator {
    EQUALS,
    NOT_EQUALS,
    GT,
    LT,
    GTE,
    LTE,
    CONTAINS,
    NOT_CONTAINS
}
```

Not every `LeafCondition` uses `Operator` — `CustomCondition` does not. But all structured leaf conditions do.

---

## Serialization Note

The condition tree is serialized to JSON for Room persistence using Moshi with a `PolymorphicJsonAdapterFactory`. Every concrete class must have a unique `type` string used as the discriminator. See [DATA_MODEL.md](./DATA_MODEL.md) for the Moshi setup. The `type` values are:

| Class | type discriminator |
|---|---|
| `CompositeCondition` | `"COMPOSITE"` |
| `BatteryCondition` | `"BATTERY"` |
| `NetworkCondition` | `"NETWORK"` |
| `LocationCondition` | `"LOCATION"` |
| `CalendarCondition` | `"CALENDAR"` |
| `TimeCondition` | `"TIME"` |
| `WeatherCondition` | `"WEATHER"` |
| `CustomCondition` | `"CUSTOM"` |
