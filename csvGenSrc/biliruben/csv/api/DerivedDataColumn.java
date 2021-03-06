package biliruben.csv.api;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

/**
 * This DataColumn derives its value from other columns and a format pattern. The format pattern complies with
 * {@link Formatter}. Specify the source columns as a csv string with the 'columns' property. Some examples:
 * 
 * #middle initial field
 * column.middleInitField.type=derived
 * column.middleInitField.columns=middleInit
 * column.middleInitField.format=%1$s.
 *
 * Takes the source column 'middleInit' and appends a '.' after the column's value
 * 
 * #acctIdBase
 * column.acctIdBase.type=derived
 * column.acctIdBase.columns=firstName,lastName
 * column.acctIdBase.format=%1.1S%1.3S
 * 
 * #empIdIncr
 * column.empIdIncr.type=incrementer
 * column.empIdIncr.start=1
 * column.empIdIncr.padding=3
 * 
 * #acctId
 * column.acctId.type=derived
 * column.acctId.columns=acctIdBase,empId
 * column.acctId.format=%1$s%2$s
 * 
 * Uses one derived column 'acctIdBase' to concatonate the first initial of the firstname and the
 * first three letters of the lastname. Then uses that derived column in conjunction with an incrementer
 * to form the final product to represent an alpha-numeric id, ex: SBAG005
 * 
 * @author trey.kirk
 *
 */
public class DerivedDataColumn extends DataColumn {

    private static final String ARG_FORMAT = "format";
    private static final String ARG_COLUMNS = "columns";

    private List<String> _srcColumns;
    private String _prettyFormat;
    private CsvObjectGenerator _generator;

    private static class DerivedValueIterator extends ValueIterator {

        private String _prettyFormat;
        private List<String> _srcColumns;
        private CsvObjectGenerator _generator;

        public DerivedValueIterator(DerivedDataColumn dc) {
            super(dc);
            this._prettyFormat = dc.getPrettyFormat();
            this._srcColumns = dc.getSourceDataColumns();
            this._generator = dc.getGenerator();
        }

        @Override
        public String next() {

            String lastValue = getLastValue();
            if (lastValue != null && !isMulti()) {
                return lastValue;
            }

            // deriving a value from other columns
            String[] tokens = new String[_srcColumns.size()];
            int i = 0;

            for (String name : _srcColumns) {
                DataColumn dc = this._generator.getDataColumn(name);
                tokens[i] = dc.nextValue(this._generator);
                i++;
            }
            StringBuilder buff = new StringBuilder();
            Formatter f = new Formatter(buff);
            f.format(this._prettyFormat, tokens);
            f.flush();
            // Being derived and being asked to return unique values is tricky, so
            // we ignore any unique checking
            String formatted = buff.toString();
            incrementNext(formatted);
            return formatted;
        }
    }

    public DerivedDataColumn(String columnName, CsvObjectGenerator generator) {
        super(columnName);
        this._generator = generator;
    }

    @Override
    public void apply(Map<String, Object> detailMap) {
        super.apply(detailMap);
        if (isUnique()) {
            throw new IllegalArgumentException(getClass().getCanonicalName() + " doesn't support unique values");
        }
        String oColumns = (String) detailMap.get(ARG_COLUMNS);
        if (oColumns != null) {
            String[] columns = oColumns.split(",");
            for (int i = 0; i < columns.length; i++) {
                String name = columns[i].trim();
                addSourceDataColumn(name);
            }
        }

        String prettyFormat = (String) detailMap.get(ARG_FORMAT);
        if (prettyFormat != null) {
            setPrettyFormat(prettyFormat);
        }
    }
    
    public void setPrettyFormat(String format) {
        this._prettyFormat = format;
    }
    
    public String getPrettyFormat() {
        return this._prettyFormat;
    }

    public void addSourceDataColumn(String dataColumn) {
        if (_srcColumns == null) {
            _srcColumns = new ArrayList<String>();
        }
        _srcColumns.add(dataColumn);
    }

    public void setSourceDataColumns(List<String> dataColumns) {
        _srcColumns = new ArrayList<String>(dataColumns);
    }
    
    public List<String> getSourceDataColumns() {
        return _srcColumns;
    }
    
    public CsvObjectGenerator getGenerator() {
        return this._generator;
    }

    @Override
    public ValueIterator getIterator() {
        return new DerivedValueIterator(this);
    }

}
