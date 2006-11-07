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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * Class presents a predictor row for basis functions providing method to apply
 * unknown data.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionPredictorRow {
    private final DataCell m_key;

    private final DataCell m_classLabel;

    private final double m_dontKnowDegree;

    /** Keeps the data cell ids of all pattern covered by this basisfunction. */
    private Map<DataCell, Set<DataCell>> m_coveredPattern = 
        new LinkedHashMap<DataCell, Set<DataCell>>();

    private int m_correctCovered = 0;

    private int m_wrongCovered = 0;
    
    private final int m_numPat;

    /**
     * Creates new predictor row.
     * 
     * @param key the key of this row
     * @param classLabel class label of the target attribute
     * @param dontKnowDegree don't know probability
     * @param numPat The overall number of pattern used for training. 
     */
    protected BasisFunctionPredictorRow(final DataCell key,
            final DataCell classLabel, final int numPat, 
            final double dontKnowDegree) {
        m_key = key;
        m_classLabel = classLabel;
        m_dontKnowDegree = dontKnowDegree;
        m_numPat = numPat;
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
        m_numPat = pp.getInt("number_pattern", 
                m_correctCovered + m_wrongCovered);
        ModelContentRO coveredContent = pp.getModelContent("covered_instances");
        DataCell[] classes = coveredContent.getDataCellArray("classes");
        for (int i = 0; i < classes.length; i++) {
            DataCell[] covCells = coveredContent.getDataCellArray(classes[i]
                    .toString());
            m_coveredPattern.put(classes[i], new LinkedHashSet<DataCell>(Arrays
                    .asList(covCells)));
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
     * @param act1 activation 1
     * @param act2 activation 2
     * @return the new activation compromising the given activation
     */
    public abstract double compose(double act1, double act2);

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
     * @return Number of pattern used for training.
     */
    public int getNumPattern() {
        return m_numPat;
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
        m_coveredPattern.clear();
        m_correctCovered = 0;
        m_wrongCovered = 0;
    }
    
    /**
     * @return The ratio of the number of all (correctly and wrong) covered 
     *         pattern to all pattern used for training. 
     */
    public final double getSupport() {
        return getNumAllCoveredPattern() / m_numPat; 
    }
    
    /**
     * @return The ratio of the number of all correctly covered pattern to all 
     *         pattern used for training. 
     */
    public final double getConfidence() {
        return getNumCorrectCoveredPattern() / m_numPat; 
    }

    /**
     * If a new instance of this class is covered.
     * 
     * @param key the instance's key
     * @param classInfo and class.
     */
    public final void addCovered(final DataCell key, final DataCell classInfo) {
        Set<DataCell> covSet;
        if (m_coveredPattern.containsKey(classInfo)) {
            covSet = m_coveredPattern.get(classInfo);
        } else {
            covSet = new LinkedHashSet<DataCell>();
            m_coveredPattern.put(classInfo, covSet);
        }
        covSet.add(key);
        if (m_classLabel.equals(classInfo)) {
            m_correctCovered++;
        } else {
            m_wrongCovered++;
        }
    }

    /**
     * Returns a set which contains all input training pattern covered by this
     * basis function.
     * 
     * @return set of covered input pattern
     */
    public final Set<DataCell> getAllCoveredPattern() {
        Set<DataCell> allCov = new LinkedHashSet<DataCell>();
        for (DataCell key : m_coveredPattern.keySet()) {
            allCov.addAll(m_coveredPattern.get(key));
        }
        return Collections.unmodifiableSet(allCov);
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
        pp.addInt("number_pattern", m_numPat);
        ModelContentWO covContent = pp.addModelContent("covered_instances");
        covContent.addDataCellArray("classes", m_coveredPattern.keySet()
                .toArray(new DataCell[0]));
        for (DataCell key : m_coveredPattern.keySet()) {
            covContent.addDataCellArray(key.toString(), m_coveredPattern.get(
                    key).toArray(new DataCell[0]));
        }
    }
}
