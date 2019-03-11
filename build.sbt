name := "akka-auto-scaling-sample"

version := "0.1"

scalaVersion := "2.12.8"
// https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.20"
// https://mvnrepository.com/artifact/com.typesafe.akka/akka-http
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.7"
// https://mvnrepository.com/artifact/com.typesafe.akka/akka-cluster
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.20"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.20"
libraryDependencies +="com.typesafe.akka" %% "akka-discovery" % "2.5.20"
//libraryDependencies += "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.0-RC3"
libraryDependencies += "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.0-RC3"
//libraryDependencies += "com.spotify" % "docker-client" % "8.9.0"

// sbt native package
enablePlugins(JavaAppPackaging)
packageName in Docker := packageName.value

version in Docker := version.value
dockerExposedPorts := Seq(8080,2552)

