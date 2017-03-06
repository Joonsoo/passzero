package com.giyeok.passzero.storage

class Path(val path: Seq[String]) {
    assert(path forall { s => !s.contains('/') })
    assert(path forall { s => s != "." && s != ".." })

    def isRoot: Boolean = path.isEmpty

    def parent: Path = Path(path.init)

    def /(child: String): Path = Path(path :+ child)
    def /(subpath: Path): Path = Path(path ++ subpath.path)

    def string: String = path mkString "/"
}

object Path {
    def apply(path: String): Path = if (path.isEmpty) Path(Seq()) else Path(path.split('/'))
    def apply(seq: Seq[String]): Path = new Path(seq)
}
