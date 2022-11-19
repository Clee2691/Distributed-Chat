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
     * If the leader is down, elect a new one!
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
                    }
                } catch (RemoteException re) {
                    LOGGER.severe(String.format("Server on port: %d is dead!", serverPort));
                    // TODO:ELECT A NEW LEADER!
                    if (i == currLeader) {
                        LOGGER.severe(String.format("Server leader on port: %d is down! Need to re-elect a leader", serverPort));
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
     * Driver for Chat Coordinator
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "1000");
        System.setProperty("sun.rmi.dgc.ackTimeout", "1000");
        System.setProperty("sun.rmi.transport.connectionTimeout", "1000");
        parseArgs(args);
        

        // Make new servers with the ports by binding them to the registry
        for (int i = 0; i < serverPorts.size(); i++){
            try{
                int currPort = serverPorts.get(i);
                // Add a new chat server impl
                chatServers.add(new ChatServerImpl(currPort));

                // Set the first replica as the leader
                if (i == 0) {
                    chatServers.get(i).setIsLeader(true);
                } else {
                    chatServers.get(i).setIsLeader(false);
                }
                
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
