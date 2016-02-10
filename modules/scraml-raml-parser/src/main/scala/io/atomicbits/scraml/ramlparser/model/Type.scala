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

import play.api.libs.json.JsObject

import scala.util.Try

/**
  * Created by peter on 10/02/16.
  */
trait Type {

  def name: String

  def baseType: String

}

case class ObjectType(name: String,
                      baseType: String,
                      properties: Map[String, PropertyDeclaration],
                      facets: Option[String]) extends Type


case class PropertyDeclaration(typeName: String, required: Boolean = true)


case class ArrayType(name: String,
                     baseType: String,  // e.g. "array", "Persion[]", "Persion[Dog[]]"
                     items: Option[String],  // e.g. Some(Person)
                     minItems: Int = 0,
                     maxItems: Option[Int] = None,
                     uniqueItems: Boolean = false) extends Type


case class ScalarType(name: String, baseType: String, enum: List[String], facets: Option[String]) extends Type


object Type {

  def apply(name: String, typeDefinition: JsObject): Try[Type] = {
    ???
  }

}