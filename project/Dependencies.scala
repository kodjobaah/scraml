
/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

import sbt._

trait Dependencies { this:Build =>

  // main dependencies
  val logback             =   "ch.qos.logback"              %     "logback-classic"     % "1.1.1"

  // java dsl dependencies
  val jacksonAnnotations  =   "com.fasterxml.jackson.core"  %     "jackson-annotations" % "2.5.4"
  val jacksonCore         =   "com.fasterxml.jackson.core"  %     "jackson-core"        % "2.5.4"
  val jacksonDatabind     =   "com.fasterxml.jackson.core"  %     "jackson-databind"    % "2.5.4"
  // val rxHttpClientJava    =   "be.wegenenverkeer"           %     "rxhttpclient-java"   % "0.2.0"

  val snakeYaml           =   "org.yaml"                    %     "snakeyaml"           % "1.16"
//  val jerkson             =   "com.codahale"                %%     "jerkson"             % "0.5.0"

  // val rxHttpClientScala   =   "be.wegenenverkeer"           %%    "rxhttpclient-scala"  % "0.2.0"
  val asyncClient         =   "com.ning"                    %     "async-http-client"   % "1.9.36"
  val asyncClientProvided =   "com.ning"                    %     "async-http-client"   % "1.9.36"   % "provided"
  val playJson            =   "com.typesafe.play"           %%    "play-json"           % "2.4.3"
  val play25Json          =   "com.typesafe.play"           %%    "play-json"           % "2.5.3"

  val scalariform         =   "org.scalariform"             %%    "scalariform"         % "0.1.7"
  // val eclipseJdt          =   "org.eclipse"                 %    "jdt"                 % "3.3.0-v20070607-1300"
  val slf4j               =   "org.slf4j"                   %    "slf4j-api"            % "1.7.12"

  // test dependencies
  val scalaTest           =   "org.scalatest"               %%    "scalatest"           % "2.2.6"    % "test"
  val wiremock            =   "com.github.tomakehurst"      %     "wiremock"            % "1.57"     % "test"
  val junit               =   "junit"                       %     "junit"               % "4.12"     % "test"
  val asyncClientTest     =   "com.ning"                    %     "async-http-client"   % "1.9.36"   % "test"


  val scramlRamlParserDeps = Seq(
    playJson,
    snakeYaml
//    ,
//    jerkson
  )

  // inclusion of the above dependencies in the modules
  val scramlGeneratorDeps = Seq (
    scalariform,
    junit
  )

  val scramlGeneratorTestDefDeps = Seq (
    junit,
    asyncClient,
    playJson,
    wiremock
  )

  val scramlDslDepsScala = Seq(
    slf4j,
    playJson,
    asyncClientProvided
  )

  val scramlDslPlay25DepsScala = Seq(
    slf4j,
    play25Json,
    asyncClientProvided
  )

  val scramlDslDepsJava = Seq(
    slf4j,
    junit,
    jacksonCore,
    jacksonAnnotations,
    jacksonDatabind,
    asyncClientProvided
  )

  val mainDeps = Seq(
    logback
  )

  val testDeps = Seq(
    scalaTest,
    wiremock,
    asyncClientTest
  )

  val allDeps = mainDeps ++ testDeps

}
