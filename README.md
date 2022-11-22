# Distributed Chat Application

A distributed chat application implemented with RPC (Java RMI). Users are able to register or login and then either create or join a chatroom. The chatroom features real time chatting along with message histories and the current participants.

# Project Structure

``` bash
config
├── clientlogging.properties
├── port-list.cfg
└── serverlogging.properties
src
├── client
│   ├── ChatClient.java
│   └── ClientInterface.java
├── gui
│   ├── ClientGUI.java
│   └── SmartScroller.java
├── paxos
│   ├── Acceptor.java
│   ├── Learner.java
│   └── Proposer.java
├── server
│   ├── ChatCoordinator.java
│   ├── ChatServerImpl.java
│   ├── ChatServerInterface.java
│   ├── DBOperation.java
│   └── Response.java
├── build.sh
├── run_client.sh
└── run_sever.sh
README.md
```

# Design Notes
* The client and server is implemented with `Java RMI`.
* `Java RMI` is already multi threaded and multiple clients can connect to it.
* Logging is done through both the console and to a file that lives in `/logs/`
* Client and server each take in command line arguments in order to start running. The Server needs at least 5 ports, client needs host and port to connect.
* Run client and server scripts are added for ease of use. Ports and other settings can be changed in the scripts.
* The PAXOS algorithm is implemented here.
* If a majority consensus is not reached, I abort the operation. The user MUST input the request again.
* See code for more comments.

# Build & Run
## Build Server/Client

1. Enter `src` directory.
``` bash
$ cd src
```
2. Run `build` script to build the `java` source files.
``` bash
$ ./build.sh
```

Clean up: 
```
$ ./build.sh --rm
```
## Run Server
* Use the `run-server.sh` script to start the server.

Usage: 
```
$ ./run-server.sh
```
## Run Client
* Use the `run_client.sh` script to start a client.

Usage: 
```
$ ./run_client.sh
```

# Other Notes
* Server and client containers contain log files in `/usr/logs/`