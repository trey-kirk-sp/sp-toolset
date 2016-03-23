package biliruben.csv.api;

import java.util.Map;
import java.util.Random;

public abstract class RandomDataColumn extends DataColumn {
    
    protected static abstract class RandomValueIterator extends ValueIterator {

        private Random _rando;

        public RandomValueIterator(RandomDataColumn rdc) {
            super(rdc);
            long seed = rdc.getRandomSeed();
            if (seed == 0L) {
                seed = System.currentTimeMillis();
            }
            _rando = new Random(seed);
            System.out.println(rdc.getColumnName() + " random seed: " + seed);
        }
        
        protected Random getRandom() {
            return _rando;
        }
    }
    
    public static String ARG_RANDOM_SEED = "randomSeed";
    private long _randomSeed = 0L;

    public RandomDataColumn(String columnName, ColumnType type) {
        super(columnName, type);
    }
    
    public void setRandomSeed(long seed) {
        this._randomSeed = seed;
    }
    
    public long getRandomSeed() {
        return _randomSeed;
    }
    
    @Override
    public void apply(Map<String, Object> detailMap) {
        super.apply(detailMap);
        String randomSeed = (String)detailMap.get(ARG_RANDOM_SEED);
        if (randomSeed != null && !"".equals(randomSeed)) {
            long seed = Long.valueOf(randomSeed);
            if (seed != 0) {
                this._randomSeed = seed;
            }
        }
    }

}