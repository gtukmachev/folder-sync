package tga.folder_sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.akka.actor
import tga.folder_sync.init.InitActor
import tga.folder_sync.params.Parameters
import tga.folder_sync.sync.SyncActor
import java.util.*

fun main(vararg args: String) {
    val params = Parameters.parse(*args)

    val akka = ActorSystem.create("AkkaSystem")
    val mainActor = akka.actor(MainActor::class)
    mainActor.tell( MainActor.Start(params), ActorRef.noSender() )
}

class MainActor : AbstractLoggingActor() {

    lateinit var params: Parameters

    override fun createReceive() = ReceiveBuilder()
         // A command to start the program
        .match(Start::class.java, {m -> isInitCommand(m)}) { this.params = it.params; init() }
        .match(Start::class.java, {m -> isSyncCommand(m)}) { this.params = it.params; sync() }
        .match(Start::class.java                         ) { printHelp(); shutdownProgram() }

        // A command to finish the program (the job's done)
        .match(InitActor.Done::class.java) { shutdownProgram() }
        .match(SyncActor.Done::class.java) { shutdownProgram() }

        .build()

    private fun isInitCommand(m: Start) = m.params.command == Parameters.Command.`init`
    private fun isSyncCommand(m: Start) = m.params.command == Parameters.Command.sync

    private fun printHelp() {
        println("""
            
            Safe Cloud Backup
            
            The utility allows you to backup your files to a cloud or another disk in a smart safty way.
            
            Syntax:
            $>java -jar folder-sync.jar [<command> [<session-folder>] [<source folder> <destination folder>] [login=<login> pass=<pass>]]

                in case of running WITHOUT parameters, the command will be = `init`
                
                <command> = { init | sync }
                    init - To compare source and destination directory and create synchronization plan.
                           The plan will be saved to the '' text file, and you can review and even edit it.
                            
                    sync - Perform synchronization using a plan that was built before. 
                           During it's work the utility will run all the commands from the plan file.
                           Each successfully handled comand will be marked in the file.
                           
                           At any time you can interrupt the process (use <Ctrl+C>).
                           After interruption you can resume, and the sync process will be continued accordingly it's plan
                
                <source folder> - a folder with source files
                    - applicable for `init` command only
                    - it can be only a local folder (accessible on local file system level)
                    - you can use both absolute or relative path
                
                <destination folder> - a destination folder
                    - applicable for `init` command only
                    - it can reffer to one of the following folder types:
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
                                
                <session-folder> - a folder with synchronization plan that was built by `init` command
                    - applicable for `sync` command only
                    - the parameter allow you to build several ynchronization plans and manually run one of them
                      (by default - the program will find a lat one and will use it)
            
        """.trimIndent())
    }

    private fun shutdownProgram() {
        context.system.terminate()
    }

    fun init() {
        val initActor = context.actorOf( Props.create (
            InitActor::class.java,
                Date(),
                params
        ), "initActor")
        initActor.tell(InitActor.Perform(self()), self())
    }

    fun sync() {
        val syncActor = context.actor( SyncActor::class, params.sessionFolder )
        syncActor.tell( SyncActor.Perform(), self() )
    }

    data class Start(val params: Parameters)

}


