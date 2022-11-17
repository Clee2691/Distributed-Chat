package server;

// Log Imports
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import client.ClientInterface;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;

// RMI Registry Imports
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.time.Instant;
// Java Imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Threading support
import java.util.concurrent.ConcurrentHashMap;

// Paxos Specific
import paxos.Acceptor;
import paxos.Learner;
import paxos.Proposer;

/**
 * Class implementing the ChatServerInterface
 */
public class ChatServerImpl implements ChatServerInterface {
    /**
     * Logging Setup
     */
    static Logger LOGGER = Logger.getLogger(ChatServerImpl.class.getName());
    static {
        String filePath = "../config/serverlogging.properties";
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(filePath));
        } catch (IOException io)  {
            LOGGER.severe("Logging config file not found.");
        }
    }

    int port;

    // Paxos
    private Proposer proposer;
    private Acceptor acceptor;
    private Learner learner;

    // User database
    // Should be a map of username : user stat object like their PW or if they are active
    private Map<String,String> userDatabase;

    // The rooms and associated users
    // Name of room : List of users in the room
    private Map<String, List<String>> chatRoomUsers;

    // The rooms and their chat histories
    // Room name: List of messages
    private Map<String,List<String>> chatRoomHistory;

    private Registry remoteReg;

    /**
     * Empty constructor initializing the store.
     */
    public ChatServerImpl(int p) {
        this.userDatabase = new ConcurrentHashMap<String,String>();
        this.chatRoomUsers = new ConcurrentHashMap<String, List<String>>();
        this.chatRoomHistory = new ConcurrentHashMap<String, List<String>>();

        // Paxos proposer,acceptor,learner
        this.proposer = new Proposer();
        this.acceptor = new Acceptor();
        this.learner = new Learner();

        this.port = p;

    }

    public void setRegistry(Registry currReg) {
        this.remoteReg = currReg;
    }

    public Registry geRegistry() {
        return this.remoteReg;
    }

    public void setServers(List<Integer> otherPorts, int port) {
        this.proposer.setPorts(otherPorts);
        this.acceptor.setServerPort(port);
    }

    // =========================

    //      Register/Login

    // =========================

    // TODO: Use PAXOS for consensus for registering
    @Override
    public synchronized Response registerUser(String username, String password) {
        // If username already in the store, user must choose a different username
        if (userDatabase.containsKey(username)) {
            String mess = String.format("Username: %s already exists!", username);
            LOGGER.severe(mess);
            return new Response(Level.SEVERE, mess);
        }
        userDatabase.put(username, password);
        LOGGER.info(String.format("Successfully registered user with username: %s.", username));
        return new Response(Level.INFO, "success");
    }

    @Override
    public synchronized Response loginUser(String username, String password) {
        // If username already in the store, user must choose a different username
        if (!userDatabase.containsKey(username)) {
            return new Response(Level.SEVERE, "incorrect");
        }

        // Check password against entered password
        String dbPass = userDatabase.get(username);
        if (password.equals(dbPass)) {
            return new Response(Level.INFO, "success");
        } else if (!password.equals(dbPass)) {
            return new Response(Level.SEVERE, "incorrect");
        }

        return new Response(Level.SEVERE, "fail");
    }

    // ======================================

    //        Create/ Join a Chat Room

    // =======================================

    public String createChatRoom(String chatName, String user) throws RemoteException {        
        if (this.chatRoomUsers.containsKey(chatName)) {
            return "exists";
        }
        // Make a new chat room and put in the user who made the room
        ArrayList<String> userList = new ArrayList<String>();
        userList.add(user);
        this.chatRoomUsers.put(chatName, userList);
        return "success";
    }

    public String joinChatRoom(String chatName, String user) {
        // Add user to the room if it contains the key
        if (this.chatRoomUsers.containsKey(chatName)) {
            this.chatRoomUsers.get(chatName).add(user);
            // Broadcast to everyone in the chatroom that a user has joined
            List<String> currRoomUsers = this.chatRoomUsers.get(chatName);
            for (String name : currRoomUsers) {
                if (name.equals(user)){
                    continue;
                }
                try {
                    // Look up the client in the registry and call its displayMessage remote method
                    ClientInterface client = (ClientInterface)remoteReg.lookup(String.format("client:%s", name));
                    client.displayUserJoinLeave(Instant.now(), user, "joined");
                    LOGGER.info(String.format("User: %s joined chatroom: %s.", user, chatName));
                } catch (NotBoundException nbe) {
                    LOGGER.severe(String.format("User: %s is no longer connected. Not bound to registry.", name));
                } catch (RemoteException re) {
                    LOGGER.severe(String.format("Error accessing the remote: %s.", name));
                }
            }
            return "success";
        }
        return "fail";
    }

    // =======================================================

    //      Broadcast a Message to Room Participants

    // ========================================================

    public void broadCastMessage(Instant timeStamp, String user, String chatroom, String message) {
        // If the room is not available just return. Nothing to do
        if (!this.chatRoomUsers.containsKey(chatroom)) {
            return;
        }

        // Iterate through all clients currently connected to room on the server.
        List<String> currRoomUsers = this.chatRoomUsers.get(chatroom);
        for (String name : currRoomUsers) {
            try {
                // Look up the client in the registry and call its displayMessage remote method
                ClientInterface client = (ClientInterface)remoteReg.lookup(String.format("client:%s", name));
                client.displayMessage(timeStamp, user, message);
                LOGGER.info(String.format("User: %s broadcasted message to: %s in chatroom: %s", user, name, chatroom));
            } catch (NotBoundException nbe) {
                LOGGER.severe(String.format("User: %s is no longer connected. Not bound to registry.", name));
            } catch (RemoteException re) {
                LOGGER.severe(String.format("Error accessing the remote: %s.", name));
            }
        }
    }

    public void notifyJoinLeave(Instant timeStamp, String user, String chatroom, String joinLeave) {
        
    }


    public Map<String,Integer> getChatRoomInformation() {
        Map<String, Integer> roomsAndUsers = new HashMap<String, Integer>();

        if (this.chatRoomUsers.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, List<String>> roomUsers : this.chatRoomUsers.entrySet()) {
            roomsAndUsers.put(roomUsers.getKey(), roomUsers.getValue().size());
        }
        return roomsAndUsers;
    }

    public List<String> getChatUsers(String chatName) {
        if (!this.chatRoomUsers.containsKey(chatName)) {
            return null;
        }
        return this.chatRoomUsers.get(chatName);
    }

    // =========================
    // Key Value Store Functions
    // =========================

    /**
     * Get the server store
     * @return The server's store
     */
    public Map<String,String> getUserStore() {
        return this.userDatabase;
    }

    /**
     * Get the associated value from a key
     * @param key a key to query the store
     * @return The value or null if not found.
     */
    public String getRequest(String key) throws InterruptedException {
        synchronized(userDatabase){
            if (userDatabase.containsKey(key)) {
                return userDatabase.get(key);
            }
            return null;
        }
    }

    /**
     * Get if store is empty
     * @return True if empty, false if not empty
     */
    public boolean isEmpty() {
        if (userDatabase.isEmpty()) {
            return true;
        }
        return false;
    }
    

    // ======================================

    //          PAXOS Methods
    //      Prepare, Accept, Commit

    // =====================================

    public boolean prepare(int propId) {
        boolean prepped = false;
        try {
            prepped = this.acceptor.prepare(propId);
        } catch(SocketTimeoutException ste) {
            LOGGER.severe("Timed out receiving prepare message from acceptor!");
        }
        return prepped;
    }

    public KVOperation accept(int propId, KVOperation val) {
        KVOperation accepted = null;
        try {
            accepted = this.acceptor.accept(propId, val);
        } catch(SocketTimeoutException ste) {
            LOGGER.severe("Timed out receiving prepare message from acceptor!");
        }
        return accepted;
    }

    public String commit(KVOperation val) {
        return this.learner.commit(userDatabase, val);
    }
}