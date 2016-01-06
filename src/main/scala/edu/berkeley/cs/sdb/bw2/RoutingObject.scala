package edu.berkeley.cs.sdb.bw2

import java.io.OutputStream
import java.nio.charset.StandardCharsets

case class RoutingObject(number: Int, body: Array[Byte]) {
  def writeToStream(stream: OutputStream): Unit = {
    val header = String.format("ro %d %d\n", number, body.length)
    stream.write(header.getBytes(StandardCharsets.UTF_8))
    stream.write(body)
    stream.write('\n')
  }
}
