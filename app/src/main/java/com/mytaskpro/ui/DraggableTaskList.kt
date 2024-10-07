package com.mytaskpro.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.mytaskpro.data.Task

@Composable
fun DraggableTaskList(
    tasks: List<Task>,
    onTaskMove: (Int, Int) -> Unit,
    taskContent: @Composable (Task) -> Unit
) {
    var draggedItem by remember { mutableStateOf<Task?>(null) }
    var draggedOver by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                val y = change.position.y
                draggedOver = (y / 100f).toInt().coerceIn(tasks.indices)
            }
        }
    ) {
        items(tasks, key = { it.id }) { task ->
            taskContent(task)
        }
    }

    LaunchedEffect(draggedOver) {
        if (draggedItem != null && draggedOver != null) {
            val fromIndex = tasks.indexOf(draggedItem)
            val toIndex = draggedOver!!
            if (fromIndex != -1 && fromIndex != toIndex) {
                onTaskMove(fromIndex, toIndex)
                draggedItem = null
                draggedOver = null
            }
        }
    }
}