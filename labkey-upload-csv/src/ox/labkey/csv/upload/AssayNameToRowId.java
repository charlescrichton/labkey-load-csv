package ox.labkey.csv.upload;

import org.apache.log4j.Logger;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.AssayListCommand;
import org.labkey.remoteapi.assay.AssayListResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by crc on 22/02/15.
 */
public class AssayNameToRowId {

    Connection connection;
    String folderPath;

    static Logger logger = Logger.getLogger(AssayNameToRowId.class);

    public AssayNameToRowId(Connection connection, String folderPath) {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        if (folderPath == null) {
            throw new IllegalArgumentException("folderPath cannot be null");
        }

        this.connection = connection;
        this.folderPath = folderPath;
    }

    public Map<String,Integer> nameToRowId() {

        HashMap<String,Integer> nameToRowId = new HashMap<String, Integer>();
        AssayListCommand asl = new AssayListCommand();

        try {
            AssayListResponse resp = asl.execute(connection, folderPath);

            List<Map<String,Object>> definitions = resp.getDefinitions();

            for(Map<String,Object> definition : definitions) {
                try {
                    String name = (String) definition.get("name");
                    Long idl = (Long) definition.get("id");
                    Integer id = idl.intValue();

                    nameToRowId.put(name, id);

                    logger.info(String.format("Discovered Assay: %1s (rowId=%2s)",name,id));

                } catch (Throwable t) {
                    logger.warn("Unable to load name and id from a definition. Skipping.",t);
                }
            }
        } catch(IOException e) {
            logger.error("IOException", e);
        }
        catch
                (CommandException e) {
            logger.error("CommandException", e);
        }

        return nameToRowId;
    }

}
