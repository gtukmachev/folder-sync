package tga.folder_sync.exts

import akka.actor.*

val Resume = SupervisorStrategy.resume() as SupervisorStrategy.Directive

fun <T : Actor> ActorRefFactory.actorOf( actorName: String, actorCreator: () -> T): ActorRef
        = this.actorOf(Props.create( actorCreator ), actorName)

/*
fun <T : Actor> ActorSystem.actorOf( actorName: String, actorCreator: () -> T): ActorRef
        = this.actorOf(Props.create( actorCreator ), actorName)
*/

