package biliruben.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.biliruben.util.GetOpts;
import com.biliruben.util.OptionLegend;
import com.biliruben.util.OptionParseException;
import com.biliruben.util.csv.CSVIllegalOperationException;
import com.biliruben.util.csv.CSVRecord;
import com.biliruben.util.csv.CSVSource;
import com.biliruben.util.csv.CSVSource.CSVType;
import com.biliruben.util.csv.CSVSourceImpl;
import com.biliruben.util.csv.CSVUtil;

public class CSVPrint {

    private static final String DEFAULT_OUTPUT_STDOUT = "STDOUT";
    private static final String OPT_CSV_FILE = "file";
    private static final String OPT_COLUMN = "col";
    private static final String OPT_OUTPUT_DELIM = "delim";
    private static final String OPT_OUTPUT_FILE = "outFile";
    private static final String OPT_NO_HEADER = "noHeader";
    private static final String OPT_MERGE_FROM_COLUMN = "mergeCol";
    private static final String OPT_RANDOMIZE = "randomize";
    private static GetOpts _opts;

    /**
     * Given a CSV file and columns, print just the columns
     * @throws IOException 
     * @throws CSVIllegalOperationException 
     */
    public static void main(String[] args) throws IOException, CSVIllegalOperationException {
        init(args);
        
        List<String> columns = _opts.getList(OPT_COLUMN);
        String mergeColumn = _opts.getStr(OPT_MERGE_FROM_COLUMN);
        List<String> inputFiles = _opts.getList(OPT_CSV_FILE);
        String outputFile = _opts.getStr(OPT_OUTPUT_FILE);
        String delim = _opts.getStr(OPT_OUTPUT_DELIM);
        Boolean noHeader = Boolean.valueOf(_opts.getStr(OPT_NO_HEADER));
        Boolean randomize = Boolean.valueOf(_opts.getStr(OPT_RANDOMIZE));
        
        CSVSource csv = null;
        if (inputFiles.size() > 1) {
            CSVRecord csvRecord = mergeCsv(inputFiles, columns, delim, mergeColumn, randomize);
            CSVUtil.exportToCsv(csvRecord, new FileOutputStream(new File(outputFile)));
            return;
        } else {
            CSVType csvType = noHeader ? CSVType.WithOutHeader : CSVType.WithHeader;
            csv = new CSVSourceImpl(new File(inputFiles.get(0)), csvType);
            
        }
        // Read CSV

        String[] line = csv.getNextLine();
        Writer writer = getWriter(outputFile);
        while (line != null) {
            if (noHeader) {
                print(line, writer, columns, delim);
            } else {
                print(csv.getCurrentLineAsMap(), writer, columns, delim);
            }
            line = csv.getNextLine();
        }
        writer.flush();
        writer.close();
    }
    
    private static CSVRecord mergeCsv(List<String> inputFiles, List<String> columns, String delim, 
            String mergeColumn, boolean randomize) throws FileNotFoundException {
        Map<String, Map<String, String>> dataMap = new HashMap<String, Map<String, String>>();
        for (String file : inputFiles) {
            CSVSourceImpl csv = new CSVSourceImpl(new File(file), CSVType.WithHeader);
            for (Map<String, String> line : csv) {
                String index = line.get(mergeColumn);
                Map<String, String> existingData = dataMap.get(index);
                if (existingData == null) {
                    existingData = new HashMap<String, String>(line);
                    dataMap.put(index, existingData);
                } else {
                    existingData.putAll(line);
                }
            }
        }

        CSVRecord record = new CSVRecord();
        for (Map<String, String> data : dataMap.values()) {
            record.addLine(data);
        }
        return record;
    }
    

    private static void shuffleMap(Map<String, Map<String, String>> dataMap) {
        List<String> keyList = new ArrayList<String>(dataMap.keySet());
        Collections.shuffle(keyList);
    }

    private static void print(Map<String, String> currentLineAsMap,
            Writer writer, List<String> columns, String delim) throws IOException {
        StringBuffer buff = new StringBuffer();
        for (String column : columns) {
            String item = currentLineAsMap.get(column);
            if (item.contains(delim)) {
                buff.append("\"").append(item).append("\"");
            } else {
                buff.append(item);
            }
            buff.append(delim);
        }
        buff.delete(buff.length() - 1, buff.length());
        buff.append("\n");
        writer.write(buff.toString());
    }


    private static void print(String[] line, Writer outputFile,
            List<String> columns, String delim) throws IOException {
        StringBuffer buff = new StringBuffer();
        for (String column : columns) {
            int el = Integer.valueOf(column);
            String item = line[el];
            if (item.contains(delim)) {
                buff.append("\"").append(item).append("\"");
            } else {
                buff.append(item);
            }
            buff.append(delim);
        }
        buff.delete(buff.length() - 1, buff.length());
        buff.append("\n");
        outputFile.write(buff.toString());
    }
    
    private static Writer getWriter(String outputFile) throws IOException {
        Writer writer = null;
        if (outputFile.equals(DEFAULT_OUTPUT_STDOUT)) {
            writer = new BufferedWriter(new OutputStreamWriter(System.out));
        } else {
            writer = new BufferedWriter(new FileWriter(outputFile));
        }
        return writer;
    }

    private static void init(String[] args) {
        _opts = new GetOpts(CSVPrint.class);
        
        OptionLegend legend = new OptionLegend(OPT_CSV_FILE, "CSV file to print from. Multiple files will require a mergeFrom column");
        legend.setRequired(true);
        legend.setMulti(true);
        _opts.addLegend(legend);
        
        legend = new OptionLegend(OPT_MERGE_FROM_COLUMN, "Column value used to merge multiple CSV sources");
        legend.setRequired(false);
        _opts.addLegend(legend);
        
        legend = new OptionLegend(OPT_COLUMN, "Column to print");
        legend.setRequired(true);
        legend.setMulti(true);
        _opts.addLegend(legend);
        
        legend = new OptionLegend(OPT_OUTPUT_DELIM, "Output delimiter");
        legend.setRequired(false);
        legend.setDefaultValue(",");
        _opts.addLegend(legend);
        
        legend = new OptionLegend(OPT_OUTPUT_FILE, "Output file");
        legend.setRequired(false);
        legend.setDefaultValue(DEFAULT_OUTPUT_STDOUT);
        _opts.addLegend(legend);
        
        legend = new OptionLegend(OPT_NO_HEADER, "No headers. Output columns must be specified as integers");
        legend.setRequired(false);
        legend.setDefaultValue("false");
        legend.setFlag(true);
        _opts.addLegend(legend);
        
        legend = new OptionLegend(OPT_RANDOMIZE, "Randomizes the output.");
        legend.setFlag(true);
        legend.setRequired(false);
        legend.setDefaultValue("false");
        //_opts.addLegend(legend);

        _opts.parseOpts(args);
        
        if (_opts.getList(OPT_CSV_FILE).size() > 1 && _opts.getStr(OPT_MERGE_FROM_COLUMN) == null) {
            throw new OptionParseException("Cannot supply multiple CSV files without a merge column", _opts, true);
        }
    }

}
