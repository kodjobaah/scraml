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

import io.atomicbits.scraml.ramlparser.model.{Id, ImplicitId}
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.util.{Success, Try}

/**
  * Created by peter on 10/02/16.
  */
trait Type {

  def id: Id

  def updated(id: Id): Type

}


trait PrimitiveType extends Type


/**
  * Only used in json-schema.
  */
trait FragmentedType extends Type {

  def fragments: Map[String, Type]

}


trait AllowedAsObjectField {

  def required: Option[Boolean]

  // The default value according to the RAML 1.0 specs is true. According to the json-schema 0.3 specs, it should be false.
  // The json-schema 0.4 specs don't specify a 'required: boolean' field any more, only a list of the required field names at the
  // level of the object definition, but this also implies a default value of false.
  def isRequired = required.getOrElse(true)

}


object Type {


  def apply(typeName: String)(implicit parseContext: ParseContext): Try[Type] = {

    typeName match {
      case StringType.value     => Try(new StringType())
      case NumberType.value     => Try(new NumberType())
      case IntegerType.value    => Try(new IntegerType())
      case BooleanType.value    => Try(new BooleanType())
      case NullType.value       => Try(new StringType())
      case ArrayType(arrayType) => arrayType
      case namedType            => Try(TypeReference(parseContext.nameToId(namedType)))
    }

  }


  //  def apply(schema: JsObject, nameOpt: Option[String] = None)(implicit parseContext: ParseContext): Try[Type] = {
  //
  //    val typeOpt = (schema \ "type").asOpt[String]
  //    val enumOpt = (schema \ "enum").asOpt[List[String]]
  //
  //    typeOpt match {
  //      case Some("object")  =>
  //        (schema \ "genericType").asOpt[String] map (_ => GenericObjectType(schema)) getOrElse ObjectType(schema, nameOpt)
  //      case Some("array")   => ArrayType(schema)
  //      case Some("string")  =>
  //        enumOpt match {
  //          case Some(enum) => EnumType(schema)
  //          case None       => StringType(schema)
  //        }
  //      case Some("number")  => NumberType(schema)
  //      case Some("integer") => IntegerType(schema)
  //      case Some("boolean") => BooleanType(schema)
  //      case Some("null")    => NullType(schema)
  //      case Some(namedType) =>
  //        sys.error(s"Unkown json-schema type $namedType") // In RAML 1.0 this can be 'User' or 'Phone | Notebook' or 'Email[]'
  //      case None            =>
  //        val propertiesOpt = (schema \ "properties").asOpt[String]
  //        val referenceOpt = (schema \ "$ref").asOpt[String]
  //        val enumOpt = (schema \ "enum").asOpt[List[String]]
  //        (propertiesOpt, referenceOpt, enumOpt) match {
  //          case (Some(properties), _, _)   =>
  //            (schema \ "genericType").asOpt[String] map (_ => GenericObjectType(schema)) getOrElse ObjectType(schema, None)
  //          case (None, Some(reference), _) => TypeReference(schema)
  //          case (None, None, Some(enum))   => EnumType(schema)
  //          case _                          =>
  //            // According to the RAML 1.0 defaults, if no 'type' field and no 'properties' field is present, the type defaults to a
  //            // string type. This, however, conflicts with the possibility of having nested json-schema schemas. We decided to only
  //            // interpret the type as a string if the fragment alternative (meaning we have nested schemas) is empty.
  //            Fragment(schema).flatMap { fragment =>
  //              if (fragment.isEmpty) StringType(schema)
  //              else Success(fragment)
  //            }
  //        }
  //    }
  //  }


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[Type]] = {

    json match {
      case StringType(tryStringType)               => Some(tryStringType)
      case NumberType(tryNumberType)               => Some(tryNumberType)
      case IntegerType(tryIntegerType)             => Some(tryIntegerType)
      case BooleanType(tryBooleanType)             => Some(tryBooleanType)
      case NullType(tryNullType)                   => Some(tryNullType)
      case EnumType(tryEnumType)                   => Some(tryEnumType)
      case ArrayType(tryArrayType)                 => Some(tryArrayType)
      case ObjectType(tryObjectType)               => Some(tryObjectType)
      case GenericObjectType(tryGenericObjectType) => Some(tryGenericObjectType)
      case TypeReference(tryTypeReferenceType)     => Some(tryTypeReferenceType)
      case Fragment(tryFragmentType)               => Some(tryFragmentType) // A Fragment is a catchall for all JsObject values, so must be at the end of the 'normal' types.
      case _                                       => None
    }

  }


  def typeDeclaration(json: JsValue): Option[JsValue] = {
    List((json \ "type").toOption, (json \ "schema").toOption).flatten.headOption
  }


  def collectFragments(schemaObject: JsValue)(implicit parseContext: ParseContext): Map[String, Try[Type]] = {

    def collectFromJsObjecct(jsObj: JsObject): Map[String, Try[Type]] = {
      // Process the fragments and exclude the json-schema fields that we don't need to consider
      // (should be only objects as other fields are ignored as fragmens) ToDo: check this
      val keysToExclude =
      Seq("id", "type", "properties", "required", "oneOf", "anyOf", "allOf", "typeVariables", "genericTypes", "genericType")
      val fragmentsToKeep =
        keysToExclude.foldLeft[Map[String, JsValue]](jsObj.value.toMap) { (schemaMap, excludeKey) =>
          schemaMap - excludeKey
        }
      fragmentsToKeep collect {
        case (fragmentFieldName, fragment: JsObject) => (fragmentFieldName, Type(fragment))
      }
    }

    schemaObject match {
      case jsObj: JsObject => collectFromJsObjecct(jsObj)
      case _               => Map.empty
    }

  }

}