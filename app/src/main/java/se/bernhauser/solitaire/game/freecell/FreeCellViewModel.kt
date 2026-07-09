package se.bernhauser.solitaire.game.freecell

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
import se.bernhauser.solitaire.persistence.FreeCellSession
import se.bernhauser.solitaire.persistence.UndoLimit
import se.bernhauser.solitaire.repository.GameSessionStore

class FreeCellViewModel(private val repo: GameSessionStore<FreeCellSession>) : ViewModel() {
  private val _session = MutableStateFlow<FreeCellSession?>(null)

  val state: StateFlow<FreeCellState?> = _session
    .map { it?.current }
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val canUndo: StateFlow<Boolean> = _session
    .map { it?.history?.isNotEmpty() == true }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val canAutoComplete: StateFlow<Boolean> = _session
    .map { it?.current?.canAutoComplete() == true }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val isWon: StateFlow<Boolean> = _session
    .map { it?.current?.isWon() == true }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val gameOver: StateFlow<Boolean> = _session
    .map { it?.current?.isStuck() == true }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val dealId: MutableStateFlow<Int> = MutableStateFlow(0)
  val isRestored: MutableStateFlow<Boolean> = MutableStateFlow(false)

  init {
    viewModelScope.launch {
      val loaded = repo.load()
      if (loaded != null) {
        isRestored.value = true
        _session.value = loaded
      } else {
        isRestored.value = false
        _session.value = FreeCellSession(current = dealNewFreeCell(System.nanoTime()))
      }
    }
  }

  fun newGame() {
    isRestored.value = false
    dealId.value += 1
    replace(FreeCellSession(current = dealNewFreeCell(System.nanoTime())))
  }

  fun undo() {
    val s = _session.value ?: return
    val prev = s.history.firstOrNull() ?: return
    replace(s.copy(current = prev, history = s.history.drop(1)))
  }

  fun onMoveToTableau(source: FreeCellTableauSource, column: Int) = act {
    it.moveToTableau(source, column) ?: it
  }

  fun onMoveToFoundation(source: FreeCellCardSource) = act {
    it.moveToFoundation(source) ?: it
  }

  fun onMoveToCell(source: FreeCellCardSource, cellIndex: Int) = act {
    it.moveToCell(source, cellIndex) ?: it
  }

  private inline fun act(transform: (FreeCellState) -> FreeCellState) {
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

  private fun replace(next: FreeCellSession) {
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
    fun factory(repo: GameSessionStore<FreeCellSession>): ViewModelProvider.Factory = viewModelFactory {
      initializer { FreeCellViewModel(repo) }
    }
  }
}
