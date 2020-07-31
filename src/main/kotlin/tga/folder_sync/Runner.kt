package tga.folder_sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.akka.actor
import tga.folder_sync.init.InitActor
import tga.folder_sync.sync.SyncActor
import java.util.*

fun main(vararg args: String) {
    val akka = ActorSystem.create("AkkaSystem")
    val mainActor = akka.actor(MainActor::class)
    mainActor.tell( MainActor.Start(args.asList()), ActorRef.noSender() )
}

class MainActor : AbstractLoggingActor() {

    lateinit var args: List<String>

    override fun createReceive() = ReceiveBuilder()
         // A command to start the program
        .match(Start::class.java, {m -> isInitCommand(m)}) { this.args = it.args; init() }
        .match(Start::class.java, {m -> isSyncCommand(m)}) { this.args = it.args; sync() }
        .match(Start::class.java                         ) { printHelp(); shutdownProgram() }

        // A command to finish the program (the job's done)
        .match(InitActor.Done::class.java) { shutdownProgram() }
        .match(SyncActor.Done::class.java) { shutdownProgram() }

        .build()

    private fun isInitCommand(m: Start) = m.args.isNotEmpty() && m.args[0] == "init"
    private fun isSyncCommand(m: Start) = m.args.isNotEmpty() && m.args[0] == "sync"

    private fun printHelp() {
        println("""
            
            Safe Cloud Backup
            
            The utility allows you to backup your files to a cloud or another disk in a smart safty way.
            
            Syntax:
            $>java -jar folder-sync.jar <command> <source folder> <destination folder>[ login=<login> pass=<pass>]
            
                <command> = { init | sync }
                    init - To compare source and destination directory and create synchronization plan.
                           The plan will be saved to the '' text file, and you can review and even edit it.
                            
                    sync - Perform synchronization using a plan that was built before. 
                           During it's work the utility will run all the commands from the plan file.
                           Each successfully handled comand will be marked in the file.
                           
                           At any time you can interrupt the process (use <Ctrl+C>).
                           After interruption you can resume, and the sync process will be continued accordingly it's plan
                
                <source folder> - a folder with source files
                    - it can be only a local folder (accessible on local file system level)
                    - you can use both absolute or relative path
                
                <destination folder> - a destination folder
                    - it can be only:
                        a local folder (accessible on local file system level)
                        a folder on yandex disk in this case use the following synttax:
                            yandex://disk:/<path on your yandex disk>
                            also, you have to provide login and password                        
                
                <login> and <password> - applicable only if the <destination folder> is a Yandex Disk folder
                          define login to your the target Yandex account
                          this parameter is optional. in case of missing - it will be requested to input.
                          
                          You can use another way to configure credentials:
                          create '~/.yandex.conf' text file with the following content:
                                login=...
                                pass=...
                          
            
        """.trimIndent())
    }

    private fun shutdownProgram() {
        context.system.terminate()
    }

    fun init() {
        val initActor = context.actorOf( Props.create (
            InitActor::class.java,
                System.getProperty("outDir"),
                Date(),
                args
        ), "initActor")
        initActor.tell(InitActor.Perform(self()), self())
    }

    fun sync() {
        val sessionFolder = if (args.size > 1) args[1] else null
        val syncActor = context.actor( SyncActor::class, sessionFolder )
        syncActor.tell( SyncActor.Perform(), self() )
    }

    data class Start(val args: List<String>)

}


