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

package io.atomicbits.scraml.generator.codegen

import io.atomicbits.scraml.generator.model._
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.ramlparser.model.Method

/**
  * Created by peter on 23/08/15.
  */
case class ActionGenerator(actionCode: ActionCode) {


  /**
    * The reason why we treat all actions of a resource together is that certain paths towards the actual action
    * execution of the resource's actions may be overlapping when it concerns actions that have overlapping mandatory
    * content-type and/or accept header paths. Although such situations may be rare, we want to support them well,
    * so we pass all actions of a single resource together.
    *
    * @param resource The resource whose actions are going to be processed (NOT recursively!)
    * @return A list of action function definitions or action paths that lead to the action function. Action paths will only be
    *         required if multiple contenttype and/or accept headers will lead to a different typed body and/or response (we
    *         don't support those yet, but we will do so in the future).
    */
  def generateActionFunctions(resource: RichResource)(implicit lang: Language): ActionFunctionResult = {

    val actions: List[RichAction] = resource.actions

    val actionsWithSafeContentAndResponseTypes =
      actions map {
        case action if action.contentTypes.isEmpty => action.copy(contentTypes = Set(NoContentType))
        case action                                => action
      } map {
        case action if action.responseTypes.isEmpty => action.copy(responseTypes = Set(NoResponseType))
        case action                                 => action
      }

    val actionsWithTypeSelection: List[RichAction] =
      actionsWithSafeContentAndResponseTypes.flatMap { action =>
        for {
          contentType <- action.contentTypes
          responseType <- action.responseTypes
          actionWithTypeSelection = action.copy(selectedContentType = contentType, selectedResponsetype = responseType)
        } yield actionWithTypeSelection
      }

    val groupedByActionType: Map[Method, List[RichAction]] = actionsWithTypeSelection.groupBy(_.actionType)

    // now, we have to map the actions onto a segment path if necessary
    val actionPathToAction: List[ActionPath] =
      groupedByActionType.values flatMap {
        case actionOfKindList@(aok :: Nil)  => List(ActionPath(NoContentHeaderSegment, NoAcceptHeaderSegment, actionOfKindList.head))
        case actionOfKindList@(aok :: aoks) =>
          actionOfKindList map { actionOfKind =>
            val contentHeader =
              actionOfKind.selectedContentType match {
                case NoContentType   => NoContentHeaderSegment
                case ct: ContentType => ActualContentHeaderSegment(ct)
              }
            val acceptHeader =
              actionOfKind.selectedResponsetype match {
                case NoResponseType   => NoAcceptHeaderSegment
                case rt: ResponseType => ActualAcceptHeaderSegment(rt)
              }
            ActionPath(contentHeader, acceptHeader, actionOfKind)
          }
      } toList

    val uniqueActionPaths: Map[ContentHeaderSegment, Map[AcceptHeaderSegment, List[RichAction]]] =
      actionPathToAction
        .groupBy(_.contentHeader)
        .mapValues(_.groupBy(_.acceptHeader))
        .mapValues(_.mapValues(_.map(_.action)))

    val baseClassReference = resource.classRep.classRef

    val actionPathExpansion: List[ActionFunctionResult] =
      uniqueActionPaths.toList map {
        case (NoContentHeaderSegment, acceptHeaderMap) =>
          expandAcceptHeaderMap(baseClassReference, acceptHeaderMap)

        case (ActualContentHeaderSegment(contentType), acceptHeaderMap) =>
          expandContentTypePath(baseClassReference, contentType, acceptHeaderMap)
      }

    if (actionPathExpansion.nonEmpty) actionPathExpansion.reduce(_ ++ _)
    else ActionFunctionResult()
  }


  private def expandContentTypePath(baseClassRef: ClassReference,
                                    contentType: ContentType,
                                    acceptHeaderMap: Map[AcceptHeaderSegment, List[RichAction]])
                                   (implicit lang: Language): ActionFunctionResult = {

    // create the content type path class extending a HeaderSegment and add the class to the List[ClassRep] result
    // add a content type path field that instantiates the above class (into the List[String] result)
    // add the List[String] results of the expansion of the acceptHeader map to source of the above class
    // add the List[ClassRep] results of the expansion of the acceptHeader map to the List[ClassRep] result

    val ActionFunctionResult(acceptSegmentMethodImports, acceptSegmentMethods, acceptHeaderClasses) =
      expandAcceptHeaderMap(baseClassRef, acceptHeaderMap)

    // Header segment classes have the same class name in Java as in Scala.
    val headerSegmentClassName = s"Content${CleanNameUtil.cleanClassName(contentType.contentTypeHeader.value)}HeaderSegment"
    val headerSegment: ClassRep =
      createHeaderSegment(baseClassRef.packageParts, headerSegmentClassName, acceptSegmentMethodImports, acceptSegmentMethods)

    val contentHeaderMethodName = s"content${CleanNameUtil.cleanClassName(contentType.contentTypeHeader.value)}"
    val contentHeaderSegment: String = actionCode.contentHeaderSegmentField(contentHeaderMethodName, headerSegment)

    ActionFunctionResult(imports = Set.empty, fields = List(contentHeaderSegment), classes = headerSegment :: acceptHeaderClasses)
  }


  private def expandAcceptHeaderMap(baseClassRef: ClassReference,
                                    acceptHeaderMap: Map[AcceptHeaderSegment, List[RichAction]])
                                   (implicit lang: Language): ActionFunctionResult = {

    val actionPathExpansion: List[ActionFunctionResult] =
      acceptHeaderMap.toList match {
        case (_, actions) :: Nil =>
          List(
            ActionFunctionResult(
              actions.toSet.flatMap(generateActionImports),
              actions.flatMap(ActionFunctionGenerator(actionCode).generate),
              List.empty[ClassRep]
            )
          )
        case ahMap@(ah :: ahs)   =>
          ahMap map {
            case (NoAcceptHeaderSegment, actions)                   =>
              ActionFunctionResult(
                actions.toSet.flatMap(generateActionImports),
                actions.flatMap(ActionFunctionGenerator(actionCode).generate),
                List.empty[ClassRep]
              )
            case (ActualAcceptHeaderSegment(responseType), actions) => expandResponseTypePath(baseClassRef, responseType, actions)
          }
      }

    if (actionPathExpansion.nonEmpty) actionPathExpansion.reduce(_ ++ _)
    else ActionFunctionResult()
  }


  private def expandResponseTypePath(baseClassRef: ClassReference,
                                     responseType: ResponseType,
                                     actions: List[RichAction])
                                    (implicit lang: Language): ActionFunctionResult = {

    // create the result type path class extending a HeaderSegment and add the class to the List[ClassRep] result
    // add a result type path field that instantiates the above class (into the List[String] result)
    // add the List[String] results of the expansion of the actions to the above class and also add the imports needed by the actions
    // into the above class

    val actionImports = actions.toSet.flatMap(generateActionImports)
    val actionMethods = actions.flatMap(ActionFunctionGenerator(actionCode).generate)

    // Header segment classes have the same class name in Java as in Scala.
    val headerSegmentClassName = s"Accept${CleanNameUtil.cleanClassName(responseType.acceptHeader.value)}HeaderSegment"
    val headerSegment: ClassRep =
      createHeaderSegment(baseClassRef.packageParts, headerSegmentClassName, actionImports, actionMethods)

    val acceptHeaderMethodName = s"accept${CleanNameUtil.cleanClassName(responseType.acceptHeader.value)}"
    val acceptHeaderSegment: String = actionCode.contentHeaderSegmentField(acceptHeaderMethodName, headerSegment)

    ActionFunctionResult(imports = Set.empty, fields = List(acceptHeaderSegment), classes = List(headerSegment))
  }


  private def generateActionImports(action: RichAction)(implicit lang: Language): Set[String] = {

    def nonPredefinedImports(classReps: List[TypedClassReference]): Set[String] = {
      classReps match {
        case cr :: crs if !cr.classReference.predef =>
          nonPredefinedImports(cr.typeVariables.values.toList) ++ nonPredefinedImports(crs) + s"import ${cr.classReference.fullyQualifiedName}"
        case cr :: crs                              => nonPredefinedImports(cr.typeVariables.values.toList) ++ nonPredefinedImports(crs)
        case Nil                                    => Set()
      }
    }

    val contentTypeImports =
      action.selectedContentType match {
        case BinaryContentType(contentTypeHeader)          => nonPredefinedImports(List(BinaryDataClassReference().asTypedClassReference))
        case TypedContentType(contentTypeHeader, classRep) => nonPredefinedImports(List(classRep))
        case _                                             => Set.empty[String]
      }

    val responseTypeImports =
      action.selectedResponsetype match {
        case BinaryResponseType(acceptHeader)          => nonPredefinedImports(List(BinaryDataClassReference().asTypedClassReference))
        case TypedResponseType(acceptHeader, classRep) => nonPredefinedImports(List(classRep))
        case _                                         => Set.empty[String]
      }

    contentTypeImports ++ responseTypeImports
  }


  private def createHeaderSegment(packageParts: List[String],
                                  className: String,
                                  imports: Set[String],
                                  methods: List[String]): ClassRep = {

    val classReference = ClassReference(name = className, packageParts = packageParts)
    val classRep = ClassRep(classReference)

    val sourceCode = actionCode.headerSegmentClass(classReference, imports, methods)

    classRep.withContent(sourceCode)
  }


  // Helper class to represent the path from a resource to an action over a content header segment and a accept header segment.
  case class ActionPath(contentHeader: ContentHeaderSegment, acceptHeader: AcceptHeaderSegment, action: RichAction)


  sealed trait HeaderSegment

  sealed trait ContentHeaderSegment extends HeaderSegment

  sealed trait AcceptHeaderSegment extends HeaderSegment

  case object NoContentHeaderSegment extends ContentHeaderSegment

  case class ActualContentHeaderSegment(header: ContentType) extends ContentHeaderSegment

  case object NoAcceptHeaderSegment extends AcceptHeaderSegment

  case class ActualAcceptHeaderSegment(header: ResponseType) extends AcceptHeaderSegment

}
