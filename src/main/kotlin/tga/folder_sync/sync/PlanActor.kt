package tga.folder_sync.sync

import java.util.concurrent.Executors

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */

class PlanActor(
    val commandsSource: Iterator<SyncCmd>
) {

    val cmdExecutor = Executors.newFixedThreadPool(4)


    var stopped = false

    fun perform() {
        while ( commandsSource.hasNext() && !stopped ) {
            exec(commandsSource.next())
        }
    }


    private fun exec(cmd: SyncCmd) {

        val cmdTask = ComplitablFutureViaSupplyAsync(cmdExecutor){
            var err: Throwable? = null

            try {
                cmd.performCommand()
            } catch (e: Throwable) {
                err = e
            }

            if (err == null) {
                //todo: write success log
            } else {
                //todo: write error log
            }

        }


    }



}
