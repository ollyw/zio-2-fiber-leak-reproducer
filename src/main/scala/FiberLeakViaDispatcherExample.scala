package zio

import cats.effect._
import cats.effect.std.Dispatcher
import zio.interop.catz._

object FiberLeakViaDispatcherExample extends ZIOAppDefault {

  override def runtime: Runtime[Any] = Runtime(ZEnvironment.empty, FiberRefs.empty, RuntimeFlags(
    RuntimeFlag.Interruption,
    RuntimeFlag.CooperativeYielding, // Removed FiberRoots to fibers still referenced in the heap dump
  ))

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    ZIO.runtime[Any].flatMap { implicit runtime =>
        Dispatcher.parallel[Task].use(dispatcher =>
          for {
            _ <- ZIO.replicateZIODiscard(1000000)(ZIO.fromFuture(_ => dispatcher.unsafeToFuture(Concurrent[Task].sleep(100.micro.asScala))))
            _ <- Console.printLine("Finished")
            _ <- ZIO.never
            _ = println(dispatcher) // Keep dispatcher from being GCd. Disposing of the dispatcher will lead to GC'ing leak which is pinned in a Dispatcher latch
          } yield ())
    }
  }
}