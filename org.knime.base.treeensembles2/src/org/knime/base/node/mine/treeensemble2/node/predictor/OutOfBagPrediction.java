/**
 *
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface OutOfBagPrediction extends Prediction {

    /**
     * @return the number of models that were used to obtain the prediction
     */
    int getModelCount();

    /**
     * @return True if a prediction was possible
     */
    public boolean hasPrediction();
}
