package com.giyeok.passzero.storage

case class Path(path: Seq[String]) {
    assert(path forall { s => !s.contains('/') })
    assert(path forall { s => s != "." && s != ".." })

    def isRoot: Boolean = path.isEmpty
    def parent: Option[Path] = if (isRoot) None else Some(Path(path.init))

    def name: String = path.last

    def /(child: String): Path = Path(path :+ child)
    def /(subpath: Path): Path = Path(path ++ subpath.path)

    def string: String = "/" + (path mkString "/")
    override def toString: String = this.string
}

object Path {
    def apply(path: String): Path = if (path.isEmpty) Path(Seq()) else Path(path.split('/') filterNot { _.isEmpty })
}
