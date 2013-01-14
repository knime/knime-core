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
 * History
 *   11.05.2006 (gabriel): created
 */
package org.knime.base.node.preproc.binner;

import org.knime.base.node.preproc.binner.BinnerColumnFactory.Bin;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Delegates bin access function to lokal structure.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class NumericBin implements Bin {
    /** Key for left value interval. */
    private static final String LEFT_VALUE = "left_value";

    /** Key for right value interval. */
    private static final String RIGHT_VALUE = "right_value";

    /** Key for left open interval. */
    private static final String LEFT_OPEN = "left_open";

    /** Key for right open interval. */
    private static final String RIGHT_OPEN = "right_open";

    /** Key for the bin's name. */
    private static final String BIN_NAME = "bin_name";

    private final String m_binName;

    private final boolean m_leftOpen;

    private final double m_leftValue;

    private final boolean m_rightOpen;

    private final double m_rightValue;

    /**
     * 
     * @param binName the bin's name
     * @param leftOpen <code>true</code> if left interval is open
     * @param leftValue the left interval value
     * @param rightOpen <code>true</code> if the right interval is open
     * @param rightValue the right interval value
     */
    public NumericBin(final String binName, final boolean leftOpen,
            final double leftValue, final boolean rightOpen,
            final double rightValue) {
        m_binName = binName;
        m_leftOpen = leftOpen;
        m_leftValue = leftValue;
        m_rightOpen = rightOpen;
        m_rightValue = rightValue;
    }

    /**
     * {@inheritDoc}
     */
    public String getBinName() {
        return m_binName;
    }

    /**
     * @return if left interval is open
     */
    public boolean isLeftOpen() {
        return m_leftOpen;
    }

    /**
     * @return left interval border
     */
    public double getLeftValue() {
        return m_leftValue;
    }

    /**
     * @return if right interval is open
     */
    public boolean isRightOpen() {
        return m_rightOpen;
    }

    /**
     * @return right interval border
     */
    public double getRightValue() {
        return m_rightValue;
    }

    /**
     * @param cell the cell to check coverage
     * @return <code>true</code>, if interval covers the given value
     * @throws ClassCastException if the cell is not of type {@link DoubleValue}
     */
    public boolean covers(final DataCell cell) {
        if (cell.isMissing()) {
            return false;
        }
        double value = ((DoubleValue)cell).getDoubleValue();
        double l = getLeftValue();
        double r = getRightValue();
        assert (l <= r);
        if (l < value && value < r) {
            return true;
        }
        if (l == value && !isLeftOpen()) {
            return true;
        }
        if (r == value && !isRightOpen()) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void saveToSettings(final NodeSettingsWO bin) {
        bin.addString(BIN_NAME, getBinName());
        bin.addBoolean(LEFT_OPEN, isLeftOpen());
        bin.addDouble(LEFT_VALUE, getLeftValue());
        bin.addBoolean(RIGHT_OPEN, isRightOpen());
        bin.addDouble(RIGHT_VALUE, getRightValue());
    }

    /**
     * Create numeric bin from NodeSettings.
     * 
     * @param bin read settings from
     * @throws InvalidSettingsException if slots could not be read
     */
    NumericBin(final NodeSettingsRO bin) throws InvalidSettingsException {
        this(bin.getString(BIN_NAME), bin.getBoolean(LEFT_OPEN), bin
                .getDouble(LEFT_VALUE), bin.getBoolean(RIGHT_OPEN), bin
                .getDouble(RIGHT_VALUE));
    }
}
