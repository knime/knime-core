/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 * History
 *   May 20, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.learner;

import org.knime.base.node.mine.treeensemble2.data.TreeColumnData;

/**
 * 
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class GiniImpurity implements IImpurity {

    public static final GiniImpurity INSTANCE = new GiniImpurity();

    private GiniImpurity() {
        // singleton
    }

    /** {@inheritDoc} */
    @Override
    public double getPartitionImpurity(final double[] targetCounts, final double partitionWeight) {
        if (partitionWeight < TreeColumnData.EPSILON) {
            return 0.0;
        }
        double result = 0.0;
        for (int i = 0; i < targetCounts.length; i++) {
            double prob = targetCounts[i] / partitionWeight;
            result += prob * prob;
        }
        return 1.0 - result;
    }

    /** {@inheritDoc} */
    @Override
    public double getPostSplitImpurity(final double[] partitionValues, final double[] partitionWeights,
        final double totalWeight) {
        if (totalWeight < TreeColumnData.EPSILON) {
            return 0.0;
        }
        double result = 0.0;
        for (int i = 0; i < partitionValues.length; i++) {
            result += partitionWeights[i] * partitionValues[i];
        }
        return result / totalWeight;
    }

    /** {@inheritDoc} */
    @Override
    public double getGain(final double priorImpurity, final double postSplitImpurity, final double[] partitionWeights,
        final double totalWeight) {
        return priorImpurity - postSplitImpurity;
    }

}
