package se.bernhauser.solitaire.game.pyramid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.bernhauser.solitaire.game.Rank
import se.bernhauser.solitaire.persistence.PyramidSession
import se.bernhauser.solitaire.persistence.UndoLimit
import se.bernhauser.solitaire.repository.GameSessionStore

class PyramidViewModel(private val repo: GameSessionStore<PyramidSession>) : ViewModel() {
  private val _session = MutableStateFlow<PyramidSession?>(null)

  /** Transient first pick of a pair; never persisted and never part of undo history. */
  private val _selected = MutableStateFlow<PyramidPick?>(null)
  val selected: StateFlow<PyramidPick?> = _selected

  val state: StateFlow<PyramidState?> = _session
    .map { it?.current }
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val canUndo: StateFlow<Boolean> = _session
    .map { it?.history?.isNotEmpty() == true }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val isWon: StateFlow<Boolean> = _session
    .map { it?.current?.isWon() == true }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val gameOver: StateFlow<Boolean> = _session
    .map { it?.current?.isStuck() == true }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  init {
    viewModelScope.launch {
      _session.value = repo.load() ?: PyramidSession(current = dealNewPyramid(System.nanoTime()))
    }
  }

  fun newGame() {
    _selected.value = null
    replace(PyramidSession(current = dealNewPyramid(System.nanoTime())))
  }

  fun undo() {
    val s = _session.value ?: return
    val prev = s.history.firstOrNull() ?: return
    _selected.value = null
    replace(s.copy(current = prev, history = s.history.drop(1)))
  }

  fun onPick(pick: PyramidPick) {
    val current = _session.value?.current ?: return
    if (!current.isAvailable(pick)) return
    if (current.cardAt(pick)?.rank == Rank.King) {
      _selected.value = null
      act { it.removeKing(pick) ?: it }
      return
    }
    val first = _selected.value
    when {
      first == null -> _selected.value = pick
      first == pick -> _selected.value = null
      current.removePair(first, pick) != null -> {
        _selected.value = null
        act { it.removePair(first, pick) ?: it }
      }
      else -> _selected.value = pick
    }
  }

  fun onStockTap() {
    _selected.value = null
    act { it.drawFromStock() ?: it.recycleWaste() ?: it }
  }

  private inline fun act(transform: (PyramidState) -> PyramidState) {
    val s = _session.value ?: return
    val next = transform(s.current)
    if (next === s.current) return
    replace(
      s.copy(
        current = next,
        history = (listOf(s.current) + s.history).take(UndoLimit),
      )
    )
  }

  private fun replace(next: PyramidSession) {
    _session.value = next
    viewModelScope.launch {
      if (next.current.isWon()) {
        repo.clear()
      } else {
        repo.save(next)
      }
    }
  }

  companion object {
    fun factory(repo: GameSessionStore<PyramidSession>): ViewModelProvider.Factory = viewModelFactory {
      initializer { PyramidViewModel(repo) }
    }
  }
}
