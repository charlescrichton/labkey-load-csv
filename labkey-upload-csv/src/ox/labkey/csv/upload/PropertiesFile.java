package ox.labkey.csv.upload;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Created by crc on 20/02/15.
 * This represents the properties file and the information loaded by it.
 */
public class PropertiesFile {

    static Logger logger = Logger.getLogger(PropertiesFile.class);

    private Properties properties;

    public String getUserName() {
        return userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public String getBaseServerURL() {
        return baseServerURL;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public Map<String, List<String>> getEnumNameToValues() {
        return enumNameToValues;
    }

    public Map<String, AssayProperties> getAssayKeyToProperties() {
        return assayKeyToProperties;
    }

    private String userName;
    private String userPassword;

    private String baseServerURL;
    private String folderPath;

    private Map<String,List<String>> enumNameToValues;
    private Map<String,AssayProperties> assayKeyToProperties;

    public List<String> getErrors() {
        return errors;
    }

    /*
        The text of any errors;
         */
    List<String> errors;

    public PropertiesFile(String propertiesFilePath) {
        attemptToLoadProperties(propertiesFilePath);
    }

    private void addError(String error) {
        logger.error(error);
        if (errors == null) {
            errors = new ArrayList<String>();
        }
        errors.add(error);
    }

    private void addErrors(List<String> extra_errors) {

        if (errors == null) {
            errors = new ArrayList<String>();
        }
        errors.addAll(extra_errors);
    }

    private void addCause(Throwable t, List<String> causes) {
        if (t.getCause() != null) {
            addCause(t,causes);
        }
        causes.add(t.getMessage());
    }

    private void addThrowable(String message, Throwable t) {
        logger.error(message,t);
        List<String> causes = new ArrayList<String>();
        causes.add(message);
        addCause(t, causes);
        addErrors(causes);
    }

    private void attemptToLoadProperties(String propertiesFilePath) {

        logger.info("Attempt to load properties from "+propertiesFilePath);

        //Try and load it
        File  propertiesFile = new File(propertiesFilePath);
        FileInputStream fis;
        try {
            fis = new FileInputStream(propertiesFile);
        } catch (FileNotFoundException e) {
            addThrowable(String.format("Could not find the properties file '%1s'",propertiesFilePath),e);
            return;
        }

        properties = new Properties();
        try {
            properties.load(fis);
        }
        catch (IOException e) {
            addThrowable(String.format("Problem loading the properties file '%1s'",propertiesFilePath),e);
            return;
        }

        //Now lets get the data out of the properties file
        userName = findStringProperty("user.name");
        userPassword = findStringProperty("user.password");
        baseServerURL = findStringProperty("base.server.url");
        folderPath = findStringProperty("folder.path");
        loadAllEnumerations();
        loadAssayDefinitions();
    }

    private void loadAssayDefinitions() {
        //Go through the assay names and load all of the details if possible

        assayKeyToProperties = new HashMap<String, AssayProperties>();

        for(String assayKey : assayKeys()) {

            logger.info("Looking for assayKey:"+assayKey);

            //Load values for 'assay.ASSAY_NAME.name', 'assay.ASSAY_NAME.rowId', 'assay.ASSAY_NAME.columnNames', 'assay.ASSAY_NAME.columnTypes'

            boolean wellFormed = true;


            String assayName = findStringProperty("assay." + assayKey + ".name");
            if (assayName == null) {
                addError("Unable to find property 'assay." + assayKey + ".name");
                wellFormed = false;
            }

            /*
            String assayRowIdString = findStringProperty("assay." + assayKey + ".rowId");
            int assayRowId = -1;
            if (assayRowIdString == null) {
                addError("Unable to find property 'assay." + assayKey + ".rowId");
                wellFormed = false;
            } else {
                try {
                    assayRowId = Integer.parseInt(assayRowIdString.trim());
                } catch (NumberFormatException nfe) {
                    addThrowable(String.format("Unable to parse number for assay.%1s.rowId", assayKey), nfe);
                    wellFormed = false;
                }
            }
            */

            String columnNames = findStringProperty("assay." + assayKey + ".columnNames");
            if (columnNames == null) {
                addError("Unable to find property 'assay." + assayKey + ".columnNames");
                wellFormed = false;
            }

            String columnTypes = findStringProperty("assay." + assayKey + ".columnTypes");
            if (columnTypes == null) {
                addError("Unable to find property 'assay." + assayKey + ".columnTypes");
                wellFormed = false;
            }

            //Check that the columnNames and columnTypes are well formed
            if (columnNames != null && columnTypes != null) {
                List<String> expectedColumnNames = splitByComma(columnNames);
                List<String> expectedColumnTypes = splitByComma(columnTypes);

                //Check that there is at least one column defined
                if (expectedColumnNames.size() == 0) {
                    addError("No columns defined in 'assay." + assayKey + ".columnNames");
                    wellFormed = false;
                }

                //Check that there are the same number of types as columns
                if (expectedColumnNames.size() != expectedColumnTypes.size()) {
                    addError("There are " + expectedColumnNames.size() + " columns defined in the property assay." + assayKey + ".columnNames' however there are " + expectedColumnTypes.size() + " types defined in the property  assay." + assayKey + ".columnNames");
                    wellFormed = false;
                }

                //Check that we understand all of the types:
                //String, Int, Double, Date, DateTime, enum.*

                for (String type : expectedColumnTypes) {

                    if (type.equals("String") || type.equals("Int") || type.equals("Double") || type.equals("Date") || type.equals("DateTime")) {
                        //Do nothing - this is fine
                    } else if (type.startsWith("enum.")) {
                        //Look up to see if we have the type in the list of enums
                        final String enumPrefix = "enum.";
                        final int enumPrefixLength = enumPrefix.length();
                        String enumName = type.substring(enumPrefixLength);
                        List<String> values = enumNameToValues.get(enumName);
                        if (values == null) {
                            addError("Unknown enumeration type '" + type + "' in the property assay." + assayKey + ".columnTypes'");
                            wellFormed = false;
                        }

                    } else {
                        addError("Unknown type '" + type + "' in the property assay." + assayKey + ".columnTypes'");
                        wellFormed = false;
                    }
                }

                if (wellFormed) {
                    AssayProperties ap = new AssayProperties(assayName,
                            //assayRowId,
                            expectedColumnNames, expectedColumnTypes);
                    assayKeyToProperties.put(assayKey, ap);
                }
            }
        }
    }

    private List<String> assayKeys() {

        //This goes through the list of keys looking for anything of the format assay.XXX(.*)
        //and returns the list of XXX

        List<String> assayNames = new ArrayList<String>();

        final String assayPrefix = "assay.";
        final int assayPrefixLength = assayPrefix.length();

        for (String pn : properties.stringPropertyNames()) {
            if (pn.startsWith(assayPrefix) && pn.length() > assayPrefixLength) {

                String assayName = pn.substring(assayPrefixLength);

                //Knock off anything after a '.'
                int firstIndexOfDot = assayName.indexOf('.');

                if (firstIndexOfDot == 0) {
                    addError(String.format("Unable to parse assay data: %1s" ,pn));
                } else if (firstIndexOfDot == 0) {
                    addError(String.format("Unable to parse assay name Double dots: %1s" ,pn));
                    //Do not record assay
                } else if (firstIndexOfDot >=0) {
                    assayName = assayName.substring(0,firstIndexOfDot);
                    assayNames.add(assayName);
                }
            }
        }

        return assayNames;
    }

    private void loadAllEnumerations() {
        //Lets load all of the enumerations
        enumNameToValues = new HashMap<String, List<String>>();
        {
            final String enumPrefix = "enum.";
            final int enumPrefixLength = enumPrefix.length();
            for (String pn : properties.stringPropertyNames()) {
                if (pn.startsWith(enumPrefix) && pn.length() > enumPrefixLength) {
                    //get enum name
                    String enumName = pn.substring(enumPrefixLength);
                    String values = properties.getProperty(pn);

                    List<String> vs = splitByComma(values);

                    enumNameToValues.put(enumName,vs);
                }
            }
        }
    }

    private List<String> splitByComma(String values) {
        List<String> vs = new ArrayList<String>();
        //Now tokenize the values by ','
        for (String v : values.split(",")) {
            vs.add(v.trim());
        }
        return vs;
    }


    private String findStringProperty(String property){
        String value = properties.getProperty(property);
        if (value == null) {
            addError(String.format("Properties file is missing %1s=",property));
        }
        return value;
    }


    public AssayProperties findMatchingProperties(HashSet<String> csvFirstLineColumns) {

        for(AssayProperties ap: assayKeyToProperties.values()) {
            if (ap.firstSCVLineMatches(csvFirstLineColumns)) {
                return ap;
            }
        }

        return null;
    }

    public void logDifferenceInProperties(HashSet<String> csvFirstLineColumns) {
        for(AssayProperties ap: assayKeyToProperties.values()) {
            HashSet diff = new HashSet(ap.getExpectedColumns());
            diff.removeAll(csvFirstLineColumns);
            logger.info(" Diff: Assay: "+ap.getName()+" diff: "+diff);
        }
    }

    public void logAvailableAssays() {

        for(AssayProperties ap: assayKeyToProperties.values()) {
           logger.info(" Assay: "+ap.getName()+" columns: "+ap.getExpectedColumns());
        }

    }

}
