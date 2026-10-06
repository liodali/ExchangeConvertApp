package dali.hamza.shared.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Long-press drag-to-reorder column (KMP, no external dependency).
 *
 * Items swap once the dragged card crosses half of the neighbouring card
 * (+ [spacing]); the dragged card stays under the finger, lifts with a
 * shadow/scale and settles with a short animation on release. Reorders are
 * reported through [onMove] as slot swaps — persist them with the caller's
 * own state (e.g. the market-overview preferences).
 *
 * Gesture/node model (the subtle part):
 * - Each slot owns a stable pointerInput node (slots are positional, nodes
 *   are NOT keyed to items). After a swap the ITEM in a slot changes, so a
 *   key captured in the node's block would be stale — that is exactly the
 *   "grabs the previous item" bug. The item is therefore resolved from the
 *   LIVE [items] list at drag START, and the whole drag is keyed by that
 *   resolved value.
 * - The drag is tracked by item key, not index: the gesture coroutine
 *   outlives recompositions, so an index captured at drag start would go
 *   stale the moment a swap reorders [items] mid-drag.
 *
 * Used by the Home dashboard cards, the onboarding market slots and the
 * Account → Market Preferences rows.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (item: T) -> Any,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    spacing: Dp = 12.dp,
    itemContent: @Composable ColumnScope.(item: T, dragging: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val spacingPx = with(density) { spacing.toPx() }

    // Live references for the gesture coroutine: plain parameter captures
    // inside pointerInput would go stale mid-drag (the block is remembered,
    // not restarted on every recomposition).
    val currentItems by rememberUpdatedState(items)
    val currentKey by rememberUpdatedState(key)
    val currentOnMove by rememberUpdatedState(onMove)

    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<Int, Int>() }

    // settle animation job — cancelled when a new drag picks up, otherwise
    // it fights the new gesture for dragOffset
    val settleHolder = remember { SettleHolder() }

    fun heightOf(index: Int): Float = heights[index]?.toFloat() ?: 0f

    /** Room the dragged card has above its slot (for offset clamping). */
    fun spaceAbove(index: Int): Float =
        (0 until index).sumOf { heightOf(it).toDouble() }.toFloat() + index * spacingPx

    /** Room the dragged card has below its slot. */
    fun spaceBelow(index: Int, count: Int): Float =
        ((index + 1) until count).sumOf { heightOf(it).toDouble() }.toFloat() +
            (count - 1 - index) * spacingPx

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        items.forEachIndexed { index, item ->
            val itemKey = key(item)
            Box(
                modifier = Modifier
                    .onGloballyPositioned { heights[index] = it.size.height }
                    .zIndex(if (draggingKey == itemKey) 1f else 0f)
                    .graphicsLayer {
                        val active = draggingKey == itemKey
                        translationY = if (active) dragOffset else 0f
                        val scale = if (active) 1.02f else 1f
                        scaleX = scale
                        scaleY = scale
                        shadowElevation = if (active) 24f else 0f
                    }
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        // the item under this SLOT at the moment the gesture
                        // runs — resolved per gesture from live state, never
                        // captured across recompositions
                        var activeKey: Any? = null
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                settleHolder.job?.cancel()
                                activeKey = currentItems.getOrNull(index)
                                    ?.let { currentKey(it) }
                                draggingKey = activeKey
                                dragOffset = 0f
                                if (activeKey != null) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                if (draggingKey != activeKey || activeKey == null) {
                                    return@detectDragGesturesAfterLongPress
                                }
                                dragOffset += amount.y

                                val count = currentItems.size
                                var slot = currentItems.indexOfFirst { currentKey(it) == activeKey }
                                if (slot < 0) {
                                    return@detectDragGesturesAfterLongPress
                                }
                                // consecutive swaps for fast drags; bounded so a
                                // missing height can never spin the loop
                                var guard = 0
                                while (guard++ < count) {
                                    if (dragOffset < 0f && slot > 0) {
                                        val neighbour = heightOf(slot - 1)
                                        if (dragOffset < -(neighbour + spacingPx) / 2f) {
                                            dragOffset += neighbour + spacingPx
                                            currentOnMove(slot, slot - 1)
                                            slot -= 1
                                            continue
                                        }
                                    }
                                    if (dragOffset > 0f && slot < count - 1) {
                                        val neighbour = heightOf(slot + 1)
                                        if (dragOffset > (neighbour + spacingPx) / 2f) {
                                            dragOffset -= neighbour + spacingPx
                                            currentOnMove(slot, slot + 1)
                                            slot += 1
                                            continue
                                        }
                                    }
                                    break
                                }
                                // never let the card leave the list bounds
                                dragOffset = dragOffset.coerceIn(
                                    -spaceAbove(slot),
                                    spaceBelow(slot, count),
                                )
                            },
                            onDragEnd = {
                                if (draggingKey == activeKey && activeKey != null) {
                                    val start = dragOffset
                                    settleHolder.job = scope.launch {
                                        animate(start, 0f, animationSpec = tween(150)) { value, _ ->
                                            dragOffset = value
                                        }
                                        if (draggingKey == activeKey) draggingKey = null
                                    }
                                }
                            },
                            onDragCancel = {
                                if (draggingKey == activeKey) {
                                    draggingKey = null
                                    dragOffset = 0f
                                }
                            },
                        )
                    },
            ) {
                with(this@Column) {
                    itemContent(item, draggingKey == itemKey)
                }
            }
        }
    }
}

/** Holds the running settle-animation job so a new drag can cancel it. */
private class SettleHolder {
    var job: Job? = null
}
