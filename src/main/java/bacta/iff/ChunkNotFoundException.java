package bacta.iff;

/**
 * Created by crush on 12/17/2014.
 */
public class ChunkNotFoundException extends RuntimeException {
    public ChunkNotFoundException(int chunkId, int foundId) {
        super(String.format("Unable to find chunk with name [%s]. Found [%s] instead.",
                Iff.getChunkName(chunkId),
                Iff.getChunkName(foundId)));
    }
}
