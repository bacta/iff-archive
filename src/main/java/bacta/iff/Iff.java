package bacta.iff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by crush on 12/17/2014.
 */
public class Iff implements IffReader {
    private static final Logger logger = LoggerFactory.getLogger(Iff.class);

    private static final int ID_FORM = createChunkId("FORM");
    private static final int ID_PROP = createChunkId("PROP");
    private static final int ID_LIST = createChunkId("LIST");
    private static final int ID_CAT = createChunkId("CAT ");
    private static final int ID_FILL = createChunkId("    ");

    private static final int CHUNK_HEADER_SIZE = 8;
    private static final int GROUP_HEADER_SIZE = 12;

    public static final int createChunkId(final String chunkId) {
        final byte[] bytes = chunkId.getBytes();
        return ((bytes[0] << 24 | bytes[1] << 16 | bytes[2] << 8 | bytes[3]));
    }

    public static final String getChunkName(final int id) {
        return new String(ByteBuffer.allocate(4).putInt(id).array());
    }

    public static final boolean isGroupChunkId(final int id) {
        return id == ID_FORM || id == ID_LIST || id == ID_CAT;
    }

    private static final int endianSwap32(int val) {
        return (((val & 0x000000ff) << 24) +
                ((val & 0x0000ff00) << 8) +
                ((val & 0x00ff0000) >> 8) +
                ((val >> 24) & 0x000000ff));
    }

    private final ByteBuffer data;
    private ChunkContext currentContext;

    public Iff() {
        data = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    }

    public Iff(byte[] bytes) {
        data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    public ChunkContext getCurrentContext() {
        return currentContext;
    }

    public ChunkContext nextChunk() {
        logger.trace("Opening next chunk.");

        if (data.remaining() < CHUNK_HEADER_SIZE)
            throw new ReadBeyondEndOfChunkException();

        ChunkContext context = new ChunkContext(data.position(), currentContext);
        data.position(data.position() + CHUNK_HEADER_SIZE); //advance data position beyond the header
        currentContext = context;

        return context;
    }

    public FormContext nextForm() {
        logger.trace("Opening next form.");

        if (data.remaining() < GROUP_HEADER_SIZE)
            throw new ReadBeyondEndOfChunkException();

        FormContext context = new FormContext(data.position(), currentContext);
        data.position(data.position() + GROUP_HEADER_SIZE); //advance data position beyond the header
        currentContext = context;

        return context;
    }

    public ChunkContext openChunk(int chunkId) {
        ChunkContext context = nextChunk();

        if (context.getName() != chunkId)
            throw new ChunkNotFoundException(chunkId, context.getName());

        logger.debug(String.format("Opened chunk with name [%s].", getChunkName(chunkId)));

        return context;
    }

    public FormContext openForm(int formId) {
        FormContext context = nextForm();

        if (context.getName() != formId)
            throw new ChunkNotFoundException(formId, context.getName());

        logger.debug(String.format("Opened form with name [%s].", getChunkName(formId)));

        return context;
    }

    public void closeChunk() {
        if (currentContext != null) {
            if (logger.isDebugEnabled())
                logger.debug(String.format("Closing chunk with chunkId [%s].", getChunkName(currentContext.getName())));

            if (currentContext.getPreviousChunk() == null)
                throw new UnableToCloseRootChunkException();

            data.position(currentContext.getOffset() + currentContext.getLength());
            currentContext = currentContext.getPreviousChunk();
        }
    }

    public String readString() {
        StringBuilder builder = new StringBuilder();
        byte b = 0;

        while ((b = data.get()) != 0)
            builder.append((char) b);

        return builder.toString();
    }

    public long readLong() {
        return data.getLong();
    }

    public int readInt() {
        return data.getInt();
    }

    public short readShort() {
        return data.getShort();
    }

    public byte readByte() {
        return data.get();
    }

    public float readFloat() {
        return data.getFloat();
    }

    public class ChunkContext {
        ChunkContext previousChunk;

        private int start = 0;

        public ChunkContext(int start, ChunkContext previousChunk) {
            this.start = start;
            this.previousChunk = previousChunk;
        }

        public ChunkContext getPreviousChunk() {
            return previousChunk;
        }

        public int getStart() {
            return start;
        }

        public int getOffset() {
            return start + CHUNK_HEADER_SIZE;
        }

        public int getDataOffset() {
            return getOffset();
        }

        public int getName() {
            return endianSwap32(data.getInt(start));
        }

        public int getLength() {
            return endianSwap32(data.getInt(start + 4));
        }

        public boolean hasRemaining() {
            return data.position() < getLength() + getOffset();
        }
    }

    public class FormContext extends ChunkContext {
        public FormContext(int start, ChunkContext previousChunk) {
            super(start, previousChunk);
        }

        @Override
        public int getName() {
            return endianSwap32(data.getInt(getStart() + 8));
        }

        @Override
        public int getDataOffset() {
            return getOffset() + 4;
        }
    }
}
