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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 */

package org.knime.base.node.preproc.matcher;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class MismatchCounter {

    private final int m_maxMismatches;

    private int m_mismatches = 0;


    /**Constructor for class MismatchCounter.
     * @param maxMismatches maximum number of allowed mismatches
     */
    public MismatchCounter(final int maxMismatches) {
        if (maxMismatches < 0) {
            throw new IllegalArgumentException(
                    "Max missmatches should be positive: " + maxMismatches);
        }
        m_maxMismatches = maxMismatches;
    }

    /**Constructor for class MismatchCounter.
     * @param maxMismatches the maximum number of mismatches
     * @param mismatches the current mismatches
     */
    MismatchCounter(final int maxMismatches, final int mismatches) {
        m_maxMismatches = maxMismatches;
        m_mismatches = mismatches;
    }

    /**
     * Increments the mismatch counter.
     * @return <code>true</code> if the the number of mismatches is below
     * or equal the maximum allowed number of mismatches
     */
    public boolean mismatch() {
        m_mismatches++;
        return mismatchesLeft();
    }

    /**
     * @return the maxMismatches
     */
    public int getMaxMismatches() {
        return m_maxMismatches;
    }

    /**
     * @return the mismatches
     */
    public int getMismatches() {
        return m_mismatches;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "(" + m_mismatches + "/" + m_maxMismatches + ")";
    }

    /**
     * @return <code>true</code> if some mismatches are left otherwise
     * <code>false</code>
     */
    public boolean mismatchesLeft() {
        return m_mismatches <= m_maxMismatches;
    }

    /**
     * @return a copy of this {@link MismatchCounter} object with all of it
     * current variable values
     */
    public MismatchCounter copy() {
        return new MismatchCounter(m_maxMismatches, m_mismatches);
    }
}
