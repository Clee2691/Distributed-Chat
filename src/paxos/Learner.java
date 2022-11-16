package paxos;

// Log Imports
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;

// Java Imports
import java.util.Map;

// Custom Imports
import server.KVOperation;

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
     * @param kvOp The operation
     * @return Resulting message
     */
    public synchronized String commit(Map<String,String> store, KVOperation kvOp) {

        // PUT request
        if (kvOp.getOp().equals("PUT")) {
            if (store.put(kvOp.getKey(), kvOp.getVal()) == null) {
                return "insert";
            }
            return "update";
        // DEL request
        } else if (kvOp.getOp().equals("DEL")) {
            if (store.containsKey(kvOp.getKey())) {
                store.remove(kvOp.getKey());
                return "delSuccess";
            }
            return "delFailed";
        }
        
        // Request failure
        return "FAIL";
    }
}
