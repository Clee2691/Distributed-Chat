package server;

import java.util.ArrayList;
import java.util.List;
// Logging imports
import java.util.logging.LogManager;
import java.util.logging.Logger;

import client.ClientInterface;

import java.io.FileInputStream;
import java.io.IOException;

// RMI Imports
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

// Threading support
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Chat coordinator class
 */
public class ChatCoordinator {

    // ===============================

    //          Logging Setup

    // ===============================
    static Logger LOGGER = Logger.getLogger(ChatCoordinator.class.getName());
    static {
        String filePath = "../config/serverlogging.properties";
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(filePath));
        } catch (IOException io)  {
            LOGGER.severe("Logging config file not found.");
        }
    }

    private static List<Integer> serverPorts = new ArrayList<Integer>();
    private static List<ChatServerImpl> chatServers = new ArrayList<ChatServerImpl>();
    private static int largestPropId = 0;
    private static int currLeader = 0;

    // Threading support
    private static ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Parse port arguments for the server replicas
     * I want 5 replicas at least
     * @param args
     */
    private static void parseArgs(String[] args) {
        if (args.length < 5) {
            LOGGER.severe("Invalid usage, you must have at least 5 ports!");
            System.exit(1);
        }

        for (int i = 0; i < args.length; i++) {
            int currPort = Integer.parseInt(args[i]);
            if (currPort <= 1000 && currPort >= 65535) {
                LOGGER.severe("Port must be in Range: 1000-65535!");
                System.exit(1);
            }
            serverPorts.add(currPort);
        }
    }

    /**
     * Keep track of connected clients to the leader.
     * Clean up dead clients from the server
     */
    private static void getLeaderConnClientsheartBeats() {
        while (true) {
            ChatServerImpl leaderServer = chatServers.get(currLeader);
            List<String> connClients = leaderServer.getLoggedInUsers();
            boolean isAlive = false;
            if (connClients.size() > 0){
                LOGGER.info("Checking for client heartbeats");
                Registry remoteReg = leaderServer.getRegistry();
                for (String client: connClients) {
                    try {
                        ClientInterface connClient = (ClientInterface)remoteReg.lookup(String.format("client:%s", client));
                        isAlive = connClient.sendHeartBeat();
                        if (isAlive) {
                            LOGGER.info(String.format("Client: %s is still connected.", client));
                        }
                    } catch (RemoteException re) {
                        LOGGER.severe(String.format("Remote Client: %s is dead!", client));
                        leaderServer.cleanUpClients(client);
                    } catch (NotBoundException nbe) {
                        LOGGER.severe(String.format("Client name: %s is not bound!", client));
                        leaderServer.cleanUpClients(client);
                    }
                }
            } else {
                LOGGER.info(String.format("No connected clients to server on port: %d",leaderServer.getPort()));
            }
            
            try {
                LOGGER.info("Sleeping client heartbeat sensor.");
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                LOGGER.severe("Interrupted sleeping client heartbeat sensor");
            }
        } 
    }

    /**
     * Keep track of which servers are down
     * If the leader is down, elect a new one
     */
    private static void getServerHeartBeats() {
        while (true) {
            boolean isAlive = false;
            for (int i = 0; i < chatServers.size(); i++) {
                ChatServerImpl currServer = chatServers.get(i);
                if (i == currLeader) {
                    // Keep track of highest proposed val so far in case
                    // new leader is elected
                    largestPropId = currServer.getProposer().getPropId();
                }
                
                int serverPort = currServer.getPort();
                try {
                    Registry registry = LocateRegistry.getRegistry(serverPort);
                    ChatServerInterface chatStub = (ChatServerInterface) registry.lookup("chat");
                    isAlive = chatStub.sendHeartBeat();
                    if (isAlive) {
                        LOGGER.info(String.format("Server on port: %d is still alive!", serverPort));
                        // If the current leader's ID is greater than the checked server's ID
                        // Relect the leader since the server is back up
                        if (currLeader > i) {
                            electNewLeader();
                        }
                    }
                } catch (RemoteException re) {
                    LOGGER.severe(String.format("Server on port: %d is dead!", serverPort));
                    if (i == currLeader) {
                        LOGGER.severe(String.format("Server leader on port: %d is down! Need to re-elect a leader", serverPort));
                        electNewLeader();
                    }
                } catch (NotBoundException nbe) {
                    LOGGER.severe(String.format("Server on port: %d is not bound! Restart servers!", serverPort));
                }
            }
    
            try {
                LOGGER.info("Sleeping server heartbeat sensor.");
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                LOGGER.severe("Interrupted sleeping server heartbeat sensor");
            }
        }
    }

    /**
     * Check to see if lower PID servers are alive and return the lowest PID found to be alive so far
     * @param proposedLeader The proposed leader should be alive
     * @return
     */
    private static int checkLowerPids(int proposedLeader) {
        LOGGER.info(String.format("Checking lower pids than: %d", proposedLeader));
        Registry reg;
        ChatServerImpl currServer;
        int serverPort;
        
        for (int i = proposedLeader - 1; i >= 0; i--) {
            currServer = chatServers.get(i);
            serverPort = currServer.getPort();
            try {
                reg = LocateRegistry.getRegistry(serverPort);
                ChatServerInterface chatStub = (ChatServerInterface)reg.lookup("chat");
                if (chatStub.sendHeartBeat()) {
                    LOGGER.info(String.format("Process #: %d is alive!", i));
                    return i;
                }
            } catch (RemoteException re) {
                continue;
            } catch (NotBoundException bne) {
                continue;
            }
        }

        LOGGER.info(
            String.format(
                "No suitable lower pIDs found to be leader. Server #: %d is leader.", 
                proposedLeader));

        return proposedLeader;
    }

    /**
     * Bully algorithm to elect new leader except using lowest process ID instead
     * of highest process ID
     */
    private static void electNewLeader() {
        LOGGER.info("Electing new leader...");
        
        int nextServer = currLeader;
        Registry reg;
        // Keep trying to elect a new leader no matter what
        int retries = 15;
        while (retries > 0) {
            // Choose potential next leader
            nextServer = (nextServer + 1) % chatServers.size();
            // See if it is alive
            ChatServerImpl currServer = chatServers.get(nextServer);
            int serverPort = currServer.getPort();
            try {
                reg = LocateRegistry.getRegistry(serverPort);
                ChatServerInterface chatStub = (ChatServerInterface)reg.lookup("chat");
                // Send message to lower processes to see if they are alive
                if (chatStub.sendHeartBeat()) {
                    int leader = nextServer;

                    while (true) {
                        // Keep checking lower PID numbers until the first server is pinged
                        int found = checkLowerPids(leader);
                        
                        if (found < leader) {
                            leader = found;
                            continue;
                        } else {
                            break;
                        }
                    } 
                    
                    // If a lower PID is found, that is the new leader
                    currLeader = leader;
                    chatServers.get(leader).setIsLeader(true);
                    // Set the proposal ID to be the last leader's proposal ID
                    chatServers.get(nextServer).getProposer().setPropId(largestPropId);
                    LOGGER.info(
                        String.format(
                            "New leader found! Server on port %d is now leader.", 
                            chatServers.get(leader).getPort()));
                    break;
                }
            }  catch (RemoteException re) {
                LOGGER.severe(String.format("Server on port: %d is dead!", serverPort));
            } catch (NotBoundException nbe) {
                LOGGER.severe(String.format("Server on port: %d is not bound! Restart servers!", serverPort));
            }
            try {
                LOGGER.info("Sleeping election retry for 1 second.");
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                LOGGER.severe("Interrupted sleeping election retry sensor");
            }
            retries--;
        }
        if (retries == 0) {
            LOGGER.severe("Max number of retries for election reached. Restart all servers!");
        }
    }

    /**
     * Driver for Chat Coordinator
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        // Set timeouts for responses
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "1000");
        System.setProperty("sun.rmi.dgc.ackTimeout", "1000");
        System.setProperty("sun.rmi.transport.connectionTimeout", "1000");

        // Parse any arguments
        parseArgs(args);

        // Make new servers with the ports by binding them to the registry
        for (int i = 0; i < serverPorts.size(); i++){
            try {
                int currPort = serverPorts.get(i);
                ChatServerImpl newServer = new ChatServerImpl(currPort);

                newServer.setPid(i);
                
                // Set the first replica as the leader
                if (i == 0) {
                    newServer.setIsLeader(true);
                } else {
                    newServer.setIsLeader(false);
                }

                // Add a new chat server impl
                chatServers.add(newServer);

                // Stub the remote object
                ChatServerInterface chatStub = (ChatServerInterface) UnicastRemoteObject.
                                            exportObject(chatServers.get(i), 
                                                         currPort);
                // Creates and exports a Registry instance on the local host that accepts requests on the specified port.
                Registry registry = LocateRegistry.createRegistry(currPort);
                registry.rebind("chat", chatStub);

                chatServers.get(i).setRegistry(registry);

                // Register information of other replicas to the current server.
                // I.e keep information about other servers
                registerServerInfo(currPort);

                LOGGER.info(
                    String.format("Server %d is running at port %d", 
                                            i + 1, 
                                            currPort));
            } catch (RemoteException e) {
                LOGGER.severe("Remote exception occurred! Could not stub and bind registry! Servers failed to start!");
            }
        }

        // Get any connected clients to the leader's heartbeat
        executorService.submit(() -> {
            getLeaderConnClientsheartBeats();
        });
        // Make sure all servers are still alive or at least the leader is alive
        executorService.submit(() -> {
            getServerHeartBeats();
        });

        executorService.submit(() -> {
            
            try {
                LOGGER.info("Sleeping thread to kill server 1 for 10 seconds");
                Thread.sleep(10000);
            } catch (InterruptedException ie) {
                LOGGER.severe("Interrupted sleep before killing server 1.");
            }

            try {
                LOGGER.info("Stopping server 1...");
                UnicastRemoteObject.unexportObject(chatServers.get(0), true);
            } catch (NoSuchObjectException noObj) {
                LOGGER.severe("ERROR stopping server 1.");
            }

            try {
                LOGGER.info("Stopping server 2...");
                UnicastRemoteObject.unexportObject(chatServers.get(1), true);
            } catch (NoSuchObjectException noObj) {
                LOGGER.severe("ERROR stopping server 2.");
            }

            try {
                LOGGER.info("Sleeping thread to start server 2 for 10 seconds");
                Thread.sleep(10000);
            } catch (InterruptedException ie) {
                LOGGER.severe("Interrupted sleep before start server 2.");
            }

            try {
                LOGGER.info("Restarting server 2...");
                // Stub the remote object
                ChatServerInterface chatStub1 = (ChatServerInterface) UnicastRemoteObject.
                                            exportObject(chatServers.get(1),
                                                         5556);
                // Creates and exports a Registry instance on the local host that accepts requests on the specified port.
                Registry registry1 = LocateRegistry.getRegistry(5556);
                registry1.rebind("chat", chatStub1);

            } catch (NoSuchObjectException noObj) {
                LOGGER.severe("ERROR no such object starting server 1.");
            } catch (RemoteException re) {
                LOGGER.severe(re.toString());
                LOGGER.severe("ERROR remoteexception starting server 1.");
            }

        });
    }

    /**
     * Sets the other server information
     * by calling the remote method from the stub
     * @param port
     */
    private static void registerServerInfo(int port){
        try{
            // Look up the bound registry for the specified port
            Registry registry = LocateRegistry.getRegistry(port);
            ChatServerInterface chatStub = (ChatServerInterface) registry.lookup("chat");

            // For each server, keep track of all server ports
            chatStub.setServers(serverPorts, port);

            LOGGER.info(
                String.format(
                    "Successfully set server information for: %d",
                            port));
        } catch (AccessException e) {
            LOGGER.severe(
                String.format(
                    "Could not bind registry for server port: %d! Access error.", 
                    port));
        } catch (RemoteException e) {
            LOGGER.severe(
                String.format(
                "Remote exception for server port: %d!.",
                port));
        } catch (NotBoundException e) {
            LOGGER.severe(
                String.format(
                "Could not bind registry for server on port: %d! Registry name not found!",
                        port));
        }
    }
}
