package com.giyeok.passzero.utils

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

sealed trait FutureStream[+T] {
    def isEmpty: Boolean

    def map[B](func: T => B): FutureStream[B]
    def map1[B](func: T => Future[B]): FutureStream[B]
    def foreach(func: (T, FutureStream[T]) => Unit): Unit
}
object FutureStream {
    case class Cons[T](future: Future[(T, FutureStream[T])])(implicit val ec: ExecutionContext) extends FutureStream[T] {
        val isEmpty: Boolean = false
        val value: Future[T] = future map { _._1 }
        val next: Future[FutureStream[T]] = future map { _._2 }

        def map[B](func: T => B): FutureStream[B] = {
            val newFuture: Future[(B, FutureStream[B])] = future map { p =>
                (func(p._1), p._2 map func)
            }
            Cons(newFuture)
        }
        def map1[B](func: T => Future[B]): FutureStream[B] = {
            val newFuture: Future[(B, FutureStream[B])] = future flatMap { p =>
                func(p._1) map { (_, p._2 map1 func) }
            }
            Cons(newFuture)
        }
        def foreach(func: (T, FutureStream[T]) => Unit): Unit = {
            future foreach { p =>
                val (value, next) = p
                func(value, next)
                next foreach func
            }
        }
    }

    case object Nil extends FutureStream[Nothing] {
        val isEmpty: Boolean = true

        def map[B](func: Nothing => B): FutureStream[B] = Nil
        def map1[B](func: Nothing => Future[B]): FutureStream[B] = Nil
        def foreach(func: (Nothing, FutureStream[Nothing]) => Unit): Unit = {}
    }

    def apply[T](items: Future[T]*): FutureStream[T] = {
        implicit val ec = ExecutionContext.global
        if (items.isEmpty) Nil else Cons[T](items.head map { head => (head, FutureStream(items.tail: _*)) })
    }

    def empty[T]: FutureStream[T] = Nil
}
