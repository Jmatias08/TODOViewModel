package com.example.todoviewmodel.ui

import com.example.todoviewmodel.data.TodoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var viewModel: TodoViewModel

  // Create the ViewModel in @Before so it is built after MainDispatcherRule has replaced Dispatchers.Main.
  @Before
  fun setUp() {
    viewModel = TodoViewModel(TodoRepository())
  }

  // uiState uses WhileSubscribed, so it only updates while someone is collecting it.
  private fun TestScope.collectUiState() {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
  }

  @Test
  fun initialState_isEmpty() = runTest {
    collectUiState()

    assertTrue(viewModel.uiState.value.isEmpty)
  }

  @Test
  fun addTodo_addsTodoToTheList() = runTest {
    collectUiState()

    viewModel.addTodo("Buy milk")

    val todos = viewModel.uiState.value.todos
    assertEquals(1, todos.size)
    assertEquals("Buy milk", todos.first().title)
  }

  @Test
  fun addTodo_withBlankTitle_isIgnored() = runTest {
    collectUiState()

    viewModel.addTodo("   ")

    assertTrue(viewModel.uiState.value.isEmpty)
  }

  @Test
  fun toggleTodo_updatesRemainingCount() = runTest {
    collectUiState()
    viewModel.addTodo("Buy milk")
    viewModel.addTodo("Walk the dog")

    viewModel.toggleTodo(viewModel.uiState.value.todos.first().id)

    assertEquals(1, viewModel.uiState.value.remainingCount)
  }

  @Test
  fun deleteTodo_removesTodoFromTheList() = runTest {
    collectUiState()
    viewModel.addTodo("Buy milk")

    viewModel.deleteTodo(viewModel.uiState.value.todos.first().id)

    assertTrue(viewModel.uiState.value.isEmpty)
  }

  @Test
  fun addTodo_trimsTitle() = runTest {
    collectUiState()

    viewModel.addTodo("  Study ViewModel  ")

    assertEquals("Study ViewModel", viewModel.uiState.value.todos.single().title)
  }

  @Test
  fun toggleTodo_twice_restoresActiveTask() = runTest {
    collectUiState()
    viewModel.addTodo("Buy milk")
    val id = viewModel.uiState.value.todos.single().id

    viewModel.toggleTodo(id)
    assertTrue(viewModel.uiState.value.todos.single().isDone)
    assertEquals(0, viewModel.uiState.value.remainingCount)
    viewModel.toggleTodo(id)

    assertEquals(1, viewModel.uiState.value.remainingCount)
    assertTrue(!viewModel.uiState.value.todos.single().isDone)
  }

  @Test
  fun deleteTodo_preservesOtherTasksAndDoesNotReuseIds() = runTest {
    collectUiState()
    viewModel.addTodo("Buy milk")
    viewModel.addTodo("Walk the dog")
    val original = viewModel.uiState.value.todos

    viewModel.deleteTodo(original.first().id)

    assertEquals(listOf(original.last()), viewModel.uiState.value.todos)
    assertEquals(1, viewModel.uiState.value.remainingCount)

    viewModel.addTodo("Study ViewModel")
    val todos = viewModel.uiState.value.todos
    assertEquals(todos.size, todos.map { it.id }.distinct().size)
    assertTrue(todos.last().id != original.first().id)
  }

  @Test
  fun events_withUnknownId_leaveTasksUnchanged() = runTest {
    collectUiState()
    viewModel.addTodo("Buy milk")
    val original = viewModel.uiState.value

    viewModel.toggleTodo(Long.MAX_VALUE)
    viewModel.deleteTodo(Long.MAX_VALUE)

    assertEquals(original, viewModel.uiState.value)
  }
}
