package server;

// Net Imports
import java.net.SocketTimeoutException;

// RMI Imports
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.Instant;
// Java Imports
import java.util.List;
import java.util.Map;
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

    /**
     * Allow a user to create a chatroom with the name.
     * @param chatName The room name
     * @param user The user's name
     * @return String indicating "success" or "fail"
     * @throws RemoteException
     */
    String createChatRoom(String chatName, String user) throws RemoteException;

    /**
     * Allow a user to join a chatroom with the given chatroom name
     * @param chatName The room name
     * @param user The user requesting access to the room
     * @return String indicating "success" or "fail"
     * @throws RemoteException
     */
    String joinChatRoom(String chatName, String user) throws RemoteException;

    /**
     * Allow the server to broadcast a message to other users in the room.
     * This is group communication
     * @param user The user that sent a message to be broadcast
     * @param chatroom The chatroom name. Wherever the user is located.
     * @param message The message to broadcast.
     * @throws RemoteException
     */
    void broadCastMessage(Instant timeStamp, String user, String chatroom, String message) throws RemoteException;

    /**
     * Notify the server to broadcast the joining or leaving of a user in the chatroom
     * @param timeStamp
     * @param user
     * @param chatroom
     * @param joinLeave
     * @throws RemoteException
     */
    void notifyJoinLeave(Instant timeStamp, String user, String chatroom, String joinLeave) throws RemoteException;

    // ====================================

    //      Server information
    
    // ====================================

    /**
     * Get the current active chatrooms and the number of users in them.
     * @return Map of room names and the current number of users
     * @throws RemoteException
     */
    Map<String,Integer> getChatRoomInformation() throws RemoteException;

    /**
     * Get the users within the specified chat room
     * @param chatName The chatroom name
     * @return The list of users currently in the chatroom
     * @throws RemoteException
     */
    List<String> getChatUsers(String chatName) throws RemoteException;

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
