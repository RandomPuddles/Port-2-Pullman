# CompositeCondition

`CompositeCondition` is the **internal node** of the Boolean Expression Tree. In classical expression trees, internal nodes hold the operator (`+`, `*`, etc.) and their children are the operands. Here, `CompositeCondition` holds the logical operator (AND / OR) and its children are `Condition` nodes — either other composites or leaf conditions.

This is directly analogous to an arithmetic expression tree node:

```kotlin
// Arithmetic expression tree internal node
class InternalNode(val op: Char, val left: Node, val right: Node) : Node {
    override fun evaluate(): Int = when(op) {
        '+' -> left.evaluate() + right.evaluate()
        '*' -> left.evaluate() * right.evaluate()
        else -> 0
    }
}

// Boolean expression tree internal node — same structure, boolean result
class CompositeCondition(val operator: LogicalOperator, val children: List<Condition>) : Condition {
    override suspend fun getCondition(): Boolean = when(operator) {
        AND -> children.all { it.getCondition() }
        OR  -> children.any { it.getCondition() }
    }
}
```

It holds a list of children rather than just left/right, making it an **n-ary** expression tree rather than strictly binary — this allows the user to AND/OR any number of conditions at the same level without forced nesting.

---

## Implementation

```kotlin
@JsonClass(generateAdapter = true)
data class CompositeCondition(
    override val id: String = UUID.randomUUID().toString(),
    val operator: LogicalOperator,
    val children: MutableList<Condition> = mutableListOf()
) : Condition {

    override val label: String
        get() = children.joinToString(" ${operator.name} ") { it.label }

    override suspend fun getCondition(): Boolean {
        if (children.isEmpty()) return false
        return when (operator) {
            LogicalOperator.AND -> children.all { it.getCondition() }
            LogicalOperator.OR  -> children.any { it.getCondition() }
        }
    }

    fun add(condition: Condition) {
        children.add(condition)
    }

    fun remove(condition: Condition) {
        children.remove(condition)
    }

    fun removeById(id: String) {
        children.removeAll { it.id == id }
    }
}

enum class LogicalOperator { AND, OR }
```

---

## Evaluation Behaviour

| Operator | Empty children | One child | Multiple children |
|---|---|---|---|
| AND | `false` | result of child | all children must be `true` |
| OR | `false` | result of child | at least one child must be `true` |

Empty children always returns `false` — a reminder with no configured conditions should never fire.

---

## Short-Circuit Evaluation

Kotlin's `all {}` and `any {}` both short-circuit, which matters for cost and performance. With AND, evaluation stops as soon as one child returns `false`. With OR, evaluation stops as soon as one child returns `true`. Order your children accordingly — put cheap system conditions before expensive API or custom AI conditions.

The `CustomCondition` checkbox feature (evaluate custom conditions only when all non-custom conditions pass) is implemented at the `Reminder` level, not here. See [CUSTOM_CONDITION.md](./CUSTOM_CONDITION.md) for details.

---

## Nesting Example

The tree your diagram shows:

```
OR
├── AND
│   ├── LeafCondition
│   └── AND
│       ├── LeafCondition
│       └── LeafCondition
└── AND
    ├── LeafCondition
    └── AND
        ├── LeafCondition
        └── LeafCondition
```

In Kotlin:

```kotlin
val root = CompositeCondition(
    operator = LogicalOperator.OR,
    children = mutableListOf(
        CompositeCondition(
            operator = LogicalOperator.AND,
            children = mutableListOf(
                WeatherCondition(...),
                CompositeCondition(
                    operator = LogicalOperator.AND,
                    children = mutableListOf(BatteryCondition(...), NetworkCondition(...))
                )
            )
        ),
        CompositeCondition(
            operator = LogicalOperator.AND,
            children = mutableListOf(
                LocationCondition(...),
                CompositeCondition(
                    operator = LogicalOperator.AND,
                    children = mutableListOf(CalendarCondition(...), CustomCondition(...))
                )
            )
        )
    )
)

root.getCondition()  // recursively evaluates the entire tree
```

---

## UI Representation

In the `AlarmOptionsScreen`, a `CompositeCondition` is rendered as a list of `ConditionTile`s with an AND/OR badge between them. The badge is tappable — tapping it toggles between AND and OR for that composite level.

When the user adds a second condition to a reminder that already has one, the two conditions are automatically wrapped in a new `CompositeCondition` with AND as the default operator.

```
┌──────────────────────────┐
│  Battery > 20%           │  ← ConditionTile (BatteryCondition)
└──────────────────────────┘
         [ AND ]               ← tappable, toggles to OR
┌──────────────────────────┐
│  Network: wifi           │  ← ConditionTile (NetworkCondition)
└──────────────────────────┘
         [ AND ]
┌──────────────────────────┐
│  Custom (AI) ✦           │  ← ConditionTile (CustomCondition)
│  "Is it a weekend?"      │
│  ☑ Check only when above │
│    conditions pass        │
└──────────────────────────┘
```

---

## Serialization

`CompositeCondition` serializes to JSON as:

```json
{
  "type": "COMPOSITE",
  "id": "cond-root",
  "operator": "AND",
  "children": [
    {
      "type": "BATTERY",
      "id": "cond-001",
      "operator": "GT",
      "threshold": 20
    },
    {
      "type": "NETWORK",
      "id": "cond-002",
      "expectedState": "WIFI"
    }
  ]
}
```

The `children` array is polymorphic — each element carries its own `type` discriminator so Moshi knows which class to deserialize into. This is handled by the `PolymorphicJsonAdapterFactory` setup in `ConditionTypeConverter`. See [DATA_MODEL.md](./DATA_MODEL.md).

---

## Building CompositeConditions from the UI

The `AlarmOptionsViewModel` is responsible for building and mutating the condition tree as the user adds, removes, or reorders conditions. The root condition of a `Reminder` is always a `CompositeCondition`, even if the user only adds one leaf — this keeps the tree structure consistent and simplifies serialization.

```kotlin
// In AlarmOptionsViewModel
private val _rootCondition = MutableStateFlow(
    CompositeCondition(operator = LogicalOperator.AND)
)

fun addCondition(condition: Condition) {
    _rootCondition.update { it.apply { add(condition) } }
}

fun removeCondition(id: String) {
    _rootCondition.update { it.apply { removeById(id) } }
}

fun toggleOperator() {
    _rootCondition.update { composite ->
        composite.copy(
            operator = if (composite.operator == LogicalOperator.AND)
                LogicalOperator.OR else LogicalOperator.AND
        )
    }
}
```
