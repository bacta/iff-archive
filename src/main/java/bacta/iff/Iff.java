package bacta.iff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by crush on 12/17/2014.
 */
public final class Iff {
    private static final Logger logger = LoggerFactory.getLogger(Iff.class);

    public static final int ID_FORM = createChunkId("FORM");
    public static final int ID_PROP = createChunkId("PROP");
    public static final int ID_LIST = createChunkId("LIST");
    public static final int ID_CAT  = createChunkId("CAT ");
    public static final int ID_FILL = createChunkId("    ");

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

    private String fileName;
    private final ByteBuffer data;
    private final List<Stack> stack;
    private int stackDepth;
    private boolean inChunk;

    public Iff(final String fileName) {
        this.fileName = fileName;
        this.data = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        this.stack = new ArrayList<>(64);
        this.stackDepth = 0;
        this.inChunk = false;

        final Stack rootStack = new Stack();
        rootStack.offset = 0;
        rootStack.length = 0;
        rootStack.used = 0;

        this.stack.add(rootStack);
    }

    public Iff(final String fileName, final byte[] bytes) {
        this.fileName = fileName;
        this.data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        this.stack = new ArrayList<>(64);
        this.stackDepth = 0;
        this.inChunk = false;

        final Stack rootStack = new Stack();
        rootStack.offset = 0;
        rootStack.length = bytes.length;
        rootStack.used = 0;

        this.stack.add(rootStack);
    }

    public final String getFileName() {
        return this.fileName;
    }

    public final int getStackDepth() {
        return this.stackDepth;
    }

    public final boolean readBoolean() {
        return readByte() == 1;
    }

    public final byte readByte() {
        if (!this.inChunk)
            throw new UnsupportedOperationException("Cannot read while not in a chunk.");

        final Stack chunk = this.stack.get(this.stackDepth);

        if (chunk.used + 1 > chunk.length)
            throw new BufferOverflowException();

        byte value = data.get(chunk.used + chunk.offset);

        chunk.used += 1;

        return value;
    }

    public final short readShort() {
        if (!this.inChunk)
            throw new UnsupportedOperationException("Cannot read while not in a chunk.");

        final Stack chunk = this.stack.get(this.stackDepth);

        if (chunk.used + 2 > chunk.length)
            throw new BufferOverflowException();

        short value = data.getShort(chunk.used + chunk.offset);

        chunk.used += 2;

        return value;
    }

    public final int readInt() {
        if (!this.inChunk)
            throw new UnsupportedOperationException("Cannot read while not in a chunk.");

        final Stack chunk = this.stack.get(this.stackDepth);

        if (chunk.used + 4 > chunk.length)
            throw new BufferOverflowException();

        int value = data.getInt(chunk.used + chunk.offset);

        chunk.used += 4;

        return value;
    }

    public final long readLong() {
        if (!this.inChunk)
            throw new UnsupportedOperationException("Cannot read while not in a chunk.");

        final Stack chunk = this.stack.get(this.stackDepth);

        if (chunk.used + 8 > chunk.length)
            throw new BufferOverflowException();

        long value = data.getLong(chunk.used + chunk.offset);

        chunk.used += 8;

        return value;
    }

    public final float readFloat() {
        if (!this.inChunk)
            throw new UnsupportedOperationException("Cannot read while not in a chunk.");

        final Stack chunk = this.stack.get(this.stackDepth);

        if (chunk.used + 4 > chunk.length)
            throw new BufferOverflowException();

        float value = data.getFloat(chunk.used + chunk.offset);

        chunk.used += 4;

        return value;
    }

    public final String readString() {
        if (!this.inChunk)
            throw new UnsupportedOperationException("Cannot read while not in a chunk.");

        final Stack chunk = this.stack.get(this.stackDepth);

        if (chunk.length - chunk.used <= 0)
            throw new UnsupportedOperationException("At end of chunk, cannot read.");

        final StringBuilder stringBuilder = new StringBuilder();

        for (int index = 0; index < chunk.length - chunk.used; ++index) {
            byte b = this.data.get(chunk.offset + chunk.used + index);

            if (b == 0)
                break;

            stringBuilder.append((char)b);
        }

        chunk.used += stringBuilder.length() + 1; //+1 for null byte terminator.

        return stringBuilder.toString();
    }

    public final String readUnicode() {
        if (!this.inChunk)
            throw new UnsupportedOperationException("Cannot read while not in a chunk.");

        final Stack chunk = this.stack.get(this.stackDepth);

        int length = readInt();

        if (chunk.length - chunk.used <= 0)
            throw new UnsupportedOperationException("At end of chunk, cannot read.");

        byte[] bytes = new byte[length];
        this.data.get(bytes, chunk.offset + chunk.used, length);

        return new String(bytes, Charset.forName("UTF16LE"));
    }

    public final void enterChunk() {
        enterChunk(0, false, false);
    }

    public final void enterChunk(final int chunkId) {
        enterChunk(chunkId, true, false);
    }

    public final boolean enterChunk(final boolean optional) {
        return enterChunk(0, false, optional);
    }

    public final boolean enterChunk(final int chunkId, final boolean optional) {
        return enterChunk(chunkId, true, optional);
    }

    public final boolean enterChunk(final int chunkId, boolean validateName, boolean optional) {
        if (chunkId != 0 && validateName) {
            final int nextChunkId = getFirstTag(this.stackDepth);

            if (nextChunkId != chunkId) {
                throw new IllegalArgumentException(String.format("Expected chunk [%s] but found [%s].",
                        Iff.getChunkName(chunkId),
                        Iff.getChunkName(nextChunkId)));
            }
        }

        if (!this.inChunk && !isAtEndOfForm() && isCurrentChunk()
                && (!validateName || getFirstTag(this.stackDepth) == chunkId)) {

            final Stack prevStack = this.stack.get(this.stackDepth);
            final Stack nextStack = this.stack.size() <= this.stackDepth + 1 ? new Stack() : this.stack.get(this.stackDepth + 1);
            nextStack.offset = prevStack.offset + prevStack.used + CHUNK_HEADER_SIZE;
            nextStack.length = getLength(this.stackDepth, 0);
            nextStack.used = 0;

            if (this.stack.size() <= this.stackDepth + 1) {
                this.stack.add(nextStack);
            } else {
                this.stack.set(this.stackDepth + 1, nextStack);
            }

            ++this.stackDepth;
            this.inChunk = true;

            return true;
        }

        if (!optional)
            throw new IllegalStateException(String.format("Enter chunk [%s] failed.", Iff.getChunkName(chunkId)));

        return false;
    }

    public final void enterForm() {
        enterForm(0);
    }

    public final void enterForm(final int formId) {
        if (formId != 0) {
            final int nextFormId = getSecondTag(this.stackDepth);

            if (nextFormId != formId) {
                throw new IllegalArgumentException(String.format("Expected form [%s] but found [%s].",
                        Iff.getChunkName(formId),
                        Iff.getChunkName(nextFormId)));
            }
        }

        if (!this.inChunk && !isAtEndOfForm() && isCurrentForm()) {
            final Stack prevStack = this.stack.get(this.stackDepth);
            final Stack nextStack = this.stack.size() <= this.stackDepth + 1 ? new Stack() : this.stack.get(this.stackDepth + 1);
            nextStack.offset = prevStack.offset + prevStack.used + GROUP_HEADER_SIZE;
            nextStack.length = getLength(this.stackDepth, 0) - 4;
            nextStack.used = 0;

            if (this.stack.size() <= this.stackDepth + 1) {
                this.stack.add(nextStack);
            } else {
                this.stack.set(this.stackDepth + 1, nextStack);
            }

            ++this.stackDepth;
            this.inChunk = false;
        }
    }

    public final void exitChunk() {
        exitChunk(0);
    }

    public final void exitChunk(final int chunkId) {
        if (chunkId != 0) {
            final int prevChunkId = getFirstTag(this.stackDepth - 1);

            if (prevChunkId != chunkId) {
                throw new IllegalArgumentException(String.format("Trying to exit chunk [%s] but found [%s].",
                        Iff.getChunkName(chunkId),
                        Iff.getChunkName(prevChunkId)));
            }
        }

        final Stack prevStack = this.stack.get(this.stackDepth - 1);
        final Stack thisStack = this.stack.get(this.stackDepth);

        prevStack.used += thisStack.length + CHUNK_HEADER_SIZE;
        --this.stackDepth;
        this.inChunk = false;
    }

    public final void exitForm() {
        exitForm(0);
    }

    public final void exitForm(final int formId) {
        if (this.stackDepth == 0)
            throw new IllegalArgumentException("Trying to exit root.");

        if (formId != 0) {
            final int prevFormId = getSecondTag(this.stackDepth - 1);

            if (prevFormId != formId) {
                throw new IllegalArgumentException(String.format("Trying to exit form [%s] but found [%s].",
                        Iff.getChunkName(formId),
                        Iff.getChunkName(prevFormId)));
            }
        }

        if (this.inChunk)
            throw new IllegalArgumentException("Tried to exit a form while within a chunk.");

        final Stack prevStack = this.stack.get(this.stackDepth - 1);
        final Stack thisStack = this.stack.get(this.stackDepth);

        prevStack.used += thisStack.length + GROUP_HEADER_SIZE;
        --this.stackDepth;
        this.inChunk = false;
    }

    public void insertChunkData(final boolean data) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void insertChunkData(final byte data) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void insertChunkData(final short data) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void insertChunkData(final int data) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void insertChunkData(final long data) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void insertChunkData(final float data) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void insertChunkData(final String data) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public final int getBlockName(int depth) {
        int value = getFirstTag(depth);

        if (isGroupChunkId(value))
            value = getSecondTag(depth);

        return value;
    }

    public final int getNumberOfBlocksLeft() {
        if (this.inChunk)
            throw new UnsupportedOperationException("Cannot get number of blocks left while in a chunk.");

        final Stack chunk = this.stack.get(this.stackDepth);

        int depth = 0;
        int offset = 0;

        if (chunk.used < chunk.length) {
            while (offset + chunk.used < chunk.length)
                offset += getLength(this.stackDepth, offset) + CHUNK_HEADER_SIZE;
        }

        return depth;
    }

    public final boolean isCurrentChunk() {
        return !Iff.isGroupChunkId(getFirstTag(this.stackDepth));
    }

    public final boolean isCurrentForm() {
        return getFirstTag(this.stackDepth) == ID_FORM;
    }

    public final int getCurrentName() {
        return getBlockName(this.stackDepth);
    }

    public final int getChunkLengthTotal(int elementSize) {
        if (!this.inChunk)
            throw new UnsupportedOperationException("Not in a chunk.");

        final Stack chunk = this.stack.get(this.stackDepth);

        int lengthMod = chunk.length % elementSize;
        int lengthDiv = chunk.length / elementSize;

        if (chunk.length % elementSize != 0) {
            throw new IllegalArgumentException(String.format("%d is not a multiple of %d.",
                    lengthMod,
                    lengthDiv));
        }

        return lengthDiv;
    }

    public final int getChunkLengthLeft() {
        return getChunkLengthLeft(1);
    }

    public final int getChunkLengthLeft(int elementSize) {
        if (!this.inChunk)
            throw new UnsupportedOperationException("Not in a chunk.");

        final Stack chunk = this.stack.get(this.stackDepth);

        int remaining = chunk.length - chunk.used;

        int remainingMod = remaining % elementSize;
        int remainingDiv = remaining / elementSize;

        if (remainingMod != 0) {
            throw new IllegalArgumentException(String.format("%d is not a multiple of %d.",
                    remainingMod,
                    remainingDiv));
        }

        return remainingDiv;
    }

    private final int getFirstTag(int depth) {
        final Stack chunk = this.stack.get(depth);

        if (chunk.length - chunk.used < CHUNK_HEADER_SIZE)
            throw new BufferOverflowException();

        return Iff.endianSwap32(data.getInt(chunk.offset + chunk.used));
    }

    private final int getSecondTag(int depth) {
        final Stack chunk = this.stack.get(depth);

        if (chunk.length - chunk.used < GROUP_HEADER_SIZE)
            throw new BufferOverflowException();

        return Iff.endianSwap32(data.getInt(chunk.offset + chunk.used + CHUNK_HEADER_SIZE));
    }

    public final boolean isAtEndOfForm() {
        final Stack chunkStack = this.stack.get(this.stackDepth);

        return chunkStack.used == chunkStack.length;
    }

    private final int getLength(int depth, int offset) {
        final Stack chunkStack = this.stack.get(depth);

        if (offset + chunkStack.length - chunkStack.used < CHUNK_HEADER_SIZE)
            throw new BufferOverflowException();

        return Iff.endianSwap32(data.getInt(offset + 4 + chunkStack.used + chunkStack.offset));
    }

    public final void goToTopOfForm() {
        if (this.inChunk)
            throw new UnsupportedOperationException("Cannot go to the top of form while in a chunk.");

        this.stack.get(this.stackDepth).used = 0;
    }

    public final void goForward(int count) {
        if (this.inChunk)
            throw new UnsupportedOperationException("Cannot go forward when in a chunk.");

        for (int index = count; count > 0 && !isAtEndOfForm(); --index)
            this.stack.get(this.stackDepth).used += getLength(this.stackDepth, 0) + CHUNK_HEADER_SIZE;
    }

    public final boolean seekForm(final int formId) {
        return seek(formId, BlockType.Form);
    }

    public final boolean seekChunk(final int chunkId) {
        return seek(chunkId, BlockType.Chunk);
    }

    public final boolean seekWithinChunk(final int chunkId, final SeekType seekType) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    private final boolean seek(final int chunkId, final BlockType blockType) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    private static final class Stack {
        int offset;
        int length;
        int used;
    }

    public static enum SeekType {
        Begin,
        Current,
        End
    }

    public static enum BlockType {
        Either,
        Form,
        Chunk
    }
}
