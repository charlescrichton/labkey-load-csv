package ox.labkey.download;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by crc on 23/02/15.
 */
public class QueryEndpoint extends ErrorReporting {

    static Logger logger = Logger.getLogger(QueryEndpoint.class);

    private Connection connection;
    private String folderPath;
    private String schemaName;
    private String queryName;
    private boolean excludeFolder;

    public QueryEndpoint(Connection connection, String folderPath, String schemaName, String queryName, boolean excludeFolder) {
        super(logger);
        checkForNonNullArgument("connection",connection);
        checkForNonNullArgument("folderPath",folderPath);
        checkForNonNullArgument("schemaName",schemaName);
        checkForNonNullArgument("queryName",queryName);

        this.connection = connection;
        this.folderPath = folderPath;
        this.schemaName = schemaName;
        this.queryName = queryName;
        this.excludeFolder = excludeFolder;
    }

    /**
     * Return the CSV file or null if there was a problem.
     *
     * @return
     */
    public String downloadCSVRowsFromLabKey() {

        //Define command
        SelectRowsCommand command = new SelectRowsCommand(schemaName, queryName);

        SelectRowsResponse response;
        try {
            response = command.execute(connection, folderPath);
        } catch (CommandException e) {
            addThrowable("Problem executing command", e);
            exitCode = -2;
            return null;
        } catch (IOException e) {
            addThrowable("Problem executing command", e);
            exitCode = -3;
            return null;
        }

        logger.info("response.getStatusCode() = " + response.getStatusCode());

        //Get the column names
        List<String> columnNames = getColumnNames(response);

        //CSV Content
        String header = calculateCSVHeader(response, columnNames);
        String body = calculateCSVRows(response, columnNames);

        return header + "\n" + body;
    }

    /**
     * Return the CSV file or null if there was a problem.
     *
     * @return
     */
    public Integer getNoCSVRows() {

        //Define command
        SelectRowsCommand command = new SelectRowsCommand(schemaName, queryName);

        SelectRowsResponse response;
        try {
            response = command.execute(connection, folderPath);
        } catch (CommandException e) {
            addThrowable("Problem executing command", e);
            exitCode = -2;
            return null;
        } catch (IOException e) {
            addThrowable("Problem executing command", e);
            exitCode = -3;
            return null;
        }

        logger.info("response.getStatusCode() = " + response.getStatusCode());
        List<Map<String, Object>> row = response.getRows();
        if(row==null){
            return null;
        }
        else {
            return row.size();
        }

    }

    private String calculateCSVHeader(SelectRowsResponse response, List<String> columnNames) {
        String headers = "";
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            if (i != 0) {
                headers += ",";
            }
            SelectRowsResponse.ColumnDataType type = response.getColumnDataType(columnName);
            headers += columnName;// + "(" + type + ")";

        }
        return headers;
    }

    private List<String> getColumnNames(SelectRowsResponse response) {
        List<Map<String, Object>> columnModel = response.getColumnModel();

        List<String> columnNames = new LinkedList<String>();

        //output the column names
        for (Map<String, Object> column : columnModel) {
            String columnName = (String) column.get("dataIndex");
            if (excludeFolder || !"Folder".equals(columnName)) {
                columnNames.add(columnName);
            }
        }
        return columnNames;
    }

    private String calculateCSVRows(SelectRowsResponse response, List<String> columnNames) {
        String output = "";

        //Output the rows
        boolean notFirstRow = false;
        for (Map<String, Object> row : response.getRows()) {

            if (notFirstRow) {
                output += "\n";
            } else {
                notFirstRow = false;
            }

            output += getCSVRow(columnNames, row) + "\n";

        }

        return output;
    }

    private String getCSVRow(List<String> columnNames,  Map<String, Object> row) {

        String output ="";

        for (int i = 0; i < columnNames.size(); i++) {

            if (i != 0) {
                output += ",";
            }

            String columnName = columnNames.get(i);
            try {

                Object value = row.get(columnName);

                if (value != null) {
                    output += "" + value;
                }

            } catch (Throwable t) {
                addThrowable("Error whilst processing value for column: " + columnName, t);
                exitCode = -4;
            }

        }
        return output;
    }

}
