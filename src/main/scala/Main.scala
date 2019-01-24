import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer

case class Pong(from: String)

class PongActor extends Actor {
	override def receive: Receive = {
		case Ping =>
			val port=context.system.settings.config.getString("akka.http.server.default-http-port")
			sender() ! Pong(port)
	}
}

object Main extends App {

	val nodeName = args.headOption.getOrElse("non-name")
	implicit val system = ActorSystem("sys")
	implicit val materializer = ActorMaterializer()
	// needed for the future flatMap/onComplete in the end
	implicit val executionContext = system.dispatcher

	implicit val timeout = akka.util.Timeout(1000, TimeUnit.MILLISECONDS)
	val actor = system.actorOf(Props(classOf[PongActor]), "pong")

	val extractEntityId: ShardRegion.ExtractEntityId = {
		case _ ⇒ (math.random().toString, Ping)
		//		case msg @ Get(id)               ⇒ (id.toString, msg)
	}

	val numberOfShards = 100

	val extractShardId: ShardRegion.ExtractShardId = {
		case _ ⇒ (math.random() % numberOfShards).toString
		case ShardRegion.StartEntity(id) ⇒
			// StartEntity is used by remembering entities feature
			(id.toLong % numberOfShards).toString
	}

	val region: ActorRef = ClusterSharding(system).start(
		typeName = "PongActor",
		entityProps = Props[PongActor],
		settings = ClusterShardingSettings(system),
		extractEntityId = extractEntityId,
		extractShardId = extractShardId
	)
	val route =
		path("ping") {
			get {

				onSuccess(region ? Ping) {
					case Pong(from) => complete(s"pong from $from")
					//					case None       => complete(StatusCodes.NotFound)
				}
				//				complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"node's name: $nodeName"))
			}
		}

	val bindingFuture = Http().bindAndHandle(route, "localhost")

	//	println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
}

case object Ping