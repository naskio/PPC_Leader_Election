lazy val root = (project in file(".")).
  settings(
    name := "ppc_leader_election",
    version := "1.0",
    scalaVersion := "2.12.0"
  )
  


libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.18"
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.5.18"



