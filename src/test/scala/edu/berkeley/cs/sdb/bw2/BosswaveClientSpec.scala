package edu.berkeley.cs.sdb.bw2

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.Semaphore

import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class BosswaveClientSpec extends FunSuite with BeforeAndAfter {
  val expectedMessages = Set("Hello, World!", "Bosswave 2", "Lorem ipsum", "dolor sit amet")

  val client = new BosswaveClient()
  client.overrideAutoChainTo(true)
  val semaphore = new Semaphore(0)
  var count = expectedMessages.size

  val messageHandler: (BosswaveResult => Unit) = { result =>
      assert(result.payloadObjects.length == 1)
      val message = new String(result.payloadObjects.head.content, StandardCharsets.UTF_8)
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

    val subResp = Await.result(client.subscribe("scratch.ns/unittests/scala", messageHandler), Duration.Inf)
    if (subResp.status != "okay") {
      throw new RuntimeException("Failed to subscribe: " + subResp.reason.get)
    }
  }

  after {
    client.close()
  }

  test("Publish basic sequence of messages") {
    expectedMessages foreach { msg =>
      val po = new PayloadObject(Some((64, 0, 0, 0)), None, msg.getBytes(StandardCharsets.UTF_8))
      client.publish("scratch.ns/unittests/scala",  payloadObjects = Seq(po)).onComplete {
        case Success(BosswaveResponse(status, reason)) =>
          if (status != "okay") {
            fail("Publish failed: " + reason.get)
          }
        case Failure(cause) => fail(cause)
      }
    }

    semaphore.acquire() // Wait until all published messages have been received
  }

  test("Publish to URI without requisite permissions") {
    val po = new PayloadObject(Some((64, 0, 0, 0)), None, "Hello, World".getBytes(StandardCharsets.UTF_8))
    // Client should not have permission on this URI
    val resp = Await.result(client.publish("jkolb/unittest", payloadObjects = Seq(po)), Duration.Inf)
    assert(resp.status != "okay" && resp.reason.isDefined)
  }

  test("Query and list on URI hierarchy") {
    val persistedData = Map(
      "Mercury" -> "Messenger",
      "Venus" -> "Venera",
      "Mars" -> "Pathfinder",
      "Jupiter" -> "Galileo",
      "Saturn" -> "Cassini",
      "Pluto" -> "New Horizons"
    )

    persistedData foreach { case (planet, probe) =>
      val po = new PayloadObject(Some((64, 0, 0 ,0)), None, probe.getBytes(StandardCharsets.UTF_8))
      val resp = Await.result(client.publish("scratch.ns/unittest/scala/persisted/" + planet, persist = true,
          payloadObjects = Seq(po)), Duration.Inf)
      if (resp.status != "okay") {
        fail("Publish failed: " + resp.reason.get)
      }
    }

    val listResults = Await.result(client.listAll("scratch.ns/unittest/scala/persisted/+"), Duration.Inf)
    assert(listResults.forall(persistedData.contains))

    val queryResults = Await.result(client.queryAll("scratch.ns/unittests/scala/persisted/+"), Duration.Inf)
    assert(queryResults.map(_.payloadObjects).forall { pos =>
      pos.length == 1 && persistedData.values.toSeq.contains(new String(pos.head.content), StandardCharsets.UTF_8)
    })
    assert(queryResults.map(rslt => rslt.from.substring(rslt.from.lastIndexOf('/') + 1)).forall(persistedData.contains))
  }

  test("Query URI without requisite permissions") {
    val resp = Await.result(client.query("jkolb/unittest", { _ => () }), Duration.Inf)
    assert(resp.status != "okay" && resp.reason.isDefined)
  }

  test("List URI without requisite permissions") {
    val resp = Await.result(client.query("jkolb/unittest", { _ => () }), Duration.Inf)
    assert(resp.status != "okay" && resp.reason.isDefined)
  }
}
