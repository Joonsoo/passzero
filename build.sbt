lazy val root = (project in file(".")).
    settings(
        organization := "com.giyeok",
        name := "passzero",
        version := "0.0.1",
        scalaVersion := "2.12.1",
        conflictManager := ConflictManager.latestTime
    )

libraryDependencies += "org.json4s" %% "json4s-native" % "3.5.0"

// swt
val swtVersion = "4.6.2"
resolvers += "Local Maven Repository" at "file:///C:/home/joonsoo/workspace/oneswt/maven"
libraryDependencies += sbtoneswt.OneSwtPlugin.archDependentSwt.value

// google drive api
libraryDependencies += "com.google.api-client" % "google-api-client" % "1.22.0"
libraryDependencies += "com.google.oauth-client" % "google-oauth-client-jetty" % "1.22.0"
libraryDependencies += "com.google.apis" % "google-api-services-drive" % "v3-rev60-1.22.0"

// https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox
libraryDependencies += "org.apache.pdfbox" % "pdfbox" % "2.0.4"

// https://mvnrepository.com/artifact/commons-logging/commons-logging
libraryDependencies += "commons-logging" % "commons-logging" % "1.2"

// ===== Test =====
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

// https://mvnrepository.com/artifact/org.apache.hbase/hbase-testing-util
libraryDependencies += "org.apache.hbase" % "hbase-testing-util" % "1.2.4" % "test"

javacOptions in compile ++= Seq("-encoding", "UTF-8")

mainClass in assembly := Some("com.giyeok.passzero.ui.MainUI")

// javaOptions in run += "-agentlib:hprof=cpu=samples"
