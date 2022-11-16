package server;

// Net Imports
import java.net.SocketTimeoutException;

// RMI Imports
import java.rmi.Remote;
import java.rmi.RemoteException;

// Java Imports
import java.util.List;
import java.util.UUID;

/**
 * The Key Value Store Remote Interface for RMI
 */
public interface ChatServerInterface extends Remote{
    
    // ====================================

    //          Register/Login
    
    // ====================================

    /**
     * Register a user with the chosen username and password. Internal Map
     * is the database.
     * @param username A unique username
     * @param password Any password
     * @return Response object containing success or failure message
     * @throws RemoteException
     */
    Response registerUser(String username, String password) throws RemoteException;

    /**
     * Log in a user if the username and password supplied are valid
     * in the database.
     * @param username The username
     * @param password The password
     * @return Response object containing success or failure message
     * @throws RemoteException
     */
    Response loginUser(String username, String password) throws RemoteException;

    // ====================================

    //     Chat Server Creation/Join
    
    // ====================================

    String createChatRoom(String chatName, String user) throws RemoteException;

    String joinChatRoom(String chatName, String user) throws RemoteException;

    void broadCastMessage(String chatroom, String message) throws RemoteException;

    /**
     * Sets the server information to keep all server replicas connected
     * @param otherServers List of other server ports.
     * @throws RemoteException
     */
    void setServers(List<Integer> otherServers, int port) throws RemoteException;

    // ====================================
    
    //         Paxos Methods
    //      Prepare, Propose, Accept
    
    // =====================================

    /**
     * Send prepare messages to the acceptors with the proposal ID
     * @param propId The proposal ID
     * @return True -> Promise | False -> Deny
     * @throws RemoteException
     * @throws SocketTimeoutException
     */
    boolean prepare(int propId) throws RemoteException, SocketTimeoutException;

    /**
     * Send a request to accept the proposal to the acceptor
     * @param propId The proposal ID
     * @param val The proposed value
     * @return The KVOperation value that the acceptor accepts or null
     * @throws RemoteException
     * @throws SocketTimeoutException
     */
    KVOperation accept(int propId, KVOperation val) throws RemoteException, SocketTimeoutException;

    /**
     * Send a commit to the learners to commit the accepted proposal and value
     * @param theVal The KVOperation as the request that was accepted
     * @return String that is the result of the request.
     * @throws RemoteException
     */
    String commit(KVOperation theVal) throws RemoteException;
}
