/*
    This file is part of CycPick.
    Copyright (C) 2022  Srđan Todorović

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

    Contact: tsrdjan@pm.me
*/

val scala3Version = "3.2.0"
val projectVersion = "1.0.0"

lazy val root = project
    .in(file("."))
    .settings(
        name := "CycPick",
        version := projectVersion,
        organization := "com.cycpick",

        scalaVersion := scala3Version,

        libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
        libraryDependencies += "org.scalafx" %% "scalafx" % "18.0.1-R28",

        //assembly / mainClass := Some("com.cycpick.Main"),
        assembly / assemblyJarName  := s"${name.value}-${version.value}.jar",
        assembly / assemblyMergeStrategy := {
            case PathList("META-INF", xs @ _*) => MergeStrategy.discard
            case x => MergeStrategy.first
        },

        // Fork to avoid JavaFX double initialization problems
        fork := true
    )
    .enablePlugins(AssemblyPlugin)


