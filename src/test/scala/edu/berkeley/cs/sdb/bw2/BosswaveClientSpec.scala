package edu.berkeley.cs.sdb.bw2

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.Semaphore

import org.scalatest.{BeforeAndAfter, FunSuite}

class BosswaveClientSpec extends FunSuite with BeforeAndAfter {
  val BW_PORT = 28589
  val expectedMessages = Set("Hello, World!", "Bosswave 2", "Lorem ipsum", "dolor sit amet")

  // We assume a local Bosswave router is running
  val client = new BosswaveClient("localhost", BW_PORT)
  val semaphore = new Semaphore(0)
  var count = expectedMessages.size

  val responseHandler: (Response => Unit) = { case Response(status, reason) =>
      if (status != "okay") {
        throw new RuntimeException("Bosswave operation failed: " + reason.get)
      }
  }

  val messageHandler: (Message => Unit) = { case Message(_, _, _, pos) =>
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
    client.setEntityFile(new File("/home/jack/bosswave/jack.key"), Some(responseHandler))

    val handler: (Response => Unit) = { case Response(status, reason) =>
      if (status == "okay") {
        semaphore.release()
      } else {
        throw new RuntimeException("Failed to subscribe: " + reason.get)
      }
    }
    client.subscribe("castle.bw2.io/foo/bar", expiryDelta = Some(3600000),
                     primaryAccessChain = Some("lGhzBEz_uyAz2sOjJ9kmfyJEl1MakBZP3mKC-DNCNYE="),
                     responseHandler = Some(handler), messageHandler = Some(messageHandler))
  }

  after {
    client.close()
  }

  test("Publish basic sequence of messages") {
    semaphore.acquire() // Block until the subscribe operation is complete

    expectedMessages foreach { msg =>
      val po = new PayloadObject(Some((64, 0, 0, 0)), None, msg.getBytes(StandardCharsets.UTF_8))
      client.publish("castle.bw2.io/foo/bar", primaryAccessChain = Some("lGhzBEz_uyAz2sOjJ9kmfyJEl1MakBZP3mKC-DNCNYE="),
                     payloadObjects = Seq(po), responseHandler = Some(responseHandler))
    }

    semaphore.acquire() // Wait until all published messages have been received
  }
}
