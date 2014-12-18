package bacta.iff;

/**
 * Created by crush on 12/17/2014.
 */
public class ChunkClosedBeforeEndException extends RuntimeException {
    public ChunkClosedBeforeEndException(int remainingBytes) {
        super(String.format("The chunk was closed before the end was reached with [%n] bytes remaining.", remainingBytes));
    }
}
