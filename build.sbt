name := "akka-auto-scaling-sample"

version := "0.1"

scalaVersion := "2.12.8"
// https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.19"
// https://mvnrepository.com/artifact/com.typesafe.akka/akka-http
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.7"
// https://mvnrepository.com/artifact/com.typesafe.akka/akka-cluster
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.19"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.19"