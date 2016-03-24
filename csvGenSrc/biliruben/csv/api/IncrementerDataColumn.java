package biliruben.csv.api;

import java.util.Map;

public class IncrementerDataColumn extends DataColumn {
    
    private static final String ARG_PADDING = "padding";
    private static final String ARG_START = "start";
    private static final Object ARG_INCREMENT = "increment";
    private static int DEFAULT_INCR_START = 1;
    private static int DEFAULT_INCREMENT = 1;
    private static int DEFAULT_INCR_PADDING = 6;

    private int _incrStart;
    private int _incrPadding;
    private int _increment;

    private static class IncrementingValueIterator extends ValueIterator {
        private int _incr;
        private int _padding;
        private int _current;
        
        public IncrementingValueIterator(IncrementerDataColumn dc) {
            super(dc);
            _current = dc.getIncrementStart();
            _incr = dc.getIncrement();
            if (_incr <= 0) {
                _incr = DEFAULT_INCREMENT;
            }
            _padding = dc.getIncrementPadding();
            if (_padding < 0) {
                _padding = DEFAULT_INCR_PADDING;
            }
        }
        
        @Override
        public String next() {
            String value = String.valueOf(_current);
            if (_padding > 0) {
                value = String.format("%1$" + _padding + "s", String.valueOf(_current));
            }
            value = value.replace(" ", "0");
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

    @Override
    public ValueIterator getIterator() {
        return new IncrementingValueIterator(this);
    }

}
