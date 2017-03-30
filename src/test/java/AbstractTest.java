import org.junit.Before;
import setting.MergeSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.mockito.Mockito.mock;


/**
 * Created by peltzer on 10/03/2017.
 * Abstract class for reading a FastQ file and setting things up for tests based on individual FastQ files.
 * This is intended to provide the basic background methods
 */
public abstract class AbstractTest {
    protected File[] in1 = null;
    protected File[] in2 = null;
    protected InputStream output = null;
    protected InputStreamReader outputStreamReader;
    protected BufferedReader bufferedReader;
    protected MergeSettings mergeSettings;

    @Before
    public abstract void setUp() throws Exception;

    protected void universalSetup(String forward, String reverse, File[] in1, File[] in2) throws Exception {
            //Mocking objects just for testing purposes
            outputStreamReader = mock(InputStreamReader.class);
            bufferedReader = mock(BufferedReader.class);

            mergeSettings = new MergeSettings();
            mergeSettings.setForwardReadsReader(in1);
            mergeSettings.setReverseReadsReader(in2);
            mergeSettings.setOutputFile("testOut/pathtooutputfile");
            mergeSettings.setLogFile("testOut/pathtologfile");

            mergeSettings.setForwardAdapter("AGATCGGAAGAGCACACGTCTGAACTCCAGTCAC");
            mergeSettings.setReverseAdapter("AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGTA");
            mergeSettings.setMinMergeOverlap(10);
            mergeSettings.setMinSequenceLength(25);
            mergeSettings.setKeepSequencesWithN(false);
            mergeSettings.setMinAdapterAlignmentLength(8);
            mergeSettings.setErrorRateForMerging(0.05);
            mergeSettings.setShowClippingStats(false);

            mergeSettings.setClipping(true);

            mergeSettings.setMatePairFileForward(new File("testOut/forward"));
            mergeSettings.setMatePairFileReverse(new File("testOut/reverse"));

            mergeSettings.setMerging(false);
            mergeSettings.setRemoveSingleReads(false);
            mergeSettings.setQualityTrimming(true);
            mergeSettings.setMinBaseQuality(20);
            mergeSettings.setQualityEncoding(33);
            mergeSettings.setNoTimeEstimation(true);
            // mergeSettings.setVerbose(verbose);
            mergeSettings.setMaxReadsPerQueue(1000);
            mergeSettings.setQualityBasedMM(false);

            if(true) {
                mergeSettings.setDiscardBadQualityReadsScore(0);
                mergeSettings.setMinGoodQualityBasePercentage(0.9);
            }

            mergeSettings.setTrim3P(0);
            mergeSettings.setTrim5P(0);
            mergeSettings.setLastBaseToKeep(Integer.MAX_VALUE);
        }


}
