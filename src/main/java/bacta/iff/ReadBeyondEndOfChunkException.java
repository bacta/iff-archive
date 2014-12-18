package bacta.iff;

/**
 * Created by crush on 12/18/2014.
 */
public class ReadBeyondEndOfChunkException extends RuntimeException {
    public ReadBeyondEndOfChunkException() {
        super("Attempted to read beyond the end of the chunk.");
    }
}
