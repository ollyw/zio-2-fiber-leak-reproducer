# Fiber leak reproducer

## Background
Since migrating some projects to ZIO 2.0.x there have been a few issues with managed heap space running out. Some improvements in recent ZIO 2.0.x versions have improved it (migration from fiber root nursery), but there still seems at least one underlying issue. The most recent issue was triggered with a Cats-Effect upgrade from 3.3.x to 3.4.x.. When trying to reproduce, a small part of the app behaviour was simulated in this reproducer. It is unclear if it is the root cause in the app, but certainly this reproducer is not behaving as expected.

## Reproducer

The app simply polls an endpoint using Http4s Client + Blaze + ZIO. Over time the number of suspended fibers grows and grows. The app logs the number of suspended fibers so it can be seen. It can also be verified with running with tools such as Visual VM and inspecting the heap after running for a while. If you want to speed up the leaking, change the schedule and point to a local http endpoint instead of the hardcoded URL (https://zio.dev)

## Results

Example trace of the leaking fibers

```
"zio-fiber-176" (32s 780ms) 
	Status: Suspended((Interruption, CooperativeYielding, FiberRoots), zio.interop.ZioAsync.async_(ZioAsync.scala:59))
	at .executeRequest(Http1Connection.scala:217:0)
	at zio.interop.package.signalOnNoExternalInterrupt(package.scala:206)
	at zio.interop.ZioConcurrent.start.trace(cats.scala:248)


"zio-fiber-39" (37s 777ms) 
	Status: Suspended((Interruption, CooperativeYielding, FiberRoots), zio.interop.ZioAsync.async_(ZioAsync.scala:59))
	at .executeRequest(Http1Connection.scala:217:0)
	at zio.interop.package.signalOnNoExternalInterrupt(package.scala:206)
	at zio.interop.ZioConcurrent.start.trace(cats.scala:248)
	
... repeated 100s of times	
```
## Do different versions of ZIO affect it?


* 2.0.0 leaks a little, then seems to stop
* 2.0.1 runs out of heap space really quickly
* 2.0.2 runs out of heap space really quickly
* 2.0.3 runs out of heap space really quickly
* 2.0.4 runs out of heap space really quickly
* 2.0.5 leaks, but heap grows slowly
* 1.0.15 heap grows but stablises

## Does the latest version of Cats-interop fix it?
Version 23.0.0.0 was release a few weeks ago with some improvements for cats-effect "lawfulness". However this version and the previous version 3.3.0 both some the same behaviour. The fiber trace points to a different line number in ZioAsync though.

## Further diagnosis
The fiber dumps seem to indicate that the leaking fiber is started as a timeout action.

```
  // Http4s Code from https://github.com/http4s/blaze/blob/main/blaze-client/src/main/scala/org/http4s/blaze/client/Http1Connection.scala#L221-L251
    
  val idleTimeoutF: F[TimeoutException] = idleTimeoutStage match {
    case Some(stage) => F.async_[TimeoutException](stage.setTimeout) // THIS IS THE START OF THE LEAKING FIBER TRACE
    case None => F.never[TimeoutException]
  }

  idleTimeoutF.start.flatMap { timeoutFiber =>
    // the request timeout, the response header timeout, and the idle timeout
    val mergedTimeouts = cancellation.race(timeoutFiber.joinWithNever).map(_.merge)
    F.bracketCase(
      writeRequest.start
    )(writeFiber =>
      ....
    }.race(mergedTimeouts) // THIS IS THE SUSPECTED RACE CONDITION THAT IS NOT CANCELLING THE TIMEOUT FIBER
      .flatMap {
        case Left(r) => F.pure(r)
        case Right(t) => F.raiseError(t)
      }
```

It could be that this issue is related existing issues https://github.com/zio/interop-cats/pull/616
and the follow on "Support the "onCancel associates over uncancelable boundary" law (ZIO 2/CE 3)" https://github.com/zio/interop-cats/issues/617. However it might also be something different. Perhaps Http4s is misusing the effects somehow?