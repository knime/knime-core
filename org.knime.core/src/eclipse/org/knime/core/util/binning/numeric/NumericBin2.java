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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
package org.knime.core.util.binning.numeric;

import java.util.Objects;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Bin implementation containing a single contiguous interval.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @since 5.6
 */
public final class NumericBin2 implements Bin {

    private static final String CFG_LEFT_VALUE = "left_value";

    private static final String CFG_RIGHT_VALUE = "right_value";

    private static final String CFG_LEFT_OPEN = "left_open";

    private static final String CFG_RIGHT_OPEN = "right_open";

    private static final String CFG_BIN_NAME = "bin_name";

    private final String m_binName;

    private final boolean m_leftOpen;

    private final double m_leftValue;

    private final boolean m_rightOpen;

    private final double m_rightValue;

    /**
     * Constructs a new {@link NumericBin2} object.
     *
     * @param binName the bin's name
     * @param leftOpen <code>true</code> if left interval is open
     * @param leftValue the left interval value
     * @param rightOpen <code>true</code> if the right interval is open
     * @param rightValue the right interval value
     */
    public NumericBin2(final String binName, final boolean leftOpen, final double leftValue, final boolean rightOpen,
        final double rightValue) {
        m_binName = binName;
        m_leftOpen = leftOpen;
        m_leftValue = leftValue;
        m_rightOpen = rightOpen;
        m_rightValue = rightValue;
    }

    /**
     * Constructs a new {@link NumericBin2} from {@link NodeSettingsRO} object.
     *
     * @param bin read settings from
     * @throws InvalidSettingsException if slots could not be read
     */
    public NumericBin2(final NodeSettingsRO bin) throws InvalidSettingsException {
        this(bin.getString(CFG_BIN_NAME), bin.getBoolean(CFG_LEFT_OPEN), bin.getDouble(CFG_LEFT_VALUE),
            bin.getBoolean(CFG_RIGHT_OPEN), bin.getDouble(CFG_RIGHT_VALUE));
    }

    @Override
    public String getBinName() {
        return m_binName;
    }

    /**
     * Gets whether the left interval is open.
     *
     * @return {@code true} if the left interval is open
     */
    public boolean isLeftOpen() {
        return m_leftOpen;
    }

    /**
     * Gets the left interval border.
     *
     * @return left interval border
     */
    public double getLeftValue() {
        return m_leftValue;
    }

    /**
     * Gets whether the right interval is open.
     *
     * @return {@code true} if the right interval is open
     */
    public boolean isRightOpen() {
        return m_rightOpen;
    }

    /**
     * Gets the right interval border.
     *
     * @return right interval border
     */
    public double getRightValue() {
        return m_rightValue;
    }

    /**
     * Gets whether the cell covers for the given value.
     *
     * @param cell the cell to check coverage
     * @return {@code true} if the interval covers the given value
     * @throws ClassCastException if the cell is not of type {@link DoubleValue}
     */
    @Override
    public boolean covers(final DataCell cell) {
        if (cell.isMissing()) {
            return false;
        }

        return covers(((DoubleValue)cell).getDoubleValue());
    }

    private boolean covers(final double value) {
        final double left = getLeftValue();
        final double right = getRightValue();
        assert (left <= right);

        if (isLeftOpen() && isRightOpen()) {
            return (left < value && value < right);
        } else if (isLeftOpen()) {
            return (left < value && value <= right);
        } else if (isRightOpen()) {
            return (left <= value && value < right);
        } else {
            return (left <= value && value <= right);
        }
    }

    @Override
    public void saveToSettings(final NodeSettingsWO bin) {
        bin.addString(CFG_BIN_NAME, getBinName());
        bin.addBoolean(CFG_LEFT_OPEN, isLeftOpen());
        bin.addDouble(CFG_LEFT_VALUE, getLeftValue());
        bin.addBoolean(CFG_RIGHT_OPEN, isRightOpen());
        bin.addDouble(CFG_RIGHT_VALUE, getRightValue());
    }

    private static char openChar(final boolean open) {
        return open ? '(' : '[';
    }

    private static char closeChar(final boolean open) {
        return open ? ')' : ']';
    }

    @Override
    public String toString() {
        return "%s%s,%s%s".formatted( //
            openChar(isLeftOpen()), //
            getLeftValue(), //
            getRightValue(), //
            closeChar(isRightOpen()) //
        );
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NumericBin2)) {
            return false;
        }
        NumericBin2 other = (NumericBin2)obj;
        return m_binName.equals(other.m_binName) //
            && m_leftOpen == other.m_leftOpen //
            && m_leftValue == other.m_leftValue // NOSONAR
            && m_rightOpen == other.m_rightOpen //
            && m_rightValue == other.m_rightValue; // NOSONAR
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_binName, m_leftOpen, m_leftValue, m_rightOpen, m_rightValue);
    }
}
