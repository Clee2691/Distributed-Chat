package server;

// Log Imports
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    private int port;

    // Paxos
    private Proposer proposer;
    private Acceptor acceptor;
    private Learner learner;

    // Threading support
    ExecutorService executorService;

    // User database
    // Should be a map of username : user stat object like their PW or if they are active
    private Map<String, String> userDatabase;
    private List<String> loggedInUsers;
    // The rooms and associated users
    // Name of room : List of users in the room
    private Map<String, List<String>> chatRoomUsers;

    // The rooms and their chat histories
    // Room name: List of messages
    private Map<String, List<String>> chatRoomHistory;

    private Registry remoteReg;

    /**
     * Empty constructor initializing the store.
     */
    public ChatServerImpl(int p) {
        this.userDatabase = new ConcurrentHashMap<String, String>();
        this.chatRoomUsers = new ConcurrentHashMap<String, List<String>>();
        this.chatRoomHistory = new ConcurrentHashMap<String, List<String>>();
        this.loggedInUsers = new ArrayList<String>();

        // Paxos proposer,acceptor,learner
        // Every server is it's own proposer, acceptor, and learner
        this.proposer = new Proposer();
        this.acceptor = new Acceptor();
        this.learner = new Learner();

        executorService = Executors.newFixedThreadPool(50);

        this.port = p;

        // Set some timeouts for RMI calls
        // Only allow 1 second between calls
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "1000");
        System.setProperty("sun.rmi.dgc.ackTimeout", "1000");
        System.setProperty("sun.rmi.transport.connectionTimeout", "1000");
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
    }

    // =========================

    //      Register/Login

    // =========================

    @Override
    public Response registerUser(String username, String password) {
        // If username already in the store, user must choose a different username
        if (userDatabase.containsKey(username)) {
            String mess = String.format("Username: %s already exists!", username);
            LOGGER.severe(mess);
            return new Response(Level.SEVERE, mess);
        }
        // Start paxos for registering
        Future<Response> future = executorService.submit(() -> {
            return this.proposer.propose("register", username, password, "", "");
        });
        
        // Get the final result and send to client if it was successful or not
        try {
            Response res = future.get();
            if (res.getServerReply().equals("success")) {
                LOGGER.info(String.format("Successfully registered user with username: %s.", username));
                return new Response(Level.INFO, "success");
            }
            return new Response(Level.INFO, "Error registering. Try again.");
        } catch (InterruptedException ie) {
            LOGGER.severe("Error registering.");
            return new Response(Level.INFO, "Server interrupted register. Try again.");
        } catch (ExecutionException ee) {
            LOGGER.severe("Error registering.");
            return new Response(Level.INFO, "Error registering. Try again.");
        }
    }

    @Override
    public Response loginUser(String username, String password) {
        // No username found
        if (!userDatabase.containsKey(username)) {
            return new Response(Level.SEVERE, "incorrect");
        }

        // Check to see if user is in the active user list
        for (String user : loggedInUsers) {
            if (user.equals(username)) {
                return new Response(Level.SEVERE, "loggedIn");
            }
        }

        // Check password against entered password
        String dbPass = userDatabase.get(username);
        if (password.equals(dbPass)) {
            // Start paxos for logging in
            Future<Response> future = executorService.submit(() -> {
                return this.proposer.propose("login", username, password, "", "");
            });

            try {
                Response res = future.get();
                if (res.getServerReply().equals("success")) {
                    LOGGER.info(String.format("Successfully logged in user with username: %s.", username));
                    return new Response(Level.INFO, "success");
                }
                return new Response(Level.INFO, "Error logging in. Try again.");
            } catch (InterruptedException ie) {
                LOGGER.severe("Error login.");
                return new Response(Level.INFO, "Server interrupted login. Try again.");
            } catch (ExecutionException ee) {
                LOGGER.severe("Error login.");
                return new Response(Level.INFO, "Error logging in. Try again.");
            }

        } else if (!password.equals(dbPass)) {
            return new Response(Level.SEVERE, "incorrect");
        }

        return new Response(Level.SEVERE, "fail");
    }

    @Override
    public String logOutUser(String user) {
        // Start paxos for logging out a user
        Future<Response> future = executorService.submit(() -> {
            return this.proposer.propose("logout", user, "", "", "");
        });

        try {
            Response res = future.get();
            if (res.getServerReply().equals("success")) {
                LOGGER.info(String.format("Successfully logged out user with username: %s.", user));
                return "success";
            }
            return "fail";
        } catch (InterruptedException ie) {
            LOGGER.severe("Error login.");
            return "fail";
        } catch (ExecutionException ee) {
            LOGGER.severe("Error login.");
            return "fail";
        }
    }

    // ======================================

    //        Create/ Join a Chat Room

    // =======================================

    @Override
    public String createChatRoom(String chatName, String user) throws RemoteException {        
        if (this.chatRoomHistory.containsKey(chatName)) {
            return "exists";
        }

        // Start paxos for creating a chat room
        Future<Response> future = executorService.submit(() -> {
            return this.proposer.propose("create", user, "", "", chatName);
        });

        try {
            Response res = future.get();
            if (res.getServerReply().equals("success")) {
                LOGGER.info(String.format("Successfully created chatroom: %s.", chatName));
                return "success";
            }
            return "fail";
        } catch (InterruptedException ie) {
            LOGGER.severe("Error creating chatroom.");
            return "fail";
        } catch (ExecutionException ee) {
            LOGGER.severe("Error creating chatroom.");
            return "fail";
        }
    }

    @Override
    public String joinChatRoom(String chatName, String user) {
        // Start paxos for joining a chat room
        Future<Response> future = executorService.submit(() -> {
            return this.proposer.propose("join", user, "", "", chatName);
        });

        try {
            Response res = future.get();
            if (res.getServerReply().equals("success")) {
                LOGGER.info(String.format("Successfully joined chatroom: %s.", chatName));
                return "success";
            }
            return "fail";
        } catch (InterruptedException ie) {
            LOGGER.severe("Error joining chatroom.");
            return "fail";
        } catch (ExecutionException ee) {
            LOGGER.severe("Error joining chatroom.");
            return "fail";
        }   
    }

    @Override
    public String leaveChatRoom(String chatName, String user) {
        
        if (!this.chatRoomUsers.containsKey(chatName)){
            return "fail";
        }

        // Start paxos for leaving a chatroom
        Future<Response> future = executorService.submit(() -> {
            return this.proposer.propose("leave", user, "", "", chatName);
        });

        try {
            Response res = future.get();
            if (res.getServerReply().equals("success")) {
                LOGGER.info(String.format("Successfully left chatroom: %s.", chatName));
                return "success";
            }
            return "fail";
        } catch (InterruptedException ie) {
            LOGGER.severe("Error leaving chatroom.");
            return "fail";
        } catch (ExecutionException ee) {
            LOGGER.severe("Error leaving chatroom.");
            return "fail";
        } 
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

        // The message
        String finalMessage = String.format("[%s] %s: %s", formattedTime, user, message);

        // Start paxos for broadcasting a message to a room.
        // Keeps the chat room history in consensus for all replicas
        Future<Response> future = executorService.submit(() -> {
            return this.proposer.propose("send", user, "", finalMessage, chatroom);
        });

        try {
            Response res = future.get();
            if (res.getServerReply().equals("success")) {
                LOGGER.info(String.format("Successfully sent %s to chatroom: %s.", finalMessage, chatroom));
            } else {
                return;
            }
        } catch (InterruptedException ie) {
            LOGGER.severe("Error sending message to chatroom.");
        } catch (ExecutionException ee) {
            LOGGER.severe("Error sending message chatroom.");
        } 

        // Iterate through all clients currently connected to room on the server.
        List<String> currRoomUsers = this.chatRoomUsers.get(chatroom);
        for (String name : currRoomUsers) {
            try {
                // Look up the client in the registry and call its displayMessage remote method
                ClientInterface client = (ClientInterface)remoteReg.lookup(String.format("client:%s", name));
                client.displayMessage(user, finalMessage);
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

    @Override
    public boolean prepare(int propId) {
        boolean prepped = this.acceptor.prepare(propId);
        return prepped;
    }

    @Override
    public DBOperation accept(int propId, DBOperation val) {
        DBOperation accepted = this.acceptor.accept(propId, val);
        return accepted;
    }

    @Override
    public String commit(int propId, DBOperation dbOp) {
        return this.learner.commit(propId, userDatabase, chatRoomUsers, chatRoomHistory, loggedInUsers, dbOp);
    }
}