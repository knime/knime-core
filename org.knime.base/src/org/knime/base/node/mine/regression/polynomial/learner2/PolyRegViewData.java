/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 * ---------------------------------------------------------------------
 *
 * Created on 08.10.2013 by thor
 */
package org.knime.base.node.mine.regression.polynomial.learner2;

import java.util.Map;

import org.knime.base.node.util.DataArray;
import org.knime.core.util.Pair;

/**
 * Simple container class that combines all data that is relevant for the two views in one object.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
class PolyRegViewData {
    final double[] meanValues;

    final double[] betas;

    final double[] m_stdErrs, m_tValues, m_pValues;

    final double squaredError, m_adjustedR2;

    final String[] columnNames;

    final int degree;

    final String targetColumn;

    private final DataArray m_rowContainer;

    @SuppressWarnings("hiding")
    PolyRegViewData(final double[] meanValues, final double[] betas, final double[] stdErrs, final double[] tValues, final double[] pValues, final double squaredError, final double adjustedR2,
        final String[] columnNames, final int degree, final String targetColumn, final DataArray rowContainer) {
        this.meanValues = meanValues;
        this.betas = betas;
        m_rowContainer = rowContainer;
        this.m_stdErrs = stdErrs.clone();
        this.m_tValues = tValues.clone();
        this.m_pValues = pValues.clone();
        this.squaredError = squaredError;
        this.m_adjustedR2 = adjustedR2;
        this.columnNames = columnNames;
        this.degree = degree;
        this.targetColumn = targetColumn;
    }

    static double[] mapToArray(final Map<Pair<String, Integer>, Double> map, final String[] colNames, final int maxDegree, final double constant) {
        final double[] ret = new double[map.size() + 1];
        assert map.size() == colNames.length * maxDegree;
        for (int c = colNames.length; c-->0;) {
            for(int d = maxDegree; d -->0;) {
                ret[colNames.length * d + c] = map.get(Pair.create(colNames[c], d + 1)).doubleValue();
            }
        }
        ret[ret.length - 1] = constant;
        return ret;
    }

    /**
     * @return the rowContainer
     */
    final DataArray getRowContainer() {
        return m_rowContainer;
    }
}
