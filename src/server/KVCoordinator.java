package server;

// Logging imports
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;

// RMI Imports
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

// Java Imports
import java.util.ArrayList;
import java.util.List;

/**
 * Coordinator class that sets up all of the server replicas
 */
public class KVCoordinator {

    // Set up logging with a custom properties file
    static Logger LOGGER = Logger.getLogger(KVCoordinator.class.getName());
    static {
        String filePath = "../config/serverlogging.properties";
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(filePath));
        } catch (IOException io)  {
            LOGGER.severe("Logging config file not found.");
        }
    }

    // List of all server replicas
    private static List<ChatServerImpl> kvServers = new ArrayList<ChatServerImpl>();

    // All of the ports used for server replicas
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
     * Driver for KVCoordinator
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{

        // Parse the arguments
        parseArgs(args);

        // Make new servers with the ports by binding them to the registry
        for (int i = 0; i < serverPorts.size(); i++){
            try{
                int currPort = serverPorts.get(i);
                // Add a new keyval server to the list
                kvServers.add(new ChatServerImpl(currPort));
                
                // Stub the remote object
                ChatServerInterface kvStub = (ChatServerInterface) UnicastRemoteObject.
                                            exportObject(kvServers.get(i), 
                                                         currPort);
                // Creates and exports a Registry instance on the local host that accepts requests on the specified port.
                Registry registry = LocateRegistry.createRegistry(currPort);
                registry.bind("KVS", kvStub);

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
            ChatServerInterface kvStub = (ChatServerInterface) registry.lookup("KVS");

            // For each server, keep track of all server ports
            kvStub.setServers(serverPorts, port);

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
