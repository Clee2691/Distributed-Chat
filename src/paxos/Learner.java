package paxos;

// Log Imports
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
// Java Imports
import java.util.Map;

// Custom Imports
import server.DBOperation;

/**
 * Learner class that serves as the replication factor. This class is the one
 * that executes and commits the actual request.
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

    /**
     * The commit method that manipulates the key value store.
     * @param store The store in the server
     * @param dbOp The operation
     * @return Resulting message
     */
    public synchronized String commit(Map<String,String> userStore, 
                                        Map<String, List<String>> chatRoomUsers,
                                        Map<String, List<String>> chatRoomHistory, 
                                        DBOperation dbOp) {

        if (dbOp.getOp().equals("register")){
            userStore.put(dbOp.getUsername(), dbOp.getPassword());
            return "success";
        } else if (dbOp.getOp().equals("create")) {
            
        } else if (dbOp.getOp().equals("join")) {
            
        } else if (dbOp.getOp().equals("send")) {

        } else {
            return "fail";
        }

        return "fail";
    }
}
