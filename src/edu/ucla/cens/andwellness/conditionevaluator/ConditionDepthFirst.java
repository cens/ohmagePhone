/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.ucla.cens.andwellness.conditionevaluator;

import java.util.List;

import org.andwellness.config.grammar.syntaxtree.NodeOptional;
import org.andwellness.config.grammar.syntaxtree.NodeSequence;
import org.andwellness.config.grammar.syntaxtree.NodeToken;
import org.andwellness.config.grammar.syntaxtree.condition;
import org.andwellness.config.grammar.syntaxtree.conjunction;
import org.andwellness.config.grammar.syntaxtree.expr;
import org.andwellness.config.grammar.syntaxtree.id;
import org.andwellness.config.grammar.syntaxtree.sentence;
import org.andwellness.config.grammar.syntaxtree.sentence_prime;
import org.andwellness.config.grammar.syntaxtree.start;
import org.andwellness.config.grammar.syntaxtree.value;
import org.andwellness.config.grammar.visitor.GJDepthFirst;

import edu.ucla.cens.andwellness.conditionevaluator.comparator.DataPointComparator;
import edu.ucla.cens.andwellness.conditionevaluator.comparator.DataPointComparatorFactory;
import edu.ucla.cens.systemlog.Log;

/**
 * Check to see if the condition outputs to true or false, based on previous responses.
 * This should be extending a custom class as we want different return values depending
 * on visitor, but GJDepthFirst plus wonky casting works well enough.
 * 
 * The basic operation is as follows:  For every expression, this visitor will lookup
 * the 'id' in the current id list, grab the 'value', then use the DataPointComparator
 * to compare the 'id' to the 'value' based on the 'condition'.  This gets passed up
 * to the overall sentence, which will combine all the expressions for a final Boolean
 * value.
 * 
 * @author jhicks
 *
 * @param <R> Must be a Boolean or a String, depending on the visitor.
 * @param <A> Must be a Boolean, only used to pass from sentence to sentence_prime
 */
public class ConditionDepthFirst<R, A> extends GJDepthFirst<R, A> {
	
	private static final String TAG = "ConditionDepthFirst";
	
    // Holds the List of current ID/value pairs
    private List<DataPoint> _currentIdList;
    //private static Logger _logger = Logger.getLogger(ConditionDepthFirst.class);
    
    /**
     * Used to set the currentIdList for the visitors to access
     * 
     * @param currentIdList The current List of ID/Value pairs
     */
    public ConditionDepthFirst(List<DataPoint> currentIdList) {
        _currentIdList = currentIdList;
    }
    
    /**
     * Return the String of the NodeToken.
     * 
     * @return A String representing the token's value.
     */
    @SuppressWarnings("unchecked")
    public R visit(NodeToken n, A argu) { 
        return (R) n.toString(); 
    }
    
    /**
     * f0 -> sentence()
     * f1 -> <EOF>
     * 
     * @return The validity of the sentence.
     */
    public R visit(start n, A argu) {
       R _ret=null;
       // Return the evaluation of the overall sentence
       _ret = n.f0.accept(this, argu);
       n.f1.accept(this, argu);
       return _ret;
    }

    /**
     * f0 -> expr() sentence_prime()
     *       | "(" sentence() ")" sentence_prime()
     *       
     *  A bit more complicated because the grammar was transformed from
     *  left recursive to right recursive, adding a "sentence_prime" condition
     *  First evaluate the expr or sentence depending on which f0 we have.
     *  Then pass this to the sentence_prime and return the eval of the sentence_prime.
     *       
     * @return The validity of the sentence_prime
     */
    @SuppressWarnings("unchecked")
    public R visit(sentence n, A argu) {
       R _ret=null;

       // Pull out the node sequence which will be one of
       // expr() sentence_prime() or "(" sentence() ")" sentence_prime()
       NodeSequence nodeSequence = (NodeSequence) n.f0.choice;
       
       // Check to see which choice we have
       // f0 -> expr() sentence_prime()
       if (n.f0.which == 0) {
           // Separate out the nodes
           expr expressionNode = (expr) nodeSequence.elementAt(0);
           sentence_prime sentencePrimeNode = (sentence_prime) nodeSequence.elementAt(1);
           
           // First eval the expr
           Boolean exprBool = (Boolean) expressionNode.accept(this, argu);
           // Now eval the sentence prime, passing in the result from the expr
           _ret = sentencePrimeNode.accept(this, (A) exprBool);
       }
       // f0 -> "(" sentence() ")" sentence_prime()
       else if (n.f0.which == 1) {
           // Separate out the meaningful nodes and evaluate
           sentence sentenceNode = (sentence) nodeSequence.elementAt(1);
           sentence_prime sentencePrimeNode = (sentence_prime) nodeSequence.elementAt(3);
           
           Boolean sentenceBool = (Boolean) sentenceNode.accept(this, argu);
           _ret = sentencePrimeNode.accept(this, (A) sentenceBool);
       }
       
       return _ret;
    }

    /**
     * f0 -> ( conjunction() sentence() sentence_prime() )?
     * 
     * The sentence_prime visitor makes the least intuitive sense.  There are two cases
     * because of the ?: 
     * 1) The base case is the sentence_prime is nullified and does not exist.  
     *    Return the input Boolean argument as the sentence_prime has no effect.
     * 2) The sentence_prime exists.  Notice that a sentence_prime is just a conjunction
     *    with a sentence, the left side of the conjunction does not exist.  The Boolean
     *    argument is the left side so what we evaluate is 'argu conjunction sentence', which
     *    is then passed recursively into the sentence_prime.
     * 
     * @param argu A Boolean that represents the left side of the conjunction
     * @return A Boolean that represents the valisity of this sentence_prime
     */
    @SuppressWarnings("unchecked")
    public R visit(sentence_prime n, A argu) {
       R _ret=null;
       
       // Pull out the initial value (the one before the first conjunction)
       Boolean initialValue = (Boolean) argu;
       
       // The NodeOptional will tell us if this node exists
       NodeOptional nodeOptional = (NodeOptional) n.f0;
       
       // If this is a nullified sentence_prime, just return the initial value back
       // (The base case of the recursiveness)
       if (!nodeOptional.present()) {
           _ret = (R) initialValue;
       }
       
       // Evaluate the initial value conjoined with the sentence, send in
       // as the new initial value for the new sentence prime, and return
       // what that sentence_prime gives us back
       else {
           // The boolean to pass to the sentence prime
           Boolean toPassToSentencePrime;
           // Since we know the node exists, grab it
           NodeSequence nodeSequence = (NodeSequence) nodeOptional.node;
           // Pull out the conjunction value
           String conjValue = (String) nodeSequence.elementAt(0).accept(this, argu);
           Boolean sentValue;  // Don't evaluate now, use shortcutting if appropriate
           sentence_prime sentencePrime = (sentence_prime) nodeSequence.elementAt(2);
           
           
           if ("and".equals(conjValue)) {
               // Use shortcutting, don't evaluate the sentence if the initialValue is false
               // because false 'and'ed with anything is false
               if (initialValue.booleanValue() == false) {
                   toPassToSentencePrime = new Boolean(false);
               }
               // Now we have to evaluate the other side of the 'and'
               else {
                   sentValue = (Boolean) nodeSequence.elementAt(1).accept(this, argu);
                   
                   // Both sides of the and must be true to return true
                   if (sentValue.booleanValue() == true) {
                       toPassToSentencePrime = new Boolean(true);
                   }
                   else {
                       toPassToSentencePrime = new Boolean(false);
                   }
               }
           }
           else if ("or".equals(conjValue)) {
               // If the first side of the 'or' is true, we don't need to evaluate the second
               if (initialValue.booleanValue() == true) {
                   toPassToSentencePrime = new Boolean(true);
               }
               // Evaluate the other side of the 'or'
               else {
                   sentValue = (Boolean) nodeSequence.elementAt(1).accept(this, argu);
                   
                   // Since the left side of the 'or' was false, this must be true to return true
                   if (sentValue.booleanValue() == true) {
                       toPassToSentencePrime = new Boolean(true);
                   }
                   else {
                       toPassToSentencePrime = new Boolean(false);
                   }
               }
           }
           else {
               // this should never happen?
               throw new IllegalArgumentException("Conjunction neither and nor or.");
           }
           
           // Now pass this recursively to the sentence_prime to see if there is anything else to do
           _ret = sentencePrime.accept(this, (A) toPassToSentencePrime);
       }

       return _ret;
    }

    /**
     * f0 -> id()
     * f1 -> condition()
     * f2 -> value()
     * 
     * A bit of logic is needed here but still fairly straight forward.  First, lookup the
     * id in the List<DataPoint> argu.  If the ID does not exist or is of value "NOT_SHOWN",
     * return false.  If the ID is of value "SKIPPED", only return true if the condition is "=="
     * and the nodeValue is "SKIPPED".  Else compare the nodeValue to the value found in the
     * id list.
     * 
     * @return A Boolean representing the validity of this expression.
     */
    @SuppressWarnings("unchecked")
    public R visit(expr n, A argu) {
       R _ret=null;
       String nodeId = (String) n.f0.accept(this, argu);
       String nodeCondition = (String) n.f1.accept(this, argu);
       String nodeValue = (String) n.f2.accept(this, argu);

       // Lookup the nodeID in the List of IDs with responses.
       DataPoint dataPointForComparison = new DataPoint(nodeId);
       int nodeIdLocation = _currentIdList.indexOf(dataPointForComparison);
       
       // If we can't find the nodeId, assume this expression is false
       if (nodeIdLocation == -1) {
           _ret = (R) new Boolean(false);
           
           /*if (ConditionDepthFirst._logger.isDebugEnabled()) {
               ConditionDepthFirst._logger.debug("Could not find node id " + nodeId);
           }*/
           
           Log.d(TAG, "Could not find node id " + nodeId);
       }
       // If we find the ID, evaluation the expression
       else {
           boolean result;
           DataPoint dataPoint = _currentIdList.get(nodeIdLocation);
           
           // Grab a DataPointComparator to compare the DataPoint to the value
           DataPointComparator dataPointComparator = 
               DataPointComparatorFactory.createDataPointComparator(dataPoint.getPromptType());
           
           result = dataPointComparator.compare(dataPoint, nodeValue, nodeCondition);
           
           _ret = (R) new Boolean(result);
       }
       
       /*if (ConditionDepthFirst._logger.isDebugEnabled()) {
           ConditionDepthFirst._logger.debug("Evaluated " + nodeId + " " + nodeCondition + 
                   " " + nodeValue + " and got " + ((Boolean) _ret).toString());
       }*/
       
       Log.d(TAG, "Evaluated " + nodeId + " " + nodeCondition + " " + nodeValue + " and got " + ((Boolean) _ret).toString());
       
       return _ret;
    }

    /**
     * f0 -> <TEXT>
     */
    public R visit(id n, A argu) {
       R _ret=null;
       _ret = n.f0.accept(this, argu);
       return _ret;
    }

    /**
     * f0 -> "=="
     *       | "!="
     *       | "<"
     *       | ">"
     *       | "<="
     *       | ">="
     */
    public R visit(condition n, A argu) {
       R _ret=null;
       _ret = n.f0.accept(this, argu);
       return _ret;
    }

    /**
     * f0 -> <TEXT>
     */
    public R visit(value n, A argu) {
       R _ret=null;
       _ret = n.f0.accept(this, argu);
       return _ret;
    }

    /**
     * f0 -> "and"
     *       | "or"
     */
    public R visit(conjunction n, A argu) {
       R _ret=null;
       _ret = n.f0.accept(this, argu);
       return _ret;
    }
}
