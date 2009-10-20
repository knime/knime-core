/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 * History: 
 *     03.06.2004 (gabriel) created
 */

package org.knime.base.node.mine.bfn;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
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
    private final RowKey m_key;

    /** The class label of this basisfunction. */
    private final DataCell m_classLabel;

    /** The don't know class degree; activation is above this threshold. */
    private final double m_dontKnowDegree;

    /** Number of correctly covered pattern. */
    private int m_correctCovered;

    /** Number of wrong covered pattern. */
    private int m_wrongCovered;

    /** Within-cluster variance. */
    private double m_clusterVariance;

    /**
     * Creates new predictor row.
     * 
     * @param key the key of this row
     * @param classLabel class label of the target attribute
     * @param dontKnowDegree don't know probability
     */
    protected BasisFunctionPredictorRow(final RowKey key,
            final DataCell classLabel, final double dontKnowDegree) {
        m_key = key;
        m_classLabel = classLabel;
        m_dontKnowDegree = dontKnowDegree;
        m_correctCovered = 0;
        m_wrongCovered = 0;
        m_clusterVariance = 0;
    }

    /**
     * Creates new predictor row on model content.
     * 
     * @param pp the model content to read the new predictor row from
     * @throws InvalidSettingsException if the model content is invalid
     */
    public BasisFunctionPredictorRow(final ModelContentRO pp)
            throws InvalidSettingsException {
        RowKey key;
        try {
            // load key before 2.0
            key = new RowKey(pp.getDataCell("row_id").toString());
        } catch (InvalidSettingsException ise) {
            key = new RowKey(pp.getString("row_id"));
        }
        m_key = key;
        m_classLabel = pp.getDataCell("class_label");
        m_dontKnowDegree = pp.getDouble("dont_know_class");
        m_correctCovered = pp.getInt("correct_covered");
        m_wrongCovered = pp.getInt("wrong_covered");
        m_clusterVariance = pp.getDouble("within-cluster_variance", 0);
    }
    
    /**
     * @param row to compute distance with
     * @return computes the distance between this row and the anchor
     */
    public abstract double computeDistance(final DataRow row);
    
    /**
     * Returns a value for the spread of this rule.
     * 
     * @return rule spread value
     */
    public abstract double computeSpread();
    
    /**
     * Computes the overlapping of two basis functions.
     * 
     * @param symmetric if the result is proportional to both basis functions,
     *            and thus symmetric, or if it is proportional to the area of 
     *            the basisfunction on which the function is called.
     * @param bf the other basisfunction to compute overlapping with
     * @return true, if both are overlapping
     */
    public abstract double overlap(final BasisFunctionPredictorRow bf,
            final boolean symmetric);
    
    /**
     * Computes the overlapping based on two lines.
     * 
     * @param minA left point line A
     * @param maxA right point line A
     * @param minB left point line B
     * @param maxB right point line B
     * @param symmetric if the result is proportional to both basis functions,
     *        and thus symmetric, or if it is proportional to the area of the
     *        basis function on which the function is called
     * @return the positive overlapping spread of this two lines or zero if none
     */
    public static final double overlapping(final double minA,
            final double maxA, final double minB, final double maxB,
            final boolean symmetric) {
        assert (minA <= maxA && minB <= maxB);
        if (minA == minB && maxA == maxB) {
            return 1;
        }
        if (maxA < minB) {
            return 0; // maxA - minB;
        }
        if (maxB < minA) {
            return 0; // maxB - minA;
        }
        if (minA < minB) {

            if (maxA < maxB) {
                if (symmetric) {
                    return (maxA - minB + 1) / (maxB - minA + 1);
                } else {
                    return (maxA - minB + 1) / (maxA - minA + 1);
                }

            } else {
                return (maxB - minB + 1) / (maxA - minA + 1);
            }
        } else {
            if (minA == maxA || minB == maxB) {
                return 1;
            }
            if (maxA < maxB) {
                if (symmetric) {
                    return (maxA - minA + 1) / (maxB - minB + 1);
                } else {
                    return 1;
                }
            } else {
                if (symmetric) {
                    return (maxB - minA + 1) / (maxA - minB + 1);
                } else {
                    return (maxB - minA + 1) / (maxA - minA + 1);
                }
            }
        }
    }
    
    /**
     * If the same class as this basisfunction is assigned to, the number of
     * correctly covered pattern is increased, otherwise the number of wrong
     * covered ones.
     * @param row to cover
     * @param classLabel a pattern of the given class has to be covered
     */
    final void cover(final DataRow row, final DataCell classLabel) {
        if (m_classLabel.equals(classLabel)) {
            m_correctCovered++;
        } else {
            m_wrongCovered++;
        }
        double d = computeDistance(row);
        m_clusterVariance += d * d;
    }
    
    /**
     * @return with-in cluster variance
     */
    public final double getVariance() {
        if (m_clusterVariance > 0) {
            return m_clusterVariance / getNumAllCoveredPattern();
        } else {
            return 0;
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
     * @return number of features that have been shrunken
     */
    public abstract int getNrUsedFeatures();

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
        m_clusterVariance = 0;
    }

    /**
     * @return row key for this row
     */
    public final RowKey getId() {
        return m_key;
    }

    /**
     * Saves this row into a model content.
     * 
     * @param pp the model content to save this row to
     */
    public void save(final ModelContentWO pp) {
        pp.addString("row_id", m_key.getString());
        pp.addDataCell("class_label", m_classLabel);
        pp.addDouble("dont_know_class", m_dontKnowDegree);
        pp.addInt("correct_covered", m_correctCovered);
        pp.addInt("wrong_covered", m_wrongCovered);
        pp.addDouble("within-cluster_variance", m_clusterVariance);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_key.getString();
    }
}
