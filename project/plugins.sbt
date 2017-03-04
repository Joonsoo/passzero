lazy val oneswtPlugin = uri("file:///C:/home/joonsoo/workspace/oneswt")
lazy val root = project.in(file(".")).dependsOn(oneswtPlugin)
