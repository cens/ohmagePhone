package edu.ucla.cens.andwellness.conditionevaluator;

import java.io.StringReader;
import java.util.List;

import org.andwellness.config.grammar.parser.ConditionParser;
import org.andwellness.config.grammar.parser.ParseException;
import org.andwellness.config.grammar.syntaxtree.start;

import edu.ucla.cens.systemlog.Log;

/**
 * Basic utility class to evaluate a string condition against a list of data points.
 * 
 * Each condition is a number of expressions 'and'ed and 'or'ed together. Each expression is
 * an 'id' conditioned against a 'value'. This class will create a new conditionParser to parse
 * the condition, then use the ConditionDepthFirst visitor class to recursively evaluate the condition
 * and produce an overall response.
 * 
 * @author jhicks
 *
 */
public class DataPointConditionEvaluator {
	
	private static final String TAG = "DataPointConditionEvaluator";
	
    private static boolean conditionParserInitialized = false;  // Track whether the condition parse is initialized
    //private static Logger _logger = Logger.getLogger(DataPointConditionEvaluator.class);
    
    /**
     * Checks if the passed condition is true based on the passed list of node responses.  If the id does
     * not exist in the responses, assume the response is NULL.
     * 
     * @param condition The condition to check.
     * @param previousResponses The List of previous responses.
     * @return Whether or not the condition is true.
     */
    public static boolean evaluateCondition(String condition, List<DataPoint> previousResponses) {
        // Blank conditions are always valid
        if(! "".equals(condition)) {
            start s = null;
            
            try {
                // The API is odd, we have to instantiate an object just to use its static methods below,
                // we do not actually need to keep the object around for any reason
                if (!conditionParserInitialized) {
                    new ConditionParser(new StringReader(condition));
                    conditionParserInitialized = true;
                }
                else {
                    ConditionParser.ReInit(new StringReader(condition));
                }
                // Call start statically even though we have to instantiate the object above
                s = ConditionParser.start();
                
                ConditionDepthFirst<Boolean, Boolean> visitor 
                    = new ConditionDepthFirst<Boolean, Boolean>(previousResponses);
                // Check the condition against the previous responses
                Boolean conditionValue = visitor.visit(s, null);
                
                /*if (DataPointConditionEvaluator._logger.isDebugEnabled()) {
                    DataPointConditionEvaluator._logger.debug("Condition [" + condition + "] evaluated as " + conditionValue.toString());
                }*/
                Log.d(TAG, "Condition [" + condition + "] evaluated as " + conditionValue.toString());
                
                return conditionValue.booleanValue();
                
            } catch (ParseException pe) {

                throw new IllegalArgumentException("Condition failed to parse, should have been checked in the XML validator: " + condition);
            }
        }
        
        return true;
    }
}
