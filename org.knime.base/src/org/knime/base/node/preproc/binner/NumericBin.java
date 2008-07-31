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
