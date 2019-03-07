package biliruben.csv.api;

import org.hibernate.id.UUIDHexGenerator;

public class UUIDDataColumn extends DataColumn {
    
    private class UUIDValueIterator extends ValueIterator {


        private UUIDHexGenerator _generator;

        public UUIDValueIterator(DataColumn dc) {
            super(dc);
            _generator = new UUIDHexGenerator();
        }

        @Override
        public String next() {
            return _generator.generate(null, null).toString();
        }
        
    }
    
    public UUIDDataColumn(String name) {
        super(name);
    }

    @Override
    public ValueIterator getIterator() {
        return new UUIDValueIterator(this);
    }


}
