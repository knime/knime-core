/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 5, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.data;

import java.text.NumberFormat;

import org.knime.base.node.mine.treeensemble.learner.IImpurity;

/**
 * 
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class PriorDistribution {

    private final double[] m_distribution;

    private final double m_totalSum;

    private final double m_priorImpurity;

    private final int m_majorityIndex;

    private final TreeTargetNominalColumnMetaData m_targetMetaData;

    private final IImpurity m_impurityCriterion;

    /**
     * @param impurityCriterion TODO
     * */
    PriorDistribution(final double[] distribution, final TreeTargetNominalColumnMetaData targetMetaData,
        final IImpurity impurityCriterion) {
        m_distribution = distribution;
        m_targetMetaData = targetMetaData;
        m_impurityCriterion = impurityCriterion;
        assert distribution.length == targetMetaData.getValues().length;
        double totalSum = 0.0;
        int majorityIndex = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < distribution.length; i++) {
            double d = distribution[i];
            if (d > max) { // strictly larger, see also TreeNode
                max = d;
                majorityIndex = i;
            }
            totalSum += d;
        }
        m_totalSum = totalSum;
        m_majorityIndex = majorityIndex;
        m_priorImpurity = impurityCriterion.getPartitionImpurity(distribution, totalSum);
    }

    /** @return the distribution */
    public double[] getDistribution() {
        return m_distribution;
    }

    /** @return the targetMetaData */
    public TreeTargetNominalColumnMetaData getTargetMetaData() {
        return m_targetMetaData;
    }

    /** @return the totalSum */
    public double getTotalSum() {
        return m_totalSum;
    }

    /** @return the impurityCriterion */
    public IImpurity getImpurityCriterion() {
        return m_impurityCriterion;
    }

    /** @return the prior impurity (gini or entropy) */
    public double getPriorImpurity() {
        return m_priorImpurity;
    }

    /** @return the majorityIndex */
    public int getMajorityIndex() {
        return m_majorityIndex;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        NumberFormat format = NumberFormat.getInstance();
        // e.g. "Iris-Setosa (50/150) - prior impurity: 0.93"
        StringBuilder b = new StringBuilder();
        NominalValueRepresentation[] values = m_targetMetaData.getValues();
        b.append("\"").append(values[m_majorityIndex].getNominalValue());
        b.append("\" (").append(format.format(m_distribution[m_majorityIndex]));
        b.append("/").append(format.format(m_totalSum));
        b.append(") - prior impurity: ");
        b.append(format.format(getPriorImpurity()));
        return b.toString();
    }

}
