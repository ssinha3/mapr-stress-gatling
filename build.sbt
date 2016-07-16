enablePlugins(GatlingPlugin)

name := "mapr-stress-gatling"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.2" % "test"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "2.2.2" % "test"
libraryDependencies += "com.mapr.db"           % "maprdb"                    % "5.2.0-mapr" % "test"
libraryDependencies += "org.ojai"              % "ojai"                      % "1.1" % "test"

resolvers += "Ojai Repo" at "http://repo1.maven.org/maven2"
resolvers += "MapRDB Repo" at "http://10.10.100.99:8081/nexus/content/repositories/releases"