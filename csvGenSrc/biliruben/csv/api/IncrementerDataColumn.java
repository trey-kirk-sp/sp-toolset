package biliruben.csv.api;

import java.util.Map;

/**
 * Increments from the starting value by the provided increment (positive or negative). Returns the
 * values padded with 0s. Note: if you provide a configuration that may result in negative values, the
 * padding will be padded before the negative sign and might look weird. Like this: 000-17
 * @author trey.kirk
 *
 */
public class IncrementerDataColumn extends DataColumn {
    
    private static final String ARG_PADDING = "padding";
    private static final String ARG_START = "start";
    private static final String ARG_INCREMENT = "increment";
    private static final String ARG_PADDING_CHAR = "paddingChar";
    private static int DEFAULT_INCR_START = 1;
    private static int DEFAULT_INCREMENT = 1;
    private static int DEFAULT_INCR_PADDING = 6;
    private static String DEFAULT_PADDING_CHAR = "0";

    private int _incrStart;
    private int _incrPadding;
    private int _increment;
    private String _paddingChar;

    private static class IncrementingValueIterator extends ValueIterator {
        private int _incr;
        private int _padding;
        private int _current;
        private String _paddingChar;
        
        public IncrementingValueIterator(IncrementerDataColumn dc) {
            super(dc);
            _current = dc.getIncrementStart();
            _incr = dc.getIncrement();
            if (_incr == 0) {
                _incr = DEFAULT_INCREMENT;
            }
            _padding = dc.getIncrementPadding();
            if (_padding < 0) {
                _padding = DEFAULT_INCR_PADDING;
            }
            
            _paddingChar = dc.getPaddingChar();
            
        }
        
        @Override
        public String next() {
            String value = String.valueOf(_current);
            if (_padding > 0) {
                value = String.format("%1$" + _padding + "s", String.valueOf(_current));
            }
            value = value.replace(" ", _paddingChar);
            return value;
        }

         @Override
        public void reset() {
            super.reset();
            _current += _incr;
        }
    }

    public IncrementerDataColumn(String columnName) {
        super(columnName);
        this._incrStart = DEFAULT_INCR_START;
        this._incrPadding = DEFAULT_INCR_PADDING;
        this._paddingChar = DEFAULT_PADDING_CHAR;
    }

    @Override
    public void apply(Map<String, Object> detailMap) {
        super.apply(detailMap);
        String incrStartStr = (String)detailMap.get(ARG_START);
        if (incrStartStr != null && !"".equals(incrStartStr.trim())) {
            setIncrementStart(Integer.valueOf(incrStartStr));
        }
        String padding = (String) detailMap.get(ARG_PADDING);
        if (padding != null && !"".equals(padding.trim())) {
            setIncrementPadding(Integer.valueOf(padding));
        }
        String incr = (String) detailMap.get(ARG_INCREMENT);
        if (incr != null && !"".equals(incr.trim())) {
            setIncrement(Integer.valueOf(incr));
        }
        String paddingChar = (String) detailMap.get(ARG_PADDING_CHAR);
        if (paddingChar != null && !"".equals(paddingChar.trim())) {
            setPaddingChar(paddingChar);
        }
    }
    
    public void setIncrement(int incr) {
        this._increment = incr;
    }
    
    public int getIncrement() {
        return this._increment;
    }

    public void setIncrementStart(int start) {
        this._incrStart = start;
    }
    
    public int getIncrementStart() {
        return this._incrStart;
    }
    
    public void setIncrementPadding(int padding) {
        this._incrPadding = padding;
    }
    
    public int getIncrementPadding() {
        return this._incrPadding;
    }
    
    public void setPaddingChar(String paddingChar) {
        this._paddingChar = paddingChar;
    }
    
    public String getPaddingChar() {
        return this._paddingChar;
    }

    @Override
    public ValueIterator getIterator() {
        return new IncrementingValueIterator(this);
    }

}
