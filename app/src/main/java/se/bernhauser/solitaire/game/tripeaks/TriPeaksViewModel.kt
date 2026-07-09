package se.bernhauser.solitaire.game.tripeaks

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
import se.bernhauser.solitaire.persistence.TriPeaksSession
import se.bernhauser.solitaire.persistence.UndoLimit
import se.bernhauser.solitaire.repository.GameSessionStore

class TriPeaksViewModel(private val repo: GameSessionStore<TriPeaksSession>) : ViewModel() {
  private val _session = MutableStateFlow<TriPeaksSession?>(null)

  val state: StateFlow<TriPeaksState?> = _session
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
      _session.value = repo.load() ?: TriPeaksSession(current = dealNewTriPeaks(System.nanoTime()))
    }
  }

  fun newGame() {
    replace(TriPeaksSession(current = dealNewTriPeaks(System.nanoTime())))
  }

  fun undo() {
    val s = _session.value ?: return
    val prev = s.history.firstOrNull() ?: return
    replace(s.copy(current = prev, history = s.history.drop(1)))
  }

  fun onPlay(index: Int) = act { it.play(index) ?: it }

  fun onDrawTap() = act { it.drawFromStock() ?: it }

  private inline fun act(transform: (TriPeaksState) -> TriPeaksState) {
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

  private fun replace(next: TriPeaksSession) {
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
    fun factory(repo: GameSessionStore<TriPeaksSession>): ViewModelProvider.Factory = viewModelFactory {
      initializer { TriPeaksViewModel(repo) }
    }
  }
}
