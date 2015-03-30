package ox.labkey.csv.upload;

import java.util.HashSet;
import java.util.List;

/**
 * Created by crc on 20/02/15.
 */
public class AssayProperties{

    public AssayProperties(String name,
                           //int rowID, //We find the row ID from the web-service.
                           List<String> expectedColumns, List<String> expectedTypes) {

        if (name == null) {
            throw new IllegalArgumentException("name may not be null");
        }

        if (expectedColumns == null) {
            throw new IllegalArgumentException("expectedColumns may not be null");
        }

        if (expectedTypes == null) {
            throw new IllegalArgumentException("expectedTypes may not be null");
        }

        this.name = name;
        //this.rowID = rowID;
        this.expectedColumns = expectedColumns;
        this.expectedTypes = expectedTypes;
    }

    String name;
    //int rowID;
    List<String> expectedColumns;
    List<String> expectedTypes;

    public String getName() {
        return name;
    }

    //public int getRowID() {
    //    return rowID;
    //}

    public List<String> getExpectedColumns() {
        return  expectedColumns;
    }

    public List<String> getExpectedTypes() {
        return expectedTypes;
    }

    public boolean firstSCVLineMatches(HashSet<String> firstLineColumns) {
        HashSet<String> expectedColumnsSet = new HashSet<String>(expectedColumns);
        return expectedColumnsSet.equals(firstLineColumns);
    }




}
