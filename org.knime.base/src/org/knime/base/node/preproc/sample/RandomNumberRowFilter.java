/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.preproc.sample;

import java.util.BitSet;

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * RowFilter implementation that fiters out rows according to a
 * {@link java.util.BitSet} where each bit represents a row number.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class RandomNumberRowFilter extends RowFilter {
    /** Lookup set. */
    private final BitSet m_bitSet;

    /**
     * Creates new filter according to filter.
     * 
     * @param bitSet the set from which to retrieve the information which row is
     *            to filter out. Bits set to one will let the corresponding row
     *            "survive".
     */
    public RandomNumberRowFilter(final BitSet bitSet) {
        if (bitSet == null) {
            throw new NullPointerException("BitSet must not be null.");
        }
        m_bitSet = bitSet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {
        if (m_bitSet.nextSetBit(rowIndex) < 0) {
            throw new EndOfTableException();
        }
        return m_bitSet.get(rowIndex);
    }

    /**
     * Throws exception, not supported.
     * 
     * @see RowFilter#loadSettingsFrom(NodeSettingsRO)
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        throw new IllegalStateException("not implemented");
    }

    /**
     * Throws exception, not supported.
     * 
     * @see RowFilter#saveSettings(NodeSettingsWO)
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        throw new IllegalStateException("not implemented");
    }

    /**
     * Throws exception, not supported.
     * 
     * @see RowFilter#configure(org.knime.core.data.DataTableSpec)
     */
    @Override
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        throw new IllegalStateException("not implemented");
    }
}
