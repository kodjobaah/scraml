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

package io.atomicbits.scraml.ramlparser.parser


import io.atomicbits.scraml.ramlparser.model.Raml
import play.api.libs.json._


/**
  * Created by peter on 6/02/16.
  */
case class RamlParser(ramlSource: String, charsetName: String) {


  def parse = {
    val ramlJson = RamlJsonParser.parseToJson(ramlSource, charsetName)
    val parsed =
      ramlJson match {
        case ramlJsObj: JsObject => ramlJsObj // parseRamlJsonDocument(ramlJsObj)
        case x                   => sys.error(s"Could not parse $ramlSource, expected a RAML document.")
      }
    parsed
//    Raml(parsed)
  }


  private def parseRamlJsonDocument(raml: JsObject): Unit = {

    def parseNested(doc: JsValue) = {

    }



  }




}