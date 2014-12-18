package bacta.iff;

/**
 * Created by crush on 12/18/2014.
 */
public class UnableToCloseRootChunkException extends RuntimeException {
    public UnableToCloseRootChunkException() {
        super("Unable to close the root chunk.");
    }
}
