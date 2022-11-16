package server;

import java.io.Serializable;
import java.util.logging.Level;

public class Response implements Serializable {
    private Level logLevel;
    private String serverReply;

    // Constructor
    public Response(Level level, String reply) {
        logLevel = level;
        serverReply = reply;
    }
    
    /** Get the log level
     * @return Level
     */
    public Level getLogLevel() {
        return this.logLevel;
    }
    
    /** Set the log level
     * @param logLevel
     */
    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }
    
    /** Get the server reply
     * @return String
     */
    public String getServerReply() {
        return this.serverReply;
    }
    
    /** Set the server reply
     * @param serverReply
     */
    public void setServerReply(String serverReply) {
        this.serverReply = serverReply;
    }


}
