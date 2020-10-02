package tga.folder_sync.exts

import akka.actor.*
import akka.japi.Creator

val Resume = SupervisorStrategy.resume() as SupervisorStrategy.Directive

fun <T : Actor> ActorRefFactory.actorOf( actorName: String, actorCreator: () -> T): ActorRef {

    val creator = LambdaCreator<T>(actorCreator)

    return this.actorOf(Props.create( creator ), actorName)
}

class LambdaCreator<T>(val actorCreator: () -> T) : Creator<T> {
    override fun create(): T {
        return actorCreator.invoke()
    }
}

/*
fun <T : Actor> ActorSystem.actorOf( actorName: String, actorCreator: () -> T): ActorRef
        = this.actorOf(Props.create( actorCreator ), actorName)
*/

