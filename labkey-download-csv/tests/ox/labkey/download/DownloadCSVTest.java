package ox.labkey.download;

import junit.framework.TestCase;

import java.util.HashMap;

public class DownloadCSVTest extends TestCase {


    public void testInner_main_test2() throws Exception {

		String pathToProject = "/projects"

        String [] args = {
                "-p",
                pathToProject + "/labkey-load-csv/labkey-download-csv/properties/test2.properties",
                "-o",
                pathToProject + "/labkey-load-csv/labkey-download-csv/properties/outputfile.csv",
                "-s",
                "sample_metadata",
                "-q",
                "Samples_Sent",
                //"Data",
                      "-l",
                "-e"

        };

        System.out.print("Test args: "); for(String a : args) { System.out.print(" "+a);} System.out.println();

        try {
            for (int i = 0; i < 1; i++) {
                int return_value = DownloadCSV.inner_main(args);

                assertEquals(0, return_value);
            }
        } catch(Throwable t) {
            fail(t.getMessage());
        } finally {
            System.out.flush();
            System.err.flush();
        }
    }


}