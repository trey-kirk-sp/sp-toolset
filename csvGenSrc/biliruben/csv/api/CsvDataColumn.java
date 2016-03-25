package biliruben.csv.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.biliruben.util.csv.CSVSource.CSVType;
import com.biliruben.util.csv.CSVSourceImpl;

/**
 * Iterates through a CSV file and returns a value derived from a CSV column. Only one column
 * is read through and returned. The following information is required:
 * file: The CSV file to be read
 * hasHeaders: True/false indicating headers are present in the source file
 * sourceColumn: Column to read values from. May either be the header name (if headers are
 *               provided) or position index
 * multi: Indicates values for a CSV object spans multiple lines. When specified, an index is required
 * indexColumn: When multi is enabled, you must provide a column provided in the full CSV data line
 *              that unites each line to form an object
 * delim: The character delimiter to use (default is comma)
 * 
 * Values in a csv object are read and reported serially, not randomly. Thus the unique flag is ignored
 * @author trey.kirk
 *
 */
public class CsvDataColumn extends RandomDataColumn {
    
    private static final char DEFAULT_DELIMITER = ',';
    private String ARG_FILE = "file";
    private String ARG_HAS_HEADERS = "hasHeaders";
    private String ARG_SOURCE_COL = "sourceCol";
    private String ARG_INDEX_COL = "indexCol";
    private String ARG_DELIM = "delim";
    private String ARG_RANDOMIZE = "randomize";
    private String _file;
    private boolean _hasHeaders;
    private String _sourceCol;
    private String _indexCol;
    private char _delimiter;
    private boolean _randomize;
    
    private class CsvDataValueIterator extends RandomValueIterator {

        private int _sourceColIdx;
        private CSVSourceImpl _csvSrc;
        private int _csvIndexIdx;
        List<String> _currentValues;
        private int _currentIdx;

        public CsvDataValueIterator(CsvDataColumn dc) throws IOException {
            super(dc);
            getCsvSource(((CsvDataColumn)getDataColumn()).getSourceCol(), ((CsvDataColumn)getDataColumn()).getIndexCol());
            getNextValues();
        }
        
        private void getCsvSource(String srcCol, String indexCol) throws IOException {
            String csvFile = ((CsvDataColumn)getDataColumn()).getFile();
            char csvDelim = ((CsvDataColumn)getDataColumn()).getDelimiter();
            boolean hasHeaders = ((CsvDataColumn)getDataColumn()).hasHeaders();
            CSVType type = null;
            File file = new File(csvFile);
            if (hasHeaders) {
                type = CSVType.WithHeader;
            } else {
                type = CSVType.WithOutHeader;
            }
            _csvSrc = new CSVSourceImpl(file, type, csvDelim);
            if (type == CSVType.WithHeader) {
                String[] fields = _csvSrc.getFields();
                for (int i = 0; i < fields.length; i++) {
                    if (isMulti() && fields[i].equals(indexCol)) {
                        _csvIndexIdx = i;
                    }
                    if (fields[i].equals(srcCol)) {
                        _sourceColIdx = i;
                    }
                }
            } else {
                if (isMulti()) {
                    _csvIndexIdx = Integer.valueOf(indexCol);
                }
                _sourceColIdx = Integer.valueOf(srcCol);
            }
            // before we do anything, make sure we increment the csv pointer
            _csvSrc.getNextLine();
        }
        
        /*
         * Builds a new list of possible values by reading the next set
         * of lines until a new index value is reached
         */
        private void getNextValues() throws IOException {
            _currentValues = new ArrayList<String>();
            _currentIdx = 0;
            boolean readNext = true;
            
            while (readNext) {
                String[] currentLine = _csvSrc.getCurrentLine();
                if (currentLine == null) {
                    // null current line means we've reached the end of the
                    // csv file. We need to loop around
                    getCsvSource(((CsvDataColumn)getDataColumn()).getSourceCol(), ((CsvDataColumn)getDataColumn()).getIndexCol());
                    currentLine = _csvSrc.getCurrentLine();
                    if (currentLine == null) {
                        throw new IOException("No CSV data to read");
                    }
                }

                _currentValues.add(currentLine[_sourceColIdx]);
                String[] nextLine = _csvSrc.getNextLine();

                if (!isMulti()) {
                    readNext = false;
                } else {
                    String currentIndex = currentLine[_csvIndexIdx];
                    if (nextLine == null || !currentIndex.equals(nextLine[_csvIndexIdx])) {
                        readNext = false;
                    }
                }
            }
        }
        
        @Override
        public String next() {
            String nextValue = getLastValue();
            boolean isRandom = ((CsvDataColumn)getDataColumn()).isRandom();

            if (!isMulti() && nextValue != null) {
                return nextValue;
            }
            // If we're here, it's either a multi-valued data column or we don't have
            // a previous value, so get a new one
            //
            // We might have an empty list of values, so check that first
            if (_currentValues.isEmpty()) {
                // Ordering may be a challenge, but we've made the loop by now
                // so fuck your ordering
                _currentValues = new ArrayList<String>(getLastValues());
            }
            
            // if it's random, send a random value. Otherwise send the next value
            if (isRandom) {
                _currentIdx = getRandom().nextInt(_currentValues.size());
            } else if (_currentIdx >= _currentValues.size()) {
                _currentIdx = 0;
            }

            nextValue = _currentValues.get(_currentIdx);

            if (isUnique()) {
                // Ensures unique values by removing this one from the current values list
                _currentValues.remove(_currentIdx);
            } else {
                // Only when we're not purging from the list do we want to increment the currentIdx
                _currentIdx++;
            }

            incrementNext(nextValue);

            return nextValue;
        }
        
        @Override
        public void reset() {
            super.reset();
            try {
                getNextValues();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public CsvDataColumn(String columnName) {
        super(columnName);
        this._hasHeaders = false;
        this._delimiter = DEFAULT_DELIMITER;
    }
    
    @Override
    public void apply(Map<String, Object> detailMap) {
        super.apply(detailMap);
        String file = (String)detailMap.get(ARG_FILE);
        if (file == null || "".equals(file.trim())) {
            throw new IllegalArgumentException(ARG_FILE + " property must be specified for column " + getColumnName());
        }
        this._file = file;
        
        String hasHeaders = (String)detailMap.get(ARG_HAS_HEADERS);
        if (hasHeaders != null && !"".equals(hasHeaders.trim())) {
            this._hasHeaders = Boolean.valueOf(hasHeaders);
        }
        
        String sourceCol = (String)detailMap.get(ARG_SOURCE_COL);
        if (sourceCol == null || "".equals(sourceCol.trim())) {
            throw new IllegalArgumentException(ARG_SOURCE_COL + " property must be specified for column " + getColumnName());
        }
        _sourceCol = sourceCol;
        
        if (isMulti()) {
            String indexCol = (String)detailMap.get(ARG_INDEX_COL);
            if (indexCol == null || "".equals(indexCol.trim())) {
                throw new IllegalArgumentException(getColumnName() + " is specified as a multi column but no " + ARG_INDEX_COL + " property was specified");
            }
            _indexCol = indexCol;
        }
        
        String delimiter = (String)detailMap.get(ARG_DELIM);
        if (delimiter != null && !"".equals(delimiter.trim())) {
            _delimiter = delimiter.charAt(0);
        }
        
        String randomize = (String)detailMap.get(ARG_RANDOMIZE);
        if (randomize != null && !"".equals(randomize.trim())) {
            _randomize = Boolean.valueOf(randomize);
        }
    }
    
    public char getDelimiter() {
        return _delimiter;
    }
    
    public boolean hasHeaders() {
        return _hasHeaders;
    }
    
    public String getFile() {
        return _file;
    }
    
    public String getSourceCol() {
        return _sourceCol;
    }
    
    public String getIndexCol() {
        return _indexCol;
    }
    
    public boolean isRandom() {
        return _randomize;
    }
    

    @Override
    public ValueIterator getIterator() {
        try {
            return new CsvDataValueIterator(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
