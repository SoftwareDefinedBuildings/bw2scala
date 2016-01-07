package edu.berkeley.cs.sdb.bw2

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

import org.scalatest.FunSuite

class PayloadObjectSpec extends FunSuite {
  test("PayloadObject writeToStream with Octet Type") {
    val content = "This is a test".getBytes(StandardCharsets.UTF_8)
    val po = new PayloadObject(Some((4, 8, 15, 16)), None, content)

    val outStream = new ByteArrayOutputStream()
    po.writeToStream(outStream)

    val expectedOutput = "po 4.8.15.16: 14\nThis is a test\n"
    val actualOutput = outStream.toString(StandardCharsets.UTF_8.name)
    assert(expectedOutput == actualOutput)

    outStream.close()
  }

  test("PayloadObject writeToStream with Number Type") {
    val content = "This is a test".getBytes(StandardCharsets.UTF_8)
    val po = new PayloadObject(None, Some(23), content)

    val outStream = new ByteArrayOutputStream()
    po.writeToStream(outStream)

    val expectedOutput = "po :23 14\nThis is a test\n"
    val actualOutput = outStream.toString(StandardCharsets.UTF_8.name)
    assert(expectedOutput == actualOutput)

    outStream.close()
  }

  test("PaylodObject writeToStream with Octet and Number Type") {
    val content = "This is a test".getBytes(StandardCharsets.UTF_8)
    val po = new PayloadObject(Some((4, 8, 15, 16)), Some(23), content)

    val outStream = new ByteArrayOutputStream()
    po.writeToStream(outStream)

    val expectedOutput = "po 4.8.15.16:23 14\nThis is a test\n"
    val actualOutput = outStream.toString(StandardCharsets.UTF_8.name)
    assert(expectedOutput == actualOutput)

    outStream.close()
  }
}
