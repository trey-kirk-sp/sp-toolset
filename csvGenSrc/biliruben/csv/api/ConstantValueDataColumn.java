package biliruben.csv.api;

import java.util.Map;

/**
 * DataColumn that always returns the same value
 * @author trey.kirk
 *
 */
public class ConstantValueDataColumn extends DataColumn {

    protected static final String ARG_CONSTANT = "constant";
    private String _constant;

    private static class ConstantValueIterator extends ValueIterator {
        private String _constant;

        public ConstantValueIterator(ConstantValueDataColumn dc) {
            super(dc);
            this._constant = dc.getConstant();
        }

        @Override
        public boolean hasNext() {
            return true;
        }
        
        @Override
        public String next() {
            return _constant;
        }
    }

    public ConstantValueDataColumn(String columnName) {
        super(columnName);
    }

    @Override
    public void apply(Map<String, Object> detailMap) {
        super.apply(detailMap);
        if (isMulti()) {
            throw new IllegalStateException("multi = true: you're pretty funny. " + getClass().getCanonicalName() + " is a constant value iterator. Don't get cheeky.");
        }
        if (isUnique()) {
            throw new IllegalStateException("unqiue = true: What part of 'constant value' did you not understand?");
        }
        setConstant((String) detailMap.get(ARG_CONSTANT));
    }
    
    public void setConstant(String constant) {
        this._constant = constant;
    }
    
    public String getConstant() {
        return this._constant;
    }

    @Override
    public ValueIterator getIterator() {
        return new ConstantValueIterator(this);
    }


}
