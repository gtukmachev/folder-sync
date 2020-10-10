package tga.folder_sync.exts

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorRefFactory
import akka.actor.Props
import akka.japi.Creator
import akka.japi.pf.ReceiveBuilder
import kotlin.reflect.KClass

fun <T : Actor> ActorRefFactory.actorOf( actorName: String, actorCreator: () -> T): ActorRef {

    val creator = LambdaCreator(actorCreator)

    return this.actorOf(Props.create( creator ), actorName)
}

class LambdaCreator<T>(private val actorCreator: () -> T) : Creator<T> {
    override fun create(): T {
        return actorCreator.invoke()
    }
}


fun <P : Any> ReceiveBuilder.on(clazz: KClass<P>, apply: (P) -> Unit): ReceiveBuilder {
    return this.match(clazz.java, apply)
}
