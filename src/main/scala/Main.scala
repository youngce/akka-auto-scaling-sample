import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor

case class Pong(msg: String)

class PongActor extends Actor {
  override def receive: Receive = {
    case Ping(to) =>
      //			val port=context.system.settings.config.getString("akka.http.server.default-http-port")
      val ip = InetAddress.getLocalHost.getHostAddress
      sender() ! Pong(s"Ping to $to from actor name: ${context.self.path.name} ip: $ip")
  }
}

case class Ping(to: String)

object Main extends App {
//  val config = addBindHostnameToConfig()
//  implicit val system: ActorSystem = ActorSystem("sys", config)
  implicit val system: ActorSystem = ActorSystem("sys")
  // Akka Management hosts the HTTP routes used by bootstrap
  AkkaManagement(system).start()

  // Starting the bootstrap process needs to be done explicitly
  ClusterBootstrap(system).start()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  implicit val timeout: Timeout = akka.util.Timeout(1000, TimeUnit.MILLISECONDS)
  val nodeName = args.headOption.getOrElse("non-name")
  val actor = system.actorOf(Props(classOf[PongActor]), "pong")
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case ping@Ping(to) ⇒ (to, ping)

  }


  val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case Ping(to) ⇒ (to.hashCode % numberOfShards).toString
    case ShardRegion.StartEntity(id) ⇒
      // StartEntity is used by remembering entities feature
      (id.toLong % numberOfShards).toString
  }
  val region: ActorRef = ClusterSharding(system).start(
    typeName = "ClusterSharding",
    entityProps = Props(classOf[PongActor]),
    settings = ClusterShardingSettings(system),
    extractEntityId = extractEntityId,
    extractShardId = extractShardId
  )
  val route =
    path("names" / Segment) { name =>
      get {

        onSuccess(region ? Ping(name)) {
          case Pong(msg) => complete(msg)
        }
      }
    }
  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0")


  def addBindHostnameToConfig(): Config = {
    var config = ConfigFactory.load()
    val seedNodes = config.getStringList("akka.cluster.seed-nodes").asScala
    val hostname = config.getString("akka.remote.netty.tcp.hostname")
    if (isSeedNode(seedNodes, hostname)) {
      val map = Map("remote.netty.tcp.bind-hostname" -> InetAddress.getLocalHost.getHostAddress,
        "remote.netty.tcp.bind-port" -> "")

      val bindConfig = ConfigFactory.parseMap(map.asJava)
      config = config.withFallback(bindConfig).resolve()
    }
    config
  }

  def isSeedNode(seedNodes: Iterable[String], hostname: String): Boolean = {
    seedNodes.exists(seed => seed.contains(hostname))

  }

  //	println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
}
