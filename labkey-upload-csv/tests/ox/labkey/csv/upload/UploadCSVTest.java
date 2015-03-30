package ox.labkey.csv.upload;

import junit.framework.TestCase;

public class UploadCSVTest extends TestCase {


    public void testInnerMainHelp() throws Exception {

        String [] args = {
                "-help"
        };

        System.out.print("Test args: "); for(String a : args) { System.out.print(" "+a);} System.out.println();

        int return_value = UploadCSV.log_exit_code(UploadCSV.inner_main(args));

        assertEquals(ErrorReporting.ExitCode.ReportHelpInfo.getExitCode() ,return_value);

    }

    public void testInnerMainVersion() throws Exception {

        String [] args = {
                "-v"
        };

        System.out.print("Test args: "); for(String a : args) { System.out.print(" "+a);} System.out.println();

        int return_value = UploadCSV.log_exit_code(UploadCSV.inner_main(args));

        assertEquals(ErrorReporting.ExitCode.ReportHelpInfo.getExitCode() ,return_value);

    }

    public void testInnerMainExitCodesHelp() throws Exception {

        String [] args = {
                "-e", "-l"
        };

        System.out.print("Test args: "); for(String a : args) { System.out.print(" "+a);} System.out.println();

        int return_value = UploadCSV.log_exit_code(UploadCSV.inner_main(args));

        assertEquals(ErrorReporting.ExitCode.ReportHelpInfo.getExitCode() ,return_value);

    }
  

}