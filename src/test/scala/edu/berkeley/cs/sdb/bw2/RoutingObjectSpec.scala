package edu.berkeley.cs.sdb.bw2

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

import org.scalatest.FunSuite

class RoutingObjectSpec extends FunSuite {
  test("RoutingObject writeToStream") {
    val content = "This is a test"
    val ro = new RoutingObject(210, content.getBytes(StandardCharsets.UTF_8))

    val outStream = new ByteArrayOutputStream()
    ro.writeToStream(outStream)

    val expectedOutput = "ro 210 14\nThis is a test\n"
    val actualOutput = outStream.toString(StandardCharsets.UTF_8.name)
    assert(expectedOutput == actualOutput)

    outStream.close()
  }
}
