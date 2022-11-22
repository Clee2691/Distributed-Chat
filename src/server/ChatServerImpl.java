package server;

// Log Imports
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // Unique process ID
    private int pId;
    private int port;
    private boolean isLeader;


    // Paxos
    private Proposer proposer;
    private Acceptor acceptor;
    private Learner learner;

    // Threading support
    private ExecutorService executorService;

    private Set<String> loggedInUsers;

    // User database
    // Should be a map of username : user stat object like their PW or if they are active
    private Map<String, String> userDatabase;

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
        this.loggedInUsers = new HashSet<String>();

        // Paxos proposer,acceptor,learner
        // Every server is it's own proposer, acceptor, and learner
        this.proposer = new Proposer();
        this.acceptor = new Acceptor();
        this.learner = new Learner();
        this.isLeader = false;

        executorService = Executors.newFixedThreadPool(50);

        this.port = p;

        // Set some timeouts for RMI calls
        // Only allow 1 second between calls
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "1000");
        System.setProperty("sun.rmi.dgc.ackTimeout", "1000");
        System.setProperty("sun.rmi.transport.connectionTimeout", "1000");
    }

    /**
     * Set the registry for this server
     * @param currReg The registry object
     */
    public void setRegistry(Registry currReg) {
        this.remoteReg = currReg;
    }

    /**
     * Get the registry of this server
     * @return The registry object
     */
    public Registry getRegistry() {
        return this.remoteReg;
    }

    /**
     * Set the server list of all other servers
     * @param otherPorts A list of other server ports
     * @param port The port this server is on
     */
    public void setServers(List<Integer> otherPorts, int port) {
        this.proposer.setPorts(otherPorts);
    }

    /**
     * Set this server as the leader or not
     * @param lead Boolean true if leader or false if not
     */
    public void setIsLeader(boolean lead) {
        this.isLeader = lead;
    }

    /**
     * Get the leader status of the server
     * @return True if leader false otherwise
     */
    public boolean getIsLeader() {
        return this.isLeader;
    }

    /**
     * Get a set of the logged in users of this server
     * @return Set
     */
    public Set<String> getLoggedInUsers() {
        return this.loggedInUsers;
    }

    /**
     * Set the logged in user set
     * @param activeUsers Set of the logged in users
     */
    public void setLoggedInUsers(Set<String> activeUsers) {
        this.loggedInUsers.addAll(activeUsers);
    }

    /**
     * Get the proposer
     * @return Proposer object
     */
    public Proposer getProposer() {
        return this.proposer;
    }

    /**
     * Get the server's port
     * @return Integer port
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Get the PID of this server
     * @return Integer PID
     */
    public int getPid() {
        return this.pId;
    }

    /**
     * Set the PID of this server
     * @param id The id of the server
     */
    public void setPid(int id) {
        this.pId = id;
    }

    /**
     * Get this server's chat room history
     * @return All chat rooms and their histories
     */
    public Map<String, List<String>> getChatRoomHistory() {
        return this.chatRoomHistory;
    }

    /**
     * Set the chatroom history map
     * @param history The chatroom history map
     */
    public void setChatRoomHistory(Map<String, List<String>> history) {
        this.chatRoomHistory = history;
    }

    /**
     * Get the servers chatroom and their users
     * @return The chatroom user map
     */
    public Map<String, List<String>> getChatRoomUsers() {
        return this.chatRoomUsers;
    }

    /**
     * Set the server's chatrooms and their users
     * @param users Map of chatrooms and their users
     */
    public void setChatRoomUsers(Map<String, List<String>> users) {
        this.chatRoomUsers = users;
    }

    /**
     * Set the user database
     * @param userdb The Map of usernames and passwords
     */
    public void setUserDB(Map<String, String> userdb) {
        this.userDatabase = userdb;
    }

    /**
     * Get the server's user database
     * @return Map of usernames and passwords
     */
    public Map<String, String> getUserDB() {
        return this.userDatabase;
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
            return;
        } catch (ExecutionException ee) {
            LOGGER.severe(ee.toString());
            LOGGER.severe("Error sending message chatroom.");
            return;
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
            } catch (NullPointerException npe) {
                LOGGER.severe("NAME IS NULL! Could not broadcast!");
            }
        }
    }

    @Override
    public void notifyJoinLeave(String chatroom, String user) {
        // Iterate through all clients currently connected to room on the server.
        List<String> currRoomUsers = this.chatRoomUsers.get(chatroom);
        for (String name : currRoomUsers) {
            try {
                if (name.equals(user)){
                    continue;
                }
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
        return getChatRoomUsers();
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

        synchronized(this.chatRoomHistory) {
            if (this.chatRoomHistory.containsKey(chatName)) {
                List<String> finalMessages = this.chatRoomHistory.get(chatName);
                List<String> listWithoutDuplicates = finalMessages.stream()
                    .distinct().collect(Collectors.toList());
                return listWithoutDuplicates;
            }
        }
        return null;
    }

    @Override
    public void cleanUpClients(String clientName) {
        // Start paxos for logging out a user which essentially cleans the client from the server
        executorService.submit(() -> {
            this.proposer.propose("logout", clientName, "", "", "");
            LOGGER.info(String.format("Successfully cleaned up client: %s.", clientName));
        });
    }

    @Override
    public boolean sendHeartBeat() {
        return true;
    }
    
    @Override
    public boolean sendIsLeader() {
        return this.isLeader;
    }

    // ======================================

    //          PAXOS Methods
    //      Prepare, Accept, Commit

    // =====================================

    @Override
    public boolean prepare(int propId) {
        return this.acceptor.prepare(propId);
    }

    @Override
    public DBOperation accept(int propId, DBOperation val) {
        return this.acceptor.accept(propId, val);
    }

    @Override
    public String commit(int propId, DBOperation dbOp) {
        return this.learner.commit(propId, userDatabase, chatRoomUsers, chatRoomHistory, loggedInUsers, dbOp);
    }
}