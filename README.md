# bw2Scala
Scala bindings for [BOSSWAVE](https://github.com/immesys/bw2). Fully compatible
with the Hadron (2.4.x) release.

## Simple Publish Example
```scala
val client = new BosswaveClient()
client.overrideAutoChainTo(true)
client.setEntityFile("devKey.ent")
val po = new PayloadObject(Some(POAllocations.PODFString), content = "Hello, World!")
client.publish("scratch.ns/test", payloadObjects = Seq(po))
```

## Simple Subscribe Example
```scala
val client = new BosswaveClient()
client.overrideAutoChainTo(true)
client.setEntityFile("devKey.ent")
client.subscribe("scratch.ns/test", { result =>
  result.payloadObjects.find(_.octet.contains(POAllocations.PODFString)).foreach(println(_.content))
})
```
The second argument is a closure that takes a `BosswaveResult` instance as its
only parameter.

## Future-Based Interface
All `BosswaveClient` methods have a return type of `Future[BosswaveResponse]`,
with the exception of the `queryAll` and `listAll` methods. This gives the
caller the ability to make each Bosswave operation either synchronous or
asynchronous.

### Execution Context
Because they involve the instantiation of `Future`s, the Bosswave client methods
all require an `ExecutionContext` as an implicit parameter. This gives the user
the ability to specify the resources underlying the asynchronous execution of
Bosswave operations, such as a traditional thread pool. If you don't know what
any of this means, just include the following `import` statement in your code,
which provides a sensible default:
```scala
import scala.concurrent.ExecutionContext.Implicits.global
```

### Fire and Forget
The two simple examples above demonstrate the "fire and forget" approach. The
client performs the operation asynchronously, and the caller does not perform
any error checking.

### Asynchronous with Error Handling
To perform a Bosswave operation asynchronously while still checking the outcome,
one can use Scala's `onComplete` method. For example:
```scala
client.publish("scratch.ns/test", payloadObjects = ...).onComplete {
  case Failure(cause) =>
    println("Publish failed with exception: " + cause)
  case Success(BossaveResponse(status, reason)) =>
    if (status != "okay") {
      println("Bosswave operation failed: " + reason.get)  
    }
}
```
Note that this does not use `Future`'s `onFailure` method, as any valid Bosswave
response is returned within a `Success` object, even if it reflects an error.
Instead, `Failure` is reserved for the case where an exception occurs.

As this is a common use case, `BosswaveClient` provides a utility function
to eliminate boilerplate code. This does involve (ab)use of implicits, so a
special import is required. The previous example can be written as:
```scala
import edu.berkeley.cs.sdb.bw2.BosswaveClient.BosswaveFuture
...
client.publish("scratch.ns/test", payloadObjects = ...).exitIfError()
```
### Fully Synchronous
To perform a blocking operation and directly obtain the relevant
`BosswaveResponse`, one can use Scala's `Await` utility. For
example, to block until a publish is finished and check for errors:
```scala
val resp = Await.result(client.publish("scratch.ns/test", payloadObjects = ...), Duration.Inf)
if (resp.status != "okay") {
  println("Bosswave publish failed: " + resp.reason.get)
}
```

The `BosswaveClient` includes a utilty to avoid boilerplate code:
```scala
import edu.berkeley.cs.sdb.bw2.BosswaveClient.BosswaveFuture
...
val resp = client.publish("scratch.ns/test", payloadObjects = ...).await()
...
```
By default, `await` takes no arguments and blocks indefinitely until the
corresponding Bosswave operation has finished. One can also provide a Scala
`Duration` instance as an argument to specify a timeout interval.

### Simplified Error Handling
The Bosswave Go bindings provide `orExit` variants on most operations to
simplify error handling. The Scala bindings feature a similar primitive that
blocks until an operation has completed and exits the program if an error has
occurred. Otherwise, execution proceeds normally. For example:
```scala
import edu.berkeley.cs.sdb.bw2.BosswaveClient.BosswaveFuture
...
client.publish("scratch.ns/test", payloadObjects = ...).completeOrExit()
```
