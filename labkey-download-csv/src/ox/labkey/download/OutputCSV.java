package ox.labkey.download;

import org.apache.log4j.Logger;

import java.io.FileWriter;

/**
 * Created by crc on 23/02/15.
 */
public class OutputCSV extends ErrorReporting {

    static Logger logger = Logger.getLogger(OutputCSV.class);

    public OutputCSV() {
        super(logger);
    }

    public void writeToFile(String fileName, String csvText) {
        try {
            logger.info("Writing to file: "+fileName);
            FileWriter fw = new FileWriter(fileName);
            fw.write(csvText);
            fw.flush();
            fw.close();
        } catch (Throwable t) {
            addThrowable("Unable to output text to file: " + fileName, t);
            exitCode = -7;
        }
    }
}
