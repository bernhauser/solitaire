package se.bernhauser.solitaire.game.spider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.bernhauser.solitaire.persistence.SpiderSession
import se.bernhauser.solitaire.persistence.UndoLimit
import se.bernhauser.solitaire.repository.GameSessionStore

class SpiderViewModel(private val repo: GameSessionStore<SpiderSession>) : ViewModel() {
  private val _session = MutableStateFlow<SpiderSession?>(null)
  private val _loaded = MutableStateFlow(false)

  val state: StateFlow<SpiderState?> = _session
    .map { it?.current }
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val difficulty: StateFlow<SpiderDifficulty?> = _session
    .map { it?.difficulty }
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

  /** True once loading finished with no saved game — the screen must ask for a difficulty. */
  val needsNewGame: StateFlow<Boolean> = combine(_loaded, _session) { loaded, session ->
    loaded && session == null
  }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val dealId: MutableStateFlow<Int> = MutableStateFlow(0)
  val isRestored: MutableStateFlow<Boolean> = MutableStateFlow(false)

  init {
    viewModelScope.launch {
      val loaded = repo.load()
      if (loaded != null) {
        isRestored.value = true
        _session.value = loaded
      }
      _loaded.value = true
    }
  }

  fun newGame(difficulty: SpiderDifficulty) {
    isRestored.value = false
    dealId.value += 1
    replace(SpiderSession(difficulty = difficulty, current = dealNewSpider(difficulty, System.nanoTime())))
  }

  fun undo() {
    val s = _session.value ?: return
    val prev = s.history.firstOrNull() ?: return
    replace(s.copy(current = prev, history = s.history.drop(1)))
  }

  fun onDealTap() = act { it.dealFromStock() ?: it }

  fun onMoveRun(fromColumn: Int, fromIndex: Int, toColumn: Int) = act {
    it.moveRun(fromColumn, fromIndex, toColumn) ?: it
  }

  private inline fun act(transform: (SpiderState) -> SpiderState) {
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

  private fun replace(next: SpiderSession) {
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
    fun factory(repo: GameSessionStore<SpiderSession>): ViewModelProvider.Factory = viewModelFactory {
      initializer { SpiderViewModel(repo) }
    }
  }
}
