/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Jan 7, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.data;

import org.knime.base.node.util.DoubleFormat;
import org.knime.core.data.RowKey;
import org.knime.core.util.Pair;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeTargetNumericColumnData extends TreeTargetColumnData {

    private final double[] m_data;

    TreeTargetNumericColumnData(final TreeTargetNumericColumnMetaData metaData,
            final RowKey[] rowKeysAsArray, final double[] data) {
        super(metaData, rowKeysAsArray);
        m_data = data;
    }

    /** {@inheritDoc} */
    @Override
    public TreeTargetNumericColumnMetaData getMetaData() {
        return (TreeTargetNumericColumnMetaData)super.getMetaData();
    }

    public double getValueFor(final int row) {
        return m_data[row];
    }

    public Pair<Double, Double> getPriorMeanAndSumSquare(
            final double[] memberships) {
        double sum = 0.0;
        double sumSquare = 0.0;
        double count = 0.0;
        for (int i = 0; i < m_data.length; ++i) {
            final double weight = memberships[i];
            if (weight < EPSILON) {
                // not in current branch or in sample
                continue;
            }
            final double d = m_data[i];
            final double v = weight * d;
            sum += v;
            sumSquare += v * d;
            count += weight;
        }
        double mean = sum / count;
        double stddev = sumSquare - (sum * sum) / count;
        return new Pair<Double, Double>(mean, stddev);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        TreeTargetNumericColumnMetaData metaData = getMetaData();
        StringBuilder b = new StringBuilder(metaData.getAttributeName());
        b.append(" [");
        int length = Math.min(100, m_data.length);
        for (int i = 0; i < length; i++) {
            b.append(i > 0 ? ", " : "");
            b.append(DoubleFormat.formatDouble(m_data[i]));
        }
        b.append(length < m_data.length ? ", ...]" : "]");
        return b.toString();
    }

}
