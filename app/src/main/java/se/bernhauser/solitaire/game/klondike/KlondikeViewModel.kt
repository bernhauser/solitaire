package se.bernhauser.solitaire.game.klondike

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
import se.bernhauser.solitaire.persistence.KlondikeSession
import se.bernhauser.solitaire.persistence.UndoLimit
import se.bernhauser.solitaire.repository.GameSessionStore

class KlondikeViewModel(private val repo: GameSessionStore<KlondikeSession>) : ViewModel() {
  private val _session = MutableStateFlow<KlondikeSession?>(null)

  val state: StateFlow<KlondikeState?> = _session
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
      val loaded = repo.load()
      if (loaded != null) {
        isRestored.value = true
        _session.value = loaded
      } else {
        isRestored.value = false
        _session.value = freshSession()
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

  fun debugWin() = loadDebugState(nearWinState())

  fun debugStuck() = loadDebugState(stuckState())

  private fun loadDebugState(s: KlondikeState) {
    isRestored.value = false
    dealId.value += 1
    _gameOver.value = false
    replace(
      KlondikeSession(
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

  private inline fun act(transform: (KlondikeState) -> KlondikeState) {
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

  private fun replace(next: KlondikeSession) {
    _session.value = next
    viewModelScope.launch {
      if (next.current.isWon()) {
        repo.clear()
      } else {
        repo.save(next)
      }
    }
  }

  private fun freshSession(): KlondikeSession {
    val initial = dealNewGame(System.nanoTime())
    return KlondikeSession(
      current = initial,
      movePossibleSinceLastRecycle = initial.hasAnyImmediateMove(),
    )
  }

  companion object {
    fun factory(repo: GameSessionStore<KlondikeSession>): ViewModelProvider.Factory = viewModelFactory {
      initializer { KlondikeViewModel(repo) }
    }
  }
}
