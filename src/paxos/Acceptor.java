package paxos;

// Log Imports
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;

// Custom Imports
import server.KVOperation;

// Java Imports
import java.util.Random;
import java.net.SocketTimeoutException;

public class Acceptor extends Thread {
    // Set up logging with a custom properties file
    static Logger LOGGER = Logger.getLogger(Acceptor.class.getName());
    static {
        String filePath = "../config/serverlogging.properties";
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(filePath));
        } catch (IOException io)  {
            LOGGER.severe("Logging config file not found.");
        }
    }

    private int prevProposalId;
    private int prevAcceptedId;
    private int serverPort;
    private KVOperation prevVal;

    Random rand = new Random();

    public void setPrevProposalId(int pId) {
        this.prevProposalId = pId;
    }

    public int getProposalId() {
        return this.prevProposalId;
    }

    public void setPrevAcceptedId(int pId) {
        this.prevProposalId = pId;
    }

    public int getPrevAcceptedId() {
        return this.prevProposalId;
    }

    public void setKVOp(KVOperation kvOp) {
        this.prevVal = kvOp;
    }

    public void setServerPort(int port) {
        this.serverPort = port;
    }
    
    public KVOperation getKVOp() {
        return this.prevVal;
    }

    /**
     * Receive a prepare message from the proposer
     * and check sent message's ID is greater than the previous one.
     * @param prop The proposal ID
     * @return True for a "promise" or false
     */
    public boolean prepare(int prop) throws SocketTimeoutException {
        // Simulate a timeout
        if (rand.nextInt(5) == 0) {
            LOGGER.severe(String.format("Socket timed out in acceptor for prepare on server: %d", this.serverPort));
            throw new SocketTimeoutException("Waiting too long");
        }

        if (!(prop > this.prevProposalId)) {
            // Rejection
            return false;
        }

        // This keeps track of most recently received proposal ID
        // ONLY if it is greater than the previous proposal ID.
        setPrevProposalId(prop);
        // Send the "promise" back that it's ready to accept
        return true;
    }

    /**
     * Receive a request to accept the proposal.
     * Check the proposal ID to make sure it is still greater than
     * any other received proposal
     * @param prop The prosposal ID
     * @return The operation if accepting the proposal
     */
    public KVOperation accept(int prop, KVOperation val) throws SocketTimeoutException{
        // Simulate a timeout
        if (rand.nextInt(5) == 0) {
            LOGGER.severe(String.format("Socket timed out in acceptor for accept in server: %d", this.serverPort));
            throw new SocketTimeoutException("Waiting too long");
        }

        if (!(prop > this.prevAcceptedId)) {
            // Rejection
            return null;
        }

        // This keeps track of most recently received proposal ID
        // ONLY if it is greater than the previous proposal ID.
        setPrevAcceptedId(prop);
        setKVOp(val);

        // Send the acceptance back to the proposer
        return prevVal;
    }

}
