package edu.berkeley.cs.sdb.bw2

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.Semaphore

import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class BosswaveClientSpec extends FunSuite with BeforeAndAfter {
  val BW_URI = "scratch.ns/unittests/scala"
  val expectedMessages = Set("Hello, World!", "Bosswave 2", "Lorem ipsum", "dolor sit amet")

  val client = new BosswaveClient()
  client.overrideAutoChainTo(true)
  val semaphore = new Semaphore(0)
  var count = expectedMessages.size

  val messageHandler: (BosswaveResult => Unit) = { case BosswaveResult(_, _, _, pos) =>
      assert(pos.length == 1)
      val message = new String(pos.head.content, StandardCharsets.UTF_8)
      if (expectedMessages.contains(message)) {
        count -= 1
        if (count == 0) {
          semaphore.release()
        }
      }
  }

  before {
    val entityPathUri = getClass.getResource("/unitTests.key").toURI
    val entResp = Await.result(client.setEntityFile(new File(entityPathUri)), Duration.Inf)
    if (entResp.status != "okay") {
      throw new RuntimeException("Failed to set entity: " + entResp.reason.get)
    }

    val subResp = Await.result(client.subscribe(BW_URI, messageHandler), Duration.Inf)
    if (subResp.status != "okay") {
      throw new RuntimeException("Failed to subscribe: " + subResp.reason.get)
    }
  }

  after {
    client.close()
  }

  test("Publish basic sequence of messages") {
    semaphore.acquire() // Block until the subscribe operation is complete

    expectedMessages foreach { msg =>
      val po = new PayloadObject(Some((64, 0, 0, 0)), None, msg.getBytes(StandardCharsets.UTF_8))
      client.publish(BW_URI,  payloadObjects = Seq(po)).onComplete {
        case Success(BosswaveResponse(status, reason)) =>
          if (status != "okay") {
            fail("Publish failed: " + reason.get)
          }
        case Failure(cause) => fail(cause)
      }
    }

    semaphore.acquire() // Wait until all published messages have been received
  }
}
