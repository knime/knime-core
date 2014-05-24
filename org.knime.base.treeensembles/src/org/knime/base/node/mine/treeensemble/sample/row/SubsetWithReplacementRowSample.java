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
 *   Jan 2, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.sample.row;

import org.apache.commons.math.random.RandomData;

/**
 * 
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class SubsetWithReplacementRowSample implements RowSample {

    private final int[] m_perRowCounts;

    /**
     *  */
    public SubsetWithReplacementRowSample(final int nrRows, final double fraction, final RandomData rd) {
        m_perRowCounts = new int[nrRows];
        int subsetSize = fraction >= 1.0 ? nrRows : (int)Math.round(fraction * nrRows);
        for (int i = 0; i < subsetSize; i++) {
            int next = rd.nextInt(0, nrRows - 1);
            m_perRowCounts[next] += 1;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getNrRows() {
        return m_perRowCounts.length;
    }

    /** {@inheritDoc} */
    @Override
    public int getCountFor(final int rowIndex) {
        return m_perRowCounts[rowIndex];
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        int max = 0;
        int sum = 0;
        int nonIncluded = 0;
        for (int i : m_perRowCounts) {
            max = Math.max(max, i);
            sum += i;
            nonIncluded += (i == 0) ? 1 : 0;
        }
        StringBuilder b = new StringBuilder("Subset w/ repl");
        b.append("; nrRows: ").append(m_perRowCounts.length);
        b.append(", max occurrence: ").append(max);
        b.append(", sum occurrence: ").append(sum);
        b.append(", #not included: ").append(nonIncluded);
        return b.toString();
    }

}
