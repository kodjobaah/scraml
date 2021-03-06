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

package io.atomicbits.scraml.generator.model

import io.atomicbits.scraml.generator.model.ClassRep.ClassMap
import io.atomicbits.scraml.generator.util.CleanNameUtil

import scala.annotation.tailrec

/**
  * Created by peter on 21/08/15.
  */

trait ClassRep {

  def name = classRef.name

  def packageParts = classRef.packageParts

  def packageName = classRef.packageName

  def typeParameters = classRef.typeParameters

  def fullyQualifiedName = classRef.fullyQualifiedName

  def classDefinitionScala = classRef.classDefinitionScala

  def classDefinitionJava = classRef.classDefinitionJava

  def classRef: ClassReference

  def fields: List[Field]

  def parentClass: Option[ClassReference]

  def subClasses: List[ClassReference]

  def content: Option[String]

  def jsonTypeInfo: Option[JsonTypeInfo]

  def withFields(fields: List[Field]): ClassRep

  def withParent(parentId: ClassReference): ClassRep

  def withChildren(childIds: List[ClassReference]): ClassRep

  def withContent(content: String): ClassRep

  def withJsonTypeInfo(jsonTypeInfo: JsonTypeInfo): ClassRep

  def isInHierarchy: Boolean = parentClass.isDefined || subClasses.nonEmpty

  /**
    * Gives the top level parent of the hierarchy this class rep takes part in if any. If this class rep is the top level class,
    * it will be returned as the result (as opposed to the method topLevelParent).
    */
  def hierarchyParent(classMap: ClassMap): Option[ClassRep] = {
    if (parentClass.isEmpty && subClasses.nonEmpty) Some(this)
    else topLevelParent(classMap)
  }

  /**
    * Gives the top level parent of this class rep. A top level parent class itself has no parent and thus no top level parent.
    */
  def topLevelParent(classMap: ClassMap): Option[ClassRep] = {

    @tailrec
    def findTopLevelParent(parentId: ClassReference): ClassRep = {
      val parentClass = classMap(parentId)
      parentClass.parentClass match {
        case Some(prntId) if prntId == parentId =>
          sys.error(s"Class $prntId has itself as parent. Did you forget to define an ID on one of the child classes?")
        case Some(prntId)                       => findTopLevelParent(prntId)
        case None                               => parentClass
      }
    }

    parentClass.map(findTopLevelParent)

  }

}


case class EnumValuesClassRep(classRef: ClassReference,
                              values: List[String] = List.empty,
                              parentClass: Option[ClassReference] = None,
                              subClasses: List[ClassReference] = List.empty,
                              content: Option[String] = None,
                              jsonTypeInfo: Option[JsonTypeInfo] = None) extends ClassRep {

  val fields: List[Field] = Nil

  def withFields(fields: List[Field]): ClassRep = sys.error("An EnumValueclassRep has no fields")

  def withContent(content: String): ClassRep = copy(content = Some(content))

  def withParent(parentId: ClassReference): ClassRep = copy(parentClass = Some(parentId))

  def withChildren(childIds: List[ClassReference]): ClassRep = copy(subClasses = childIds)

  def withJsonTypeInfo(jsonTypeInfo: JsonTypeInfo): ClassRep = copy(jsonTypeInfo = Some(jsonTypeInfo))

}

object EnumValuesClassRep {

  def apply(classRef: ClassReference, values: List[String]): EnumValuesClassRep =
    new EnumValuesClassRep(classRef = classRef, values = values)

}


case class CommonClassRep(classRef: ClassReference,
                          fields: List[Field] = List.empty,
                          parentClass: Option[ClassReference] = None,
                          subClasses: List[ClassReference] = List.empty,
                          content: Option[String] = None,
                          jsonTypeInfo: Option[JsonTypeInfo] = None) extends ClassRep {

  def withFields(fields: List[Field]): ClassRep = copy(fields = fields)

  def withContent(content: String): ClassRep = copy(content = Some(content))

  def withParent(parentId: ClassReference): ClassRep = copy(parentClass = Some(parentId))

  def withChildren(childIds: List[ClassReference]): ClassRep = copy(subClasses = childIds)

  def withJsonTypeInfo(jsonTypeInfo: JsonTypeInfo): ClassRep = copy(jsonTypeInfo = Some(jsonTypeInfo))

}


case class Field(fieldName: String, classPointer: ClassPointer, required: Boolean) {

  def fieldExpressionScala: String =
    if (required) s"$safeFieldNameScala: ${classPointer.classDefinitionScala}"
    else s"$safeFieldNameScala: Option[${classPointer.classDefinitionScala}] = None"

  def fieldFormatUnliftScala: String =
    if (required)
      s""" (__ \\ "$fieldName").format[${classPointer.classDefinitionScala}]"""
    else
      s""" (__ \\ "$fieldName").formatNullable[${classPointer.classDefinitionScala}]"""

  lazy val safeFieldNameScala: String = {

    val cleanName = CleanNameUtil.cleanFieldName(fieldName)

    CleanNameUtil.escapeScalaKeyword(cleanName)
  }


  def fieldExpressionJava: String = s"${classPointer.classDefinitionJava} $safeFieldNameJava"

  lazy val safeFieldNameJava: String = {

    val cleanName = CleanNameUtil.cleanFieldName(fieldName)

    CleanNameUtil.escapeJavaKeyword(cleanName)
  }

}


case class JsonTypeInfo(discriminator: String, discriminatorValue: Option[String])

object ClassRep {

  type ClassMap = Map[ClassReference, ClassRep]

  /**
    *
    * @param classReference The class reference for the class representation.
    * @param fields         The public fields for this class rep (to become a scala case class or java pojo).
    * @param parentClass    The class rep of the parent class of this class rep.
    * @param subClasses     The class reps of the children of this class rep.
    * @param content        The source content of the class.
    * @param jsonTypeInfo   Info about JSON-typing of case classes.
    */
  def apply(classReference: ClassReference,
            fields: List[Field] = List.empty,
            parentClass: Option[ClassReference] = None,
            subClasses: List[ClassReference] = List.empty,
            content: Option[String] = None,
            jsonTypeInfo: Option[JsonTypeInfo] = None): ClassRep = {

    CommonClassRep(classReference, fields, parentClass, subClasses, content, jsonTypeInfo)

  }

}