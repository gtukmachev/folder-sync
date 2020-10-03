## major
- `plaun update algorithm`
    - do not store all the lines in memory at all in the Actor
    - read original file
    - change lines that should be changed -> to a copy with another name
    - if ok - replace the old file with the new one
    - read + write - will be (slowly) than just write -> move it to another actor  

- `shutdown hook`: to do not break "plan file". 
  if a user press <Ctrl+C> duting the "plan file" updating process - the writing process do not finis.
  As result - the file wil be broken, and you can't eun "sync" command again.     

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
