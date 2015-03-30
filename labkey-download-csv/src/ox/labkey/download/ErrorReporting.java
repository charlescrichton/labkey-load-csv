package ox.labkey.download;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by crc on 23/02/15.
 */
public abstract class ErrorReporting {

    //Store the logger of the class which extends this one.
    Logger logger;

    int exitCode = 0;

    public int getExitCode() {
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
