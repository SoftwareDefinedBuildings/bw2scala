package edu.berkeley.cs.sdb.bw2

import java.io.{OutputStream, InputStream}
import java.nio.charset.StandardCharsets

import scala.collection.mutable
import scala.util.{Success, Failure, Try, Random}

case class Frame(seqNo: Int, command: Command, kvPairs: Seq[(String, Array[Byte])] = Nil,
                 routingObjects: Seq[RoutingObject] = Nil, payloadObjects: Seq[PayloadObject] = Nil) {
  def writeToStream(stream: OutputStream): Unit = {
    val header = String.format("%s 0000000000 %010d\n", command.code, seqNo)
    stream.write(header.getBytes(StandardCharsets.UTF_8))

    kvPairs foreach { case (key, value) =>
      val kvHeader = String.format("kv %s %d\n", key, value.length)
      stream.write(kvHeader.getBytes(StandardCharsets.UTF_8))
      stream.write(value)
      stream.write('\n')
    }
    routingObjects foreach(_.writeToStream(stream))
    payloadObjects foreach(_.writeToStream(stream))

    stream.write("end\n".getBytes(StandardCharsets.UTF_8))
  }
}

case class InvalidFrameException(msg: String) extends Exception

object Frame {
  private val BW_HEADER_LEN = 27

  val random = new Random()

  def generateSequenceNumber: Int =
    Math.abs(random.nextInt())

  def readFromStream(stream: InputStream): Try[Frame] = {
    val headerBytes = new Array[Byte](BW_HEADER_LEN)
    stream.read(headerBytes, 0, BW_HEADER_LEN)
    val frameHeader = new String(headerBytes, StandardCharsets.UTF_8)

    val headerTokens = frameHeader.split(" ")
    if (headerTokens.length != 3) {
      return Failure(new InvalidFrameException("Frame header must contain 3 fields"))
    }
    val commandCode = headerTokens(0)
    val command = Command.fromString(commandCode)
    if (command.isEmpty) {
      return Failure(new InvalidFrameException("Frame header contains invalid command " + commandCode))
    }

    val lenStr = headerTokens(1)
    val frameLength = Try(lenStr.toInt)
    if (frameLength.isFailure) {
      return Failure(new InvalidFrameException("Frame header contains invalid length " + lenStr))
    }
    if (frameLength.get < 0) {
      return Failure(new InvalidFrameException("Frame header contains negative length"))
    }

    val seqNoStr = headerTokens(2)
    val seqNo = Try(seqNoStr.toInt)
    if (seqNo.isFailure) {
      return Failure(new InvalidFrameException("Frame header contains invalid sequence number " + seqNoStr))
    }

    var currentLine = readLineFromStream(stream)
    val kvPairs = new mutable.ArrayBuffer[(String, Array[Byte])]
    val routingObjects = new mutable.ArrayBuffer[RoutingObject]
    val payloadObjects = new mutable.ArrayBuffer[PayloadObject]

    while (currentLine != "end\n") {
      val tokens = currentLine.split(" ")
      val length = Try(tokens(1).toInt)
      if (length.isFailure) {
        return Failure(new InvalidFrameException("Invalid length in item header: " + currentLine))
      }
      if (length.get < 0) {
        return Failure(new InvalidFrameException("Negative length in item header: " + currentLine))
      }

      tokens(0) match {
        case "kv" =>
          val key = tokens(1)
          val body = new Array[Byte](length.get)
          stream.read(body, 0, length.get)
          kvPairs.append((key, body))
          // Remove trailing '\n'
          stream.read()

        case "ro" =>
          val routingObjNumStr = tokens(1)
          val routingObjNum = Try(routingObjNumStr.toInt)
          if (routingObjNum.isFailure) {
            return Failure(new InvalidFrameException("Invalid routing object number: " + routingObjNumStr))
          }
          if (routingObjNum.get < 0 || routingObjNum.get > 255) {
            return Failure(new InvalidFrameException(
              "Routing object number outside of acceptable range: " + routingObjNumStr))
          }

          val body = new Array[Byte](length.get)
          stream.read(body, 0, length.get)
          val ro = new RoutingObject(routingObjNum.get, body)
          routingObjects += ro

          // Strip trailing '\n'
          stream.read()

        case "po" =>
          val payloadTypeStr = tokens(1)
          val parsedType = PayloadObject.typeFromString(payloadTypeStr)
          if (parsedType.isFailure) {
            return Failure(new InvalidFrameException("Invalid payload object type: " + payloadTypeStr))
          }
          val (poTypeOctet, poTypeNum) = parsedType.get
          val body = new Array[Byte](length.get)
          stream.read(body, 0, length.get)
          val po = new PayloadObject(poTypeOctet, poTypeNum, body)
          payloadObjects += po

           // Strip trailing '\n'
           stream.read()

        case _ =>
          return Failure(new InvalidFrameException("Invalid frame item header: " + currentLine))
      }
      currentLine = readLineFromStream(stream)
    }

    Success(new Frame(seqNo.get, command.get, kvPairs.toSeq, routingObjects.toSeq, payloadObjects.toSeq))
  }

  private def readUntil(stream: InputStream, end: Byte): Array[Byte] = {
    val buffer = new mutable.ArrayBuffer[Byte]()
    var b = stream.read().toByte
    while (b != -1 && b != end) {
      buffer.append(b)
      b = stream.read().toByte
    }
    if (b != -1) {
      // Add the end byte
      buffer += b
    }

    buffer.toArray
  }

  private def readLineFromStream(stream: InputStream): String = {
    val bytes = readUntil(stream, '\n')
    if (bytes.last == '\n') {
      new String(bytes.dropRight(1), StandardCharsets.UTF_8)
    } else {
      new String(bytes, StandardCharsets.UTF_8)
    }
  }
}
