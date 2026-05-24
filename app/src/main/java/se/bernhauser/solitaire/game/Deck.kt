package se.bernhauser.solitaire.game

import kotlin.random.Random

fun shuffledDeck(seed: Long): List<Card> = FullDeck.shuffled(Random(seed))
