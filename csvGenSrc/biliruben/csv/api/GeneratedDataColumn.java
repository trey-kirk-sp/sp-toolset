package biliruben.csv.api;

import java.util.Map;

/**
 * Generates a random string based on the provided character class and min/max limits
 * @author trey.kirk
 *
 */
public class GeneratedDataColumn extends RandomDataColumn {

    private static final String ARG_MAX_LENGTH = "maxLength";
    private static final String ARG_MIN_LENGTH = "minLength";
    private static final String ARG_CHARACTERS = "characters";
    private String _characterClass;
    private int _genMaxLn;
    private int _genMinLn;
    
    /*
     * Generated value iterator.  Given a set of rules, values
     * are derived from a base character class.
     */
    private static class GeneratedValueIterator extends RandomValueIterator {

        private String _characterClass;
        private int _genMaxLn;
        private int _genMinLn;

        public GeneratedValueIterator(GeneratedDataColumn dc) {
            super(dc);
            this._characterClass = dc.getCharacterClass();
            this._genMaxLn = dc.getGenMaxLn();
            this._genMinLn = dc.getGenMinLn();
            if (this._genMinLn > this._genMaxLn) {
                this._genMinLn = this._genMaxLn;
            }
        }

        @Override
        public String next() {
            String lastValue = getLastValue();
            if (lastValue != null && !isMulti()) {
                return lastValue;
            }
            StringBuilder buff = null;
            boolean unique = false;
            int count = 0; // only gonna try 10 times
            do {
                buff = new StringBuilder();
                // generating a random value
                int delta = _genMaxLn - _genMinLn;
                int length = _genMinLn;
                if (delta > 0) {
                    length = getRandom().nextInt(delta) + _genMinLn + 1;
                }
                for (int i = 0; i < length; i++) {
                    int pos = getRandom().nextInt(this._characterClass.length());
                    buff.append(this._characterClass.charAt(pos));
                }

                unique = incrementNext(buff.toString());
                count++;
            } while  (isUnique() && !unique && count < 10);

            return buff.toString();
        }
    }


    public GeneratedDataColumn(String columnName) {
        super(columnName);
    }

    @Override
    public void apply(Map<String, Object> detailMap) {
        super.apply(detailMap);
        String characters = (String) detailMap.get(ARG_CHARACTERS);
        if (characters != null) {
            setCharacterClass(characters);
        }
        String sMinLength = (String) detailMap.get(ARG_MIN_LENGTH);
        if (sMinLength != null) {
            int minLength = Integer.valueOf(sMinLength);
            setGenMinLn(minLength);
        }
        String sMaxLength = (String) detailMap.get(ARG_MAX_LENGTH);
        if (sMaxLength != null) {
            int maxLength = Integer.valueOf(sMaxLength);
            setGenMaxLn(maxLength);
        }
    }

    public String getCharacterClass() {
        return _characterClass;
    }
    public void setCharacterClass(String characterClass) {
        this._characterClass = characterClass;
    }
    public int getGenMaxLn() {
        return _genMaxLn;
    }
    public void setGenMaxLn(int genMaxLn) {
        this._genMaxLn = genMaxLn;
    }
    public int getGenMinLn() {
        return _genMinLn;
    }
    public void setGenMinLn(int genMinLn) {
        this._genMinLn = genMinLn;
    }

    @Override
    public ValueIterator getIterator() {
        return new GeneratedValueIterator(this);
    }

}
