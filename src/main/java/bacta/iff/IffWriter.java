package bacta.iff;

/**
 * Created by crush on 12/18/2014.
 */
public interface IffWriter {
    void writeString(String value);
    void writeFloat(float value);
    void writeLong(long value);
    void writeInt(int value);
    void writeShort(short value);
    void writeByte(byte value);
}
