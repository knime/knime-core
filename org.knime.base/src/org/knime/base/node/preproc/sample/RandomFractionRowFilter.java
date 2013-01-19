/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
