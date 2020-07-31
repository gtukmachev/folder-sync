package tga.folder_sync.mesages

import akka.actor.ActorRef

data class Start(val resultsListenerActor: ActorRef, val args: List<String>)
