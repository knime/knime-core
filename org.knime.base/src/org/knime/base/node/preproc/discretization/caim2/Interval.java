/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   23.10.2006 (sieb): created
 */
package org.knime.base.node.preproc.discretization.caim2;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;

/**
 * Represents a double interval.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class Interval {

    private static final String CONFIG_KEY_LEFT_BOUND = "LeftBound";

    private static final String CONFIG_KEY_RIGHT_BOUND = "RightBound";

    private static final String CONFIG_KEY_INCLUDE_LEFT = "IncludeLeft";

    private static final String CONFIG_KEY_INCLUDE_RIGHT = "IncludeRight";

    private double m_leftBound;

    private double m_rightBound;

    private boolean m_includeLeft;

    private boolean m_includeRight;

    /**
     * Creates an interval from its boundaries and whether the boundaries belong
     * to the interval or not.
     * 
     * @param leftBound the left bound of the interval
     * @param rightBound the right bound of the interval
     * @param includeLeft whether the left bound is included in the interval
     * @param includeRight whether the right bound is included in the interval
     */
    public Interval(final double leftBound, final double rightBound,
            final boolean includeLeft, final boolean includeRight) {

        if (leftBound > rightBound) {
            throw new IllegalArgumentException(
                    "Left bound must be smaller or equal to right bound.");
        }
        m_leftBound = leftBound;
        m_rightBound = rightBound;
        m_includeLeft = includeLeft;
        m_includeRight = includeRight;
    }

    /**
     * Creates an interval from its boundaries and whether the boundaries belong
     * to the interval or not.
     * 
     * @param content the {@link Config} object to create this interval from
     * @throws InvalidSettingsException thrown if the settings to restore are
     *             invalid
     */
    public Interval(final Config content) throws InvalidSettingsException {

        m_leftBound = content.getDouble(CONFIG_KEY_LEFT_BOUND);
        m_rightBound = content.getDouble(CONFIG_KEY_RIGHT_BOUND);
        m_includeLeft = content.getBoolean(CONFIG_KEY_INCLUDE_LEFT);
        m_includeRight = content.getBoolean(CONFIG_KEY_INCLUDE_RIGHT);

    }

    /**
     * @return wheter the left bound is included in the interval
     */
    public boolean isIncludeLeft() {
        return m_includeLeft;
    }

    /**
     * @return wheter the right bound is included in the interval
     */
    public boolean isIncludeRight() {
        return m_includeRight;
    }

    /**
     * @return the left bound
     */
    public double getLeftBound() {
        return m_leftBound;
    }

    /**
     * @return the right bound
     */
    public double getRightBound() {
        return m_rightBound;
    }

    /**
     * Compares the left bound of this interval to the left bound of the given
     * interval. If this left bound is smaller, equal or greater a negative,
     * zero or positive integer is returned. Also the include property is
     * respected
     * 
     * @param intervalToCompare the interval to compare to this interval
     * @return a negativ, zero or positive integer if this left bound is smaler
     *         equal or bigger than the left one to compare to
     */
    public int compareLeftBoundToLeft(final Interval intervalToCompare) {

        if (m_leftBound < intervalToCompare.m_leftBound) {
            return -1;
        }
        if (m_leftBound == intervalToCompare.m_leftBound) {
            if (m_includeLeft && intervalToCompare.m_includeLeft) {
                return 0;
            }
            if (m_includeLeft) {
                return 1;
            }

            // else
            return -1;
        }

        // in any other case this is bigger
        return 1;
    }

    /**
     * Compares the right bound of this interval to the right bound of the given
     * interval. If this right bound is smaller, equal or greater a negative,
     * zero or positive integer is returned. Also the include property is
     * respected
     * 
     * @param intervalToCompare the interval to compare to this interval
     * @return a negativ, zero or positive integer if this right bound is smaler
     *         equal or bigger than the right one to compare to
     */
    public int compareRightBoundToRight(final Interval intervalToCompare) {

        if (m_rightBound < intervalToCompare.m_rightBound) {
            return -1;
        }
        if (m_rightBound == intervalToCompare.m_rightBound) {
            if (m_includeLeft && intervalToCompare.m_includeLeft) {
                return 0;
            }
            if (m_includeRight) {
                return 1;
            }

            // else
            return -1;
        }

        // in any other case this is bigger
        return 1;
    }

    /**
     * Compares the right bound of this interval to the left bound of the given
     * interval. If this right bound is smaller, equal or greater a negative,
     * zero or positive integer is returned. Also the include property is
     * respected
     * 
     * @param intervalToCompare the interval to compare to this interval
     * @return a negativ, zero or positive integer if this right bound is smaler
     *         equal or bigger than the left one of the interval to compare to
     */
    public int compareRightBoundToLeft(final Interval intervalToCompare) {

        if (m_rightBound < intervalToCompare.m_leftBound) {
            return -1;
        }
        if (m_rightBound == intervalToCompare.m_leftBound) {
            if (m_includeRight && intervalToCompare.m_includeLeft) {
                return 0;
            }
            if (m_includeRight) {
                return 1;
            }

            // else
            return -1;
        }

        // in any other case this is bigger
        return 1;
    }

    /**
     * Compares the left bound of this interval to the right bound of the given
     * interval. If this left bound is smaller, equal or greater a negative,
     * zero or positive integer is returned. Also the include property is
     * respected
     * 
     * @param intervalToCompare the interval to compare to this interval
     * @return a negativ, zero or positive integer if this left bound is smaler
     *         equal or bigger than the right one of the interval to compare to
     */
    public int compareLeftBoundToRight(final Interval intervalToCompare) {

        if (m_leftBound < intervalToCompare.m_rightBound) {
            return -1;
        }
        if (m_leftBound == intervalToCompare.m_rightBound) {
            if (m_includeLeft && intervalToCompare.m_includeRight) {
                return 0;
            }
            if (m_includeLeft) {
                return 1;
            }

            // else
            return -1;
        }

        // in any other case this is bigger
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (m_includeLeft) {
            sb.append("[");
        } else {
            sb.append("(");
        }

        sb.append(m_leftBound).append(",").append(m_rightBound);

        if (m_includeRight) {
            sb.append("]");
        } else {
            sb.append(")");
        }

        return sb.toString();
    }

    /**
     * Checks if the given value is located within this interval or not.
     * 
     * @param value the value to check
     * @return true if the given value is located within the interval
     */
    public boolean contains(final double value) {
        if (value > m_leftBound && value < m_rightBound) {
            return true;
        }

        // check the border cases
        if (value == m_leftBound && m_includeLeft) {
            return true;
        }

        if (value == m_rightBound && m_includeRight) {
            return true;
        }

        // else the value is outside the interval
        return false;
    }

    /**
     * Saves this interval to a {@link org.knime.core.node.ModelContentWO} 
     * object.
     * 
     * @param modelContent the {@link Config} object to store the
     *            {@link Interval} to
     */
    public void saveToModelContent(final Config modelContent) {

        modelContent.addDouble(CONFIG_KEY_LEFT_BOUND, m_leftBound);
        modelContent.addDouble(CONFIG_KEY_RIGHT_BOUND, m_rightBound);
        modelContent.addBoolean(CONFIG_KEY_INCLUDE_LEFT, m_includeLeft);
        modelContent.addBoolean(CONFIG_KEY_INCLUDE_RIGHT, m_includeRight);
    }
}
