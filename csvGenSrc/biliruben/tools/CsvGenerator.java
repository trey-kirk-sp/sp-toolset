package biliruben.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

import biliruben.csv.api.ConstantValueDataColumn;
import biliruben.csv.api.CsvObjectGenerator;
import biliruben.csv.api.DataColumn;
import biliruben.csv.api.DerivedDataColumn;
import biliruben.csv.api.FileDataColumn;
import biliruben.csv.api.GeneratedDataColumn;
import biliruben.csv.api.HierarchyDataColumn;
import biliruben.csv.api.IncrementerDataColumn;
import biliruben.csv.api.RandomDataColumn;

import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;
import com.biliruben.util.OptionParseException;
import com.biliruben.util.Printer;

/**
 * Launcher class.  Takes the following parameters:
 * - csv file:  What file to create
 * - properties: What properties file to read from
 * 
 * The properties file will drive most of the behavior:

 * - column.x: defines the name of the column
 * - column.x.type: the column type
 * - column.x.source: defines the source of the data type.  Options are 'generated' and 'file'
 * - column.x.source.generated.characters: character class for generation
 * - column.x.source.generated.minLength: Minimum data length
 * - column.x.source.generated.maxLength: Maximum data length
 * - column.x.source.file: source filename
 * - column.x.multi: boolean true/false.  Multi valued data types will have unique values for each line.
 *                  Single value data types will repeat across lines.
 * - column.x.generated.charClass: the character class for generated columns
 * - column.x.generated.minLn: minimum character length
 * - column.x.generated.maxLn: maximum character length
 *                  
 * - objects: total number of objects to create.  One object may span multiple lines
 * @author trey.kirk
 *
 */
public class CsvGenerator {

    private static final String OPT_CSV_FILE = "csvFile";
    private static final String OPT_PROPERTY_FILE = OptionLegend.OPT_PROPERTY_FILE;
    private static GetOpts _opts;
    private static CsvObjectGenerator _generator;

    /**
     * The Welcom mat
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        initialize(args);

        readProperties(_opts.getStr(OptionLegend.OPT_PROPERTY_FILE));
        if (_generator == null) {
            // because we didn't read the properties file
            throw new OptionParseException("Nonexistent properties file", _opts);
        }
        generateCsv();
    }

    private static void generateCsv() throws IOException {
        _generator.write();
    }

    private static void readProperties(String propertyFile) throws IOException {
        Map<String, Object> propertyMap = _opts.getProperties();
        if (propertyMap == null) {
            return;
        }

        // build CsvObjGenerator
        String csvFile = _opts.getStr(OPT_CSV_FILE);
        Writer writer = null;
        if (csvFile != null) {
            File fCsvFile = new File(csvFile);
            writer = new FileWriter(fCsvFile);
        } else {
            writer = new PrintWriter(System.out);
        }
        
        String sObjects = (String)propertyMap.get("objects");
        int objects = Integer.valueOf(sObjects);

        String sMultiMax = (String)propertyMap.get("multiMax");
        int multiMax = 1;
        if (sMultiMax != null && !"".equals(sMultiMax.trim())) {
            multiMax = Integer.valueOf(sMultiMax);
        }
        
        _generator = new CsvObjectGenerator(writer, objects, multiMax);
        
        String fields = (String)propertyMap.get("fields");
        if (fields != null) {
            String[] columnsArry = fields.split(",");
            _generator.setFields(columnsArry);
        }

        Map<String, Object> columnMap = (Map<String, Object>) propertyMap.get("column");
        for (String columnName : columnMap.keySet()){
            Map detailMap = (Map) columnMap.get(columnName);
            String sType = (String) detailMap.get("type");
            DataColumn.ColumnType type = DataColumn.ColumnType.valueOf(sType);
            DataColumn dc = null;
            // TODO: refactor all of this into an abstract 'apply(Map)' method
            // that each DataColumn must implement
            switch (type) {
            case file:
            case serialFile:
                dc = new FileDataColumn(columnName, type);
                break;
            case generated:
                dc = new GeneratedDataColumn(columnName);
                break;
            case derived:
                dc = new DerivedDataColumn(columnName, _generator);
                break;
            case incrementer:
                dc = new IncrementerDataColumn(columnName);
                break;
            case constant:
                dc = new ConstantValueDataColumn(columnName);
                break;
            case hierarchy:
                dc = new HierarchyDataColumn(columnName, _generator);
                break;
            }
            dc.apply(detailMap);
            _generator.addColumn(dc);
        }

    }

    private static void initialize(String[] args) {
        _opts = new GetOpts(CsvGenerator.class);

        OptionLegend legend = new OptionLegend(OPT_PROPERTY_FILE);
        legend.setRequired(true);
        legend.setDescription("Property file with column definitions");
        _opts.addLegend(legend);

        legend = new OptionLegend(OPT_CSV_FILE);
        legend.setRequired(false);
        legend.setDescription("Target CSV file.  If no file is specified, CSV data is sent to STDOUT");
        _opts.addLegend(legend);

        _opts.parseOpts(args);
    }

}
