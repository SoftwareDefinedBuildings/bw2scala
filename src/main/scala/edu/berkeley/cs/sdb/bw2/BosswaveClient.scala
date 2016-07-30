package edu.berkeley.cs.sdb.bw2

import java.io.{BufferedInputStream, File, FileInputStream}
import java.net.{Socket, SocketException, SocketTimeoutException}
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Semaphore

import scala.collection.mutable
import scala.concurrent.{Await, Channel, Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object BosswaveClient {
  val BW_PORT = 28589 // Bosswave IANA Port Number
  private val SOCK_TIMEOUT_MS = 2000 // Frame read timeout length
  val RFC_3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")

  private def getFirstBoolean(kvPairs: Seq[(String, Array[Byte])], key: String, default: Boolean): Boolean = {
    kvPairs.find { case (k, _) => k == key } match {
      case None => default
      case Some((k,v)) => Try(new String(v, StandardCharsets.UTF_8).toBoolean) match {
        case Failure(_) => default
        case Success(b) => b
      }
    }
  }
}

class BosswaveClient(val hostName: String = "localhost", val port: Int = BosswaveClient.BW_PORT) {
  private var defaultAutoChain: Option[Boolean] = None

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

  private val responses = new mutable.HashMap[Int, Channel[BosswaveResponse]]
  private val resultHandlers = new mutable.HashMap[Int, (BosswaveResult => Unit)]
  private val listResultHandlers = new mutable.HashMap[Int, (Option[String] => Unit)]

  def close(): Unit = {
    listener.stop()
    inStream.close()
    outStream.close()
    socket.close()
    listenerThread.join()
  }

  def overrideAutoChainTo(autoChain: Boolean) = {
    this.defaultAutoChain = Some(autoChain)
  }

  private def determineActualAutoChain(autoChain: Boolean) = {
    defaultAutoChain match {
      case Some(ac) => ac
      case None => autoChain
    }
  }

  private def awaitResponse(seqNo: Int): BosswaveResponse = {
    val respChannel = new Channel[BosswaveResponse]
    responses.synchronized { responses.put(seqNo, respChannel) }
    val resp = respChannel.read

    responses.synchronized { responses.remove(seqNo) }
    resp
  }

  def setEntity(key: Array[Byte]): Future[BosswaveResponse] = Future {
    val seqNo = Frame.generateSequenceNumber
    val po = new PayloadObject(Some((0, 0, 0, 50)), None, key)
    val frame = new Frame(seqNo, SET_ENTITY, payloadObjects = Seq(po))
    frame.writeToStream(outStream)
    outStream.flush()

    blocking { awaitResponse(seqNo) }
  }

  def setEntityFile(f: File): Future[BosswaveResponse] = {
    val stream = new BufferedInputStream(new FileInputStream(f))
    val keyFile = new Array[Byte]((f.length() - 1).toInt)
    stream.read() // Strip the first byte
    stream.read(keyFile, 0, keyFile.length)
    stream.close()

    setEntity(keyFile)
  }

  def publish(uri: String, persist: Boolean = false, primaryAccessChain: Option[String] = None,
              expiry: Option[Date] = None, expiryDelta: Option[Long] = None,
              elaboratePac: ElaborationLevel = UNSPECIFIED, autoChain: Boolean = false,
              routingObjects: Seq[RoutingObject] = Nil,
              payloadObjects: Seq[PayloadObject] = Nil): Future[BosswaveResponse] = Future {
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
    if (determineActualAutoChain(autoChain)) {
      kvPairs.append(("auto_chain", "true".getBytes(StandardCharsets.UTF_8)))
    }

    val frame = new Frame(seqNo, command, kvPairs, routingObjects, payloadObjects)
    frame.writeToStream(outStream)
    outStream.flush()

    blocking { awaitResponse(seqNo) }
  }

  def subscribe(uri: String, resultHandler: (BosswaveResult => Unit), expiry: Option[Date] = None,
                expiryDelta: Option[Long] = None, primaryAccessChain: Option[String] = None,
                elaboratePac: ElaborationLevel = UNSPECIFIED, autoChain: Boolean = false, leavePacked: Boolean = false,
                routingObjects: Seq[RoutingObject] = Nil): Future[BosswaveResponse] = Future {
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
    if (determineActualAutoChain(autoChain)) {
      kvPairs.append(("auto_chain", "true".getBytes(StandardCharsets.UTF_8)))
    }
    if (!leavePacked) {
      kvPairs.append(("unpack", "true".getBytes(StandardCharsets.UTF_8)))
    }
    val frame = new Frame(seqNo, SUBSCRIBE, kvPairs, routingObjects, Nil)
    frame.writeToStream(outStream)
    outStream.flush()

    resultHandlers.synchronized { resultHandlers.put(seqNo, resultHandler) }
    blocking { awaitResponse(seqNo) }
  }

  def list(uri: String, listResultHandler: (Option[String] => Unit), expiry: Option[Date] = None,
           expiryDelta: Option[Long] = None, primaryAccessChain: Option[String] = None,
           elaboratePac: ElaborationLevel = UNSPECIFIED, autoChain: Boolean = false,
           routingObjects: Seq[RoutingObject] = Nil): Future[BosswaveResponse] = Future {
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
    if (determineActualAutoChain(autoChain)) {
      kvPairs.append(("autochain", "true".getBytes(StandardCharsets.UTF_8)))
    }
    val frame = new Frame(seqNo, LIST, kvPairs, routingObjects, Nil)
    frame.writeToStream(outStream)
    outStream.flush()

    listResultHandlers.synchronized {
      listResultHandlers.put(seqNo, listResultHandler)
    }
    blocking { awaitResponse(seqNo) }
  }

  def listAll(uri: String, expiry: Option[Date] = None, expiryDelta: Option[Long] = None,
              primaryAccessChain: Option[String] = None, elaboratePac: ElaborationLevel = UNSPECIFIED,
              autoChain: Boolean = false, routingObjects: Seq[RoutingObject] = Nil): Future[Seq[String]] = Future {
    val results = mutable.ArrayBuffer.empty[String]
    val semaphore = new Semaphore(0)
    val handler: (Option[String] => Unit) = {
      case None => semaphore.release()
      case Some(result) => results.append(result)
    }

    val resp = blocking {
      Await.result(list(uri, handler, expiry, expiryDelta, primaryAccessChain, elaboratePac,
          autoChain, routingObjects), Duration.Inf)
    }
    if (resp.status != "okay") {
      throw BosswaveException("List operation failed: " + resp.reason.get)
    }

    blocking { semaphore.acquire() }
    results
  }

  def query(uri: String, resultHandler: (BosswaveResult => Unit), expiry: Option[Date] = None,
            expiryDelta: Option[Long] = None, primaryAccessChain: Option[String] = None,
            elaboratePac: ElaborationLevel = UNSPECIFIED, autoChain: Boolean = false,  leavePacked: Boolean = false,
            routingObjects: Seq[RoutingObject] = Nil): Future[BosswaveResponse] = Future {
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

    resultHandlers.synchronized { resultHandlers.put(seqNo, resultHandler) }
    blocking { awaitResponse(seqNo) }
  }

  def queryAll(uri: String, expiry: Option[Date] = None, expiryDelta: Option[Long] = None,
               primaryAccessChain: Option[String] = None, elaboratePac: ElaborationLevel = UNSPECIFIED,
               autoChain: Boolean = false, leavePacked: Boolean = false, routingObjects: Seq[RoutingObject] = Nil):
               Future[Seq[BosswaveResult]] = Future {
    val results = mutable.ArrayBuffer.empty[BosswaveResult]
    val semaphore = new Semaphore(0)
    val handler: (BosswaveResult => Unit) = { result =>
      results.append(result)
      if (BosswaveClient.getFirstBoolean(result.kvPairs, "finished", default = false)) {
        semaphore.release()
      }
    }

    val resp = blocking {
      Await.result(query(uri, handler, expiry, expiryDelta, primaryAccessChain, elaboratePac, autoChain,
          leavePacked, routingObjects), Duration.Inf)
    }
    if (resp.status != "okay") {
      throw BosswaveException("Query failed: " + resp.reason.get)
    }

    blocking { semaphore.acquire() }
    results
  }

  def makeEntity(contact: Option[String] = None, comment: Option[String] = None, expiry: Option[Date] = None,
                 expiryDelta: Option[Long] = None, revokers: Seq[String] = Nil, omitCreationDate: Boolean = false):
                 Future[BosswaveResponse]  = Future {
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

    blocking { awaitResponse(seqNo) }
  }

  def makeDot(to: String, timeToLive: Option[Int], isPermission: Boolean= false, contact: Option[String] = None,
              comment: Option[String] = None, expiry: Option[Date] = None, expiryDelta: Option[Long] = None,
              revokers: Seq[String] = Nil, omitCreationDate: Boolean = false, accessPermissions: Option[String] = None,
              uri: Option[String] = None): Future[BosswaveResponse] = Future {
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
      kvPairs.append(("revoker", revoker.getBytes(StandardCharsets.UTF_8)))
    }
    val frame = new Frame(seqNo, MAKE_DOT, kvPairs, Nil, Nil)
    frame.writeToStream(outStream)
    outStream.flush()

    blocking { awaitResponse(seqNo) }
  }

  def makeChain(isPermission: Boolean, unelaborate: Boolean, dots: Seq[String]): Future[BosswaveResponse] = Future {
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

    blocking { awaitResponse(seqNo) }
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
            responses.synchronized { responses.get(seqNo) } foreach { chan =>
              val (_, rawStatus) = kvPairs.find { case (key, _) => key == "status" }.get
              val status = new String(rawStatus, StandardCharsets.UTF_8)
              val reason = status match {
                case "okay" => None
                case _ =>
                  val (_, rawReason) = kvPairs.find { case (key, _) => key == "reason" }.get
                  Some(new String(rawReason, StandardCharsets.UTF_8))
              }
              chan.write(BosswaveResponse(status, reason))

              if (reason.isDefined) {
                // If error, garbage collect handlers
                resultHandlers.synchronized { resultHandlers.remove(seqNo) }
                listResultHandlers.synchronized { listResultHandlers.remove(seqNo) }
              }
            }

          case Success(Frame(seqNo, RESULT, kvPairs, routingObjects, payloadObjects)) =>
            val finished = BosswaveClient.getFirstBoolean(kvPairs, "finished", default = false)

            resultHandlers.synchronized { resultHandlers.get(seqNo) } foreach { handler =>
              val (_, rawUri) = kvPairs.find { case (key, _) => key == "uri" }.get
              val uri = new String(rawUri, StandardCharsets.UTF_8)
              val (_, rawFrom) = kvPairs.find { case (key, _) => key == "from" }.get
              val from = new String(rawFrom, StandardCharsets.UTF_8)

              val unpack = BosswaveClient.getFirstBoolean(kvPairs, "unpack", default = true)
              val message = if (unpack) {
                BosswaveResult(from, uri, kvPairs, routingObjects, payloadObjects)
              } else {
                BosswaveResult(from, uri, kvPairs, Nil, Nil)
              }

              handler.apply(message)
              if (finished) {
                resultHandlers.synchronized { resultHandlers.remove(seqNo) }
              }
            }

            listResultHandlers.synchronized { listResultHandlers.get(seqNo) } foreach { handler =>
              val result = if (finished) None else {
                val (_, rawResult) = kvPairs.find { case (key, _) => key == "child" }.get
                val s = new String(rawResult, StandardCharsets.UTF_8)
                Some(s)
              }
              handler.apply(result)
              if (finished) {
                listResultHandlers.synchronized { listResultHandlers.remove(seqNo) }
              }
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

case class BosswaveResponse(status: String, reason: Option[String])

case class BosswaveResult(from: String, to: String, kvPairs: Seq[(String, Array[Byte])],
                          routingObjects: Seq[RoutingObject], payloadObjects: Seq[PayloadObject])
