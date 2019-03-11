import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import scala.collection.JavaConverters._

case class Pong(msg: String)

class PongActor extends Actor {
	override def receive: Receive = {
		case Ping(to) =>
//			val port=context.system.settings.config.getString("akka.http.server.default-http-port")
			val ip=InetAddress.getLocalHost.getHostAddress
			sender() ! Pong(s"Ping to $to from actor name: ${context.self.path.name} ip: $ip")
	}
}

object Main extends App {


	var config= ConfigFactory.load()

	val hostIP=config.getString("clustering.ip")
	if (hostIP.contains("seed")){
		val map=Map("remote.netty.tcp.bind-hostname"->InetAddress.getLocalHost.getHostAddress,
			"remote.netty.tcp.bind-port"->"")

		val bindConfig=ConfigFactory.parseMap(map.asJava)
		config=config.withFallback(bindConfig).resolve()
		println(config.getString("remote.netty.tcp.bind-hostname"))
	}

	val nodeName = args.headOption.getOrElse("non-name")
	implicit val system = ActorSystem("sys",config)
	implicit val materializer = ActorMaterializer()
	// needed for the future flatMap/onComplete in the end
	implicit val executionContext = system.dispatcher

	implicit val timeout = akka.util.Timeout(1000, TimeUnit.MILLISECONDS)
	val actor = system.actorOf(Props(classOf[PongActor]), "pong")

	val extractEntityId: ShardRegion.ExtractEntityId = {
		case ping@Ping(to) ⇒ (to, ping)
		//		case msg @ Get(id)               ⇒ (id.toString, msg)
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
		path("names" / Segment) {name=>
			get {

				onSuccess(region ? Ping(name)) {
					case Pong(msg) => complete(msg)
					//					case None       => complete(StatusCodes.NotFound)
				}
				//				complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"node's name: $nodeName"))
			}
		}

	val bindingFuture = Http().bindAndHandle(route, "0.0.0.0")

	//	println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
}

case class Ping(to:String)
