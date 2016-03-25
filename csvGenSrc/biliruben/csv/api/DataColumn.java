package biliruben.csv.api;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Basic class that defines the content and behavior of one column in a data set.  Specifically,
 * we intend this to be for CSV.  However, it doesn't depend on any output format. Data column
 * iteration is not intuitive, but you'll get used to it. The important distinction is 'value' vs
 * 'object'. A value is a discrete piece of data to be incorporated into an object. An object may
 * be comprised of multiple pieces of data from this DataColumn:
 * - On 'nextValue': returns the next value with the provided stipulation:
 *      - If the DataColumn is a 'multi' column, another possible value of the defined set is provided
 *      - If the DataColumn is a 'multi' and 'unique' column, another unique possible value of the defined set is
 *        provided. If a unique value cannot be found, a previous value is instead provided
 *      - If the DataColumn is not a 'multi' column, the nextValue is alwasy the same value
 * - On 'reset': the underlying data value iterator increments to a new (or possibly previous) value. For
 *      'multi' data columns, the previously used values list is cleared thus allowing for previously used
 *      uniques
 *      
 * For example, given the following two objects:
 * acctId,count,title,firstName,lastName,group
 * LJAS001,000001,Archduke,Leona,Jastrzebski,DBAs
 * LJAS001,000001,Archduke,Leona,Jastrzebski,CompCommittee
 * LJAS001,000001,Archduke,Leona,Jastrzebski,FinancialAnalyist
 * LJAS001,000001,Archduke,Leona,Jastrzebski,ServerRoomToken
 * CKAD002,000002,Assistant to the President & Deputy National Security Advisor,Charita,Kady,FinancialAnalyist
 * CKAD002,000002,Assistant to the President & Deputy National Security Advisor,Charita,Kady,PayrollAnalysis
 * CKAD002,000002,Assistant to the President & Deputy National Security Advisor,Charita,Kady,SecCompliance
 * 
 * The DataColumn 'group' is a multi column and returns multiple (and unique) data for each data line for both objects.
 * However, it's not garuanteed that unqiue values in the first object are also unique when compared to the second object,
 * 'FinancialAnalyst' being the case in point.
 * DataColumns 'acctId', 'count', 'title', 'firstName', & 'lastName' were non-multi DataColumns. For each object, they
 * returned data pertinent to the object, but duplicated values for each unique data line of those object.
 * 
 * @author trey.kirk
 *
 */
public abstract class DataColumn {

    private static final String ARG_UNIQUE = "unique";
    private static final String ARG_MULTI = "multi";

    /*
     * Base Iterator class.  Handles duplication.
     */
    protected abstract static class ValueIterator implements Iterator<String> {
        private boolean _isUnique;
        private Set<String> _previousValues;
        private String _lastValue;
        private boolean _isMulti;
        private DataColumn _dc;

        public ValueIterator(DataColumn dc) {
            _dc = dc;
            _isUnique = dc.isUnique();
            _isMulti = dc.isMulti();
            _previousValues = new HashSet<String>();
        }
        
        protected DataColumn getDataColumn() {
            return _dc;
        }

        public void reset() {
            _previousValues = new HashSet<String>();
            _lastValue = null;
        }

        protected boolean isMulti() {
            return _isMulti;
        }

        protected boolean isUnique() {
            return _isUnique;
        }

        @Override
        public boolean hasNext() {
            // The iterators will always return a value on 'next()'.  They
            // only need to worry about duplicate values
            return true;
        }

        /**
         * Tracking method for implementations requiring unique values. It adds the candidate
         * value to the previousValues list and determines if the value was unique.
         * The submitted value is always saved as the lastValue
         */
        protected boolean incrementNext(String nextValue) {
            boolean added = _previousValues.add(nextValue);
            if (!_isUnique || added) {
                added = true;
            }
           _lastValue = nextValue;
            return added;
        }

        /**
         * For implementations that will not generate multiple values per object, they may return the 'lastValue' here providing
         * they submitted the value to {@link #incrementNext(String)}
         */
        protected String getLastValue() {
            return _lastValue;
        }
        
        protected Set<String> getLastValues() {
            return _previousValues;
        }

        @Override
        public void remove() {
            // not supported
            throw new UnsupportedOperationException();
        }
    }


    


    /******************
     * CONSTANTS
     ******************/

    public static final String STANDARD_CHARACTER_CLASS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    public static final boolean     DEFAULT_IS_MULTI                = false;
    public static final ColumnType  DEFAULT_COLUMN_TYPE             = ColumnType.generated;
    public static final String      DEFAULT_CHARACTER_CLASS         = STANDARD_CHARACTER_CLASS;
    public static final int         DEFAULT_GENERATED_MAX_LENGTH    = 16;
    public static final int         DEFAULT_GENERATED_MIN_LENGTH    = 4;
    public static final boolean     DEFAULT_IS_UNIQUE               = false;

    /*****************
     * Fields
     * 
     * These fields are largely values we'll ultimately pass on to our
     * underlying iterator.  However it's convenient to hold on to them
     * in this class for later reference.
     *****************/
    private boolean _isMulti;
    private boolean _isUnique;
    private ValueIterator _valueIterator;
    private String _columnName;
    private boolean _applied;

    public enum ColumnType {
        generated,
        file,
        serialFile,
        derived,
        constant,
        incrementer,
        hierarchy,
        csv
    };

    /**
     * Primary constructor
     * @param columnName Name of the column definition
     */
    public DataColumn(String columnName) {
        _isMulti = DEFAULT_IS_MULTI;
        _isUnique = DEFAULT_IS_UNIQUE;
        _columnName = columnName;
        _applied = false;
    }
    
    public void apply(Map<String, Object> detailMap) {
        String sMulti = (String) detailMap.get(ARG_MULTI);
        if (sMulti != null) {
            setMulti(Boolean.valueOf(sMulti));
        }
        String sUnique = (String)detailMap.get(ARG_UNIQUE);
        if (sUnique != null) {
            setUnique(Boolean.valueOf(sUnique));
        }
        this._applied = true;
    }

    public String getColumnName() {
        return _columnName;
    }

    public boolean isUnique() {
        return _isUnique;
    }

    public void setUnique(boolean isUnique) {
        _isUnique = isUnique;
    }

    public boolean isMulti() {
        return _isMulti;
    }

    public void setMulti(boolean isMulti) {
        this._isMulti = isMulti;
    }

    /**
     * Returns the next value from the value iterator.  It may
     * be a unique value or the same value already provided.
     * The following contract is used:
     * - if the column supports multiple values, a unique
     *   value is attempted.  Only when a unique value could
     *   not be retrieved will a duplicate value be returned.
     * - if the column does not support multiple values, the same
     *   value is always returned.
     * - Resetting the DataColumn will invoke a new value, restarting
     *   the duplication contract from scratch.
     */
    public String nextValue(CsvObjectGenerator generator) {
        // Implementers must apply the detailsMap to this
        // parent instance, so check and barf here
        if (!_applied) {
            throw new IllegalStateException(getClass() + " has not applied the detailsMap properly!");
        }
        // get the value iterator and pull the next value token
        if (_valueIterator == null) {
            _valueIterator = getIterator();
        }

        if (_valueIterator.hasNext()) {
            return _valueIterator.next();
        }

        // all iterators always return a value;  This is unreachable.
        return null;
    }

    public void reset() {
        if (_valueIterator != null) {
            _valueIterator.reset();
        }
    }
    
    public abstract ValueIterator getIterator();

}