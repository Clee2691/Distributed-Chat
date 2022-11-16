package client;

// RMI Imports
import java.rmi.Remote;
import java.rmi.RemoteException;

import server.Response;

public interface ClientInterface extends Remote {
    
    /**
     * Called from the server to display messages from others
     * @param message
     * @throws RemoteException
     */
    void displayMessage(String message) throws RemoteException;
}
