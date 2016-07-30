package edu.berkeley.cs.sdb.bw2

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.Semaphore

import org.scalatest.{BeforeAndAfter, FunSuite}

class BosswaveClientSpec extends FunSuite with BeforeAndAfter {
  val BW_URI = "scratch.ns/unittests/scala"
  val expectedMessages = Set("Hello, World!", "Bosswave 2", "Lorem ipsum", "dolor sit amet")

  val client = new BosswaveClient()
  client.overrideAutoChainTo(true)
  val semaphore = new Semaphore(0)
  var count = expectedMessages.size

  val responseHandler: (BosswaveResponse => Unit) = { case BosswaveResponse(status, reason) =>
      if (status != "okay") {
        throw new RuntimeException("Bosswave operation failed: " + reason.get)
      }
  }

  val messageHandler: (BosswaveMessage => Unit) = { case BosswaveMessage(_, _, _, pos) =>
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
    client.setEntityFile(new File(entityPathUri), Some({ case BosswaveResponse(status, reason) =>
        if (status != "okay") {
          fail("Set entity failed: " + reason.get)
        } else {
          semaphore.release()
        }
    }))
    semaphore.acquire()

    val handler: (BosswaveResponse => Unit) = { case BosswaveResponse(status, reason) =>
      if (status == "okay") {
        semaphore.release()
      } else {
        throw new RuntimeException("Failed to subscribe: " + reason.get)
      }
    }
    client.subscribe(BW_URI, responseHandler = Some(handler), messageHandler = Some(messageHandler))
  }

  after {
    client.close()
  }

  test("Publish basic sequence of messages") {
    semaphore.acquire() // Block until the subscribe operation is complete

    expectedMessages foreach { msg =>
      val po = new PayloadObject(Some((64, 0, 0, 0)), None, msg.getBytes(StandardCharsets.UTF_8))
      client.publish(BW_URI,  payloadObjects = Seq(po), responseHandler = Some(responseHandler))
    }

    semaphore.acquire() // Wait until all published messages have been received
  }
}
