package ox.labkey.csv.upload;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;

/**
 * Created by crc on 23/02/15.
 */
public abstract class ErrorReporting {

    public enum ExitCode {

        ServerError(-7,"Error on server"),
        IOExceptionOccurred(-6,"IO Exception occurred"),
        BadArguments(-5,"Unable to parse arguments"),
        UnableToFindMatchingAssay(-4,"Unable to find matching Assay"),
        BadSiteArgument(-3,"Bad site argument"),
        UnableToLoadPropertiesFile(-2,"Unable to load properties file"),
        ReportHelpInfo(-1,"Reported help information"),
        NoErrors(0,"The program exited without an error"),
        UnableToLoadCSVFile(1,"Unable to load CSV file"),
        DataFormatError(2,"There was an error with the format of the the CSV data")
        ;

        int code;
        String description;

        ExitCode(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getExitCode() {
            return code;
        }

        public String toString() { return String.format("%+d = '%s'",code,description).replace('+',' ');}

        public static String exitCodeHelp() {
            String values = "";
            for (ExitCode info : EnumSet.allOf(ExitCode.class)) {
                values += "    " + info.toString() + "\n";
            }
            return values;
        }
    }

    //Store the logger of the class which extends this one.
    Logger logger;

    ExitCode exitCode = ExitCode.NoErrors;

    public ExitCode getExitCode() {
        return exitCode;
    }

    public ErrorReporting(Logger logger) {
        this.logger = logger;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return errors != null;
    }

    public String lastErrorMessage() {
        return errors.get(errors.size()-1);
    }

    /*
        The text of any errors;
         */
    List<String> errors;

    protected void addError(String error) {
        logger.error(error);
        if (errors == null) {
            errors = new ArrayList<String>();
        }
        errors.add(error);
    }

    protected void addErrors(List<String> extra_errors) {

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

    protected void addThrowable(String message, Throwable t) {
        logger.error(message,t);
        List<String> causes = new ArrayList<String>();
        causes.add(message);
        addCause(t, causes);
        addErrors(causes);
    }

    protected void checkForNonNullArgument(String fieldName, Object value) {
        if (value == null) {
            String errorMessage = ""+fieldName+" cannot be null";
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

}
