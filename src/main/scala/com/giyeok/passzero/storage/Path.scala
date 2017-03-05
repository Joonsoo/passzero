package com.giyeok.passzero.storage

case class Path(path: Seq[String]) {
    def isRoot: Boolean = path.isEmpty

    def \(child: String): Path = Path(path :+ child)
}

object Path {
    def apply(path: String): Path = if (path.isEmpty) Path(Seq()) else Path(path.split('/'))
}
