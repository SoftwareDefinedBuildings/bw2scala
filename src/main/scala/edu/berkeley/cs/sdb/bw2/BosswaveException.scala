package edu.berkeley.cs.sdb.bw2

case class BosswaveException(msg: String = null, cause: Throwable = null) extends Exception(msg, cause)
