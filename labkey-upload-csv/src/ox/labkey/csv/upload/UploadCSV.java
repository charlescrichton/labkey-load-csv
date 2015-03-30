package ox.labkey.csv.upload;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.labkey.remoteapi.Connection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.cli.*;


/**
 * Created by crc on 2015-02-10.
 */
public class UploadCSV {

    static String semantic_version = "v1.0.3";
    static String defaultCommandLine = "java -jar labkey-upload-csv.jar -p <csv.properties> -s <Site> -f <File.csv>";

    static Logger logger = Logger.getLogger(UploadCSV.class);

    public static void main(String[] args) {
        System.exit(log_exit_code(inner_main(args)));
    }

    public static int log_exit_code(ErrorReporting.ExitCode exitCode) {
        if (exitCode.getExitCode() == 0) {
            logger.info("Exiting with exit code: "+exitCode);
        } else {
            logger.error("Exiting with exit code: "+exitCode);
        }
        return exitCode.getExitCode();
    }

    private static String printArgs(String [] args) {
        String b = "";
        for(String a : args) { b+=" "+a;}
        return b;
    }

    public static ErrorReporting.ExitCode inner_main(String[] args) {

        //java -jar xxx.jar ox.labkey.csv.upload.UploadCSV -p <csv.properties> -s <Site> -a <AssayType> -f <File.csv>

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
                formatter.printHelp(defaultCommandLine, options2);
                //Normally we would exit with a good code - but here. We want help, if requested to cause a error exit code.

                return ErrorReporting.ExitCode.ReportHelpInfo;
            }

            if (line.hasOption("v")) {
                System.out.println("labkey-upload-csv.jar: " + semantic_version);
                return ErrorReporting.ExitCode.ReportHelpInfo;
            }

            if (line.hasOption("e")) {
                System.out.println("Exit codes");
                System.out.println(ErrorReporting.ExitCode.exitCodeHelp());
                return ErrorReporting.ExitCode.ReportHelpInfo;
            }
        }

        try {

            //Re-run with full parser

            line = parser.parse(options2, args);

            if (line.hasOption("l")) {
                BasicConfigurator.configure();
                logger.info("Program: java -jar labkey-upload-csv.jar");
                logger.info("Version: "+semantic_version);
                logger.info("Args: "+printArgs(args));
                Logger.getRootLogger().setLevel(Level.INFO);
            } else {
                Logger.getRootLogger().setLevel(Level.OFF);
            }

            //Get the properties file and load it

            String propertiesFilePath = line.getOptionValue("p");
            PropertiesFile propertiesFile = new PropertiesFile(propertiesFilePath);

            if (propertiesFile.getErrors() != null) {
                System.err.println("Unable to load properties file: " + propertiesFilePath);
                return ErrorReporting.ExitCode.UnableToLoadPropertiesFile;
            }

            //Find the AssayType of the file
            String csvFilePath = line.getOptionValue("f");
            HashSet<String> csvFirstLineColumns = loadFirstLineColumns(csvFilePath);

            if (csvFirstLineColumns == null) {
                System.err.println("Unable to load first line of csv file: " + csvFilePath);
                return ErrorReporting.ExitCode.UnableToLoadCSVFile;
            }

            //Find matching assay
            AssayProperties assayProperties = propertiesFile.findMatchingProperties(csvFirstLineColumns);

            if (assayProperties == null) {
                System.err.println("Unable to find a matching assay for first line of csv file: " + csvFilePath);
                logger.info("The first line has the following column headings: "+csvFirstLineColumns);

                propertiesFile.logDifferenceInProperties(csvFirstLineColumns);
                propertiesFile.logAvailableAssays();

                return ErrorReporting.ExitCode.UnableToLoadCSVFile;
            }

            //Find the site
            String siteName = line.getOptionValue("s");
            if (siteName == null) {
                System.err.println("Unable to find a site name specified using the -s property");
                return ErrorReporting.ExitCode.BadSiteArgument;
            }

            boolean useJson = false;
            if (line.hasOption("j")) {
                useJson = true;
            }

            Connection connection = new Connection(propertiesFile.getBaseServerURL(), propertiesFile.getUserName(), propertiesFile.getUserPassword());

            AssayNameToRowId anri = new AssayNameToRowId(connection, propertiesFile.getFolderPath());

            Map<String,Integer> nameToRowId = anri.nameToRowId();
            Integer rowId = nameToRowId.get(assayProperties.getName());
            if (rowId == null) {
                System.err.println("Unable to find a rowId for assay named: '"+assayProperties.getName()+"'");
                System.err.println("Available Assays:");
                for(String name: nameToRowId.keySet()) {
                    System.err.print(" Assay: " + name);
                    System.err.println(" (rowId= "+nameToRowId.get(name)+")");
                }
                return ErrorReporting.ExitCode.UnableToFindMatchingAssay;
            }

            if (line.hasOption("a")) {
                for(String name: nameToRowId.keySet()) {
                    System.out.print(" Assay: " + name);
                    System.out.println(" (rowId= " + nameToRowId.get(name) + ")");
                }
            }

            AssayEndpoint ae = new AssayEndpoint(connection, propertiesFile.getFolderPath(), rowId);

            File csvFile = new File(csvFilePath);

            HashMap<String, Object> batchProperties = new HashMap<String, Object>();
            batchProperties.put("Site", siteName);

            HashMap<String, Object> runProperties = new HashMap<String, Object>();
            runProperties.put("Site", siteName);

            //if (line.hasOption("a")) {
            //    ae.printAssayList();
            //}

            ae.uploadCSVFileToBatch(
                    csvFile, 0, null, null, runProperties, batchProperties, useJson);


            if (ae.hasErrors()) {
                System.out.println(ae.lastErrorMessage());
                System.out.flush();
            }

            return ae.getExitCode();


        } catch (ParseException exp) {
            System.out.println(exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(defaultCommandLine, options);

            return ErrorReporting.ExitCode.IOExceptionOccurred;
        }

        //return ErrorReporting.ExitCode.NoErrors.getExitCode();
    }

    private static HashSet<String> loadFirstLineColumns(String csvFilePath) {
        File csvFile = new File(csvFilePath);
        try {
            FileReader namereader = new FileReader(csvFile);
            BufferedReader in = new BufferedReader(namereader);

            String firstLine = in.readLine();

            namereader.close();

            return splitByComma(firstLine);

        } catch (IOException e) {
            logger.error("Unable to load CSV file: "+csvFilePath,e);
        }
        return null;
    }

    private static HashSet<String> splitByComma(String values) {
        HashSet<String> vs = new HashSet<String>();
        //Now tokenize the values by ','
        for (String v : values.split(",")) {

            String processed = v.replaceAll("[^A-z0-9_ ]","");
            if (!processed.equals(v)) {
                logger.warn("Read in the heading '"+v+"' as '"+processed+"'");
            }

            vs.add(processed.trim());
        }
        return vs;
    }

    public static Options defineOptions(boolean onlyHelp) {
            Option help = new Option("help", "Print this message");

            Option versionOption = new Option("v", "Print version");
        Option exitOption = new Option("e", "Print exit codes");

            Option propertiesFileOption = OptionBuilder.withArgName("properties")
                    .isRequired()
                    .hasArg()
                    .withDescription("Use given properties file")
                    .create("p");

            Option siteOption = OptionBuilder.withArgName("site")
                    .isRequired()
                    .hasArg()
                    .withDescription("The Site metadata in the uploaded batch")
                    .create("s");

            Option csvFileOption = OptionBuilder.withArgName("csvfile")
                    .isRequired()
                    .hasArg()
                    .withDescription("the CSV file to load into the LabKey AssayEndpoint")
                    .create("f");


        Option printAssayList = OptionBuilder.withDescription("Print assay List").create("a");

        Option turnOnLogging = OptionBuilder.withDescription("Turn on console logging").create("l");

        Option turnOnJson = OptionBuilder.withDescription("Use JSON in communications").create("j");

        Options options = new Options();

        options.addOption(help);
        options.addOption(versionOption);
        options.addOption(exitOption);

        if (!onlyHelp) {
            options.addOption(propertiesFileOption);
            options.addOption(siteOption);
            options.addOption(csvFileOption);
        }
        options.addOption(turnOnLogging);
        options.addOption(turnOnJson);
        options.addOption(printAssayList);

        return options;
    }




}
