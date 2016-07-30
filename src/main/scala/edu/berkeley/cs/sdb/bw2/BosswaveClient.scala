package edu.berkeley.cs.sdb.bw2

import java.io.{BufferedInputStream, File, FileInputStream}
import java.net.{Socket, SocketException, SocketTimeoutException}
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object BosswaveClient {
  val BW_PORT = 28589 // Bosswave IANA Port Number
  private val SOCK_TIMEOUT_MS = 2000 // Frame read timeout length
  val RFC_3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
}

class BosswaveClient(val hostName: String = "localhost", val port: Int = BosswaveClient.BW_PORT) {
  private var autoChain: Option[Boolean] = None

  private val socket = new Socket(hostName, port)
  socket.setSoTimeout(BosswaveClient.SOCK_TIMEOUT_MS)
  private val inStream = socket.getInputStream
  private val outStream = socket.getOutputStream

  // Check that we receive a well-formed acknowledgment
  Frame.readFromStream(inStream) match {
    case Failure(e) =>
      close()
      throw new RuntimeException(e)
    case Success(Frame(_, HELLO, _, _, _))  => () // No further action is needed
    case Success(_) =>
      throw new RuntimeException("Received invalid Bosswave ACK")
  }

  // Start up thread to listen for incoming frames
  private val listener = new BWListener()
  private val listenerThread = new Thread(listener)
  listenerThread.start()

  private val responseHandlers = new mutable.HashMap[Int, BosswaveResponse => Unit]()
  private val messageHandlers = new mutable.HashMap[Int, BosswaveMessage => Unit]()
  private val listResultHandlers = new mutable.HashMap[Int, String => Unit]()

  def close(): Unit = {
    listener.stop()
    inStream.close()
    outStream.close()
    socket.close()
    listenerThread.join()
  }

  def overrideAutoChainTo(autoChain: Boolean) = {
    this.autoChain = Some(autoChain)
  }

  def setEntity(key: Array[Byte], responseHandler: Option[BosswaveResponse => Unit]): Unit = {
    val seqNo = Frame.generateSequenceNumber
    val po = new PayloadObject(Some((0, 0, 0, 50)), None, key)
    val frame = new Frame(seqNo, SET_ENTITY, payloadObjects = Seq(po))
    frame.writeToStream(outStream)
    outStream.flush()
    responseHandler foreach { handler =>
      installResponseHandler(seqNo, handler)
    }
  }

  def setEntityFile(f: File, responseHandler: Option[BosswaveResponse => Unit]): Unit = {
    val stream = new BufferedInputStream(new FileInputStream(f))
    val keyFile = new Array[Byte]((f.length() - 1).toInt)
    stream.read() // Strip the first byte
    stream.read(keyFile, 0, keyFile.length)
    stream.close()

    setEntity(keyFile, responseHandler)
  }

  def publish(uri: String, responseHandler: Option[BosswaveResponse => Unit] = None, persist: Boolean = false,
              primaryAccessChain: Option[String] = None, expiry: Option[Date] = None,
              expiryDelta: Option[Long] = None, elaboratePac: ElaborationLevel = UNSPECIFIED,
              autoChain: Boolean = false, routingObjects: Seq[RoutingObject] = Nil,
              payloadObjects: Seq[PayloadObject] = Nil): Unit = {
    val seqNo = Frame.generateSequenceNumber
    val command = if (persist) PERSIST else PUBLISH

    val kvPairs = new mutable.ArrayBuffer[(String, Array[Byte])]()
    kvPairs.append(("uri", uri.getBytes(StandardCharsets.UTF_8)))
    expiry foreach { exp =>
      kvPairs.append(("expiry", BosswaveClient.RFC_3339.format(exp).getBytes(StandardCharsets.UTF_8)))
    }
    expiryDelta foreach { expDelta =>
      kvPairs.append(("expiryDelta", f"$expDelta%dms".getBytes(StandardCharsets.UTF_8)))
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

    val frame = new Frame(seqNo, command, kvPairs, routingObjects, payloadObjects)
    frame.writeToStream(outStream)
    outStream.flush()
    responseHandler foreach { handler =>
      installResponseHandler(seqNo, handler)
    }
  }

  def subscribe(uri: String, responseHandler: Option[BosswaveResponse => Unit] = None,
                messageHandler: Option[BosswaveMessage => Unit] = None, expiry: Option[Date] = None,
                expiryDelta: Option[Long] = None, primaryAccessChain: Option[String] = None,
                elaboratePac: ElaborationLevel = UNSPECIFIED, autoChain: Boolean = false, leavePacked: Boolean = false,
                routingObjects: Seq[RoutingObject] = Nil): Unit = {
    val seqNo = Frame.generateSequenceNumber

    val kvPairs = new mutable.ArrayBuffer[(String, Array[Byte])]()
    kvPairs.append(("uri", uri.getBytes(StandardCharsets.UTF_8)))
    expiry foreach { exp =>
      kvPairs.append(("expiry", BosswaveClient.RFC_3339.format(exp).getBytes(StandardCharsets.UTF_8)))
    }
    expiryDelta foreach { expDelta =>
      kvPairs.append(("expiryDelta", f"$expDelta%dms".getBytes(StandardCharsets.UTF_8)))
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

    val frame = new Frame(seqNo, SUBSCRIBE, kvPairs, routingObjects, Nil)
    frame.writeToStream(outStream)
    outStream.flush()
    responseHandler foreach { handler =>
      installResponseHandler(seqNo, handler)
    }
    messageHandler foreach { handler =>
      installMessageHandler(seqNo, handler)
    }
  }

  def list(uri: String, responseHandler: Option[BosswaveResponse => Unit] = None,
           listResultHandler: Option[String => Unit] = None, expiry: Option[Date] = None,
           expiryDelta: Option[Long] = None, primaryAccessChain: Option[String] = None,
           elaboratePac: ElaborationLevel = UNSPECIFIED, autoChain: Boolean = false,
           routingObjects: Seq[RoutingObject] = Nil): Unit = {
    val seqNo = Frame.generateSequenceNumber

    val kvPairs = new mutable.ArrayBuffer[(String, Array[Byte])]()
    kvPairs.append(("uri", uri.getBytes(StandardCharsets.UTF_8)))
    expiry foreach { exp =>
      kvPairs.append(("expiry", BosswaveClient.RFC_3339.format(exp).getBytes(StandardCharsets.UTF_8)))
    }
    expiryDelta foreach { expDelta =>
      kvPairs.append(("expiryDelta", f"$expDelta%dms".getBytes(StandardCharsets.UTF_8)))
    }
    primaryAccessChain foreach { pac =>
      kvPairs.append(("primary_access_chain", pac.getBytes(StandardCharsets.UTF_8)))
    }
    if (elaboratePac != UNSPECIFIED) {
      kvPairs.append(("elaborate_pac", elaboratePac.name.getBytes(StandardCharsets.UTF_8)))
    }
    if (autoChain) {
      kvPairs.append(("autochain", "true".getBytes(StandardCharsets.UTF_8)))
    }

    val frame = new Frame(seqNo, LIST, kvPairs, routingObjects, Nil)
    frame.writeToStream(outStream)
    outStream.flush()
    responseHandler foreach { handler =>
      installResponseHandler(seqNo, handler)
    }
    listResultHandler foreach { handler =>
      installListResultHandler(seqNo, handler)
    }
  }

  def query(uri: String, responseHandler: Option[BosswaveResponse => Unit] = None,
            messageHandler: Option[BosswaveMessage => Unit] = None, expiry: Option[Date] = None,
            expiryDelta: Option[Long] = None, primaryAccessChain: Option[String] = None,
            elaboratePac: ElaborationLevel = UNSPECIFIED, autoChain: Boolean = false,
            leavePacked: Boolean = false, routingObjects: Seq[RoutingObject] = Nil): Unit = {
    val seqNo = Frame.generateSequenceNumber

    val kvPairs = new mutable.ArrayBuffer[(String, Array[Byte])]()
    kvPairs.append(("uri", uri.getBytes(StandardCharsets.UTF_8)))
    expiry foreach { exp =>
      kvPairs.append(("expiry", BosswaveClient.RFC_3339.format(exp).getBytes(StandardCharsets.UTF_8)))
    }
    expiryDelta foreach { expDelta =>
      kvPairs.append(("expiryDelta", f"$expDelta%dms".getBytes(StandardCharsets.UTF_8)))
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

    val frame = new Frame(seqNo, QUERY, kvPairs, routingObjects)
    frame.writeToStream(outStream)
    outStream.flush()
    responseHandler foreach { handler =>
      installResponseHandler(seqNo, handler)
    }
    messageHandler foreach { handler =>
      installMessageHandler(seqNo, handler)
    }
  }

  def makeEntity(responseHandler: Option[BosswaveResponse => Unit], messageHandler: Option[BosswaveMessage => Unit] = None,
                 contact: Option[String] = None, comment: Option[String] = None, expiry: Option[Date] = None,
                 expiryDelta: Option[Long] = None, revokers: Seq[String] = Nil,
                 omitCreationDate: Boolean = false): Unit = {
    val seqNo = Frame.generateSequenceNumber

    val kvPairs = new mutable.ArrayBuffer[(String, Array[Byte])]()
    contact foreach { ctct =>
      kvPairs.append(("contact", ctct.getBytes(StandardCharsets.UTF_8)))
    }
    comment foreach { cmmt =>
      kvPairs.append(("comment", cmmt.getBytes(StandardCharsets.UTF_8)))
    }
    expiry foreach { exp =>
      kvPairs.append(("expiry", BosswaveClient.RFC_3339.format(exp).getBytes(StandardCharsets.UTF_8)))
    }
    expiryDelta foreach { expDelta =>
      kvPairs.append(("expiryDelta", f"$expDelta%dms".getBytes(StandardCharsets.UTF_8)))
    }

    revokers foreach { revoker =>
      kvPairs.append(("revoker", revoker.getBytes(StandardCharsets.UTF_8)))
    }

    kvPairs.append(("omitcreationdate", omitCreationDate.toString.getBytes(StandardCharsets.UTF_8)))

    val frame = new Frame(seqNo, MAKE_ENTITY, kvPairs, Nil, Nil)
    frame.writeToStream(outStream)
    outStream.flush()
    responseHandler foreach { handler =>
      installResponseHandler(seqNo, handler)
    }
    messageHandler foreach { handler =>
      installMessageHandler(seqNo, handler)
    }
  }

  def makeDot(to: String, responseHandler: Option[BosswaveResponse => Unit], messageHandler: Option[BosswaveMessage => Unit] = None,
              timeToLive: Option[Int], isPermission: Boolean= false, contact: Option[String] = None,
              comment: Option[String] = None, expiry: Option[Date] = None, expiryDelta: Option[Long] = None,
              revokers: Seq[String] = Nil, omitCreationDate: Boolean = false, accessPermissions: Option[String] = None,
              uri: Option[String] = None): Unit = {
    val seqNo = Frame.generateSequenceNumber

    val kvPairs = new mutable.ArrayBuffer[(String, Array[Byte])]()
    kvPairs.append(("to", to.getBytes(StandardCharsets.UTF_8)))
    timeToLive foreach { ttl =>
      kvPairs.append(("ttl", ttl.toString.getBytes(StandardCharsets.UTF_8)))
    }
    contact foreach { ctct =>
      kvPairs.append(("contact", ctct.getBytes(StandardCharsets.UTF_8)))
    }
    comment foreach { cmmt =>
      kvPairs.append(("comment", cmmt.getBytes(StandardCharsets.UTF_8)))
    }
    expiry foreach { exp =>
      kvPairs.append(("expiry", BosswaveClient.RFC_3339.format(exp).getBytes(StandardCharsets.UTF_8)))
    }
    expiryDelta foreach { expDelta =>
      kvPairs.append(("expiryDelta", f"$expDelta%dms".getBytes(StandardCharsets.UTF_8)))
    }
    accessPermissions foreach { perms =>
      kvPairs.append(("accesspermissions", perms.getBytes(StandardCharsets.UTF_8)))
    }
    kvPairs.append(("omitcreationdate", omitCreationDate.toString.getBytes(StandardCharsets.UTF_8)))

    revokers foreach { revoker =>
      kvPairs.append(("reovker", revoker.getBytes(StandardCharsets.UTF_8)))
    }

    val frame = new Frame(seqNo, MAKE_DOT, kvPairs, Nil, Nil)
    frame.writeToStream(outStream)
    outStream.flush()
    responseHandler foreach { handler =>
      installResponseHandler(seqNo, handler)
    }
    messageHandler foreach { handler =>
      installMessageHandler(seqNo, handler)
    }
  }

  def makeChain(isPermission: Boolean, unelaborate: Boolean, dots: Seq[String],
                responseHandler: Option[BosswaveResponse => Unit] = None,
                messageHandler: Option[BosswaveMessage => Unit] = None): Unit = {
    val seqNo = Frame.generateSequenceNumber

    val kvPairs = new mutable.ArrayBuffer[(String, Array[Byte])]()
    kvPairs.append(("ispermission", isPermission.toString.getBytes(StandardCharsets.UTF_8)))
    kvPairs.append(("unelaborate", unelaborate.toString.getBytes(StandardCharsets.UTF_8)))
    dots foreach { dot =>
      kvPairs.append(("dot", dot.getBytes(StandardCharsets.UTF_8)))
    }

    val frame = new Frame(seqNo, MAKE_CHAIN, kvPairs, Nil, Nil)
    frame.writeToStream(outStream)
    outStream.flush()
    responseHandler foreach { handler =>
      installResponseHandler(seqNo, handler)
    }
    messageHandler foreach { handler =>
      installMessageHandler(seqNo, handler)
    }
  }

  private def installResponseHandler(seqNo: Int, handler: BosswaveResponse => Unit): Unit = {
    responseHandlers.synchronized {
      responseHandlers.put(seqNo, handler)
    }
  }

  private def installMessageHandler(seqNo: Int, handler: BosswaveMessage => Unit): Unit = {
    messageHandlers.synchronized {
      messageHandlers.put(seqNo, handler)
    }
  }

  private def installListResultHandler(seqNo: Int, handler: String => Unit): Unit = {
    listResultHandlers.synchronized {
      listResultHandlers.put(seqNo, handler)
    }
  }

  private class BWListener extends Runnable {
    @volatile
    private var continueRunning = true

    override def run(): Unit = {
      while (continueRunning) {
        val nextFrame = Frame.readFromStream(inStream)
        nextFrame match {
          case Failure(InvalidFrameException(msg)) =>
            println(msg) // We can safely ignore invalid frames

          case Failure(_: SocketTimeoutException) =>
            () // Safe to Ignore, loop back and check 'continueRunning'

          case Failure(_: SocketException) =>
            () // This should only occur when we are terminating the client and is safe to ignore

          case Failure(e) =>
            throw new RuntimeException(e)

          case Success(Frame(seqNo, RESPONSE, kvPairs, _, _)) =>
            responseHandlers.synchronized { responseHandlers.get(seqNo) } foreach { handler =>
              val (_, rawStatus) = kvPairs.find { case (key, _) => key == "status" }.get
              val status = new String(rawStatus, StandardCharsets.UTF_8)
              val reason = status match {
                case "okay" => None
                case _ =>
                  val (_, rawReason) = kvPairs.find { case (key, _) => key == "reason" }.get
                  Some(new String(rawReason, StandardCharsets.UTF_8))
              }
              handler.apply(new BosswaveResponse(status, reason))
            }

          case Success(Frame(seqNo, RESULT, kvPairs, routingObjects, payloadObjects)) =>
            messageHandlers.synchronized { messageHandlers.get(seqNo) } foreach { handler =>
              val (_, rawUri) = kvPairs.find { case (key, _) => key == "uri" }.get
              val uri = new String(rawUri, StandardCharsets.UTF_8)
              val (_, rawFrom) = kvPairs.find { case (key, _) => key == "from" }.get
              val from = new String(rawFrom, StandardCharsets.UTF_8)

              val unpack = kvPairs.find { case (key, _) => key == "unpack" } match {
                case None => true
                case Some((_, rawUnpack)) => Try(new String(rawUnpack, StandardCharsets.UTF_8).toBoolean) match {
                  case Failure(_) => true
                  case Success(bool) => bool
                }
              }

              val message = if (unpack) {
                new BosswaveMessage(from, uri, routingObjects, payloadObjects)
              } else {
                new BosswaveMessage(from, uri, Nil, Nil)
              }

              handler.apply(message)
            }

            listResultHandlers.synchronized { listResultHandlers.get(seqNo) } foreach { handler =>
              val (_, rawFinished) = kvPairs.find { case (key, _) => key == "finished" }.get
              val finished = Try(new String(rawFinished, StandardCharsets.UTF_8).toBoolean) match {
                case Failure(_) => false
                case Success(bool) => bool
              }

              val result = if (finished) null else {
                val (_, rawResult) = kvPairs.find { case (key, _) => key == "child" }.get
                new String(rawResult, StandardCharsets.UTF_8)
              }
              handler.apply(result)
            }

          case Success(_) => () // Ignore any invalid frames
        }
      }
    }

    def stop(): Unit = {
      continueRunning = false
    }
  }
}

case class BosswaveMessage(from: String, to: String, routingObjects: Seq[RoutingObject], payloadObjects: Seq[PayloadObject])
case class BosswaveResponse(status: String, reason: Option[String])