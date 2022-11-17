package client;

// RMI Imports
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.Instant;

public interface ClientInterface extends Remote {
    
    /**
     * Called from the server to display messages from others to itself
     * @param timeStamp The time stamp of the message
     * @param sender The sender of the message
     * @param message The message to display
     * @throws RemoteException
     */
    void displayMessage(Instant timeStamp, String sender, String message) throws RemoteException;

    /**
     * Notify that a user has joined or left
     * @param timeStamp The time stamp of joining or leaving
     * @param user The user that joined or left
     * @throws RemoteException
     */
    void displayUserJoinLeave(Instant timeStamp, String user, String action) throws RemoteException;
}
