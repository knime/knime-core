/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * History: 
 *     03.06.2004 (gabriel) created
 */

package org.knime.base.node.mine.bfn;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * Class presents a predictor row for basisfunctions providing method to apply
 * unknown data (compose).
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionPredictorRow {
    
    /** The key of this row. */
    private final DataCell m_key;

    /** The class label of this basisfunction. */
    private final DataCell m_classLabel;

    /** The don't know class degree; activation is above this threshold. */
    private final double m_dontKnowDegree;

    /** Number of correctly covered pattern. */
    private int m_correctCovered;

    /** Number of wrong covered pattern. */
    private int m_wrongCovered;
    
    /** Number of pattern of this class used for training. */
    private final int m_numPatOfClass;

    /**
     * Creates new predictor row.
     * 
     * @param key the key of this row
     * @param classLabel class label of the target attribute
     * @param dontKnowDegree don't know probability
     * @param numPatOfThisClass number of pattern for this class 
     */
    protected BasisFunctionPredictorRow(final DataCell key,
            final DataCell classLabel, final int numPatOfThisClass, 
            final double dontKnowDegree) {
        m_key = key;
        m_classLabel = classLabel;
        m_dontKnowDegree = dontKnowDegree;
        m_numPatOfClass = numPatOfThisClass;
        m_correctCovered = 0;
        m_wrongCovered = 0;

    }

    /**
     * Creates new predictor row on model content.
     * 
     * @param pp the model content to read the new predictor row from
     * @throws InvalidSettingsException if the model content is invalid
     */
    public BasisFunctionPredictorRow(final ModelContentRO pp)
            throws InvalidSettingsException {
        m_key = pp.getDataCell("row_id");
        m_classLabel = pp.getDataCell("class_label");
        m_dontKnowDegree = pp.getDouble("dont_know_class");
        m_correctCovered = pp.getInt("correct_covered");
        m_wrongCovered = pp.getInt("wrong_covered");
        m_numPatOfClass = pp.getInt("number_pattern_class", 1); //backward comp.
    }
    
    /**
     * If the same class as this basisfunction is assigned to, the number of
     * correctly covered pattern is increased, otherwise the number of wrong
     * covered ones.
     * @param classLabel a pattern of the given class has to be covered
     */
    final void cover(final DataCell classLabel) {
        if (m_classLabel.equals(classLabel)) {
            m_correctCovered++;
        } else {
            m_wrongCovered++;
        }
    }
    
    /**
     * Computes the activation based on the given row for this basisfunction.
     * @param row compute activation for
     * @return activation between 0 and 1
     */
    public abstract double computeActivation(final DataRow row);

    /**
     * Composes the activation of the given array and of the calculated one
     * based on the given row. All values itself have to be between
     * <code>0</code> and <code>1</code>.
     * 
     * @param row combine activation with this pattern
     * @param act activation to combine with
     * @return the new activation compromising the given activation
     */
    public abstract double compose(DataRow row, double act);

    /**
     * @return <i>don't know</i> class probability
     */
    public final double getDontKnowClassDegree() {
        return m_dontKnowDegree;
    }

    /**
     * @return class label
     */
    public final DataCell getClassLabel() {
        return m_classLabel;
    }
    
    /**
     * @return Number of pattern of this class used for training.
     */
    public int getNumPattern() {
        return m_numPatOfClass;
    }

    /**
     * Returns the number of covered input pattern.
     * 
     * @return the current number of covered input pattern
     */
    public final int getNumAllCoveredPattern() {
        return m_correctCovered + m_wrongCovered;
    }

    /**
     * Returns the number of correctly covered data pattern.
     * 
     * @return the current number of covered input pattern
     */
    public final int getNumCorrectCoveredPattern() {
        return m_correctCovered;
    }

    /**
     * Returns the number of wrong covered data pattern.
     * 
     * @return the current number of covered input pattern
     */
    public final int getNumWrongCoveredPattern() {
        return m_wrongCovered;
    }

    /**
     * Resets all covered pattern. Called by the learner only.
     */
    final void resetCoveredPattern() {
        m_correctCovered = 0;
        m_wrongCovered = 0;
    }
    
    /**
     * @return The ratio of the number of all (correctly and wrong) covered 
     *         pattern to all pattern of this class 
     */
    public final double getSupport() {
        return getNumAllCoveredPattern() / m_numPatOfClass; 
    }
    
    /**
     * @return The ratio of the number of all correctly covered pattern to all 
     *         pattern of this class 
     */
    public final double getConfidence() {
        return getNumCorrectCoveredPattern() / m_numPatOfClass; 
    }

    /**
     * @return row key for this row
     */
    public final DataCell getId() {
        return m_key;
    }

    /**
     * Saves this row into a model content.
     * 
     * @param pp the model content to save this row to
     */
    protected void save(final ModelContentWO pp) {
        pp.addDataCell("row_id", m_key);
        pp.addDataCell("class_label", m_classLabel);
        pp.addDouble("dont_know_class", m_dontKnowDegree);
        pp.addInt("correct_covered", m_correctCovered);
        pp.addInt("wrong_covered", m_wrongCovered);
        pp.addInt("number_pattern_class", m_numPatOfClass);
    }
}
