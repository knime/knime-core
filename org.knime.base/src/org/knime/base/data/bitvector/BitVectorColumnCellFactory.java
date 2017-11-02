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
 * -------------------------------------------------------------------
 *
 * History
 *   15.08.2006 (Fabian Dill): created
 */
package org.knime.base.data.bitvector;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.vector.bitvector.BitVectorType;

/**
 *
 * @author Fabian Dill, University of Konstanz
 * @author Tobias Koetter
 */
public abstract class BitVectorColumnCellFactory extends BitVectorCellFactory {

    private int m_columnIndex;

    private final BitVectorType m_vectorType;

    /**
     * Create new cell factory that provides one column given by newColSpec.
     *
     * @param columnSpec the spec of the new column
     * @param columnIndex index of the column to be replaced
     */
    public BitVectorColumnCellFactory(final DataColumnSpec columnSpec, final int columnIndex) {
        this(BitVectorType.DENSE, columnSpec, columnIndex);
    }

    /**
     * @param vectorType {@link BitVectorType}
     * @param columnSpec the spec of the new column
     * @param columnIndex index of the column to be replaced
     * @since 2.10
     */
    public BitVectorColumnCellFactory(final BitVectorType vectorType, final DataColumnSpec columnSpec,
        final int columnIndex) {
        this(false, vectorType, columnSpec, columnIndex);
    }
    /**
     * @param processConcurrently If to process the rows concurrently (must
     * only be true if there are no interdependency between the rows).
     * @param vectorType {@link BitVectorType}
     * @param columnSpec the spec of the new column
     * @param columnIndex index of the column to be replaced
     * @since 2.10
     * @see #setParallelProcessing(boolean)
     */
    public BitVectorColumnCellFactory(final boolean processConcurrently, final BitVectorType vectorType,
        final DataColumnSpec columnSpec, final int columnIndex) {
        super(processConcurrently, columnSpec);
        m_vectorType = vectorType;
        m_columnIndex = columnIndex;
    }

    /**
     *
     * @return index of the column to replace.
     */
    public int getColumnIndex() {
        return m_columnIndex;
    }

    /**
     * @return the vectorType
     */
    BitVectorType getVectorType() {
        return m_vectorType;
    }
}
