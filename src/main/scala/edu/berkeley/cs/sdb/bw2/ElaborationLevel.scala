package edu.berkeley.cs.sdb.bw2

sealed trait ElaborationLevel { def name: String}

case object UNSPECIFIED extends ElaborationLevel { val name = "unspecified"}
case object PARTIAL     extends ElaborationLevel { val name = "partial"}
case object FULL        extends ElaborationLevel { val name = "full"}

