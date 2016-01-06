package edu.berkeley.cs.sdb.bw2

sealed trait Command { def code: String }

case object PUBLISH        extends Command { val code = "publ" }
case object PERSIST        extends Command { val code = "pers" }
case object SUBSCRIBE      extends Command { val code = "subs" }
case object LIST           extends Command { val code = "list" }
case object QUERY          extends Command { val code = "quer" }
case object TAP_SUBSCRIBE  extends Command { val code = "tsub" }
case object TAP_QUERY      extends Command { val code = "tque" }
case object PUT_DOT        extends Command { val code = "putd" }
case object PUT_ENTITY     extends Command { val code = "pute" }
case object PUT_CHAIN      extends Command { val code = "putc" }
case object MAKE_DOT       extends Command { val code = "makd" }
case object MAKE_ENTITY    extends Command { val code = "make" }
case object MAKE_CHAIN     extends Command { val code = "makc" }
case object BUILD_CHAIN    extends Command { val code = "bldc" }
case object ADD_PREF_DOT   extends Command { val code = "adpd" }
case object ADD_PREF_CHAIN extends Command { val code = "adpc" }
case object DEL_PREF_CHAIN extends Command { val code = "dlpc" }
case object SET_ENTITY     extends Command { val code = "sete" }

case object HELLO          extends Command { val code = "helo" }
case object RESPONSE       extends Command { val code = "resp" }
case object RESULT         extends Command { val code = "rslt" }

object Command {
  def fromString(s: String): Option[Command] = {
    s match {
      case "publ" => Some(PUBLISH)
      case "pers" => Some(PERSIST)
      case "subs" => Some(SUBSCRIBE)
      case "list" => Some(LIST)
      case "quer" => Some(QUERY)
      case "tsub" => Some(TAP_SUBSCRIBE)
      case "tque" => Some(TAP_QUERY)
      case "putd" => Some(PUT_DOT)
      case "pute" => Some(PUT_ENTITY)
      case "putc" => Some(PUT_CHAIN)
      case "makd" => Some(MAKE_DOT)
      case "make" => Some(MAKE_ENTITY)
      case "makc" => Some(MAKE_CHAIN)
      case "bldc" => Some(BUILD_CHAIN)
      case "adpd" => Some(ADD_PREF_DOT)
      case "adpc" => Some(ADD_PREF_CHAIN)
      case "dlpc" => Some(DEL_PREF_CHAIN)
      case "sete" => Some(SET_ENTITY)
      case "helo" => Some(HELLO)
      case "resp" => Some(RESPONSE)
      case "rslt" => Some(RESULT)
      case _ => None
    }
  }
}
