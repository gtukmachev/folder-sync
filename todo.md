## major
- `tree worker/master pattern`
	- implement the new pattern of Master/Worker approach:
		- use "tree" of tasks instead of lenear Sequence
		- run "children" tasks in parallel, but only after "parent" one is done
		- do not run "children" at all in case of "parent" crash
	- use this new pattern for the Fitst Phase - creating of all directories before files copying.
	- Note: looks like "fork-join" case - check if it make sence to use "frok-join-dispatcher" for such kind of master-worker actors

- `online statistic`: 
	- now we change statistic on a 'file' level, so we have to wait until the full file well be uploaded to see it's effect in the statistic.
    - implement the statistic updating (amount of bytes only) in an online manner

- `Smart comparing`: comparing files using not "names" only, but involve 
  - `size`, 
  - `dates`, 
  - and `hash sum` to detect any differences inside files content. 

- `Archiving` instead of removing files

- `move files`: sometime, original files/folders can be moved into another folder.
   In this case the tol recognize it as "deletion" of original files, and "creation" of new ones.
   It will be nice if the tool can recognize this case correctly and 
   `move` instead of `delete` -> `create` files
   
- `Errors handling` investigate the `akka way` and implement it for
    - sync-actor
    - sync-actor-coordinator
    - report-actor
    - init-actor
    - scan-actor   

## minor

- `pause` / `resume` commands without the program stopping
- `scheduling` - run nightly

## plans

- `service mode`: to run the program as service
- `UI`: Add a UI for watching the sync process not in logs only
    - configure


## done
- `errors log`: log all errors to a separate file
	-  workers should log().error(...) adetailed information in case of errors:
		- the current task - to see `file name` 
		- stack-trace 

- `plaun update algorithm`
    - do not store all the lines in memory at all in the Actor
    - read original file
    - change lines that should be changed -> to a copy with another name
    - if ok - replace the old file with the new one
    - read + write - will be (slowly) than just write -> move it to another actor  


