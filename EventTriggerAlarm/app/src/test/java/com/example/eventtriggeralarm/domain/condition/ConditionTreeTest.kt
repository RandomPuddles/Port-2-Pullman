package com.example.eventtriggeralarm.domain.condition

import com.example.eventtriggeralarm.domain.condition.leaf.StubLeafCondition
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionTreeTest {

    @Test
    fun emptyComposite_returnsFalse() = runBlocking {
        val root = CompositeCondition(operator = LogicalOperator.AND)
        assertFalse(root.getCondition())
    }

    @Test
    fun singleLeaf_returnsLeafResult() = runBlocking {
        val root = CompositeCondition(operator = LogicalOperator.AND).apply {
            add(StubLeafCondition(label = "A", result = true))
        }
        assertTrue(root.getCondition())
    }

    @Test
    fun and_allTrue_returnsTrue() = runBlocking {
        val root = CompositeCondition(operator = LogicalOperator.AND).apply {
            add(StubLeafCondition(label = "A", result = true))
            add(StubLeafCondition(label = "B", result = true))
        }
        assertTrue(root.getCondition())
    }

    @Test
    fun and_oneFalse_returnsFalse() = runBlocking {
        val root = CompositeCondition(operator = LogicalOperator.AND).apply {
            add(StubLeafCondition(label = "A", result = true))
            add(StubLeafCondition(label = "B", result = false))
        }
        assertFalse(root.getCondition())
    }

    @Test
    fun or_anyTrue_returnsTrue() = runBlocking {
        val root = CompositeCondition(operator = LogicalOperator.OR).apply {
            add(StubLeafCondition(label = "A", result = false))
            add(StubLeafCondition(label = "B", result = true))
        }
        assertTrue(root.getCondition())
    }

    @Test
    fun or_allFalse_returnsFalse() = runBlocking {
        val root = CompositeCondition(operator = LogicalOperator.OR).apply {
            add(StubLeafCondition(label = "A", result = false))
            add(StubLeafCondition(label = "B", result = false))
        }
        assertFalse(root.getCondition())
    }

    @Test
    fun nestedTree_evaluatesCorrectly() = runBlocking {
        // (true AND false) OR (true AND true) = false OR true = true
        val root = CompositeCondition(operator = LogicalOperator.OR).apply {
            add(
                CompositeCondition(operator = LogicalOperator.AND).apply {
                    add(StubLeafCondition(label = "A", result = true))
                    add(StubLeafCondition(label = "B", result = false))
                }
            )
            add(
                CompositeCondition(operator = LogicalOperator.AND).apply {
                    add(StubLeafCondition(label = "C", result = true))
                    add(StubLeafCondition(label = "D", result = true))
                }
            )
        }
        assertTrue(root.getCondition())
    }
}
