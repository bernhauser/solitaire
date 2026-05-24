package se.bernhauser.solitaire.game

import kotlinx.serialization.Serializable

@Serializable
enum class SuitColor { Red, Black }

@Serializable
enum class Suit(val symbol: Char, val color: SuitColor) {
  Clubs('C', SuitColor.Black),
  Diamonds('D', SuitColor.Red),
  Hearts('H', SuitColor.Red),
  Spades('S', SuitColor.Black),
}

@Serializable
enum class Rank(val symbol: Char, val value: Int) {
  Ace('A', 1),
  Two('2', 2),
  Three('3', 3),
  Four('4', 4),
  Five('5', 5),
  Six('6', 6),
  Seven('7', 7),
  Eight('8', 8),
  Nine('9', 9),
  Ten('T', 10),
  Jack('J', 11),
  Queen('Q', 12),
  King('K', 13),
}

@Serializable
data class Card(val rank: Rank, val suit: Suit) {
  val assetKey: String = "${rank.symbol}${suit.symbol}"
}

enum class CardBack(val assetKey: String) {
  Red("1B"),
  Blue("2B"),
}

val FullDeck: List<Card> = Suit.entries.flatMap { suit ->
  Rank.entries.map { rank -> Card(rank, suit) }
}
