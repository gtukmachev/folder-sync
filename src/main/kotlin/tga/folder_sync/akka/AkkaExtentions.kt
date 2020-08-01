package tga.folder_sync.akka

import akka.actor.ActorRef
import akka.actor.Props
import kotlin.reflect.KClass

fun akka.actor.ActorSystem.actor(actorName: String, actorClass: KClass<*>, vararg props: Any?): ActorRef {
    return this.actorOf(
        Props.create(
            actorClass.java,
            props
        ),
        actorName
    )
}

fun akka.actor.AbstractActor.ActorContext.actor(actorClass: KClass<*>, vararg props: Any?): ActorRef {
    return this.actorOf(
        Props.create(
            actorClass.java,
            props
        ),
        actorClass.simpleName!!.decapitalize()
    )
}


fun akka.actor.ActorSystem.actor(actorClass: KClass<*>, vararg props: Any?): ActorRef {
    val name = actorClass.simpleName!!.decapitalize()
    return when {
        props.isEmpty() -> this.actorOf(Props.create(actorClass.java        ), name)
                   else -> this.actorOf(Props.create(actorClass.java, *props), name)
    }
}
