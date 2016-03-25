package biliruben.csv.api;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.biliruben.util.csv.CSVRecord;

/**
 * CSV data generator.  Uses {@link DataColumn} objects to define what data is generated 
 * in the CSV. The entire framework relies on the notion of data vs object. A line of data
 * is a single line of tokens in a CSV. An object may be represented by one or more lines
 * of data.
 * @author trey.kirk
 *
 */
public class CsvObjectGenerator {

    /*
     * Output writer
     */
    private Writer _writer;

    /*
     * Column definitions
     */
    private List<DataColumn> _columns;

    /*
     * CSV Utility class for no muss, no fuss output
     */
    private CSVRecord _csvRecord;

    /*
     * Number of unique objects to generate.  A unique object in a CSV
     * may span multiple lines.  Therefore this is not a line limiter.
     */
    private int _objects;

    /*
     * Number of multiple values to use per object
     */
    private int _multiMax;
    
    /*
     * Minimum number of multiple values to use per object
     */
    private int _multiMin;

    private String[] _fields;

    /**
     * Constructor
     * @param csvWriter where the CSV output is written to
     * @param objects number of objects to represent in the CSV output
     * @param multiMax number of multi-valued attributes to use (maximum) per object
     */
    public CsvObjectGenerator(Writer csvWriter, int objects, int multiMin, int multiMax) {
        _writer = csvWriter;
        _objects = objects;
        _multiMax = multiMax > 0 ? multiMax : 1;
        _multiMin = multiMin > 0 ? multiMin : 1;
    }

    public void setColumns(List<DataColumn> columns) {
        _columns = columns;
    }

    public void addColumn(DataColumn column) {
        if (_columns == null) {
            _columns = new ArrayList<DataColumn>();
        }
        _columns.add(column);
    }

    /**
     * Generates CSV data to the target writer.  Subsequent calls to this method
     * will result in additional data written to the target writer, which may
     * result in unexpected output.
     * @throws IOException
     */
    public void write() throws IOException {
        if (_columns == null || _columns.size() == 0) {
            throw new NullPointerException("Data columns not defined!");
        }
        buildCSVRecord();
        for (String line : _csvRecord) {
            _writer.write(line);
        }
        _writer.flush();
    }

    private String[] getRecordFields() {
        if (_fields == null) {
            _fields = new String[_columns.size()];

            for (int i = 0; i < _fields.length; i++) {
                _fields[i] = _columns.get(i).getColumnName();
            }

        }

        return _fields;
    }

    private void buildCSVRecord() {
        _csvRecord = new CSVRecord(getRecordFields());
        Random rando = new Random();
        // To determine the number of multi-values to generate, we have to get a random
        // number from 0 to delta (min/max) and add min
        int deltaMulti = _multiMax - _multiMin + 1; // All deltas must have a difference of +1 the actual difference
        for (int object = 0; object < _objects; object++) {
            int actualMulti = rando.nextInt(deltaMulti) + _multiMin;
            for (int multi = 0; multi < actualMulti; multi++) {
                Map<String, String> dataLine = new HashMap<String, String>();
                for (DataColumn column : _columns) {
                    for (String fieldName : _fields) {
                        if (fieldName.equals(column.getColumnName())) {
                            // only put fields of data in our csv
                            dataLine.put(column.getColumnName(), column.nextValue(this));
                        }
                    }
                }
                _csvRecord.addLine(dataLine);
            }
            // reset the data columns
            for (DataColumn column : _columns) {
                column.reset();
            }
        }
    }

    /**
     * Retrieves a DataColumn by name.  This is needed when we're building a
     * generated DataColumn that relies on other columns and we don't have any
     * other reference to those columns
     * @param name
     * @return
     */
    public DataColumn getDataColumn(String name) {
        for (DataColumn c : _columns) {
            if (name.equalsIgnoreCase(c.getColumnName())) {
                return c;
            }
        }
        return (DataColumn)null;
    }

    public void setFields(String[] fields) {
        _fields = fields;
    }
    
    public int getObjects() {
        return _objects;
    }
}
