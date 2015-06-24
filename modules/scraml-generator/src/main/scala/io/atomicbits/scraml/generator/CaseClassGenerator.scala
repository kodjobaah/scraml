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

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.jsonschemaparser.{AbsoluteId, SchemaLookup}
import io.atomicbits.scraml.jsonschemaparser.model._

import scala.reflect.macros.whitebox
import scala.language.experimental.macros


/**
 * Created by peter on 4/06/15, Atomic BITS (http://atomicbits.io).
 *
 * JSON schema and referencing:
 * http://json-schema.org/latest/json-schema-core.html
 * http://tools.ietf.org/html/draft-zyp-json-schema-03
 * http://spacetelescope.github.io/understanding-json-schema/structuring.html
 * http://forums.raml.org/t/how-do-you-reference-another-schema-from-a-schema/485
 *
 */
object CaseClassGenerator {

  def generateCaseClasses(schemaLookup: SchemaLookup, c: whitebox.Context) = {

    // Expand all canonical names into their case class definitions.

    val caseClasses = schemaLookup.objectMap.keys.toList.map { key =>
      generateCaseClassWithCompanionObject(
        schemaLookup.canonicalNames(key).name,
        schemaLookup.objectMap(key),
        schemaLookup,
        c
      )
    }

    caseClasses.flatten

  }

  def generateCaseClassWithCompanionObject(canonicalName: String,
                                           objectEl: ObjectEl,
                                           schemaLookup: SchemaLookup,
                                           c: whitebox.Context): List[c.universe.Tree] = {

    println(s"Generating case class for: $canonicalName")

    import c.universe._

    def expandFieldName(fieldName: TermName,
                        typeName: Tree,
                        required: Boolean): Tree = {
      if (required) {
        q"val $fieldName: $typeName"
      } else {
        q"val $fieldName: Option[$typeName]"
      }
    }

    def fetchTypeNameFor(schema: Schema, schemaLookup: SchemaLookup): Tree = {

      // ToDo: this code to get the absolute id appears everywhere, we must find a way to refactor this!
      val absoluteId = schema.id match {
        case absId: AbsoluteId => absId
        case _ => sys.error("All schema references must be absolute for case class generation.")
      }

      schema match {
        case objEl: ObjectEl =>
          val typeName = TypeName(schemaLookup.canonicalNames(absoluteId).name)
          val q"val foo: $objectType" = q"val foo: $typeName"
          objectType
        case arrEl: ArrayEl =>
          val q"val foo: $listType" = q"val foo: List[${fetchTypeNameFor(arrEl.items, schemaLookup)}]"
          listType
        case stringEl: StringEl =>
          val q"val foo: $stringType" = q"val foo: String"
          stringType
        case numberEl: NumberEl =>
          val q"val foo: $doubleType" = q"val foo: Double"
          doubleType
        case integerEl: IntegerEl =>
          val q"val foo: $intType" = q"val foo: Int"
          intType
        case booleanEl: BooleanEl =>
          val q"val foo: $booleanType" = q"val foo: Boolean"
          booleanType
        case schemaRef: SchemaReference => fetchTypeNameFor(schemaLookup.lookupSchema(schemaRef.refersTo), schemaLookup)
        case enumEl: EnumEl =>
          val typeName = TypeName(schemaLookup.canonicalNames(absoluteId).name)
          val q"val foo: $enumType" = q"val foo: $typeName"
          enumType
        case otherSchema => sys.error(s"Cannot transform schema with id ${otherSchema.id} to a List type parameter.")
      }
    }

    def toField(property: (String, Schema), requiredFields: List[String]): Tree = {

      import c.universe._

      val (propertyName, schema) = property
      val cleanName = cleanFieldName(propertyName)

      val absoluteId = schema.id match {
        case absId: AbsoluteId => absId
        case _ => sys.error("All schema references must be absolute for case class generation.")
      }

      val field =
        schema match {
          case objField: AllowedAsObjectField =>
            val required = requiredFields.contains(propertyName) || objField.required
            expandFieldName(TermName(cleanName), fetchTypeNameFor(objField, schemaLookup), required)
          case noObjectField =>
            sys.error(s"Cannot transform schema with id ${noObjectField.id} to a case class field.")
        }

      field
    }

    val className = TypeName(canonicalName)
    val classAsTermName = TermName(canonicalName)
    val caseClassFields: List[Tree] = objectEl.properties.toList.map(toField(_, objectEl.requiredFields))

    List(
      q"""
       case class $className(..$caseClassFields)
     """,
      q"""
       object $classAsTermName {

         implicit val jsonFormatter: Format[$className] = Json.format[$className]

       }
     """)

  }




  private def cleanFieldName(propertyName: String): String = {

    def unCapitalize(name: String): String =
      if (name.length == 0) ""
      else if (name.charAt(0).isLower) name
      else {
        val chars = name.toCharArray
        chars(0) = chars(0).toLower
        new String(chars)
      }

    // capitalize after special characters and drop those characters along the way
    val capitalizedAfterDropChars =
      List('-', '_', '+', ' ').foldLeft(propertyName) { (cleaned, dropChar) =>
        cleaned.split(dropChar).filter(_.nonEmpty).map(_.capitalize).mkString("")
      }
    // capitalize after numbers 0 to 9, but keep the numbers
    val capitalized =
      (0 to 9).map(_.toString.head).toList.foldLeft(capitalizedAfterDropChars) { (cleaned, numberChar) =>
        // Make sure we don't drop the occurrences of numberChar at the end by adding a space and removing it later.
        val cleanedWorker = s"$cleaned "
        cleanedWorker.split(numberChar).map(_.capitalize).mkString(numberChar.toString).stripSuffix(" ")
      }
    // Make the first charachter a lower case to get a camel case name and do some
    // final cleanup of all strange characters
    unCapitalize(capitalized).replaceAll("[^A-Za-z0-9]", "")
  }

}
