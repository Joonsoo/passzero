addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.4")

addSbtPlugin("com.giyeok.oneswt" % "sbt-oneswt" % "0.0.1")

lazy val oneswtPlugin = uri("https://github.com/Joonsoo/oneswt.git#master")
lazy val root = project.in(file(".")).dependsOn(oneswtPlugin)
