package se.bernhauser.solitaire.game

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
import se.bernhauser.solitaire.persistence.SavedSession
import se.bernhauser.solitaire.persistence.UndoLimit
import se.bernhauser.solitaire.repository.SolitaireRepository

class GameViewModel(private val repo: SolitaireRepository) : ViewModel() {
  private val _session = MutableStateFlow<SavedSession?>(null)

  val state: StateFlow<GameState?> = _session
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

  val canRecycle: StateFlow<Boolean> = _session
    .map { s ->
      s != null &&
        s.current.stock.isEmpty() &&
        s.current.waste.isNotEmpty() &&
        s.movePossibleSinceLastRecycle
    }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  private val _gameOver = MutableStateFlow(false)
  val gameOver: StateFlow<Boolean> = _gameOver

  fun nextAutoCompleteSource(): FoundationMoveSource? =
    _session.value?.current?.nextAutoCompleteSource()

  val dealId: MutableStateFlow<Int> = MutableStateFlow(0)
  val isRestored: MutableStateFlow<Boolean> = MutableStateFlow(false)

  init {
    viewModelScope.launch {
      val loaded = repo.loadSession()
      if (loaded != null) {
        _session.value = loaded
        isRestored.value = true
      } else {
        _session.value = freshSession()
        isRestored.value = false
      }
    }
  }

  fun newGame() {
    isRestored.value = false
    dealId.value += 1
    _gameOver.value = false
    replace(freshSession())
  }

  fun dismissGameOver() {
    _gameOver.value = false
  }

  fun debugWin() {
    isRestored.value = false
    dealId.value += 1
    _gameOver.value = false
    val s = nearWinState()
    replace(
      SavedSession(
        current = s,
        movePossibleSinceLastRecycle = s.hasAnyImmediateMove(),
      )
    )
  }

  fun undo() {
    val s = _session.value ?: return
    val prev = s.history.firstOrNull() ?: return
    _gameOver.value = false
    replace(
      s.copy(
        current = prev,
        history = s.history.drop(1),
        movePossibleSinceLastRecycle = s.movePossibleSinceLastRecycle || prev.hasAnyImmediateMove(),
      )
    )
  }

  fun onStockTap() {
    val s = _session.value ?: return
    val cur = s.current
    when {
      cur.stock.isNotEmpty() -> {
        val next = cur.drawFromStock() ?: return
        replace(
          s.copy(
            current = next,
            history = (listOf(cur) + s.history).take(UndoLimit),
            movePossibleSinceLastRecycle =
              s.movePossibleSinceLastRecycle || next.hasAnyImmediateMove(),
          )
        )
      }
      cur.waste.isNotEmpty() -> {
        if (!s.movePossibleSinceLastRecycle) {
          _gameOver.value = true
          return
        }
        val next = cur.recycleWaste() ?: return
        replace(
          s.copy(
            current = next,
            history = (listOf(cur) + s.history).take(UndoLimit),
            movePossibleSinceLastRecycle = next.hasAnyImmediateMove(),
          )
        )
      }
    }
  }

  fun onWasteTap() = act { it.moveToFoundation(FoundationMoveSource.WasteTop) ?: it }
  fun onTableauTopTap(column: Int) = act {
    it.moveToFoundation(FoundationMoveSource.TableauTop(column)) ?: it
  }

  fun onDropOnTableau(source: TableauMoveSource, column: Int) = act {
    it.moveToTableau(source, column) ?: it
  }

  fun onDropOnFoundation(source: FoundationMoveSource) = act {
    it.moveToFoundation(source) ?: it
  }

  private inline fun act(transform: (GameState) -> GameState) {
    val s = _session.value ?: return
    val next = transform(s.current)
    if (next === s.current) return
    replace(
      s.copy(
        current = next,
        history = (listOf(s.current) + s.history).take(UndoLimit),
        movePossibleSinceLastRecycle = true,
      )
    )
  }

  private fun replace(next: SavedSession) {
    _session.value = next
    viewModelScope.launch {
      if (next.current.isWon()) {
        repo.clearSession()
      } else {
        repo.saveSession(next)
      }
    }
  }

  private fun freshSession(): SavedSession {
    val initial = dealNewGame(System.nanoTime())
    return SavedSession(
      current = initial,
      movePossibleSinceLastRecycle = initial.hasAnyImmediateMove(),
    )
  }

  companion object {
    fun factory(repo: SolitaireRepository): ViewModelProvider.Factory = viewModelFactory {
      initializer { GameViewModel(repo) }
    }
  }
}
