val akkaVersion = "2.5.12"
val constructrVersion = "0.19.0"

lazy val root = (project in file("."))
  .settings(
    name := "CloudMapReduce",
    scalaVersion := "2.12.5",
    version := "1.0",
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "de.heikoseeberger" %% "constructr" % constructrVersion,
      "de.heikoseeberger" %% "constructr-coordination-etcd" % constructrVersion),
      fork in run := true
  )
dockerRepository := Some("bgomez89")
dockerBaseImage := "java"
dockerEntrypoint := Seq("bin/cloud-master-actor")
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)