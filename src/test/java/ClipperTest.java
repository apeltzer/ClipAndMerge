import clipping.Clipper;
import clipping.Read;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by peltzer on 10/03/2017.
 */
public class ClipperTest extends AbstractTest{

    @Mock
    private InputStreamReader outReader;

    @Mock
    private BufferedReader mockBfReader;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();



    @Override
    public void setUp() throws Exception {
        universalSetup("AGATCGGAAGAGCACACGTCTGAACTCCAGTCAC", "AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGTA", new File[]{}, new File[]{}, outReader, mockBfReader);
    }

    /*
     * There are several potential cases that could happen when clipping adapters from reads
     * We try to test several for each potential case here and will have a look at test results later on.
     * @throws IOException
     */

    /*
     Case 1: No overlap between sequence and adapter here
     */

    @Test
    public void clipper_no_overlap() throws Exception {
      Clipper clipper = new Clipper(mergeSettings);
      clipper.init();
      clipper.setAdapter(mergeSettings.getForwardAdapter());
      Read raw = new Read("@NoOverlap", "ATTTATTAAATTT", "+", "!!!!!!!!!!!!!");
      Read clipped = clipper.clip(raw);
      //Testing output now here
      assertEquals(raw.sequence, clipped.sequence);
    }

    @Test
    public void clipper_overlap_9bpAdapter() throws Exception {
        Clipper clipper = new Clipper(mergeSettings);
        clipper.init();
        clipper.setAdapter(mergeSettings.getForwardAdapter());
        Read raw = new Read("@NoOverlap", "ATTTAAGATCGGAA", "+", "!!!!!!!!!!!!!!");
        Read clipped = clipper.clip(raw);
        assertEquals(raw.sequence.substring(0,5), clipped.sequence);
    }


    /*
    Case 2: Complete overlap (only adapter basically, there should be nothing left of the read here!)
     */

    @Test
    public void clipper_complete_overlap_adapter_sequence() throws Exception{
        Clipper clipper = new Clipper(mergeSettings);
        clipper.init();
        clipper.setAdapter(mergeSettings.getForwardAdapter());

        Read raw = new Read("@CompleteOverlap", "AGATCGGAAGAGCAC", "+", "!!!!!!!!!!!!!!!");
        Read clipped = clipper.clip(raw);
        assertEquals("null", clipped.name);
    }




    /*
    Case 3: Partial overlap (not complete adapter sequence, trickiest case)
     */

    @Test
    public void clipper_very_small_adapter_sequence_withm8() throws Exception{
        Clipper clipper = new Clipper(mergeSettings);
        clipper.init();
        clipper.setAdapter(mergeSettings.getForwardAdapter());

        Read raw = new Read("@VerySmallAdapter", "ATTTAAATTAAGGAAA", "+", "!!!!!!!!!!!!!!!!");
        Read clipped = clipper.clip(raw);
        assertEquals("ATTTAAATTAAGGAAA", clipped.sequence);
    }

    /*
    Case 4: Read contains full adapter sequence at 3'
     */

    /*
    Case 5: Read contains full adapter sequence at 5'
     */

}
