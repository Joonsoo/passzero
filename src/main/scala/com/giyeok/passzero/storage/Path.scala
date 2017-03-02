package com.giyeok.passzero.storage

case class Path(path: Seq[String]) {
    def isRoot: Boolean = path.isEmpty
}

object Path {
    def apply(path: String): Path = Path(path.split('/'))
}
