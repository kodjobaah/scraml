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

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.parser.{RamlParseException, ParseContext}
import play.api.libs.json.{JsString, JsArray, JsObject, JsValue}

import scala.util.{Failure, Success, Try}

/**
  * Created by peter on 10/02/16.
  */
case class Types(nativeTypes: List[Type] = List.empty, external: Map[String, String] = Map.empty) {

  def ++(otherTypes: Types): Types = {
    Types(nativeTypes ++ otherTypes.nativeTypes, external ++ otherTypes.external)
  }

}

object Types {

  def apply(typesJson: JsValue)(implicit parseContext: ParseContext): Try[Types] = {

    def doApply(tpsJson: JsValue): Try[Types] = {
      tpsJson match {
        case JsInclude(included, source) =>
          implicit val newParseContext = parseContext.copy(source = source)
          doApply(included)
        case typesJsObj: JsObject        => typesJsObjToTraitMap(typesJsObj)
        case typesJsArr: JsArray         =>
          val tryTypes = typesJsArr.value.collect {
            case typesJsObj: JsObject => typesJsObjToTraitMap(typesJsObj)
          }
          foldTryTypes(tryTypes)
        case x                           =>
          Failure(RamlParseException(s"The types (or schemas) definition in ${parseContext.source} is malformed."))
      }
    }


    def typesJsObjToTraitMap(typesJsObj: JsObject): Try[Types] = {
      val tryTypes =
        typesJsObj.fields.collect {
          case (key: String, JsInclude(JsString(included), source)) => Success(Types(external = Map(key -> included)))
          case (key: String, JsInclude(included: JsObject, source)) => typeObjectToNativeTypes(key, included)
          case (key: String, JsString(value))                       => Success(Types(external = Map(key -> value)))
          case (key: String, value: JsObject)                       => typeObjectToNativeTypes(key, value)
        }
      foldTryTypes(tryTypes)
    }


    def foldTryTypes(tryTypes: Seq[Try[Types]]): Try[Types] = {
      tryTypes.foldLeft[Try[Types]](Success(Types())) {
        case (Success(aggr), Success(types))   => Success(aggr ++ types)
        case (fail@Failure(e), _)              => fail
        case (_, fail@Failure(e))              => fail
        case (Failure(eAggr), Failure(eTypes)) => Failure(RamlParseException(s"${eAggr.getMessage}\n${eTypes.getMessage}"))
      }
    }


    def typeObjectToNativeTypes(name: String, typeDefinition: JsObject): Try[Types] = {
      Type(name, typeDefinition).map(tp => Types(nativeTypes = List(tp)))
    }


    doApply(typesJson)
  }

}