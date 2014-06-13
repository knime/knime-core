/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   04.06.2014 (koetter): created
 */
package org.knime.core.data.vector.bitvector;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * Represents the supported BitVector types.
 * @author Tobias Koetter
 * @since 2.10
 */
public enum BitVectorType implements ButtonGroupEnumInterface {

    /**Dense bit vector type.*/
    DENSE("Dense", "Standard option recommended for dense vectors e.g. with more than 10% set bits", true,
        DenseBitVectorCell.TYPE),
    /**Sparse bit vector type.*/
    SPARSE("Sparse", "Option recommended for sparse vectors e.g. less than 10% set bits", false,
        SparseBitVectorCell.TYPE);

    private final String m_label;
    private final String m_tooltip;
    private final boolean m_isDefault;
    private final DataType m_cellType;

    private BitVectorType(final String label, final String tooltip, final boolean isDefault,
        final DataType cellDataType) {
        m_label = label;
        m_tooltip = tooltip;
        m_isDefault = isDefault;
        m_cellType = cellDataType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return m_label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getActionCommand() {
        return name();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTip() {
        return m_tooltip;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDefault() {
        return m_isDefault;
    }

    /**
     * @param action the action
     * @return the corresponding {@link BitVectorType}
     */
    public static BitVectorType getType(final String action) {
        return BitVectorType.valueOf(action);
    }

    /**
     * @return the default {@link BitVectorType} to use
     */
    public static BitVectorType getDefault() {
        for (BitVectorType type : values()) {
            if (type.isDefault()) {
                return type;
            }
        }
        throw new IllegalStateException("No default vector type defined");
    }

    /**
     * @return the {@link DataType} of the resulting data cell
     */
    public DataType getCellDataType() {
        return m_cellType;
    }

    /**
     * Initializes the created bit vector from the hex representation in the passed string. Only characters
     * <code>'0' - '9'</code> and <code>'A' - 'F'</code> are allowed. The character at string position
     * <code>(length - 1)</code> represents the bits with index 0 to 3 in the vector. The character at position 0
     * represents the bits with the highest indices. The length of the vector created is the length of the string times
     * 4 (as each character represents four bits).
     *
     * @param hexString containing the hex value to initialize the vector with
     * @return {@link BitVectorCellFactory} initialized by the given hex string
     * @throws IllegalArgumentException if <code>hexString</code> contains characters other then the hex characters
     *             (i.e. <code>0 - 9, A - F</code>)
     */
    public BitVectorCellFactory<? extends DataCell> getCellFactory(final String hexString) {
        switch (this) {
            case DENSE:
                return new DenseBitVectorCellFactory(hexString);
            case SPARSE:
                return new SparseBitVectorCellFactory(hexString);
        }
        //use the dense bit vector as default
        return new DenseBitVectorCellFactory(hexString);
    }

    /**
     * Returns the BitVectorCellFactory of this type with the given length and all bits clear.
     *
     * @param length of the vector in the cell to create
     * @return the {@link BitVectorCellFactory} for this type
     */
    public BitVectorCellFactory<? extends DataCell> getCellFactory(final long length) {
        switch (this) {
            case DENSE:
                return new DenseBitVectorCellFactory(length);
            case SPARSE:
                return new SparseBitVectorCellFactory(length);
        }
        //use the dense bit vector as default
        return new DenseBitVectorCellFactory(length);
    }
}