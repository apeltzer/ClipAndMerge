import clipping.Clipper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by peltzer on 10/03/2017.
 */
public class ClipperTest extends AbstractTest{

    @Override
    public void setUp() throws Exception {
        in1 = new File[]{new File("/clipper_tests_fw.fq.gz")};
        in2 = new File[]{new File("/clipper_tests_rv.fq.gz")};
        output = getClass().getResourceAsStream("/clipper_tests.fq.gz"); //This should be the output data for CM, not the testing data

        universalSetup("/clipperout.fw.fq.gz", "/clipperout.rv.fq.gz");
    }

    @Test
    public void clipperTest() throws IOException {
        Clipper clipper = new Clipper(mergeSettings);



    }
}
