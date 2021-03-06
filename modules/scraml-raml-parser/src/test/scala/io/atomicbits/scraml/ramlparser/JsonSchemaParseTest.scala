/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License for more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser

import io.atomicbits.scraml.ramlparser.model.types._
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import io.atomicbits.util.TestUtils
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}
import org.scalatest.Matchers._

import scala.language.postfixOps
import scala.util.Try


/**
  * Created by peter on 6/02/16.
  */
class JsonSchemaParseTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("json schema parsing") {

    scenario("test parsing fragments in json-schema") {

      Given("a json-schema containing fragments")
      val parser = RamlParser("/fragments/TestFragmentsApi.raml", "UTF-8", List("io", "atomicbits", "model"))

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse

      Then("we get a properly parsed fragments object")
      val raml = parsedModel.get

      val objectWithFragments: ObjectType = raml.types.typeReferences(NativeId("myfragments")).asInstanceOf[ObjectType]

      val barType: Type = objectWithFragments.fragments.fragmentMap("bar")

      barType shouldBe a[TypeReference]
      barType.asInstanceOf[TypeReference].refersTo shouldBe NativeId("baz")


      val definitionFragment: Fragments = objectWithFragments.fragments.fragmentMap("definitions").asInstanceOf[Fragments]

      val addressType = definitionFragment.fragmentMap("address")
      addressType shouldBe a[ObjectType]
      val address = addressType.asInstanceOf[ObjectType]

      address.id shouldBe FragmentId(List("definitions", "address"))
      address.properties("city") shouldBe a[StringType]
      address.properties("state") shouldBe a[StringType]
      address.properties("zip") shouldBe a[IntegerType]
      address.properties("streetAddress") shouldBe a[StringType]

      //      val prettyModel = TestUtils.prettyPrint(parsedModel)
      //       println(s"Parsed raml: $prettyModel")
    }


    scenario("test parsing json-schema types in a RAML 1.0 model") {

      Given("a RAML 1.0 specification with json-schema types")
      val parser = RamlParser("/json-schema-types/TestApi.raml", "UTF-8", List("io", "atomicbits", "model"))

      When("we parse the specification")
      val parsedModel = parser.parse

      Then("we get a ...")
      val prettyModel = TestUtils.prettyPrint(parsedModel)
      //       println(s"Parsed raml: $prettyModel")
    }

  }


}
