# Klondike Solitaire — Game Rules

This document specifies the rules of the game implemented by this project. It is the source of truth that the game logic and UI must conform to.

**Variant:** Klondike, Draw 3, unlimited redeals, substack moves allowed, foundations reversible.

---

## 1. Equipment

- One standard 52-card deck.
- Four suits: Clubs (♣), Diamonds (♦), Hearts (♥), Spades (♠).
- Two colors: black (♣, ♠) and red (♦, ♥).
- Ranks, low to high: A, 2, 3, 4, 5, 6, 7, 8, 9, 10, J, Q, K.

No jokers.

---

## 2. Layout

The play area has four distinct zones:

### 2.1 Tableau

Seven columns, dealt left to right:

- Column 1: 1 card
- Column 2: 2 cards
- Column 3: 3 cards
- Column 4: 4 cards
- Column 5: 5 cards
- Column 6: 6 cards
- Column 7: 7 cards

In each column, only the **topmost** card is dealt face-up. All cards beneath it are face-down. Total tableau cards: 28.

### 2.2 Stock

The remaining 24 cards form the stock, placed face-down. The stock is the draw pile.

### 2.3 Waste

Empty at the start. Cards drawn from the stock are turned face-up onto the waste.

### 2.4 Foundations

Four empty piles, one per suit. The goal is to fill these.

---

## 3. Objective

Move all 52 cards to the foundations. Each foundation is built up by suit, starting from Ace and ending with King:

> A → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → J → Q → K

The game is **won** when all four foundations are complete (each holds 13 cards in suit order, A through K).

---

## 4. Moves

During play the player may make any of the following legal moves. There is no turn limit and no opponent.

### 4.1 Draw from stock

Tap the stock to flip the **top 3 cards** of the stock face-up onto the waste, preserving their order so the last-flipped card is on top.

- If the stock has fewer than 3 cards, flip all remaining cards (1 or 2) in the same way.
- Only the **top card of the waste** is playable.

### 4.2 Recycle the waste

When the stock is empty, tapping the stock area turns the entire waste pile over, without shuffling or reordering it, to form a new face-down stock. The waste becomes empty.

There is **no limit** on the number of times the player may recycle the waste.

### 4.3 Move to a tableau column

A card or a run of cards may be moved onto a tableau column when:

- **Onto a non-empty column:** the moved card, or the first card of the moved run (the card that will be placed directly onto the destination column), is one rank lower than the destination column's top card **and** of the opposite color.
  - Example: a red 7 may be placed on a black 8.
- **Onto an empty column:** only a King (or a run whose first card is a King) may be placed.

Allowed sources for a tableau move:

- The top card of the waste.
- The top card of any foundation (see §4.5).
- Any face-up card from a tableau column, **together with every face-up card below it in that same column**, provided that group forms a valid descending alternating-color run. This is a **substack move**.

After a tableau move, if the source column has a face-down card now exposed at the top, that card is automatically flipped face-up.

### 4.4 Move to a foundation

A single card may be moved onto a foundation when:

- **Onto an empty foundation:** the card is an Ace. Any Ace may start any empty foundation slot; that slot then becomes the foundation for that Ace's suit.
- **Onto a non-empty foundation:** the card is the same suit as the foundation and exactly one rank higher than the foundation's top card.

Allowed sources for a foundation move:

- The top card of the waste.
- The top card of any tableau column.

Only **one card at a time** moves to a foundation. Runs cannot be moved as a group to a foundation.

### 4.5 Move from a foundation back to the tableau

The top card of a foundation may be moved back onto a tableau column, subject to the same placement rules as §4.3. This is sometimes useful to free a needed card of the opposite color.

---

## 5. Illegal moves

For clarity, the following are explicitly **not** allowed:

- Moving a face-down card.
- Moving a card from the middle of a tableau column without taking all cards above it.
- Moving a tableau run that is not a valid descending alternating-color sequence.
- Placing a non-King onto an empty tableau column.
- Placing a card on a tableau column of the same color or wrong rank.
- Placing a card on a foundation of the wrong suit or wrong rank.
- Moving more than one card at a time onto a foundation.
- Moving cards directly from the stock; cards must first be turned to the waste.
- Reordering, peeking at, or shuffling the stock or face-down tableau cards.

---

## 6. End of game

### 6.1 Win

The game ends in a win the moment all four foundations hold King as their top card (all 52 cards on foundations).

### 6.2 Loss / stuck

The game is **stuck** when no further legal play can be made from the current position. Because redeals are unlimited, this practically means:

- No card from the waste, any tableau column top, or any foundation top can be placed on any tableau column or foundation, **and**
- Further stock draws and waste recycles only repeat the same stock/waste cycle without making any new card playable.

The game implementation may detect this state and offer the player the option to concede; it is not required to auto-detect it.

### 6.3 Abandon

The player may abandon a game at any time. An abandoned game counts as a loss for statistics purposes.

---

## 7. Scoring (optional, to be decided)

This document does not yet specify a scoring system. Candidate systems for later decision:

- **Standard Klondike scoring:** points for moves to foundation, moves from waste to tableau, turning over a tableau card, etc.
- **Vegas scoring:** monetary stake per deal, payout per foundation card.
- **No score, time and moves only.**

Scoring is intentionally left out of the initial implementation scope.

---

## 8. Glossary

- **Tableau** — the seven main columns where most play happens.
- **Stock** — the face-down draw pile.
- **Waste** — the face-up discard pile next to the stock; top card is playable.
- **Foundation** — one of four target piles, built up by suit from Ace to King.
- **Run / substack** — a face-up sequence of tableau cards in descending rank and alternating color, movable as a unit.
- **Redeal / recycle** — moving the waste back to the stock when the stock empties.
