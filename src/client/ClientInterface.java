package client;

// RMI Imports
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for the chat client
 */
public interface ClientInterface extends Remote {
    
    /**
     * Called from the server to display messages from others to itself
     * @param sender The sender of the message
     * @param message The message to display
     * @throws RemoteException
     */
    void displayMessage(String sender, String message) throws RemoteException;

    /**
     * Notify self that a user has joined or left the chatroom
     * The server calls this function whenever a person joins or leaves a chatroom
     * @throws RemoteException
     */
    void notifyJoinLeave() throws RemoteException;

    /**
     * Send a heartbeat back to whoever called it.
     * @return True if alive
     * @throws RemoteException
     */
    boolean sendHeartBeat() throws RemoteException;
}
