package edu.berkeley.cs.sdb.bw2

import java.io.OutputStream
import java.nio.charset.StandardCharsets

case class RoutingObject(number: Int, body: Array[Byte]) {
  def writeToStream(stream: OutputStream): Unit = {
    val header = f"ro $number%d ${body.length}%d\n"
    stream.write(header.getBytes(StandardCharsets.UTF_8))
    stream.write(body)
    stream.write('\n')
  }

  // Necessary because Scala does not have nice array equality checks
  override def equals(other: Any): Boolean =
    other match {
      case ro: RoutingObject => this.number == ro.number && this.body.sameElements(ro.body)
      case _ => false
    }
}