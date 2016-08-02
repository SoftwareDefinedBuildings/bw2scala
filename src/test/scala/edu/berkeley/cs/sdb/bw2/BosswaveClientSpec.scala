package edu.berkeley.cs.sdb.bw2

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.Semaphore

import edu.berkeley.cs.sdb.bw2.BosswaveClient._
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class BosswaveClientSpec extends FunSuite with BeforeAndAfterAll {
  val expectedMessages = Set("Hello, World!", "Bosswave 2", "Lorem ipsum", "dolor sit amet")

  val client = new BosswaveClient()
  client.overrideAutoChainTo(true)
  val semaphore = new Semaphore(0)
  var count = expectedMessages.size

  override def beforeAll = {
    val entityPathUri = getClass.getResource("/unitTests.key").toURI
    val entResp = Await.result(client.setEntityFile(new File(entityPathUri)), Duration.Inf)
    if (entResp.status != "okay") {
      throw new RuntimeException("Failed to set entity: " + entResp.reason.get)
    }
  }

  override def afterAll = {
    client.close()
  }

  test("Publish basic sequence of messages") {
    var count = expectedMessages.size
    val messageHandler: (BosswaveResult => Unit) = { result =>
      assert(result.payloadObjects.length == 1)
      val message = new String(result.payloadObjects.head.content, StandardCharsets.UTF_8)
      assert(expectedMessages.contains(message))
      count -= 1
      if (count == 0) {
        semaphore.release()
      }
    }
    client.subscribe("scratch.ns/unittests/scala", messageHandler).completeOrExit()

    expectedMessages foreach { msg =>
      val po = new PayloadObject(Some(POAllocations.PODFString), content = msg.getBytes(StandardCharsets.UTF_8))
      client.publish("scratch.ns/unittests/scala",  payloadObjects = Seq(po)).exitIfError()
    }

    semaphore.acquire() // Wait until all published messages have been received
  }

  test("Publish to URI without requisite permissions") {
    val po = new PayloadObject(Some(POAllocations.PODFString), content = "Hello, World".getBytes(StandardCharsets.UTF_8))
    // Client should not have permission on this URI
    val resp = client.publish("jkolb/unittests", payloadObjects = Seq(po)).await()
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
      val po = new PayloadObject(Some(POAllocations.PODFString), content = probe.getBytes(StandardCharsets.UTF_8))
      val resp = client.publish("scratch.ns/unittests/scala/persisted/" + planet, persist = true,
                                payloadObjects = Seq(po)).await()
      if (resp.status != "okay") {
        fail("Publish failed: " + resp.reason.get)
      }
    }

    val listResults = Await.result(client.listAll("scratch.ns/unittests/scala/persisted"), Duration.Inf)
    assert(listResults.length == persistedData.size)
    assert(listResults.map(childUri => childUri.substring(childUri.lastIndexOf('/') + 1)).forall(persistedData.contains))

    val queryResults = Await.result(client.queryAll("scratch.ns/unittests/scala/persisted/+"), Duration.Inf)
    val uris = queryResults flatMap { queryResult =>
      queryResult.kvPairs find { case (k, _) => k == "uri" } map { case (k, v) =>
        val uri = new String(v, StandardCharsets.UTF_8)
        uri.substring(uri.lastIndexOf('/') + 1)
      }
    }
    assert(persistedData.keys == uris.toSet)
    val queryValues = queryResults flatMap { queryResult =>
      queryResult.payloadObjects.find(_.octet.contains(POAllocations.PODFString)).map { po =>
        new String(po.content, StandardCharsets.UTF_8)
      }
    }
    assert(queryValues.toSet == persistedData.values.toSet)
  }

  test("Query URI without requisite permissions") {
    val resp = client.query("jkolb/unittest", { _ => () }).await()
    assert(resp.status != "okay" && resp.reason.isDefined)
  }

  test("List URI without requisite permissions") {
    val resp = client.query("jkolb/unittest", { _ => () }).await()
    assert(resp.status != "okay" && resp.reason.isDefined)
  }
}
