# Folder synchronizer

Initiall y this utility was designed for copiyng of a big file structure to Yandex Desk.

There was a number of challanges within the copyiong pprocess:

- More then 2 Tb of data
- Hudreds of thiuthand files  


The utility can synchronize 2 of your folders:

- source folder (local or on Yndex.disk)
- destination folder (local or on Yndex.disk)

with a garantue of one way synchronizing: source folder wil be never changed 


The utility works in 2 steps:  

- step 1: a scaning of defined source and destination folders and biolding of the synchronization plan (list of commands to make). This plan will be saved to a file.
- step 2: performing of the saved plan.    

Step 2 can be stopped/continued at any moment. So if you have a huge files archive - you should take care about switching your coputer on too long - the process will be continued after a reboot. 