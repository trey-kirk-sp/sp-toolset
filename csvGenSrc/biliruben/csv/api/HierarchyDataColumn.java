package biliruben.csv.api;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Map;


/**
 * Based on a given node column, values are returned as what is intended to be the
 * current object's parent node. For example, in an object that represents and employee
 * and requires a manager value, this DataColumn provides the manager value.
 * @author trey.kirk
 *
 */
public class HierarchyDataColumn extends DataColumn {
    
    private class HierarchyDataValueIterator extends ValueIterator {
        
        private String _currentNode;
        private int _siblingLimit;
        private int _currentSiblings;
        private DataColumn _nodeDataColumn;
        private LinkedList<String> _nodeStack;
        private boolean _gotNext;

        public HierarchyDataValueIterator(HierarchyDataColumn dc, String nodeColumn) {
            super(dc);
            _currentNode = null; // First line is the root node and has no parent node
            _currentSiblings = 0;
            _siblingLimit = dc.getSiblings();
            _nodeDataColumn = getGenerator().getDataColumn(nodeColumn);
            _nodeStack = new LinkedList<String>();
        }
        
        @Override
        public void reset() {
            super.reset();
            String nextNodeValue = _nodeDataColumn.nextValue(getGenerator());
            _nodeStack.add(nextNodeValue); // tack it onto the end
            // When we're reset, determine if we've reached our sibling limit
            // If so, get the next value in our Node DC
            _currentSiblings++;
            
            if (_currentSiblings >= _siblingLimit || _currentNode == null) {
                // next parent node plz
                _currentNode = _nodeStack.pollFirst();
                _currentSiblings = 0;
            }
        }

        @Override
        public String next() {
            return _currentNode;
        }
    }

    private static final Object ARG_NODE_COLUMN = "node";
    private static final Object ARG_DEPTH = "depth";
    private static final Object ARG_SIBLINGS = "siblings";
    private CsvObjectGenerator _generator;
    private String _nodeColumn;
    private int _depth;
    private int _siblings;
    private HierarchyDataValueIterator _iterator;

    public HierarchyDataColumn(String columnName, CsvObjectGenerator generator) {
        super(columnName);
        this._generator = generator;

    }
    
    @Override
    public void apply(Map<String, Object> detailMap) {
        super.apply(detailMap);
        if (isMulti()) {
            // How the fuck is that supposed to work?
            throw new IllegalStateException(getClass().getCanonicalName() + " does not support multiple values");
        }
        this._nodeColumn = (String)detailMap.get(ARG_NODE_COLUMN);
        if (_nodeColumn == null || "".equals(_nodeColumn.trim())) {
            throw new IllegalStateException(ARG_NODE_COLUMN + " property must be specified and correspond to another defined column");
        }
        this._siblings = DEFAULT_SIBLINGS;
        String depth = (String)detailMap.get(ARG_DEPTH);
        // If you specify a depth, we'll use that. Otherwise we'll
        // use your siblings value or the default siblings value
        if (depth != null && !"".equals(depth.trim())) {
            // We assume that the depth does not factor in
            // the root node. So a depth of '1' is the single
            // layer below the root node. The root node is
            // depth 0.
            this._depth = Integer.valueOf(depth);
        }
        if (this._depth <= 0) {
            // Either they didn't supply a depth or the one they gave us
            // is really stupid. TODO: logger indicates what we're calculating
            String siblings = (String)detailMap.get(ARG_SIBLINGS);
            if (siblings != null && !"".equals(siblings.trim())) {
                this._siblings = Integer.valueOf(siblings);
            }
        }
    }

    private static final int DEFAULT_SIBLINGS = 10;
    
    private int getSiblings() {
        // if we weren't given a depth, use whatever's set for siblings
        if (_depth <= 0) {
            return _siblings;
        }
        // Else we gotta calculate it:
        // We know how deep we want to go and the total of how many there will be,
        // so we should be able to determine how many siblings is required.
        // That formula:
        // s^0 + s^1 ... + s^n = t
        // where:
        // s: siblings to solve for
        // n: depth
        // t: total objects
        
        // Since this could be a very complex polynomial equation (or a dumb quadratic)
        // we're going to avoid attempting fancy calculus and smartly guess
        // Just take the ceiling of the nth root of t

        int total = _generator.getObjects();
        double ceil = Math.ceil(Math.pow(total, 1/((double)_depth)));
        int siblings = 0;

        // I'm a computer and math is stupid, so walk
        // down from our ceiling and make a few calculations to 'hone in'
        while (siblings == 0) {
            // might have to return to this algorithm and turn it into a 
            // binary guessing game instead of just walking down the values
            for (int i = (int) ceil; i > 0; i--) {
                int totalPossible = 0;
                for (int pow = 0; pow <= _depth; pow++) {
                    totalPossible += Math.pow(i, pow);
                }
                if (totalPossible > total) {
                    // this one is fine, but maybe there's a better choice?
                    siblings = i;
                } else {
                    // this one is terrible and it's not gonna get better
                    break; // for loop
                }
            }
            // is siblings still 0? Shit, our guess is stupid!
            ceil = ceil * 2;
        }

        return siblings;
    }
    
    private CsvObjectGenerator getGenerator() {
        return _generator;
    }


    @Override
    public ValueIterator getIterator() {
        if (_iterator == null) {
            _iterator = new HierarchyDataValueIterator(this, _nodeColumn);
        }
        return _iterator;
    }
    
}
