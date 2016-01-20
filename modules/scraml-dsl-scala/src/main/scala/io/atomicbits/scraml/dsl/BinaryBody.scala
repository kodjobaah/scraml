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

package io.atomicbits.scraml.dsl

import _root_.java.io.{InputStream, File}


/**
 * Created by peter on 15/01/16.
 */
sealed trait BinaryBody

case class FileBinaryBody(file: File) extends BinaryBody

case class InputStreamBinaryBody(inputStream: InputStream) extends BinaryBody

case class ByteArrayBinaryBody(byteArray: Array[Byte]) extends BinaryBody

case class StringBinaryBody(text: String) extends BinaryBody


object BinaryBody {

  def apply(file: File): BinaryBody = FileBinaryBody(file)

  def apply(inputStream: InputStream): BinaryBody = InputStreamBinaryBody(inputStream)

  def apply(byteArray: Array[Byte]): BinaryBody = ByteArrayBinaryBody(byteArray)

  def apply(text: String): BinaryBody = StringBinaryBody(text)

}
