lazy val root = (project in file(".")).
    settings(
        organization := "com.giyeok",
        name := "passzero",
        version := "0.0.1",
        scalaVersion := "2.12.1",
        conflictManager := ConflictManager.latestTime
    )

libraryDependencies += "org.json4s" %% "json4s-native" % "3.5.0"

// TODO google drive api

// https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox
libraryDependencies += "org.apache.pdfbox" % "pdfbox" % "2.0.4"

// ===== Test =====
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

// https://mvnrepository.com/artifact/org.apache.hbase/hbase-testing-util
libraryDependencies += "org.apache.hbase" % "hbase-testing-util" % "1.2.4" % "test"

javacOptions in compile ++= Seq("-encoding", "UTF-8")

fork in run := true

// javaOptions in run += "-agentlib:hprof=cpu=samples"
