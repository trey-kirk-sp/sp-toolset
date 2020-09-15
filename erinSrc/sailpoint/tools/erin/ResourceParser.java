package sailpoint.tools.erin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;

/**
 * Parses an input of log data from SCIM2Connector and extracts values from the log representing
 * Resources. For example, given the following log event:
 * 
 * 2020-09-11T08:10:53,282 DEBUG QuartzScheduler_Worker-2 openconnector.connector.scim2.SCIM2Connector:2419 - Response returned: {"schemas":["urn:ietf:params:scim:api:messages:2.0:ListResponse"],"totalResults":2,"itemsPerPage":5000,"startIndex":1,"Resources":[{"id":"27","schemas":["urn:ietf:params:scim:schemas:pam:1.0:ContainerPermission"],"container":{"name":"SonicWall","display":"SonicWall","value":"27","$ref":"http://10.0.0.2/SCIMConnector/v2/containers/27"},"user":{"display":"SS Admin","value":"2","$ref":"http://10.0.0.2/SCIMConnector/v2/users/2"},"rights":["Owner"],"meta":{"resourceType":"ContainerPermission","created":"2020-08-06T21:07:58Z","lastModified":"2020-08-12T15:38:46Z"}},{"id":"63","schemas":["urn:ietf:params:scim:schemas:pam:1.0:ContainerPermission"],"container":{"name":"SonicWall","display":"SonicWall","value":"27","$ref":"http://10.0.0.2/SCIMConnector/v2/containers/27"},"user":{"display":"SCIMConnector","value":"4","$ref":"http://10.0.0.2/SCIMConnector/v2/users/4"},"rights":["View"],"meta":{"resourceType":"ContainerPermission","created":"2020-08-06T21:07:58Z","lastModified":"2020-08-12T15:38:46Z"}}]}
 * 
 * The following Resources are extracted:
 * 
 * [
        {
            "id": "27",
            "schemas": ["urn:ietf:params:scim:schemas:pam:1.0:ContainerPermission"],
            "container": {
                "name": "SonicWall",
                "display": "SonicWall",
                "value": "27",
                "$ref": "http://10.0.0.2/SCIMConnector/v2/containers/27"
            },
            "user": {
                "display": "SS Admin",
                "value": "2",
                "$ref": "http://10.0.0.2/SCIMConnector/v2/users/2"
            },
            "rights": ["Owner"],
            "meta": {
                "resourceType": "ContainerPermission",
                "created": "2020-08-06T21:07:58Z",
                "lastModified": "2020-08-12T15:38:46Z"
            }
        },
        {
            "id": "63",
            "schemas": ["urn:ietf:params:scim:schemas:pam:1.0:ContainerPermission"],
            "container": {
                "name": "SonicWall",
                "display": "SonicWall",
                "value": "27",
                "$ref": "http://10.0.0.2/SCIMConnector/v2/containers/27"
            },
            "user": {
                "display": "SCIMConnector",
                "value": "4",
                "$ref": "http://10.0.0.2/SCIMConnector/v2/users/4"
            },
            "rights": ["View"],
            "meta": {
                "resourceType": "ContainerPermission",
                "created": "2020-08-06T21:07:58Z",
                "lastModified": "2020-08-12T15:38:46Z"
            }
        }
    ]
    
 * @author trey.kirk
 *
 */
public class ResourceParser {

    private static final String OPT_DB = "db";
    private static final String OPT_LOG = "log";
    private static GetOpts opts;

    public static void main(String[] args) throws IOException, JSONException {
        init (args);
        String outputFileName = opts.getStr(OPT_DB);
        OutputStream out = System.out;
        if (outputFileName != null) {
            out = new FileOutputStream (new File (outputFileName));
        }
        String inputFileName = opts.getStr(OPT_LOG);
        InputStream in = new FileInputStream (new File (inputFileName));
        ResourceParser p = new ResourceParser (in, out);
        p.parse();
    }

    private static void init(String[] args) {
        opts = new GetOpts(ResourceParser.class);
        OptionLegend legend = new OptionLegend (OPT_LOG);
        legend.setRequired(true);
        legend.setDescription("Log file to parse containing SCIM2Connector debug log events");
        opts.addLegend(legend);

        legend = new OptionLegend (OPT_DB);
        legend.setRequired(false);
        legend.setDescription("JSON Server DB file");
        opts.addLegend(legend);

        opts.parseOpts(args);
    }

    /*
     * Inner class to encapsulate comparing and accessing
     */
    private static class Resource implements Comparable<Resource> {
        private JSONObject object;
        
        Resource (JSONObject object) {
            if (object == null) {
                try {
                    object = new JSONObject ("{}");
                } catch (JSONException e) {
                    // ignore; won't happen
                }
            }
            this.object = object;
        }

        public List<String> getSchemas() {
            List<String> schemas = new ArrayList<String>();
            try {
                JSONArray schemaArry = object.getJSONArray("schemas");
                for (int i = 0; i < schemaArry.length(); i++) {
                    String schema = schemaArry.getString(i);
                    schemas.add(schema);
                }
            } catch (JSONException e) {
                // no schemas key; That could happen
                return null;
            }
            return schemas;
        }

        public String getId() {
            try {
                String id = object.getString("id");
                return id;
            } catch (JSONException e) {
                // No id; that could happen
                return null;
            }
        }

        @Override
        public int hashCode() {
            int hashCode;
            try {
                hashCode = stripMeta().toString().hashCode();
            } catch (JSONException e) {
                // At this point, we shouldn't throw a JSONException
                throw new RuntimeException (e);
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Resource)) {
                return false;
            }
            try {
            JSONObject myStripped = stripMeta();
            JSONObject yourStripped = ((Resource) obj).stripMeta();
            boolean equals = myStripped.toString().equals(yourStripped.toString());
            return equals;
            } catch (JSONException e) {
                // something went wrong. Chances are better it was the target object so just return neq
                System.err.println(e.getMessage());
                return false;
            }
        }

        // For comparison activities, strip out the date stamps in the meta key.
        private JSONObject stripMeta () throws JSONException {
            JSONObject stripped = new JSONObject (object.toString());
            JSONObject meta = stripped.optJSONObject("meta");
            if (meta != null) {
                meta.remove("lastModified");
                meta.remove("created");
            }
            return stripped;
        }

        @Override
        public int compareTo(Resource yours) {
            // first compare schemas
            // If i have more things than you, return positive
            // If i have less things than you, return negative
            List<String> mySchemas = getSchemas();
            List<String> yourSchemas = yours.getSchemas();
            if (mySchemas == null && yourSchemas != null) {
                return -1;
            }
            if (mySchemas != null && yourSchemas == null) {
                return 1;
            }
            if (mySchemas == null && yourSchemas == null) {
                // Schema objects themselves may have null schemas
                // Just do a straight up string comparison
                return object.toString().compareTo(yours.getObject().toString());
            }

            yourSchemas.removeAll(mySchemas);
            int inYours = yourSchemas.size();
            if (inYours != 0) {
                return -1 * inYours;
            }
            yourSchemas = yours.getSchemas();
            mySchemas.removeAll(yourSchemas);
            int inMine = mySchemas.size();
            if (inMine != 0) {
                return inMine;
            }
            // Our schemas match; Compare IDs
            String myId = getId();
            String yourId = yours.getId();
            if (myId == null && yourId != null) {
                return -1;
            }
            if (myId != null && yourId == null) {
                return 1;
            }

            if (myId == null && yourId == null) {
                // Some objects may not have an ID
                // Just do a straight up string comparison
                return object.toString().compareTo(yours.getObject().toString());
            }

            return myId.compareTo(yourId);
        }

        public JSONObject getObject() {
            return object;
        }

    }

    private InputStream input;
    private OutputStream output;
    private Set<Resource> resources;

    public ResourceParser(InputStream is, OutputStream os) {
        this.input = is;
        this.output = os;
        //resources = new TreeSet<Resource>();
        resources = new HashSet<Resource>();
    }

    public void parse() throws IOException, JSONException {
        BufferedReader reader = new BufferedReader (new InputStreamReader(input));
        String line = reader.readLine();
        Pattern responseReturnedPattern = Pattern.compile("^.* openconnector\\.connector\\.scim2\\.SCIM2Connector:\\d* - Response returned: (\\{.*\\})\\s*$");
        Pattern lambdaReturnsPattern = Pattern.compile("^.* openconnector\\.connector\\.scim2\\.SCIM2Connector:\\d* .* => (\\{.*\\})\\s*$");
        while (line != null) {
            Matcher m = responseReturnedPattern.matcher(line);
            if (m.matches()) {
                // found a matching log event. Add capture group 1 to our list of resources
                addResource (m.group(1));
            } else {
                m = lambdaReturnsPattern.matcher(line);
                if (m.matches()) {
                    addResource (m.group(1));
                }
            }
            line = reader.readLine();
        }
        // Write out our resources as a single JSON array
        writeJSONArray();
    }

    private void writeJSONArray () throws JSONException, IOException {
        JSONObject objOut = new JSONObject();
        JSONArray resourceArray = new JSONArray();
        for (Resource resource : resources) {
            resourceArray.put(resource.getObject());
        }
        objOut.put("Resources", resourceArray);
        Writer writer = new OutputStreamWriter(output);
        objOut.write(writer);
        writer.close();
    }

    private void addResource (String jsonString) throws JSONException {
        // This might be a Resource object but is more likely a ListResult of Resource objects. Compile the json object to some minimal extent (maps and lists)
        // and use that object to extract the list of Resources
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject (jsonString);
        } catch (JSONException e) {
            // Some of the logging returns a JSON object that is invalid; values are not quoted
            // Ignore these
            System.err.println("Ignoring json: " + jsonString);
            return;
        }
        JSONArray resourceArry = jsonObj.optJSONArray("Resources");
        if (resourceArry != null) {
            for (int i = 0; i < resourceArry.length(); i++) {
                try {
                    JSONObject resourceObj = resourceArry.getJSONObject(i);
                    Resource resource = new Resource(resourceObj);
                    resources.add(resource);
                } catch (JSONException innerE) {
                    // Resource array didn't have a Resource JSONObject; unexpected but ignorable
                    //log.error ("We should probably use a log4j logger here");
                    System.err.println("Object not a Resource; ignoring.");
                }
            }
        } else {
            // no Resource array found. Check for a 'schemas' key and add it as a Resource if it has one
            Object schemas = jsonObj.opt("schemas");
            if (schemas != null) {
                Resource resource = new Resource (jsonObj);
                resources.add (resource);
            } else {
                System.err.println ("Ignoring json: " + jsonString);
            }
        }
    }
}
