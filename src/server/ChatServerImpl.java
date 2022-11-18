package server;

// Log Imports
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;

// RMI Registry Imports
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

// Time Imports
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// Java Imports
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Threading support
import java.util.concurrent.ConcurrentHashMap;

// Paxos Specific
import paxos.Acceptor;
import paxos.Learner;
import paxos.Proposer;

// Custom Imports
import client.ClientInterface;

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

    // Set the registry for this server
    public void setRegistry(Registry currReg) {
        this.remoteReg = currReg;
    }

    // Get the registry for this server
    public Registry getRegistry() {
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

    @Override
    public String logOutUser(String user) {
        // Remove from all rooms
        // Just a final cleanup if something was wrong
        for (Map.Entry<String, List<String>> roomUsers: this.chatRoomUsers.entrySet()) {
            if (roomUsers.getValue().size() > 0) {
              roomUsers.getValue().remove(user);
            }
          }
        return "success";
    }

    // ======================================

    //        Create/ Join a Chat Room

    // =======================================

    @Override
    public String createChatRoom(String chatName, String user) throws RemoteException {        
        if (this.chatRoomUsers.containsKey(chatName)) {
            return "exists";
        }

        // Make a new chat room and put in the user who made the room
        ArrayList<String> userList = new ArrayList<String>();
        userList.add(user);
        this.chatRoomUsers.put(chatName, userList);

        // Start the chatroom's history
        ArrayList<String> messages = new ArrayList<String>();
        this.chatRoomHistory.put(chatName, messages);

        return "success";
    }

    @Override
    public String joinChatRoom(String chatName, String user) {
        // Add user to the room if it contains the key (Room exists)
        if (this.chatRoomUsers.containsKey(chatName)) {
            this.chatRoomUsers.get(chatName).add(user);
            return "success";
        }
        return "fail";
    }

    @Override
    public String leaveChatRoom(String chatname, String user) {
        if (!this.chatRoomUsers.containsKey(chatname)){
            return "fail";
        }
        // Attempt to remove the user
        if (this.chatRoomUsers.get(chatname).remove(user)) {
            return "success";
        }

        return "fail";
    }

    // =======================================================

    //      Broadcast a Message to Room Participants

    // ========================================================

    @Override
    public void broadCastMessage(Instant timeStamp, String user, String chatroom, String message) {
        // If the room is not available just return. Nothing to do
        if (!this.chatRoomUsers.containsKey(chatroom)) {
            return;
        }
        // Format the timeStamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault());

        String formattedTime = formatter.format(timeStamp);
        // Add it to the room's message history
        this.chatRoomHistory.get(chatroom).add(
            String.format("[%s] %s: %s", formattedTime, user, message));

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

    @Override
    public void notifyJoinLeave(String chatroom, String user) {
        // Iterate through all clients currently connected to room on the server.
        List<String> currRoomUsers = this.chatRoomUsers.get(chatroom);
        for (String name : currRoomUsers) {
            try {
                // Look up the client in the registry and call its displayMessage remote method
                ClientInterface client = (ClientInterface)remoteReg.lookup(String.format("client:%s", name));
                client.notifyJoinLeave();
                LOGGER.info(String.format("Notified %s of %s", chatroom, user));
            } catch (NotBoundException nbe) {
                LOGGER.severe(String.format("User: %s is no longer connected. Not bound to registry.", name));
            } catch (RemoteException re) {
                LOGGER.severe(String.format("Error accessing the remote: %s.", name));
            }
        }
    }

    @Override
    public Map<String, List<String>> getChatRoomInformation() {

        if (this.chatRoomUsers.isEmpty()) {
            return null;
        }

        return this.chatRoomUsers;
    }

    @Override
    public List<String> getChatUsers(String chatName) {
        if (!this.chatRoomUsers.containsKey(chatName)) {
            return null;
        }
        return this.chatRoomUsers.get(chatName);
    }

    @Override
    public List<String> getChatRoomMessageHistory(String chatName) {
        if (this.chatRoomHistory.containsKey(chatName)) {
            return this.chatRoomHistory.get(chatName);
        }
        return null;
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