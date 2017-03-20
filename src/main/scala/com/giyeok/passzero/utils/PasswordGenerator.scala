package com.giyeok.passzero.utils

import scala.io.Source
import scala.util.Random

object PasswordGenerator {
    object Chars {
        case class Recipe(length: Int, numbers: Int, specials: Int)

        private val base = ('a' to 'z').toIndexedSeq ++ ('A' to 'Z').toIndexedSeq
        private val numbers = ('0' to '9').toIndexedSeq
        private val specials = "!@#$%^&*()_+-=`~[]\\{}|;':\",./<>?".toIndexedSeq

        def generate(recipe: Recipe): String = {
            val _numbers = (0 until recipe.numbers) map { _ => numbers(Random.nextInt(numbers.length)) }
            val _specials = (0 until recipe.specials) map { _ => specials(Random.nextInt(specials.length)) }
            val _bases = (0 until (recipe.length - (recipe.numbers + recipe.specials))) map { _ => base(Random.nextInt(base.length)) }
            new String((Random.shuffle(_numbers ++ _specials ++ _bases) take recipe.length).toArray)
        }
    }

    object Words {
        // connector: - .,_
        case class Recipe(count: Int, connector: String)

        private def wordsStream = getClass.getResourceAsStream("/words.txt")
        private lazy val wordsSeq = (Source.fromInputStream(wordsStream).getLines() map { _.trim } filter { _.nonEmpty }).toIndexedSeq

        private def pickWords0(): String = wordsSeq(Random.nextInt(wordsSeq.length))
        // TODO wordsStream에 random access해서 바로 다음 \n부터 그 다음 \n까지의 단어를 읽어서 trim해서 보내도록 수정 - 필요하면..

        def generate(recipe: Recipe): String = {
            ((0 until recipe.count) map { _ => pickWords0() }) mkString recipe.connector
        }
    }
}
