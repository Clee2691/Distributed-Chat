package paxos;

// Log Imports
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;

// Custom Imports
import server.DBOperation;

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
    
    private DBOperation prevVal;

    /**
     * Set the previously accepted proposal ID
     * @param pId The previous ID
     */
    public void setPrevProposalId(int pId) {
        this.prevProposalId = pId;
    }

    /**
     * Get the proposal ID
     * @return Integer proposal ID
     */
    public int getProposalId() {
        return this.prevProposalId;
    }

    /**
     * Set the prev accepted ID
     * @param pId The ID
     */
    public void setPrevAcceptedId(int pId) {
        this.prevProposalId = pId;
    }

    /**
     * Get the previous accepted ID
     * @return
     */
    public int getPrevAcceptedId() {
        return this.prevProposalId;
    }

    /**
     * Set the DBOperation to be done
     * @param dbOp The DBOperation object
     */
    public void setDBOp(DBOperation dbOp) {
        this.prevVal = dbOp;
    }
    
    /**
     * Get the DBOperation object
     * @return DBOperation object
     */
    public DBOperation getDBOp() {
        return this.prevVal;
    }

    /**
     * Receive a prepare message from the proposer
     * and check sent message's ID is greater than the previous one.
     * @param prop The proposal ID
     * @return True for a "promise" or false
     */
    public boolean prepare(int prop) {

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
    public DBOperation accept(int prop, DBOperation val) {
        if (!(prop > this.prevAcceptedId)) {
            // Rejection
            return null;
        }

        // This keeps track of most recently received proposal ID
        // ONLY if it is greater than the previous proposal ID.
        setPrevAcceptedId(prop);
        setDBOp(val);

        // Send the acceptance back to the proposer
        return prevVal;
    }

}
