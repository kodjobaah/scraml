package io.atomicbits.scraml.generator.path

import io.atomicbits.scraml.generator.client.Client
import play.api.libs.json.Reads


/**
 * Created by peter on 21/05/15, Atomic BITS (http://atomicbits.io). 
 */
case class Request(client: Client,
                   reversePath: List[String] = Nil,
                   method: Method = Get,
                   queryParameters: Map[String, String] = Map.empty,
                   validAcceptHeaders: List[String] = Nil,
                   validContentTypeHeaders: List[String] = Nil,
                   headers: Map[String, String] = Map(),
                   body: Option[String] = None,
                   formatJsonResultBody: Boolean = false) {

  def relativePath = reversePath.reverse.mkString("/", "/", "")

  def execute() = client.execute(this)

  def executeToJson() = client.executeToJson(this)

  def executeToJsonDto[T]()(implicit reader: Reads[T]) = client.executeToJsonDto[T](this)

}