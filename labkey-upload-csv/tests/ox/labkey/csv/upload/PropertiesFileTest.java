package ox.labkey.csv.upload;

import junit.framework.TestCase;

public class PropertiesFileTest extends TestCase {

    public void testCanLoadProperties() {

		String pathToProject = "/projects"

        PropertiesFile pf = new PropertiesFile( pathToProject + "/labkey-load-csv/properties-files/example.properties");

        assertNull(pf.getErrors());

    }

}