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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy.membership;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.FuzzyIntervalCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.util.MutableDouble;

/**
 * Trapezoid membership function with four values for support and core left and
 * right values whereby the support region can be defined infinity. The anchor
 * need to be a value within the core-region. If the anchor's value is changed,
 * the core- and support-region is adjusted if necessary. If the core-region
 * changes, the support-region is - if necessary - adjusted. But not the other
 * way around in both cases.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class MembershipFunction {
    
    /** left support. */
    private double m_suppLeft;

    /** left core of core. */
    private double m_coreLeft;

    /** right core of core. */
    private double m_coreRight;

    /** right support. */
    private double m_suppRight;

    /** initial value; anchor. */
    private double m_anchor;

    /** true if this membership function is missing. */
    private boolean m_missing;

    /** true if the left support is max. */
    private boolean m_suppLeftMax;

    /** true if the right support is max. */
    private boolean m_suppRightMax;

    /** left border of the input domain of this feature value. */
    private final MutableDouble m_min;

    /** right border of the input domain of this feature value. */
    private final MutableDouble m_max;

    /** Minimum flag <i>min</i>. */
    static final String MIN_FLAG = "min";

    /** Maximum flag <i>max</i>. */
    static final String MAX_FLAG = "max";

    /**
     * Creates a new membership function based on the given model content.
     * @param pp Reads this membership function's properties from.
     * @throws InvalidSettingsException If the properties can't be read.
     */
    public MembershipFunction(final ModelContentRO pp)
            throws InvalidSettingsException {
        m_missing = pp.getBoolean("is_missing");
        m_min = new MutableDouble(pp.getDouble("min"));
        m_max = new MutableDouble(pp.getDouble("max"));
        if (m_missing) {
            m_anchor = Double.NaN;
            m_suppLeftMax = true; // left side unconstrained
            m_suppRightMax = true; // right side unconstrained
            m_suppLeft = Double.NaN; // not used if left support
                                        // unconstrained
            m_suppRight = Double.NaN; // not used if right support
                                        // unconstrained
        } else {
            m_suppLeftMax = pp.getBoolean("is_support_left_max");
            if (!isSuppLeftMax()) {
                m_suppLeft = pp.getDouble("support_left");
            } else {
                m_suppLeft = Double.NaN;
            }
            m_coreLeft = pp.getDouble("core_left");
            m_anchor = pp.getDouble("anchor");
            m_coreRight = pp.getDouble("core_right");
            m_suppRightMax = pp.getBoolean("is_support_right_max");
            if (!isSuppRightMax()) {
                m_suppRight = pp.getDouble("support_right");
            } else {
                m_suppRight = Double.NaN;
            }
        }
    }

    /**
     * Creates a new trapezoid membership function with its given anchor and two
     * values used to assign the min and max border.
     * 
     * @param anchor the initial center point of this fuzzy function
     * @param min the minimum left border
     * @param max the maximum right border
     */
    public MembershipFunction(final DoubleValue anchor, 
            final MutableDouble min, final MutableDouble max) {
        if (!Double.isNaN(min.doubleValue()) 
                && !Double.isNaN(max.doubleValue())) {
            assert (min.doubleValue() <= max.doubleValue());
        }
        m_min = min;
        m_max = max;
        m_suppLeftMax = true; // left side unconstrained
        m_suppRightMax = true; // right side unconstrained
        m_suppLeft = Double.NaN; // not used if left support unconstrained
        m_suppRight = Double.NaN; // not used if right support unconstrained
        if (anchor != null) {
            m_missing = false;
            m_anchor = anchor.getDoubleValue();
            // core has no spread initially
            m_coreLeft = m_anchor;
            m_coreRight = m_anchor;
            repairMinMax(m_anchor);
        } else {
            m_missing = true;
            assert m_missing;
            m_anchor = Double.NaN;
        }
    }

    /**
     * Minimum and maximum are adapted to the (new) value.
     * @param value The new value for min and max.
     */
    protected final void repairMinMax(final double value) {
        double min = m_min.doubleValue();
        if (Double.isNaN(min) || value < min) {
            m_min.setValue(value);
        }
        double max = m_max.doubleValue();
        if (Double.isNaN(max) || value > max) {
            m_max.setValue(value);
        }
    }

    /**
     * Sets the left support border.
     * 
     * @param value the value to set
     */
    public void setSuppLeft(final double value) {
        repairMinMax(value);
        if (isMissingIntern()) {
            assert (false);
        }
        // sets new left support border
        m_suppLeft = value;
        // support left is constrained
        m_suppLeftMax = false;

        assert (m_coreLeft <= m_anchor && m_anchor <= m_coreRight);
        assert (m_suppLeft <= m_anchor);
        assert (isSuppRightMax() || m_anchor <= m_suppRight);
        assert (m_suppLeft <= m_coreLeft);
    }

    /**
     * Resets the core to the current anchor.
     */
    public void resetCore() {
        if (isMissingIntern()) {
            assert (false);
        }
        m_coreLeft = m_anchor;
        m_coreRight = m_anchor;
    }

    /**
     * Resets the core and anchor to the given value.
     * 
     * @param anchor the new value for the core borders and anchor
     */
    public void setAnchor(final double anchor) {
        repairMinMax(anchor);
        m_anchor = anchor;
        if (isMissingIntern()) {
            m_missing = false;
            m_coreLeft = anchor;
            m_coreRight = anchor;
        } else {
            assert (m_coreLeft <= m_anchor && m_anchor <= m_coreRight);
        }
    }

    /**
     * Sets the left core border.
     * 
     * @param value to set
     */
    public void setCoreLeft(final double value) {
        repairMinMax(value);
        if (isMissingIntern()) {
            assert (false);
        }
        // sets new left core border
        m_coreLeft = value;
        if (!isSuppLeftMax() && m_coreLeft < m_suppLeft) {
            m_suppLeft = m_coreLeft;
        }
        assert (m_coreLeft <= m_anchor && m_anchor <= m_coreRight);
        assert (isSuppLeftMax() || m_suppLeft <= m_anchor);
        assert (isSuppRightMax() || m_anchor < m_suppRight);
    }

    /**
     * Sets the right core border.
     * 
     * @param value to set
     */
    public void setCoreRight(final double value) {
        repairMinMax(value);
        if (isMissingIntern()) {
            assert (false);
        }
        // sets new core right border
        m_coreRight = value;
        if (!isSuppRightMax() && m_coreRight > m_suppRight) {
            m_suppRight = m_coreRight;
        }
        assert (m_coreLeft <= m_anchor && m_anchor <= m_coreRight);
        assert (isSuppLeftMax() || m_suppLeft <= m_anchor);
        assert (isSuppRightMax() || m_anchor < m_suppRight);
    }

    /**
     * Sets the right support border.
     * 
     * @param value to set
     */
    public void setSuppRight(final double value) {
        repairMinMax(value);
        if (isMissingIntern()) {
            assert (false);
        }
        // sets new support right border
        m_suppRight = value;
        // support right border is constrained
        m_suppRightMax = false;

        assert (m_coreLeft <= m_anchor && m_anchor <= m_coreRight);
        assert (isSuppLeftMax() || m_suppLeft <= m_anchor);
        assert (m_anchor < m_suppRight);
        assert (m_suppRight >= m_coreRight);
    }

    // /**
    // * @see DataCell#equalsDataCell(org.knime.core.data.DataCell)
    // */
    // protected boolean equalsDataCell(final DataCell other) {
    // if (this == other) {
    // return true;
    // }
    // if (other == null) {
    // return false;
    // }
    // if (!(other instanceof TrapezoidMembershipFunction)) {
    // return false;
    // }
    // TrapezoidMembershipFunction cell =
    // (TrapezoidMembershipFunction) other;
    // if (cell.isMissingIntern() && isMissingIntern()) {
    // return true;
    // }
    // if (cell.isMissingIntern() || isMissingIntern()) {
    // return false;
    // }
    // if (!compare(m_anchor, cell.m_anchor)) {
    // return false;
    // }
    // if (!compare(m_coreLeft, cell.m_coreLeft)) {
    // return false;
    // }
    // if (!compare(m_coreRight, cell.m_coreRight)) {
    // return false;
    // }
    // if (!compare(m_suppLeft, cell.m_suppLeft)) {
    // return false;
    // }
    // if (!compare(m_suppRight, cell.m_suppRight)) {
    // return false;
    // }
    // return true;
    // }
    //    
    // private static boolean compare(final double a, final double b) {
    // if (Double.isNaN(a) && Double.isNaN(b)) {
    // return true;
    // }
    // if (Double.isNaN(a) || Double.isNaN(b)) {
    // return false;
    // }
    // return a == b;
    // }
    //
    // /**
    // * @see org.knime.core.data.DataCell#hashCode()
    // */
    // public int hashCode() {
    // if (isMissingIntern()) {
    // return DataType.getMissingCell().hashCode();
    // }
    // long bits = Double.doubleToLongBits(getCenterOfGravity());
    // return (int)(bits ^ (bits >>> 32));
    // }

    /**
     * @return a string summary for this membership function.
     */
    @Override
    public String toString() {
        // if (true) {
        // return getMinCore() + " " + getMaxCore();
        // }
        // if (true) {
        // return "" + this.getCenterOfGravity();
        // }
        if (isMissingIntern()) {
            return "<" + MIN_FLAG + ",(?)," + MAX_FLAG + ")>";
        }
        StringBuffer buf = new StringBuffer();
        // if max left border
        if (isSuppLeftMax()) {
            buf.append("<" + MIN_FLAG);
        } else {
            buf.append("<" + getMinSupport());
        }
        // add left core border
        buf.append("," + getMinCore());
        // add anchor, initial value
        buf.append(",(" + getAnchor() + "),");
        // add right core border
        buf.append(getMaxCore() + ",");
        // if max right border
        if (isSuppRightMax()) {
            buf.append(MAX_FLAG + ">");
        } else {
            buf.append(getMaxSupport() + ">");
        }
        return buf.toString();
    }

    // WEKA formatted output
    // public String toString() {
    // if (isSuppLeftMax() && isSuppRightMax()) {
    // return "()";
    // }
    // StringBuffer buf = new StringBuffer();
    // if (!isSuppLeftMax()) {
    // buf.append("(" + getMinSupport() + " , 0.0) - ");
    // buf.append("(" + getMinCore() + " , 1.0)");
    // }
    // if (!isSuppRightMax()) {
    // if (!isSuppLeftMax()) {
    // buf.append(" - ");
    // }
    // buf.append("(" + getMaxCore() + " , 1.0) - ");
    // buf.append("(" + getMaxSupport() + " , 0.0)");
    // }
    // return buf.toString();
    // }

    /**
     * @return <code>true</code> if left support border is unconstrained
     *         otherwise <code>false</code>
     */
    public boolean isSuppLeftMax() {
        return m_suppLeftMax;
    }

    /**
     * @return support left border
     */
    public double getMinSupport() {
        if (isMissingIntern()) {
            assert false;
        }
        if (!m_suppLeftMax) {
            return m_suppLeft;
        } else {
            double min = m_min.doubleValue();
            if (Double.isNaN(min)) {
                return m_coreLeft;
            } else {
                return min;
            }
        }
    }

    /**
     * @return core left border
     */
    public double getMinCore() {
        if (isMissingIntern()) {
            assert (false);
        }
        return m_coreLeft;
    }

    /**
     * @return anchor (initial) value of this membership function
     */
    public double getAnchor() {
        if (isMissingIntern()) {
            assert (false);
        }
        return m_anchor;
    }

    /**
     * @return core right border
     */
    public double getMaxCore() {
        if (isMissingIntern()) {
            assert (false);
        }
        return m_coreRight;
    }

    /**
     * @return support right border
     */
    public double getMaxSupport() {
        if (!m_suppRightMax) {
            return m_suppRight;
        } else {
            double max = m_max.doubleValue();
            if (Double.isNaN(max)) {
                return m_coreRight;
            } else {
                return max;
            }
        }
    }

    /**
     * @return true is right support border is unconstrained otherwise false
     */
    public boolean isSuppRightMax() {
        if (isMissingIntern()) {
            assert (false);
        }
        return m_suppRightMax;
    }

    /**
     * @return suppRight - suppLeft, support spread
     */
    public double getSupport() {
        if (isMissingIntern()) {
            assert (false);
        }
        return (getMaxSupport() - getMinSupport());
    }

    /**
     * @return coreRight - coreLeft, core spread
     */
    public double getCore() {
        if (isMissingIntern()) {
            assert (false);
        }
        return (getMaxCore() - getMinCore());
    }
    
    /**
     * @return min of this membership (not min support)
     */
    public final MutableDouble getMin() {
        return m_min;
    }
    
    /**
     * @return max of this membership (not max support)
     */
    public final MutableDouble getMax() {
        return m_max;
    }

    /**
     * getActivation(.).
     * 
     * @param value x to apply
     * @return calculated fuzzy degree in [0.0,1.0] range
     */
    public final double getActivation(final double value) {

        // check if membership function is missing
        if (isMissingIntern()) {
            return 1.0;
        }

        // if left side of anchor
        if (value < m_anchor) {
            // if left is unconstrained
            if (isSuppLeftMax() || value >= getMinCore()) {
                // degree is 1.0, no changes ... go on
                return 1.0;
            } else {
                // if value is outside support
                if (value < getMinSupport()) {
                    return 0.0;
                } else {
                    // the value is not equal!
                    if (getMinSupport() == getMinCore()) {
                        assert (value < getMinSupport());
                        return 0.0;
                    }
                    // double h = Math.pow(
                    // FuzzyBasisFunctionLearnerRow.MINACT,
                    // (getMinCore() - value)
                    // / (getMinCore() - getMinSupport()));
                    double h = (value - getMinSupport())
                            / (getMinCore() - getMinSupport());
                    assert (h >= 0.0 && h <= 1.0);
                    return h;
                }
            }
        } else {
            // if value is on right side of the anchor
            if (value > m_anchor) {
                // if right side is unconstrained
                if (isSuppRightMax() || value <= getMaxCore()) {
                    // degree is 1.0, no changes ... go on
                    return 1.0;
                } else {
                    // if value is outside support
                    if (value > getMaxSupport()) {
                        return 0.0;
                    } else {
                        // the value is not equal!
                        if (getMaxSupport() == getMaxCore()) {
                            assert (value > getMaxSupport());
                            return 0.0;
                        }
                        // double h = Math.pow(
                        // FuzzyBasisFunctionLearnerRow.MINACT,
                        // (value - getMaxCore())
                        // / (getMaxSupport() - getMaxCore()));
                        double h = (getMaxSupport() - value)
                                / (getMaxSupport() - getMaxCore());
                        assert (h >= 0.0 && h <= 1.0);
                        return h;
                    }
                }
            } else { // value is equal to the anchor
                return 1.0;
            }
        }
    } // getActivation(double)

    /**
     * @return The center of gravity of this trapezoid membership function which
     *         are the weighted (by the area) gravities of each of the three
     *         areas (left triangle, core rectangle, right triangle) whereby the
     *         triangles' gravity point is 2/3 and 1/3 resp. is computed by the
     *         product of the gravity point and area for each interval. This
     *         value is divided by the overall membership function volume.
     */
    public double getCenterOfGravity() {
        if (isMissingIntern()) {
            assert false;
            return Double.NaN;
        }
        // left support
        double a1 = 0.0;
        double s1 = 0.0;
        if (!isSuppLeftMax()) {
            a1 = (getMinCore() - getMinSupport()) / 2.0;
            s1 = getMinSupport() + (getMinCore() - getMinSupport()) * 2.0 / 3.0;
        }
        // core
        double a2 = getMaxCore() - getMinCore();
        double s2 = getMinCore() + (getMaxCore() - getMinCore()) / 2.0;
        // right support
        double a3 = 0.0;
        double s3 = 0.0;
        if (!isSuppRightMax()) {
            a3 = (getMaxSupport() - getMaxCore()) / 2.0;
            s3 = getMaxCore() + (getMaxSupport() - getMaxCore()) / 3.0;
        }
        if (a1 + a2 + a3 == 0.0) {
            assert (getMinCore() == getMaxCore());
            return getMinCore();
        }
        return (a1 * s1 + a2 * s2 + a3 * s3) / (a1 + a2 + a3);
    }

    /**
     * @return <code>true</code> if this membership function is undefined.
     */
    public final boolean isMissingIntern() {
        return m_missing;
    }

    /**
     * Returns a <code>DataCell</code>, either missing if undefined, or a 
     * <code>FuzzyIntervalCell</code> using the membership function properties.
     * Internally used to <i>convert</i> this membership function a final 
     * <code>DataCell</code>. 
     * @return A data cell for this membership function.
     */
    public final DataCell createFuzzyIntervalCell() {
        if (isMissingIntern()) {
            return DataType.getMissingCell();
        }
        double min = (isSuppLeftMax() ? getMinCore() : getMinSupport());
        double max = (isSuppRightMax() ? getMaxCore() : getMaxSupport());
        return new FuzzyIntervalCell(min, m_coreLeft, m_coreRight, max);
    }

    /**
     * Saves this membership function the given model content.
     * @param pp Model content to save properties to.
     */
    public final void save(final ModelContentWO pp) {
        pp.addBoolean("is_missing", isMissingIntern());
        pp.addDouble("min", m_min.doubleValue());
        pp.addDouble("max", m_max.doubleValue());
        if (!isMissingIntern()) {
            pp.addBoolean("is_support_left_max", isSuppLeftMax());
            if (!isSuppLeftMax()) {
                pp.addDouble("support_left", getMinSupport());
            }
            pp.addDouble("core_left", getMinCore());
            pp.addDouble("anchor", getAnchor());
            pp.addDouble("core_right", getMaxCore());
            pp.addBoolean("is_support_right_max", isSuppRightMax());
            if (!isSuppRightMax()) {
                pp.addDouble("support_right", getMaxSupport());
            }
        }
    }
}
