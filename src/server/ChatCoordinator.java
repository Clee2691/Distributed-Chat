package server;

import java.util.ArrayList;
import java.util.List;
// Logging imports
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;

// RMI Imports
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

/**
 * Chat coordinator class
 */
public class ChatCoordinator {

    // ===============================

    //          Logging Setup

    // ===============================
    static Logger LOGGER = Logger.getLogger(KVCoordinator.class.getName());
    static {
        String filePath = "../config/serverlogging.properties";
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(filePath));
        } catch (IOException io)  {
            LOGGER.severe("Logging config file not found.");
        }
    }

    private static List<Integer> serverPorts = new ArrayList<Integer>();

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
     * Driver for Chat Coordinator
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
        parseArgs(args);
        try{
            // New chat server implementation
            ChatServerImpl chat = new ChatServerImpl(serverPorts.get(0));
            
            // Stub the remote object
            ChatServerInterface chatStub = (ChatServerInterface) UnicastRemoteObject.
                                        exportObject(chat,serverPorts.get(0));
            // Creates and exports a Registry instance on the local host that accepts requests on the specified port.
            Registry registry = LocateRegistry.createRegistry(serverPorts.get(0));
            registry.bind("chat", chatStub);

            // Set the registry for the server
            chat.setRegistry(registry);

            LOGGER.info(
                String.format("Server is running at port %d",5555));

        } catch (RemoteException e) {
            LOGGER.severe("Remote exception occurred! Could not stub and bind registry! Servers failed to start!");
        }
        
    }
}
