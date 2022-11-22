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
import server.DBOperation;

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
    DBOperation proposedVal;

    // List of servers
    List<Integer> serverPorts;

    /**
     * Empty constructor
     * Initializes the proposal ID
     */
    public Proposer() {
        this.propId = 0;
    }

    /**
     * Increments the proposal ID
     */
    public void incrementPropID() {
        propId++;
    }

    /**
     * Get the proposal ID
     * @return The proposal ID
     */
    public int getPropId() {
        return this.propId;
    }

    /**
     * Set the proposal ID
     * @param prop Integer of the proposal ID
     */
    public void setPropId(int prop) {
        this.propId = prop;
    }

    /**
     * Set the ports for the proposer
     * @param otherPorts List of ports
     */
    public void setPorts(List<Integer> otherPorts) {
        this.serverPorts = otherPorts;
    }
    
    /**
     * Start paxos proposal
     * @param operation The operation - Register or send message or join/create chatroom
     * @param key If registering, the username/password
     * @param val The value of the operation
     * @param message String message if operation is sending a message
     * @param chatroom Chatroom name if joining or creating a chatroom
     * @return Response object with the server's reply
     */
    public Response propose(String operation, String key, String val, String message, String chatroom) {
        // On each propose, increment the proposal ID
        incrementPropID();

        // Keep track of the majority of the servers
        int majority = (serverPorts.size() / 2) + 1;

        // Keep track of the proposed value
        // In this case the value is the new operation
        proposedVal = new DBOperation(operation, key, val, message, chatroom);

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
                "Consensus not reached for prepare. Aborted: %s",
                proposedVal.getOp()));
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
                "Consensus not reached for acceptance. Aborting: %s.",
                proposedVal.getOp());
            return new Response(logLevel, serverReply);
        }

        // Parse the resultant message from learner and send back the Response object
        return makeResponse(res);
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
                ChatServerInterface chatStub = (ChatServerInterface) reg.lookup("chat");
                boolean serverPrepped = chatStub.prepare(getPropId());
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
                LOGGER.severe(e.toString());
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
    private int sendAccepts(DBOperation propVal) {
        int numAccept = 0;
        for(int port: serverPorts) {
            try {
                Registry reg = LocateRegistry.getRegistry(port);
                ChatServerInterface chatStub = (ChatServerInterface) reg.lookup("chat");
                DBOperation serverAccept = chatStub.accept(getPropId(), propVal);
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
        String res = "fail";

        // Iterate through all servers to commit the action using the proposed val
        for(int port: serverPorts) {
            try {
                Registry reg = LocateRegistry.getRegistry(port);
                ChatServerInterface chatStub = (ChatServerInterface) reg.lookup("chat");
                res = chatStub.commit(this.propId, proposedVal);

                if (res.equals("fail")) {
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
     * @return Response object
     */
    private Response makeResponse(String res) {
        String serverReply = "";
        Level logLevel = Level.SEVERE;

        if (res.equals("success")) {
            serverReply = "success";
            logLevel = Level.INFO;
        } else {
            serverReply = "fail";
        }
        return new Response(logLevel, serverReply);
    }
}
