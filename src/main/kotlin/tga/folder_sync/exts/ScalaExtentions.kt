package tga.folder_sync.exts

import akka.actor.SupervisorStrategy

val Resume = SupervisorStrategy.resume() as SupervisorStrategy.Directive
