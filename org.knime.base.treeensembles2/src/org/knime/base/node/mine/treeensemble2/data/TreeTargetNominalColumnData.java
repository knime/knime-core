/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 7, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import org.knime.base.node.mine.treeensemble2.data.memberships.DataMemberships;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.core.data.RowKey;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeTargetNominalColumnData extends TreeTargetColumnData {

    final int[] m_data;

    /**
     * Standard constructor for this class
     *
     * @param metaData
     * @param rowKeysAsArray
     * @param data
     */
    public TreeTargetNominalColumnData(final TreeTargetNominalColumnMetaData metaData, final RowKey[] rowKeysAsArray, final int[] data) {
        super(metaData, rowKeysAsArray);
        m_data = data;
    }

    /**
     * Calculates the distribution of the target column for the given row weights.
     *
     * @param rowWeights
     * @param config
     * @return ClassificationPriors
     */
    public ClassificationPriors
        getDistribution(final double[] rowWeights, final TreeEnsembleLearnerConfiguration config) {
        NominalValueRepresentation[] nominalValues = getMetaData().getValues();
        double[] result = new double[nominalValues.length];
        for (int i = 0; i < m_data.length; i++) {
            final double weight = rowWeights[i];
            if (weight < EPSILON) {
                // ignore record: not in branch or record not included
            } else {
                result[m_data[i]] += weight;
            }
        }
        double totalSum = 0.0;
        int majorityIndex = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < result.length; i++) {
            double d = result[i];
            if (d > max) { // strictly larger, see also TreeNode
                max = d;
                majorityIndex = i;
            }
            totalSum += d;
        }
        return new ClassificationPriors(getMetaData(), result, totalSum, majorityIndex,
            config.createImpurityCriterion());
    }

    /**
     * Calculates the distribution of the target column for the given row weights.
     *
     * @param dataMemberships Provides information which rows are included
     * @param config
     * @return ClassificationPriors
     */
    public ClassificationPriors getDistribution(final DataMemberships dataMemberships, final TreeEnsembleLearnerConfiguration config) {
        NominalValueRepresentation[] nominalValues = getMetaData().getValues();
        final double[] result = new double[nominalValues.length];
        final double[] weights = dataMemberships.getRowWeights();
        final int[] indexInOriginal = dataMemberships.getOriginalIndices();
        for (int i = 0; i < weights.length; i++) {
            final double weight = weights[i];
            assert (weight > EPSILON) : "Instances in the dataMemberships object should have weights larger than EPSILON!";
            result[m_data[indexInOriginal[i]]] += weight;
        }
        double totalSum = 0.0;
        int majorityIndex = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < result.length; i++) {
            double d = result[i];
            if (d > max) {
                max = d;
                majorityIndex = i;
            }
            totalSum += d;
        }
        return new ClassificationPriors(getMetaData(), result, totalSum, majorityIndex, config.createImpurityCriterion());
    }

    /**
     * @param row
     * @return The nominal target value for row <b>row</b>
     */
    public int getValueFor(final int row) {
        return m_data[row];
    }

    /** {@inheritDoc} */
    @Override
    public TreeTargetNominalColumnMetaData getMetaData() {
        return (TreeTargetNominalColumnMetaData)super.getMetaData();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        TreeTargetNominalColumnMetaData metaData = getMetaData();
        NominalValueRepresentation[] values = metaData.getValues();
        StringBuilder b = new StringBuilder(metaData.getAttributeName());
        b.append(" [");
        int length = Math.min(100, m_data.length);
        for (int i = 0; i < length; i++) {
            b.append(i > 0 ? ", " : "");
            b.append(values[m_data[i]].getNominalValue());
        }
        b.append(length < m_data.length ? ", ...]" : "]");
        return b.toString();
    }

}
