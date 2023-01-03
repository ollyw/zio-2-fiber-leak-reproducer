import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import zio.Fiber.Status
import zio._
import zio.interop.catz._

object Main extends ZIOAppDefault {
  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = fiberMetricsWorker *> poller.provide(httpClient)

  private def poller =
    (for {
      client <- ZIO.service[Client[Task]]
      result <- client.expect[String]("https://zio.dev/")
//      _ = println(result)
    } yield ()).repeat(Schedule.fixed(2.seconds))

  private def fiberMetricsWorker =
    (for {
      roots <- Fiber.roots
      total = roots.size
      statues <- ZIO.foldLeft(roots)((0, 0, 0)) { case ((running, suspended, done), fiber) =>
        fiber.status.map {
          case _: Status.Running => (running + 1, suspended, done)
          case _: Status.Suspended => (running, suspended + 1, done)
          case Status.Done => (running, suspended, done + 1)
        }
      }
      _ <-
        ZIO.debug(
          s"Total roots $total. Fibers running: ${statues._1}, suspended: ${statues._2}, done: ${statues._3}"
        )
//            sampledRoots = roots.take(20)
//            _ <- ZIO.foreachDiscard(sampledRoots)(f =>
//                   f.dump.flatMap(dump => ZIO.when(dump.status.isSuspended)(dump.prettyPrint.flatMap(ZIO.debug(_))))
//                 )
    } yield ()).repeat(Schedule.fixed(5.seconds)).fork


  private val httpClient: TaskLayer[Client[Task]] = ZLayer.scoped(
    ZIO.runtime[Any].flatMap { implicit rts =>
      BlazeClientBuilder[Task].resource.toScopedZIO
    }
  )
}