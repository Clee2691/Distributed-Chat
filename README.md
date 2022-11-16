# Project 3: Multi-threaded Key-Value Store using RPC With 2 Phase Commit Protocol

A key value server implemented with RPC (Java RMI) and a 2 phase commit protocol to ensure the atomic commitment of distributed transactions.

# Project Structure

``` bash
src
├── Dockerfile
├── client
│   ├── Client.java
│   ├── clientlogging.properties
│   ├── default_data.txt
│   └── KeyValClient.java
├── paxos
│   ├── Acceptor.java
│   ├── Learner.java
│   └── Proposer.java
├── deploy.sh
├── run_client.sh
├── server
│   ├── ACKEnum.java
│   ├── KeyValueImpl.java
│   ├── KVCoordinator.java
│   ├── KVOperation.java
│   ├── KVStoreInterface.java
│   ├── Response.java
│   └── serverlogging.properties 
README.md
```

# Design Notes
* A server implementation with `Java RMI`.
* `Java RMI` is already multi threaded and multiple clients can connect to it.
* Client pre-populates data into the store **ONLY** if store is empty upon connection.
* Pre-populated data lives in a `text` file called `default_data.txt` in the outermost `/config/` folder of the container. The file contains valid `PUT` commands, one on each line. If new data is added, it must be a valid `PUT` command.
* Server store holds key/value pairs of `<String, String>` data. **NO** spaces allowed between words in the key or value
* Logging is done through both the console and to a file that lives in `/logs/`
* Client and server each take in command line arguments in order to start running. Server needs a port, client needs host and port to connect.
* Deploy and client scripts are added for ease of use. Ports and other settings can be changed in the scripts.
* The PAXOS algorithm is implemented here.
* To simulate the Acceptor crashing, I use a random number generator and set it to throw a `SocketTimeoutException` to simulate a very slow response.
* If a majority consensus is not reached, I abort the operation. The user MUST input the request again.
* See code for more comments.

# Deploy & Run
## Deploy Server & Client

1. Enter `src` directory.
``` bash
$ cd src
```
2. Run `deploy` script to deploy client & sever images/ server container.
``` bash
$ ./deploy.sh
```

Clean up: 
```
$ ./deploy.sh --clean-only
```

Notes:
* Port can be changed in the `deploy.sh` script.
* Default server listens on port **5555-5559**. Minimum is 5 ports.
* You can add more in the `deploy` script.

## Watch Server Logs

Find container name of servers:
```bash
$ docker ps -a
```
Follow server logs
``` bash
$ docker logs <container-name> -f
```
Example:
```bash
$ docker logs kvServer -f 
```

Notes:
* Split a terminal screen to watch logs in one terminal.
* Default server container name `kvServer`.
* Container name can be changed in `deploy.sh` script.

## Run Client
* Use the `run_client.sh` script to start a client.

Usage: 
```
$ ./run_client.sh <client-container-name> <port-number>
```

### Example Script Usage
```
$ ./run_client.sh kvClientContainer1 5555
```

Notes:
* Passed in parameter `<client-container-name>` must be unique for the client. Can be anything you want.
* In `run_client.sh`,  `SERVER_CONTAINER` variable must match `SERVER_CONTAINER` variable in `deploy.sh`.
* Default `SERVER_CONTAINER` name is `kvServer`.
* You can run multiple servers connecting to any of the server container ports. Default is **5555-5559**.

## Example PUT GET DEL Requests To Use:

### PUT Requests
```
PUT KEY=moon VAL=sun
PUT KEY=kill VAL=cool
PUT KEY=vim VAL=look
PUT KEY=go VAL=java
PUT KEY=rmi VAL=reg
```
### GET Requests
```
GET KEY=moon
GET KEY=rmi
GET KEY=look
GET KEY=pool
GET KEY=kill
```
### DEL Requests
```
DEL KEY=moon
DEL KEY=rmi
DEL KEY=go
DEL KEY=kill
DEL KEY=vim
```

# Other Notes
* Server and client containers contain log files in `/usr/logs/`

# Write Ups

## Executive Summary

Project 4 utilized the same Java RMI server/client architecture that was implemented previously in project 2 and 3. For this project, I replaced the 2 phase commit protocol with the PAXOS distributed consensus protocol. This protocol provides replication on top of fault tolerance. The 2 phase commit protocol was good to ensure consistency across replicas. PAXOS allows for consensus in event ordering.

## Technical Impression

For this project, I implemented a `Coordinator` class for the servers. This class manages the start and shutdown of servers and their replicas. It registers each server replica into the RMI registry and then allows other servers to keep track of all other replicas.

I think the PAXOS protocol was slightly easier to implement instead of the 2PC protocol.

On the server side, I implemented the PAXOS protocol. This introduced 3 new classes, the `Proposer`, `Acceptor`, and `Learner`. I also set it so that the `Acceptor` periodically fails by throwing a `SocketTimeoutException`. This is to allow for simulation of replica failures. As long as a majority is able to response, it shows that the specified request can still continue.
PAXOS works in 2 phases with the first phase being the proposer sending a message to acceptors which would then send back a promise. Once the proposer receives a majority promise, it will then send an accept request to the acceptor with the proposed value. When the acceptor accepts that request, it will send back the accepted value. Finally, once the majority accepts the proposal, the value is sent to the learner which commits the transaction.
