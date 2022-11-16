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

    /**
     * Driver for Chat Coordinator
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{

        try{
            // New chat server implementation
            ChatServerImpl chat = new ChatServerImpl(5555);
            
            // Stub the remote object
            ChatServerInterface chatStub = (ChatServerInterface) UnicastRemoteObject.
                                        exportObject(chat,5555);
            // Creates and exports a Registry instance on the local host that accepts requests on the specified port.
            Registry registry = LocateRegistry.createRegistry(5555);
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
