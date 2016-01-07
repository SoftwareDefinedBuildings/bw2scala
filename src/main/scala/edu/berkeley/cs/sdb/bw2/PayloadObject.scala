package edu.berkeley.cs.sdb.bw2

import java.io.OutputStream
import java.nio.charset.StandardCharsets

import scala.util.{Failure, Success, Try}

case class PayloadObject(octet: Option[(Int, Int, Int, Int)], number: Option[Int], content: Array[Byte]) {
  require(octet.isDefined || number.isDefined)

  private def typeToString: String =
    octet match {
      case None => f":${number.get}%d"
      case Some(oct) =>
        number match {
          case None => f"${oct._1}%d.${oct._2}%d.${oct._3}%d.${oct._4}%d:"
          case Some(num) => f"${oct._1}%d.${oct._2}%d.${oct._3}%d.${oct._4}%d:$num%d"
        }
    }

  def writeToStream(stream: OutputStream): Unit = {
    val header = f"po $typeToString%s ${content.length}%d\n"
    stream.write(header.getBytes(StandardCharsets.UTF_8))
    stream.write(content)
    stream.write('\n')
  }
}

object PayloadObject {
  private def parseOctet(s: String): Try[(Int, Int, Int, Int)] = {
    val tokens = s.split('.')
    if (tokens.length != 4) {
      return Failure(new IllegalArgumentException("Octet must contain 4 elements"))
    }

    val parsedTokens = tokens map (x => Try(x.toInt))
    val errIndex = parsedTokens.indexWhere(_.isFailure)
    if (errIndex >= 0) {
      return Failure(new IllegalArgumentException("Invalid octet element: " + tokens(errIndex)))
    }
    val negativeIndex = parsedTokens.indexWhere(_.get < 0)
    if (negativeIndex >= 0) {
      return Failure(new IllegalArgumentException("Negative octet element: " + tokens(errIndex)))
    }

    val octetElems = parsedTokens map(_.get)
    Success((octetElems(0), octetElems(1), octetElems(2), octetElems(3)))
  }

  def typeFromString(s: String): Try[(Option[(Int, Int, Int, Int)], Option[Int])] = {
    if (s.startsWith(":")) {
      val poNum = Try(s.substring(1).toInt)
      if (poNum.isFailure) {
        Failure(new IllegalArgumentException("Payload object type contains invalid number"))
      } else if (poNum.get < 0 || poNum.get > 99) {
        Failure(new IllegalArgumentException("Payload object type number must contain 1 or 2 digits"))
      } else {
        Success((None, Some(poNum.get)))
      }

    } else if (s.endsWith(":")) {
      val octet = parseOctet(s.substring(0, s.length - 1))
      if (octet.isFailure) {
        Failure(new IllegalArgumentException("Payload object type contains invalid octet"))
      } else {
        Success((Some(octet.get), None))
      }
    } else {
      val tokens = s.split(":")
      if (tokens.length != 2) {
        return Failure(new IllegalArgumentException("Malformed payload object type: " + s))
      }

      val octet = parseOctet(tokens(0))
      if (octet.isFailure) {
        return Failure(new IllegalArgumentException("Payload object type contains invalid octet"))
      }

      val poNum = Try(tokens(1).toInt)
      if (poNum.isFailure) {
        return Failure(new IllegalArgumentException("Payload object type contains invalid number"))
      }
      if (poNum.get < 0 || poNum.get > 99) {
        return Failure(new IllegalArgumentException("Payload object type number must contain 1 or 2 digits"))
      }

      Success((Some(octet.get), Some(poNum.get)))
    }
  }
}
