package edu.berkeley.cs.sdb.bw2

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets

import org.scalatest.FunSuite

import scala.util.Failure

class FrameSpec extends FunSuite {

  test("Frame with invalid header") {
    val frameContent = "helo 0000000000 0000000410 foobar\nend\n".getBytes(StandardCharsets.UTF_8)
    val inputStream = new ByteArrayInputStream(frameContent)
    val frameTry = Frame.readFromStream(inputStream)
    inputStream.close()
    assert(frameTry.isFailure)
  }

  test("Read empty frame from stream") {
    val frameContent = "helo 0000000000 0000000410\nend\n".getBytes(StandardCharsets.UTF_8)
    val inStream = new ByteArrayInputStream(frameContent)
    val frameTry = Frame.readFromStream(inStream)
    inStream.close()

    assert(frameTry.isSuccess)
    val frame = frameTry.get
    assert(frame.command == HELLO)
    assert(frame.seqNo == 410)
    assert(frame.kvPairs.isEmpty)
    assert(frame.routingObjects.isEmpty)
    assert(frame.payloadObjects.isEmpty)
  }

  test("Read frame with key/value pair from stream") {
    val frameStr = "publ 0000000099 0000000410\n" +
      "kv testKey 9\n" +
      "testValue\n" +
      "kv testKey2 10\n" +
      "testValue2\n" +
      "kv testKey 6\n" +
      "foobar\n" +
      "end\n"
    val frameContent = frameStr.getBytes(StandardCharsets.UTF_8)
    val inStream = new ByteArrayInputStream(frameContent)
    val frameTry = Frame.readFromStream(inStream)

    frameTry match {
      case Failure(InvalidFrameException(msg)) => println(msg)
      case _ => ()
    }
    assert(frameTry.isSuccess)
    val frame = frameTry.get
    assert(frame.command == PUBLISH)
    assert(frame.seqNo == 410)
    assert(frame.routingObjects.isEmpty)
    assert(frame.payloadObjects.isEmpty)

    val expectedKvPairs = Seq(
      ("testKey", "testValue"),
      ("testKey2", "testValue2"),
      ("testKey", "foobar")
    ) map { case (key, value) => (key, value.getBytes(StandardCharsets.UTF_8)) }
    assert(frame.kvPairs.zip(expectedKvPairs).forall { case ((k1,v1), (k2, v2)) => k1 == k2 && v1.sameElements(v2) })
  }

  test("Read frame with payload object from stream") {
    val frameStr = "subs 0000000059 0000000410\n" +
      "po 1.2.3.4:25 11\n" +
      "testPayload\n" +
      "end\n"
    val frameContent = frameStr.getBytes(StandardCharsets.UTF_8)
    val inStream = new ByteArrayInputStream(frameContent)
    val frameTry = Frame.readFromStream(inStream)
    inStream.close()

    assert(frameTry.isSuccess)
    val frame = frameTry.get
    assert(frame.command == SUBSCRIBE)
    assert(frame.seqNo == 410)
    assert(frame.kvPairs.isEmpty)
    assert(frame.routingObjects.isEmpty)

    val payloadContent = "testPayload".getBytes(StandardCharsets.UTF_8)
    val expectedPayloadObjects = Seq(PayloadObject(Some((1, 2, 3, 4)), Some(25), payloadContent))
    assert(frame.payloadObjects == expectedPayloadObjects)
  }

  test("Read frame with routing object from stream") {
    val frameStr = "pers 0000000046 0000000410\n" +
      "ro 255 6\n" +
      "testRO\n" +
      "end\n"
    val frameContent = frameStr.getBytes(StandardCharsets.UTF_8)
    val inStream = new ByteArrayInputStream(frameContent)
    val frameTry = Frame.readFromStream(inStream)
    inStream.close()

    assert(frameTry.isSuccess)
    val frame = frameTry.get
    assert(frame.command == PERSIST)
    assert(frame.seqNo == 410)
    assert(frame.kvPairs.isEmpty)
    assert(frame.payloadObjects.isEmpty)

    val routingObjContent = "testRO".getBytes(StandardCharsets.UTF_8)
    val expectedRoutingObjects = Seq(RoutingObject(255, routingObjContent))
    assert(frame.routingObjects == expectedRoutingObjects)
  }

  test("Write empty frame to stream") {
    val frame = new Frame(1840, SUBSCRIBE)
    val outStream = new ByteArrayOutputStream()
    frame.writeToStream(outStream)

    val actualContents = outStream.toString(StandardCharsets.UTF_8.name)
    outStream.close()
    val expectedContents = "subs 0000000000 0000001840\nend\n"
    assert(expectedContents == actualContents)
  }

  test("Write frame with key/value pair to stream") {
    val kvs = Seq(
      ("testKey1", "testValue1"),
      ("testKey2", "testValue2")
    ) map { case (key, value) => (key, value.getBytes(StandardCharsets.UTF_8)) }
    val frame = new Frame(1600, PUBLISH, kvPairs = kvs)

    val outStream = new ByteArrayOutputStream()
    frame.writeToStream(outStream)

    val actualContents = outStream.toString(StandardCharsets.UTF_8.name)
    outStream.close()
    val expectedContents = "publ 0000000000 0000001600\n" +
      "kv testKey1 10\n" +
      "testValue1\n" +
      "kv testKey2 10\n" +
      "testValue2\n" +
      "end\n"
    assert(expectedContents == actualContents)
  }

  test("Write frame with payload object to stream") {
    val payloadContent = "testPayload".getBytes(StandardCharsets.UTF_8)
    val pos = Seq(PayloadObject(None, Some(42), payloadContent))
    val frame = Frame(1840, SUBSCRIBE, payloadObjects = pos)

    val outStream = new ByteArrayOutputStream()
    frame.writeToStream(outStream)

    val actualContents = outStream.toString(StandardCharsets.UTF_8.name)
    outStream.close()
    val expectedContents = "subs 0000000000 0000001840\n" +
      "po :42 11\n" +
      "testPayload\n" +
      "end\n"
    assert(expectedContents == actualContents)
  }

  test("Write frame with routing object to stream") {
    val routingObjContent = "testRO".getBytes(StandardCharsets.UTF_8)
    val ros = Seq(RoutingObject(99, routingObjContent))
    val frame = new Frame(1234, PUBLISH, routingObjects = ros)

    val outStream = new ByteArrayOutputStream()
    frame.writeToStream(outStream)

    val actualContents = outStream.toString(StandardCharsets.UTF_8.name)
    outStream.close()
    val expectedContents = "publ 0000000000 0000001234\n" +
      "ro 99 6\n" +
      "testRO\n" +
      "end\n"
    assert(expectedContents == actualContents)
  }
}
