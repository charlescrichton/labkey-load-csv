package ox.labkey.download;

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
public class PropertiesFile extends ErrorReporting {

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



    private String userName;
    private String userPassword;

    private String baseServerURL;
    private String folderPath;

    private Map<String,List<String>> enumNameToValues;


    public List<String> getErrors() {
        return errors;
    }

    /*
        The text of any errors;
         */
    List<String> errors;

    public PropertiesFile(String propertiesFilePath) {
        super(logger);
        attemptToLoadProperties(propertiesFilePath);
    }


    /*
    private void addCause(Throwable t, List<String> causes) {
        if (t.getCause() != null) {
            addCause(t,causes);
        }
        causes.add(t.getMessage());
    }
    */

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
            exitCode = -5;
            return;
        }

        //Now lets get the data out of the properties file
        userName = findStringProperty("user.name");
        userPassword = findStringProperty("user.password");
        baseServerURL = findStringProperty("base.server.url");
        folderPath = findStringProperty("folder.path");



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
            exitCode = -6;
        }
        return value;
    }


}
