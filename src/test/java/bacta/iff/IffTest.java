package bacta.iff;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by crush on 12/17/2014.
 */
public class IffTest {
    private static final int ID_SCOT = Iff.createChunkId("SCOT");
    private static final int ID_DERV = Iff.createChunkId("DERV");
    private static final int ID_XXXX = Iff.createChunkId("XXXX");
    private static final int ID_0012 = Iff.createChunkId("0012");
    private static final int ID_PCNT = Iff.createChunkId("PCNT");
    private static final int ID_STOT = Iff.createChunkId("STOT");
    private static final int ID_0007 = Iff.createChunkId("0007");
    private static final int ID_SHOT = Iff.createChunkId("SHOT");

    private byte[] testBytes;

    private final String resourcesPath = IffTest.class.getResource("/").getPath();

    @Before
    public void before() {
        try {
            RandomAccessFile file = new RandomAccessFile(resourcesPath + "human_male.iff", "r");
            testBytes = new byte[(int) file.length()];
            file.read(testBytes);
            file.close();
        } catch (FileNotFoundException ex) {
            Assert.fail("Could not find test file in resources.");
        } catch (IOException ex) {
            ex.printStackTrace();
            Assert.fail("Could not open test file for reading.");
        }
    }

    @Test
    public void shouldReadDerivedFromTemplate() {
        Iff iff = new Iff(testBytes);
        iff.openForm(ID_SCOT);
        iff.openForm(ID_DERV);
        iff.openChunk(ID_XXXX);

        Assert.assertTrue("object/creature/player/shared_human_male.iff".equals(iff.readString()));
    }

    @Test
    public void shouldReadAllChunksWithoutException() {
        Iff iff = new Iff(testBytes);
        iff.openForm(ID_SCOT);
        iff.openForm(ID_DERV);
        iff.openChunk(ID_XXXX);
        iff.closeChunk();
        iff.closeChunk();
        iff.openForm(ID_0012);
        iff.openChunk(ID_PCNT);
        iff.closeChunk();
        iff.closeChunk();
        iff.openForm(ID_STOT);
        iff.openForm(ID_DERV);
        iff.openChunk(ID_XXXX);
        iff.closeChunk();
        iff.closeChunk();
        iff.openForm(ID_0007);
        iff.openChunk(ID_PCNT);
        iff.closeChunk();
        iff.closeChunk();
        iff.openForm(ID_SHOT);
        iff.openForm(ID_DERV);
        iff.openChunk(ID_XXXX);
        iff.closeChunk();
        iff.closeChunk();
        iff.openForm(ID_0007);
        iff.openChunk(ID_PCNT);
        iff.closeChunk();
    }

    @Test
    public void shouldSelectNextChunk() {
        Iff iff = new Iff(testBytes);
        iff.nextForm(); //ID_SCOT
        iff.nextForm(); //ID_DERV
        iff.nextChunk(); //ID_XXXX

        Assert.assertTrue("object/creature/player/shared_human_male.iff".equals(iff.readString()));
    }
}
