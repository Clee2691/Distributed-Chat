package server;

import java.io.Serializable;

/**
 * A Key Value Operation class that contains the key/value and the operation
 * to be done.
 */
public class KVOperation implements Serializable {
    private String op;
    private String key;
    private String value;

    /**
     * Public constructor that needs an operation, key, and value.
     * @param operation The operation
     * @param k A key
     * @param v A value
     */
    public KVOperation(String operation, String k, String v) {
        this.op = operation;
        this.key = k;
        this.value = v;
    }

    /**
     * Get the operation.
     * @return String operation.
     */
    public String getOp() {
        return this.op;
    }

    /**
     * Get the key.
     * @return String key.
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Get the value.
     * @return String value.
     */
    public String getVal() {
        return this.value;
    }
}
