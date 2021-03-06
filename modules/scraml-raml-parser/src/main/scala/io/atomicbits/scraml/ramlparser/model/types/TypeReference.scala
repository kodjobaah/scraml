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

package io.atomicbits.scraml.ramlparser.model.types

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import io.atomicbits.scraml.util.TryUtils
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.util.{Success, Try}

/**
  * Created by peter on 1/04/16.
  */
case class TypeReference(refersTo: Id,
                         id: Id = ImplicitId,
                         required: Option[Boolean] = None,
                         genericTypes: Map[String, Type] = Map.empty,
                         fragments: Fragments = Fragments(),
                         model: TypeModel = RamlModel) extends NonePrimitiveType with AllowedAsObjectField with Fragmented {

  override def updated(updatedId: Id): TypeReference = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): TypeReference = copy(model = typeModel)

}


object TypeReference {

  val value = "$ref"


  def apply(json: JsValue)(implicit parseContext: ParseContext): Try[TypeReference] = {

    val model: TypeModel = TypeModel(json)

    val id = json match {
      case IdExtractor(schemaId) => schemaId
    }

    val ref = json match {
      case RefExtractor(refId) => refId
    }

    val required = (json \ "required").asOpt[Boolean]

    val genericTypes: Try[Map[String, Type]] =
      (json \ "genericTypes").toOption.collect {
        case genericTs: JsObject =>
          val genericTsMap =
            genericTs.value collect {
              case (field, Type(t)) => (field, t)
            }
          TryUtils.accumulate[String, Type](genericTsMap.toMap)
      } getOrElse Try(Map.empty[String, Type])

    val fragments = json match {
      case Fragments(fragment) => fragment
    }

    TryUtils.withSuccess(
      Success(ref),
      Success(id),
      Success(required),
      genericTypes,
      fragments,
      Success(model)
    )(new TypeReference(_, _, _, _, _, _))
  }


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[TypeReference]] = {

    def checkOtherType(theOtherType: String): Option[Try[TypeReference]] = {
      Type(theOtherType) match {
        case typeRef: Try[TypeReference] => Some(typeRef) // It is not a primitive type and not an array, so it is a type reference.
        case _                           => None
      }
    }

    (Type.typeDeclaration(json), (json \ TypeReference.value).toOption, json) match {
      case (None, Some(_), _)                   => Some(TypeReference(json))
      case (Some(JsString(otherType)), None, _) => checkOtherType(otherType)
      case (_, _, JsString(otherType))          => checkOtherType(otherType)
      case _                                    => None
    }

  }

}
