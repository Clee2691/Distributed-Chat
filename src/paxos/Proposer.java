package paxos;

// Logging imports
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;

// RMI Imports
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
// Java Imports
import java.util.List;

// Self Imports
import server.ChatServerInterface;
import server.Response;
import server.KVOperation;

public class Proposer extends Thread {
    // Set up logging with a custom properties file
    static Logger LOGGER = Logger.getLogger(Proposer.class.getName());
    static {
        String filePath = "../config/serverlogging.properties";
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(filePath));
        } catch (IOException io)  {
            LOGGER.severe("Logging config file not found.");
        }
    }

    // Proposal ID that will be sent to acceptors
    private int propId;

    // Proposed "value" to accept and commit
    KVOperation proposedVal;

    // List of servers
    List<Integer> serverPorts;

    // Constructor initializing the proposal ID.
    public Proposer() {
        this.propId = 0;
    }

    public void incrementPropID() {
        propId++;
    }

    public int getPropId() {
        return this.propId;
    }

    public void setPorts(List<Integer> otherPorts) {
        this.serverPorts = otherPorts;
    }
    
    /**
     * Start paxos proposal
     * @param operation The operation, PUT/DEL
     * @param key The key to PUT or DEL
     * @param val The value if operation is PUT
     * @return Response object with the server's reply
     */
    public synchronized Response propose(String operation, String key, String val, String client) {
        // On each propose, increment the proposal ID
        incrementPropID();

        // Keep track of the majority of the servers
        int majority = serverPorts.size() / 2;

        // Keep track of the proposed value
        // In this case the value is the new operation
        proposedVal = new KVOperation(operation, key, val);

        // Send prepare messages to acceptors
        // Phase 1a: Prepare
        int numPromises = sendPrepares();
        
        int numAccept = 0;
        // Check that majority of acceptors sent back a promise
        if (numPromises > majority) {
            LOGGER.info(
                String.format("Prop ID: %d reached majority promises! Proceeding...", 
                getPropId()));
            // Send a request to acceptors to accept the proposal
            // Phase 2a: Accept
            numAccept = sendAccepts(proposedVal);
        } else {
            LOGGER.severe(
                String.format("Prop ID: %d failed reaching majority promises! Aborting...", 
                getPropId()));
            return new Response(Level.SEVERE, String.format(
                "Consensus not reached for prepare. Aborted: %s %s %s",
                proposedVal.getOp(), proposedVal.getKey(), proposedVal.getVal()));
        }

        String res = "";
        // Check majority of servers accepted the proposal
        // If it did, then commit the action on all servers
        if (numAccept > majority) {
            LOGGER.info(
                String.format("Prop ID: %d reached majority accepts! Proceeding...", 
                getPropId()));
            res = sendCommits();
        } else {
            LOGGER.severe(
                String.format("Prop ID: %d failed reaching majority accepts! Aborting...", 
                getPropId()));
            Level logLevel = Level.SEVERE;
            String serverReply = String.format(
                "Consensus not reached for acceptance. Aborting: %s %s %s",
                proposedVal.getOp(), proposedVal.getKey(), proposedVal.getVal());
            return new Response(logLevel, serverReply);
        }

        // Parse the resultant message from learner and send back the Response object
        return makeResponse(res, client);
    }

    /**
     * Send a prepare message to the "Quorum" of Acceptors
     * @return The total number of accept replies
     */
    private int sendPrepares() {
        int numPrepped = 0;

        for (int port: serverPorts) {
            // For each server, send a prepare message to it
            // with the current proposal ID
            try {
                Registry reg = LocateRegistry.getRegistry(port);
                ChatServerInterface kvStub = (ChatServerInterface) reg.lookup("KVS");
                boolean serverPrepped = kvStub.prepare(getPropId());
                if (serverPrepped) {
                    numPrepped++;
                    LOGGER.info(
                        String.format("Server port: %d, sent back a promise to prop ID: %d!", 
                        port,
                        getPropId()));
                } else {
                    LOGGER.severe(
                        String.format("Server port: %d, did NOT promise the proposal with ID: %d!", 
                        port,
                        getPropId()));
                }
            } catch (RemoteException e) {
                LOGGER.severe(
                    String.format(
                    "Remote exception for server port: %d!.",
                    port));
                continue;
            } catch (NotBoundException e) {
                LOGGER.severe(
                    String.format(
                    "Could not bind registry for server on port: %d! Registry name not found!",
                            port));
                continue;
            } catch (SocketTimeoutException ste) {
                LOGGER.severe(
                    String.format(
                    "Timed out for server on port: %d! Could not accept proposal!",
                            port));
                continue;
            }
        }
        return numPrepped;
    }

    /**
     * Send accept messages to Acceptors to get them to accept the proposal
     * @return Number of Acceptors that accepted the proposal
     */
    private int sendAccepts(KVOperation propVal) {
        int numAccept = 0;
        for(int port: serverPorts) {
            try {
                Registry reg = LocateRegistry.getRegistry(port);
                ChatServerInterface kvStub = (ChatServerInterface) reg.lookup("KVS");
                KVOperation serverAccept = kvStub.accept(getPropId(), propVal);
                if (serverAccept != null) {
                    numAccept++;
                    proposedVal = serverAccept;
                    LOGGER.info(
                        String.format("Server port: %d, accepted the proposal with ID: %d!", 
                        port,
                        getPropId()));
                } else {
                    LOGGER.severe(
                        String.format("Server port: %d, denied the proposal with ID: %d!", 
                        port,
                        getPropId()));
                } 
            } catch (RemoteException e) {
                LOGGER.severe(
                    String.format(
                    "Remote exception for server port: %d!. Could not accept proposal.",
                    port));
                continue;
            } catch (NotBoundException e) {
                LOGGER.severe(
                    String.format(
                    "Could not bind registry for server on port: %d! Registry name not found! Could not accept proposal!",
                            port));
                continue;
            } catch (SocketTimeoutException ste) {
                LOGGER.severe(
                    String.format(
                    "Socket timed out for server on port: %d! Could not accept proposal!",
                            port));
                continue;
            }
        }
        return numAccept;
    }

    /**
     * Send commit messages to the Learners.
     * @return String The final status of the commit
     */
    private String sendCommits() {
        String res = "FAIL";

        // Iterate through all servers to commit the action using the proposed val
        for(int port: serverPorts) {
            try {
                Registry reg = LocateRegistry.getRegistry(port);
                ChatServerInterface kvStub = (ChatServerInterface) reg.lookup("KVS");
                res = kvStub.commit(proposedVal);

                if (res.equals("FAIL")) {
                    LOGGER.severe(
                        String.format("Server port: %d, commit failed!", 
                        port,
                        res));
                } else {
                    LOGGER.info(
                        String.format("Server port: %d, committed. Result: %s", 
                        port,
                        res));
                }
            } catch (RemoteException e) {
                LOGGER.severe(
                    String.format(
                    "Remote exception for server port: %d!. Could not commit!",
                    port));
                continue;
            } catch (NotBoundException e) {
                LOGGER.severe(
                    String.format(
                    "Could not bind registry for server on port: %d! Registry name not found! Could not commit!",
                            port));
                continue;
            }    
        }
        return res;
    }

    /**
     * Put together the final response to send back to the calling client.
     * @param res The result from the commits
     * @param client The client that sent the request
     * @return Response object
     */
    private Response makeResponse(String res, String client) {
        String serverReply = "";
        Level logLevel = Level.SEVERE;

        // Parse the resultant commit message from learners
        if (proposedVal.getOp().equals("DEL")) {
            // Success delete
            if (res.equals("delSuccess")) {
                serverReply = String.format(
                    "Successfully deleted Key='%s' from the store.", proposedVal.getKey());
                logLevel = Level.INFO;
                LOGGER.info(String.format(
                    "SUCCESS: Received DEL request of KEY='%s' from %s.",
                    proposedVal.getKey(),
                    proposedVal.getKey()));
            // Failed delete, key not in store
            } else if (res.equals("delFail")) {
                serverReply = String.format(
                    "Key='%s' not found in the store. Nothing deleted.", proposedVal.getKey());
                logLevel = Level.WARNING;
                LOGGER.warning(String.format(
                    "FAIL: Received DEL request of KEY='%s' from %s. Key not found.",
                    proposedVal.getKey(),
                    client));
            } else if (res.equals("FAIL")) {
                serverReply = String.format(
                    "DEL REQ FAIL: Error with delete request. Nothing deleted.");
                logLevel = Level.WARNING;
                LOGGER.warning(String.format(
                    "DEL REQ FAIL: Received DEL request of KEY='%s' from %s.",
                    proposedVal.getKey(),
                    client)); 
            }
        } else if (proposedVal.getOp().equals("PUT")) {
            if (res.equals("insert")) {
                logLevel = Level.INFO;
                LOGGER.info(String.format(
                    "Received PUT (Insert) request of ('%s', '%s') from %s.", 
                    proposedVal.getKey(), 
                    proposedVal.getVal(), 
                    client));
                serverReply = String.format(
                    "Successfully inserted new KEY='%s' with VAL='%s' into the store.", proposedVal.getKey(), proposedVal.getVal());
            // Updated a key
            } else if (res.equals("update")) {
                serverReply = String.format(
                    "Updated KEY='%s' to '%s'.", proposedVal.getKey(), proposedVal.getVal());
                logLevel = Level.WARNING;
                LOGGER.warning(String.format(
                    "Received PUT (Update) request of ('%s', '%s') from %s.", 
                    proposedVal.getKey(),
                    proposedVal.getVal(),
                    client));
            } else if (res.equals("FAIL")) {
                serverReply = String.format(
                    "FAILED Inserting KEY='%s' VAL='%s'.", proposedVal.getKey(), proposedVal.getVal());
                logLevel = Level.SEVERE;
                LOGGER.warning(String.format(
                    "FAILED PUT request of ('%s', '%s') from %s.", 
                    proposedVal.getKey(),
                    proposedVal.getVal(),
                    client));
            }
        }
        return new Response(logLevel, serverReply);
    }
}
