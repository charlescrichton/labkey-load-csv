package ox.labkey.download;

import org.apache.log4j.Logger;

import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

import org.json.simple.JSONArray;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.AssayListResponse;
import org.labkey.remoteapi.query.*;

import java.io.*;
import java.util.*;

/**
 * Created by crc on 23/02/15.
 */
public class DownloadCSV {

    static String semantic_version = "v1.1.0";

    static Logger logger = Logger.getLogger(DownloadCSV.class);
    private static String javaCommand = "java -jar labkey-download-csv.jar -p <properties> -s <schemaname> -q <queryname> -o <output.file.csv>";


    public static void main(String[] args) {
        System.exit(inner_main(args));
    }

    public static int inner_main(String[] args) {

        Options options = defineOptions(true);
        Options options2 = defineOptions(false);

        CommandLineParser parser = new BasicParser();
        CommandLine line = null;

        try {
            // parse the command line arguments with partial parser
            line = parser.parse(options, args);
        } catch (Throwable t) {
            //We will accept parsing errors at this point. This is only help options.
        }

        if (line != null) {
            if (line.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(javaCommand, options2);
                //Normally we would exit with a good code - but here. We want help, if requested to cause a error exit code.

                return (-1);
            }

            if (line.hasOption("v")) {
                System.out.println("labkey-download-csv.jar: " + semantic_version);
                return (-1);
            }
        }

        try {
            //Re-run with full parser
            line = parser.parse(options2, args);

        } catch (ParseException exp) {
            System.out.println(exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(javaCommand, options);

            return (-1);
        }


        //Set Logging level

        if (line.hasOption("l")) {
            BasicConfigurator.configure();
            logger.info("Logging turned on");
            Logger.getRootLogger().setLevel(Level.INFO);
        } else {
            Logger.getRootLogger().setLevel(Level.OFF);
        }



        //Get the properties file and load it

        String propertiesFilePath = line.getOptionValue("p");
        PropertiesFile propertiesFile = new PropertiesFile(propertiesFilePath);

        if (propertiesFile.hasErrors()) {
            System.err.println("Unable to load properties file: " + propertiesFilePath);
            return propertiesFile.getExitCode();
        }

        //Load variables
        boolean excludeFolder = line.hasOption("e");
        String schemaName = line.getOptionValue("s");
        String queryName = line.getOptionValue("q");
        String exportCSVFileNameName = line.getOptionValue("o");

        logger.info("schemaName = " + schemaName);
        logger.info("queryName = " + queryName);
        logger.info("exportCSVFileNameName = " + exportCSVFileNameName);
        logger.info("excludeFolder = "+excludeFolder);

        //Get properties from files
        String baseURL = propertiesFile.getBaseServerURL();
        String folderPath = propertiesFile.getFolderPath();
        String userName = propertiesFile.getUserName();

        logger.info("baseURL = " + baseURL);
        logger.info("folderPath = " + folderPath);
        logger.info("userName = " + userName);

        //Create connection
        Connection connection = new Connection(baseURL, userName, propertiesFile.getUserPassword());

        QueryEndpoint queryEndpoint = new QueryEndpoint(connection, folderPath, schemaName, queryName,excludeFolder);
        if (queryEndpoint.hasErrors() ) {
            System.err.println(queryEndpoint.lastErrorMessage());
            return queryEndpoint.getExitCode();
        }

        String csvText = queryEndpoint.downloadCSVRowsFromLabKey();
        if (queryEndpoint.hasErrors() ) {
            System.err.println(queryEndpoint.lastErrorMessage());
            return queryEndpoint.getExitCode();
        }

        Integer noRows = queryEndpoint.getNoCSVRows();
        if(noRows == null){
            System.err.println("noRows == null");
            System.err.println(queryEndpoint.lastErrorMessage());
            return queryEndpoint.getExitCode();
        }
        if(noRows == 0){
            System.out.println("Nothing to do!");
            return 0;
        }

        OutputCSV outputCSV = new OutputCSV();

        outputCSV.writeToFile(exportCSVFileNameName, csvText);
        if (outputCSV.hasErrors() ) {
            System.err.println(outputCSV.lastErrorMessage());
            return queryEndpoint.getExitCode();
        }

        return 0;
    }
 
    public static Options defineOptions(boolean onlyHelp) {
        Option help = new Option("help", "print this message");

        Option versionOption = new Option("v", "Print version");

        Option propertiesFileOption = OptionBuilder.withArgName("properties")
                .isRequired()
                .hasArg()
                .withDescription("use given properties file")
                .create("p");


        Option csvFileOption = OptionBuilder.withArgName("output.file.csv")
                .isRequired()
                .hasArg()
                .withDescription("the file path CSV file to download the CSV file to")
                .create("o");


        Option schemaNameOption = OptionBuilder.withArgName("schemaname")
                .isRequired()
                .hasArg()
                .withDescription("The schema name containing the query to export a CSV file from")
                .create("s");

        Option queryNameOption = OptionBuilder.withArgName("queryname")
                .isRequired()
                .hasArg()
                .withDescription("The name of the query to export a CSV file from")
                .create("q");

        Option excludeFolder = OptionBuilder.withDescription("Exclude 'Folder column'").create("e");



        Option printAssayList = OptionBuilder.withDescription("Print query List").create("b");

        Option turnOnLogging = OptionBuilder.withDescription("Turn on console logging").create("l");

//        Option turnOnJson = OptionBuilder.withDescription("Use JSON in communications").create("j");

        Options options = new Options();


        options.addOption(help);
        options.addOption(versionOption);
        if (!onlyHelp) {
            options.addOption(propertiesFileOption);
            options.addOption(csvFileOption);
            options.addOption(schemaNameOption);
            options.addOption(queryNameOption);
        }
        options.addOption(turnOnLogging);
 //       options.addOption(turnOnJson);
        options.addOption(printAssayList);
        options.addOption(excludeFolder);

        return options;
    }

}
