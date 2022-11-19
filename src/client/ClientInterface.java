package client;

// RMI Imports
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientInterface extends Remote {
    
    /**
     * Called from the server to display messages from others to itself
     * @param timeStamp The time stamp of the message
     * @param sender The sender of the message
     * @param message The message to display
     * @throws RemoteException
     */
    void displayMessage(String sender, String message) throws RemoteException;

    /**
     * Notify self that a user has joined or left the chatroom
     * The server calls this function whenever a person joins or leaves a chatroom
     * @param timeStamp The time stamp of joining or leaving
     * @param user The user that joined or left
     * @throws RemoteException
     */
    void notifyJoinLeave() throws RemoteException;
}
