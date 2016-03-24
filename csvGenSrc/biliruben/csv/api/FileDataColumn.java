package biliruben.csv.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FileDataColumn extends RandomDataColumn {

    private static final String ARG_FILE = "file";
    private String _srcFile;
    private boolean _random;
    
    /*
     * Sources values from a given file.  Each value is isolated on its own
     * line.  Values are read in fully and then picked from randomly.
     */
    private static class SourceFileValueIterator extends RandomValueIterator {

        private List<String> _values;
        private boolean _isRandom;
        private Iterator<String> _serialIterator;

        public SourceFileValueIterator(FileDataColumn dc) {
            super(dc);
            try {
                readFile(dc.getSrcFile());
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
            _isRandom = dc.isRandom();
        }

        private void readFile(String srcFile) throws IOException {
            _values = new ArrayList<String>();
            File f = new File(srcFile);
            FileReader r = new FileReader(f);
            BufferedReader br = new BufferedReader(r);
            String line = null;
            do {
                line = null;
                if (br.ready()) {
                    line = br.readLine();
                    _values.add(line);
                }
            } while (line != null);
            _serialIterator = _values.iterator();
        }

        @Override
        public String next() {
            String word = null;
            if (_isRandom) {
                String lastValue = getLastValue();
                if (lastValue != null && !isMulti()) {
                    return lastValue;
                }
                boolean unique = false;
                int count = 0;

                do {
                    int pos = getRandom().nextInt(_values.size());
                    word = _values.get(pos);
                    unique = incrementNext(word);
                    count++;
                } while (isUnique() && !unique && count < 10);
            } else {
                // serially iterate the file
                if (!_serialIterator.hasNext()) {
                    // reset the iterator
                    _serialIterator = _values.iterator();
                }
                word = _serialIterator.next();
            }
            return word;
        }
    }

    public FileDataColumn(String columnName, ColumnType type) {
        super(columnName);
        switch (type) {
        case file:
            this._random = true; break;
        case serialFile:
            this._random = false; break;
        default:
            throw new IllegalArgumentException(type + " is not a valid file type");
        }
    }

    @Override
    public void apply(Map<String, Object> detailMap) {
        super.apply(detailMap);
        setSrcFile((String) detailMap.get(ARG_FILE));
    }
    
    public String getSrcFile() {
        return _srcFile;
    }
    public void setSrcFile(String srcFile) {
        this._srcFile = srcFile;
    }
    
    public boolean isRandom() {
        return this._random;
    }

    @Override
    public ValueIterator getIterator() {
        return new SourceFileValueIterator(this);
    }

}
