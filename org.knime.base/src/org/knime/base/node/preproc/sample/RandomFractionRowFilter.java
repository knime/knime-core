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

import java.util.Random;

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Row Filter class that extracts randomly a given fraction of rows. The
 * implementation of the {@link #matches(DataRow, int)} method only tests if a
 * random number is less or equal to the fraction argument and if so it will
 * return <code>true</code>, i.e. will accept the row currently requested.
 * 
 * <p>
 * The implementation ensures that cloned objects from this object have the same
 * behaviour (presuming that the {@link #matches(DataRow, int)} method is called
 * in the same sequence)
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class RandomFractionRowFilter extends RowFilter {
    /** We need the seed when this object is cloned. */
    private final long m_seed;

    /** The random object, not final since clone writes this field. */
    private Random m_rand;

    /** The fraction to use. */
    private final double m_fraction;

    /**
     * Creates new Filter that filters out (1-fraction) * 100 percent of the
     * rows.
     * 
     * @param fraction the fraction of the rows to surive
     */
    public RandomFractionRowFilter(final double fraction) {
        if (fraction < 0.0 || fraction > 1.0) {
            throw new IllegalArgumentException("Fraction not in [0, 1]: "
                    + fraction);
        }
        m_fraction = fraction;
        m_seed = System.currentTimeMillis();
        m_rand = new Random(m_seed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {
        return m_rand.nextDouble() <= m_fraction;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() {
        RandomFractionRowFilter clone = (RandomFractionRowFilter)super.clone();
        clone.m_rand = new Random(m_seed);
        return clone;
    }
}
