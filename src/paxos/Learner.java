package paxos;

// Log Imports
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
// Java Imports
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Custom Imports
import server.DBOperation;

/**
 * Learner class that serves as the replication factor. This class is the one
 * that executes and commits the actual requests.
 */
public class Learner extends Thread {
    // Set up logging with a custom properties file
    static Logger LOGGER = Logger.getLogger(Learner.class.getName());
    static {
        String filePath = "../config/serverlogging.properties";
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(filePath));
        } catch (IOException io)  {
            LOGGER.severe("Logging config file not found.");
        }
    }

    private Map<Integer, DBOperation> acceptedVals;

    public Learner() {
        this.acceptedVals = new ConcurrentHashMap<Integer, DBOperation>();
    }

    /**
     * The commit method that manipulates the data store
     * @param propId The current proposal ID
     * @param userStore The user stored database
     * @param chatRoomUsers The chatroom and its users
     * @param chatRoomHistory The chatroom's history
     * @param activeUsers Currently active users
     * @param dbOp The operation to commit
     * @return success or failure string
     */
    public synchronized String commit(int propId, Map<String,String> userStore, 
                                        Map<String, List<String>> chatRoomUsers,
                                        Map<String, List<String>> chatRoomHistory,
                                        Set<String> activeUsers,
                                        DBOperation dbOp) {
        acceptedVals.put(propId, dbOp);

        // Commit the specified operation
        if (dbOp.getOp().equals("register")){
            userStore.put(dbOp.getUsername(), dbOp.getPassword());
            activeUsers.add(dbOp.getUsername());
            return "success";

        } else if (dbOp.getOp().equals("login")){
            activeUsers.add(dbOp.getUsername());
            return "success";

        } else if (dbOp.getOp().equals("logout")){
            for (Map.Entry<String, List<String>> roomUsers: chatRoomUsers.entrySet()) {
                if (roomUsers.getValue().size() > 0) {
                  roomUsers.getValue().remove(dbOp.getUsername());
                }
            }
            activeUsers.remove(dbOp.getUsername());
            return "success";

        } else if (dbOp.getOp().equals("create")) {
            // Initialize the room history and room users
            List<String> chatUsers = new ArrayList<String>();
            chatUsers.add(dbOp.getUsername());
            List<String> chatHistory = new ArrayList<String>();

            chatRoomUsers.put(dbOp.getChatroom(), chatUsers);
            chatRoomHistory.put(dbOp.getChatroom(), chatHistory);
            return "success";
        } else if (dbOp.getOp().equals("join")) {
            // Add user to the room if it contains the key (Room exists)

            if (chatRoomHistory.containsKey(dbOp.getChatroom())) {
                chatRoomUsers.get(dbOp.getChatroom()).add(dbOp.getUsername());
                return "success";
            }
            return "fail";
        } else if (dbOp.getOp().equals("send")) {
            // Add it to the room's message history
            chatRoomHistory.get(dbOp.getChatroom()).add(dbOp.getMessage());

            return "success";
        } else if (dbOp.getOp().equals("leave")){
            // Attempt to remove the user
            if (chatRoomUsers.get(dbOp.getChatroom()).remove(dbOp.getUsername())) {
                return "success";
            }
            return "fail";
        }

        return "fail";
    }
}
