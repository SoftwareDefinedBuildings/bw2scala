package edu.berkeley.cs.sdb.bw2

import java.io.{FileInputStream, BufferedInputStream, File}
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class BosswaveClient(val hostName: String, val port: Int) {
  val socket = new Socket(hostName, port)
  val inStream = socket.getInputStream
  val outStream = socket.getOutputStream

  // Check that we receive a well-formed acknowledgment

  val responseHandlers = new mutable.HashMap[Int, Response => Unit]()
  val messageHandlers = new mutable.HashMap[Int, Message => Unit]()

  val RFC_3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")

  def close(): Unit = {
    inStream.close()
    outStream.close()
    socket.close()
  }

  def setEntity(key: Array[Byte], handler: Response => Unit): Unit = {
    val seqNo = Frame.generateSequenceNumber
    val po = new PayloadObject(Some((1, 0, 1, 2)), None, key)
    val frame = new Frame(seqNo, SET_ENTITY, payloadObjects = Seq(po))
    frame.writeToStream(outStream)
    outStream.flush()
    installResponseHandler(seqNo, handler)
  }

  def setEntityFile(f: File, handler: Response => Unit): Unit = {
    val stream = new BufferedInputStream(new FileInputStream(f))
    val keyFile = new Array[Byte]((f.length() - 1).toInt)
    stream.read() // Strip the first byte
    stream.read(keyFile, 0, keyFile.length)
    stream.close()

    setEntity(keyFile, handler)
  }

  def publish(uri: String, responseHandler: Option[Response => Unit] = None, persist: Boolean = false,
              primaryAccessChain: Option[String] = None, expiry: Option[Date] = None,
              expiryDelta: Option[Long] = None, elaboratePac: ElaborationLevel = UNSPECIFIED,
              autoChain: Boolean = false, routingObjects: Seq[RoutingObject] = Nil,
              payloadObjects: Seq[PayloadObject] = Nil): Unit = {
    val seqNo = Frame.generateSequenceNumber
    val command = if (persist) PERSIST else PUBLISH

    val kvPairs = new mutable.ArrayBuffer[(String, Array[Byte])]()
    kvPairs.append(("uri", uri.getBytes(StandardCharsets.UTF_8)))
    expiry foreach { exp =>
      kvPairs.append(("expiry", RFC_3339.format(exp).getBytes(StandardCharsets.UTF_8)))
    }
    expiryDelta foreach { expDelta =>
      kvPairs.append(("expiryDelta", String.format("%dms", expDelta).getBytes(StandardCharsets.UTF_8)))
    }
    primaryAccessChain foreach { pac =>
      kvPairs.append(("primary_access_chain", pac.getBytes(StandardCharsets.UTF_8)))
    }
    if (elaboratePac != UNSPECIFIED) {
      kvPairs.append(("elaborate_pac", elaboratePac.name.getBytes(StandardCharsets.UTF_8)))
    }
    if (autoChain) {
      kvPairs.append(("auto_chain", "true".getBytes(StandardCharsets.UTF_8)))
    }

    val frame = new Frame(seqNo, command, kvPairs.toSeq, routingObjects, payloadObjects)
    frame.writeToStream(outStream)
    outStream.flush()
    responseHandler foreach { handler =>
      installResponseHandler(seqNo, handler)
    }
  }

  def subscribe(uri: String, responseHandler: Option[Response => Unit] = None,
                messageHandler: Option[Message => Unit] = None, expiry: Option[Date] = None,
                expiryDelta: Option[Long] = None, primaryAccessChain: Option[String] = None,
                elaboratePac: ElaborationLevel = UNSPECIFIED, autoChain: Boolean = false, leavePacked: Boolean = false,
                routingObjects: Seq[RoutingObject] = Nil, payloadObjects: Seq[PayloadObject] = Nil): Unit = {
    val seqNo = Frame.generateSequenceNumber

    val kvPairs = new mutable.ArrayBuffer[(String, Array[Byte])]()
    kvPairs.append(("uri", uri.getBytes(StandardCharsets.UTF_8)))
    expiry foreach { exp =>
      kvPairs.append(("expiry", RFC_3339.format(exp).getBytes(StandardCharsets.UTF_8)))
    }
    expiryDelta foreach { expDelta =>
      kvPairs.append(("expiryDelta", String.format("%dms", expDelta).getBytes(StandardCharsets.UTF_8)))
    }
    primaryAccessChain foreach { pac =>
      kvPairs.append(("primary_access_chain", pac.getBytes(StandardCharsets.UTF_8)))
    }
    if (elaboratePac != UNSPECIFIED) {
      kvPairs.append(("elaborate_pac", elaboratePac.name.getBytes(StandardCharsets.UTF_8)))
    }
    if (autoChain) {
      kvPairs.append(("auto_chain", "true".getBytes(StandardCharsets.UTF_8)))
    }
    if (!leavePacked) {
      kvPairs.append(("unpack", "true".getBytes(StandardCharsets.UTF_8)))
    }

    val frame = new Frame(seqNo, SUBSCRIBE, kvPairs.toSeq, routingObjects, payloadObjects)
    frame.writeToStream(outStream)
    outStream.flush()
    responseHandler foreach { handler =>
      installResponseHandler(seqNo, handler)
    }
    messageHandler foreach { handler =>
      installMessageHandler(seqNo, handler)
    }
  }

  private def installResponseHandler(seqNo: Int, handler: Response => Unit): Unit = {
    responseHandlers.synchronized {
      responseHandlers.put(seqNo, handler)
    }
  }

  private def installMessageHandler(seqNo: Int, handler: Message => Unit): Unit = {
    messageHandlers.synchronized {
      messageHandlers.put(seqNo, handler)
    }
  }

  private class BWListener extends Runnable {
    override def run(): Unit = {
      while (true) {
        val nextFrame = Frame.readFromStream(inStream)
        nextFrame match {
          case Failure(InvalidFrameException(msg)) =>
            println(msg) // We can safely ignore invalid frames

          case Failure(e) =>
            throw new RuntimeException(e)

          case Success(Frame(seqNo, RESPONSE, kvPairs, _, _)) =>
            responseHandlers.synchronized { responseHandlers.get(seqNo) } foreach { handler =>
              val (_, rawStatus) = kvPairs.find { case (key, _) => key == "status" }.get
              val status = new String(rawStatus, StandardCharsets.UTF_8)
              val (_, rawReason) = kvPairs.find { case (key, _) => key == "reason" }.get
              val reason = new String(rawReason, StandardCharsets.UTF_8)
              handler.apply(new Response(status, reason))
            }

          case Success(Frame(seqNo, RESULT, kvPairs, routingObjects, payloadObjects)) =>
            messageHandlers.synchronized { messageHandlers.get(seqNo) } foreach { handler =>
              val (_, rawUri) = kvPairs.find { case (key, _) => key == "status" }.get
              val uri = new String(rawUri, StandardCharsets.UTF_8)
              val (_, rawFrom) = kvPairs.find { case (key, _) => key == "status" }.get
              val from = new String(rawFrom, StandardCharsets.UTF_8)

              val unpack = kvPairs.find { case (key, _) => key == "unpack" } match {
                case None => true
                case Some(_, rawUnpack) => Try(new String(rawUnpack, StandardCharsets.UTF_8).toBoolean) match {
                  case Failure(_) => true
                  case Success(bool) => bool
                }
              }

              val message = if (unpack) {
                new Message(from, uri, routingObjects, payloadObjects)
              } else {
                new Message(from, uri, Nil, Nil)
              }

              handler.apply(message)
            }
        }
      }
    }
  }
}

case class Message(from: String, to: String, routingObjects: Seq[RoutingObject], payloadObjects: Seq[PayloadObject])
case class Response(status: String, reason: String)
