package com.giyeok.passzero.utils

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

sealed trait FutureStream[+T] {
    def isEmpty: Boolean
}
object FutureStream {
    case class Cons[T](future: Future[(T, FutureStream[T])])(implicit val ec: ExecutionContext) extends FutureStream[T] {
        val isEmpty: Boolean = false
        val value: Future[T] = future map { _._1 }
        val next: Future[FutureStream[T]] = future map { _._2 }
    }
    case object Nil extends FutureStream[Nothing] {
        val isEmpty: Boolean = true
    }

    def apply[T](items: Future[T]*): FutureStream[T] = {
        implicit val ec = ExecutionContext.global
        if (items.isEmpty) Nil else Cons[T](items.head map { head => (head, FutureStream(items.tail: _*)) })
    }

    def empty[T]: FutureStream[T] = Nil
}
