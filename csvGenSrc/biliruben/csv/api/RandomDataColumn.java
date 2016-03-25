package biliruben.csv.api;

import java.util.Map;
import java.util.Random;

/**
 * This DataColumn supports random data selection. This class is not concerned with the underlying
 * data set. It only provides the mechanism to randomize the selection process
 * @author trey.kirk
 *
 */
public abstract class RandomDataColumn extends DataColumn {
    
    protected static abstract class RandomValueIterator extends ValueIterator {

        private Random _rando;
        private long _seed;

        public RandomValueIterator(RandomDataColumn rdc) {
            super(rdc);
            _seed = rdc.getRandomSeed();
            if (_seed == 0L) {
                _seed = System.currentTimeMillis();
            }
            _rando = new Random(_seed);
            //System.out.println(rdc.getColumnName() + " random seed: " + _seed);
        }
        
        protected Random getRandom() {
            return _rando;
        }
        
        @Override
        public void reset() {
            super.reset();
        }
    }
    
    public static String ARG_RANDOM_SEED = "randomSeed";
    private long _randomSeed = 0L;

    public RandomDataColumn(String columnName) {
        super(columnName);
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