package ox.labkey.csv.upload;

import org.apache.log4j.Logger;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.remoteapi.assay.ImportRunResponse;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * An assay is a set of information on a server.
 *
 * This is represented by a connection, a folder path and the rowID of the assay.
 *
 * Created by crc on 2015-02-09.
 */
public class AssayEndpoint extends ErrorReporting {

    static Logger logger = Logger.getLogger(AssayEndpoint.class);

    private Connection connection;
    private String folderPath;
    private int rowId;


    /**
     * Create a representation of an AssayEndpoint on LabKey server.
     * An AssayEndpoint is the schema for a set of CSV files we are going to upload.
     *
     * @param connection The baseURL and credentials of the user.
     * @param folderPath The path to the LabKey folder containing the AssayEndpoint. For example: /Home/Samples/Blood
     * @param rowId      The rowId of the assay. You can find this in LabKey as follows. Admin > Manage Assays. Select AssayEndpoint. Look for the rowId in the URL.
     */
    public AssayEndpoint(Connection connection, String folderPath, int rowId) {
        super(logger);
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        if (folderPath == null) {
            throw new IllegalArgumentException("folderPath cannot be null");
        }
        if (rowId < 0) {
            throw new IllegalArgumentException("rowId cannot be negative");
        }

        this.connection = connection;
        this.folderPath = folderPath;
        this.rowId = rowId;
    }


    /**
     * This uploads the CSV file into a batch within an AssayEndpoint.
     * @param file The CSV file.
     * @param batchId The batch ID you want ot load into. Or -1 if you don't want to specify a batch.
     * @param runName The name you want ot associate with the run contained within the batch. This can be null if you do not want to specify a run name.
     * @param runComment The comment to associate with the run being uploaded. Can be null if not specified.
     * @param runProperties This is a mapping of properties to associate with the uploaded run. Can be null.
     * @param batchProperties This is a mapping of properties to associate with the uploaded batch. Can be null.
     * @return Null if successful, or a string explaining the error if there was a problem.
     */
    public void uploadCSVFileToBatch(File file,
                                        int batchId,
                                        String runName,
                                        String runComment,
                                        Map<String, Object> runProperties,
                                        Map<String, Object> batchProperties,
                                        boolean useJson) {
        //1. Check that the file exists
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("file '%1s' should exist", file.getAbsolutePath()));
        }

        //2. Set up command to call in the LabKey Remote API
        ImportRunCommand command = new ImportRunCommand(rowId, file);

        //3. Set the batchID if one is specified
        if (batchId >= 0) {
            command.setBatchId(batchId);
        }

        //4. Set up the runName if one is specified
        if (runName != null) {
            command.setName(runName);
        }

        //5.Set up the runComment if one is specified
        if (runComment != null) {
            command.setComment(runComment);
        }

        //6.Set any run properties if specified.
        if (runProperties != null) {
            command.setProperties(runProperties);
        }

        //7.Set any batch properties if specified
        if (batchProperties != null) {
            command.setBatchProperties(batchProperties);
        }

        //Not sure why this needs to be set? But it is set in the examples.
        command.setUseJson(useJson);
        logger.info("UseJSON = "+useJson);


        try {
            ImportRunResponse resp = command.execute(connection, folderPath);
            logger.info(String.format("Success: ImportRunResponse(url=\"%1s\", assayId=%2s, batchId=%3s, runId=%4s", resp.getSuccessURL(), resp.getAssayId(), resp.getBatchId(), resp.getRunId()));
        } catch (IOException e) {
            logger.error("CommandException", e);
            exitCode = ExitCode.IOExceptionOccurred;
            addThrowable("Error executing command", e);
            addError(e.getMessage());
        } catch (CommandException e) {
            logger.error(String.format("CommandException(statusCode=%1s,text='%2s')",e.getStatusCode(),e.getResponseText()), e);
            switch (e.getStatusCode()) {

                case 200:
                    exitCode = ExitCode.DataFormatError;
                    break;

                case 400:
                    exitCode = ExitCode.DataFormatError;
                    break;

                default:
                    exitCode = ExitCode.ServerError;
            }
            addError(e.getResponseText());
        }
    }

    /*
    private String getCauseMessage(Throwable t) {
        if (t.getCause() != null) {
            return getCauseMessage(t);
        }
        return t.getMessage();
    }
    */


    /*
    public void printAssayList() {
        AssayListCommand asl = new AssayListCommand();

        try {
            AssayListResponse resp = asl.execute(connection, folderPath);
            System.out.println("Success!");
            System.out.println("  definitions:     " + resp.getDefinitions());
            List<Map<String,Object>> definitions = resp.getDefinitions();
            for(Map<String,Object> definition : definitions) {
                System.out.print("rowId: "+definition.get("id"));
                System.out.println(" name: " + definition.get("name"));
            }
        } catch(IOException e) {
            logger.error("IOException", e);
        }
        catch
         (CommandException e) {
            logger.error("CommandException", e);
        }

    }
    */
}










