/* --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.config.Config;

/**
 * A column domain object holds information about one column's domain, that are,
 * possible values, and an upper and lower bound - if available.
 * <p>
 * Note: It is crutial that the creator of a column domain ensures that the data
 * filled in is correct. In the sense that no value in the data table will be
 * outside the provided bounds and that no other value appears in that column
 * than the one listed in the array returned by <code>getValues()</code>. It
 * is assumed that the domain - if available - contains reliable data. If you
 * are not sure about the data to come in your column, don't provide a domain.
 * 
 * @author Thomas Gabriel, Konstanz University
 */
public final class DataColumnDomain {

    /**
     * Lower bound value or <code>null</code>.
     */
    private final DataCell m_lowerBound;

    /**
     * Upper bound value or <code>null</code>.
     */
    private final DataCell m_upperBound;

    /**
     * Set of possible values or <code>null</code>.
     */
    private final Set<DataCell> m_values;

    /**
     * Create new column name with lower and upper bounds, and set of possible
     * values. All arguments can be <code>null</code> in case non of these
     * properties are avaiable.
     * 
     * @param lower The lower bound value.
     * @param upper The upper bound value.
     * @param values A set of nominal values.
     */
    DataColumnDomain(final DataCell lower, final DataCell upper,
            final Set<DataCell> values) {
        m_lowerBound = lower;
        m_upperBound = upper;
        m_values = values;
    }

    /**
     * Return all possible values in this column. Note that this array can be
     * <code>null</code> if this information is not available (for continuous
     * double values, for example) and that the <code>DataCell</code>s in the
     * returned array do not have to be of the same type (but the column type
     * should be compatible to all of their values).<br>
     * If the returned array is not null, the corresponding column must not
     * contain any value other than the ones contained in the array. The array
     * could contain a superset of the values in the table though.
     * 
     * @return An array of possible values or <code>null</code>.
     * 
     * @see #hasValues()
     */
    public Set<DataCell> getValues() {
        // in order to keep the domain read-only, return a copy of the set
        if (m_values != null) {
            return Collections.unmodifiableSet(m_values);
        } else {
            return null;
        }
    }

    /**
     * @return true, if this column spec has nominal values defined (i.e. the
     *         <code>getValues</code> method returns a non-null value).
     * 
     * @see #getValues()
     */
    public boolean hasValues() {
        return m_values != null;
    }

    /**
     * Return the lower bound of the domain of this column, if available. Note
     * that this value does not necessarily need to actually occur in the
     * corresponding <code>DataTable</code> but it is describing the range of
     * the domain of this attribute.
     * 
     * @return a DataCell with the lowest possible value or <code>null</code>.
     * 
     * @see #hasLowerBound()
     */
    public DataCell getLowerBound() {
        return m_lowerBound;
    }

    /**
     * @return true, if the lower bound value has been defined.
     * 
     * @see #getLowerBound()
     */
    public boolean hasLowerBound() {
        return m_lowerBound != null;
    }

    /**
     * Return the upper bound of the domain of this column, if available. Note
     * that this value does not necessarily need to actually occur in the
     * corresponding <code>DataTable</code> but it is describing the range of
     * the domain of this attribute.
     * 
     * @return a DataCell with the largest possible value or <code>null</code>.
     * 
     * @see #hasUpperBound()
     */
    public DataCell getUpperBound() {
        return m_upperBound;
    }

    /**
     * @return true, if the upper bound value has been defined.
     * 
     * @see #getUpperBound()
     */
    public boolean hasUpperBound() {
        return m_upperBound != null;
    }

    /**
     * @return true, if lower and upper bound are defined.
     */
    public boolean hasBounds() {
        return this.hasLowerBound() && this.hasUpperBound();
    }

    /**
     * Compares this domain with the other one by the possible values and lower
     * and upper bound.
     * 
     * @param obj The other domain to compare with.
     * @return true if possible values, and lower and upper bounds are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DataColumnDomain)) {
            return false;
        }
        DataColumnDomain domain = (DataColumnDomain)obj;
        if (hasValues() ^ domain.hasValues()) {
            return false;
        }
        if (hasLowerBound() ^ domain.hasLowerBound()) {
            return false;
        }
        if (hasUpperBound() ^ domain.hasUpperBound()) {
            return false;
        }
        boolean ret = true;
        if (hasValues() && domain.hasValues()) {
            ret &= getValues().equals(domain.getValues());
        }
        if (hasLowerBound() && domain.hasLowerBound()) {
            ret &= getLowerBound().equals(domain.getLowerBound());
        }
        if (hasUpperBound() && domain.hasUpperBound()) {
            ret &= getUpperBound().equals(domain.getUpperBound());
        }
        return ret;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int tempHash = 0;
        if (hasLowerBound()) {
            tempHash ^= m_lowerBound.hashCode();
        }
        if (hasUpperBound()) {
            tempHash ^= m_upperBound.hashCode();
        }
        if (hasValues()) {
            for (DataCell cell : m_values) {
                tempHash ^= cell.hashCode();
            }
        }
        return tempHash;
    }

    /**
     * Returns summary of this domain including lower and upper bound, and
     * possible values.
     * 
     * @return Summary as String.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String lower = 
            (m_lowerBound == null ? "null" : m_lowerBound.toString());
        String upper = 
            (m_upperBound == null ? "null" : m_upperBound.toString());
        String values = (m_values == null ? "null" : m_values.toString());
        return "lower=" + lower + ",upper=" + upper + ",values=" + values;
    }

    private static final String CFG_LOWER_BOUND = "lower_bound";
    private static final String CFG_UPPER_BOUND = "upper_bound";
    private static final String CFG_POSS_VALUES = "possible_values";

    /**
     * Save this domain to the given <code>Config</code> including lower and
     * upper bound, and possible values - if available.
     * 
     * @param config The <code>Config</code> to write into.
     */
    public void save(final Config config) {
        assert config.keySet().isEmpty() : "Subconfig must be empty: "
                + Arrays.toString(config.keySet().toArray());
        config.addDataCell(CFG_LOWER_BOUND, m_lowerBound);
        config.addDataCell(CFG_UPPER_BOUND, m_upperBound);
        if (m_values != null) {
            DataCell[] values = m_values.toArray(new DataCell[0]);
            config.addDataCellArray(CFG_POSS_VALUES, values);
        }
    }

    /**
     * Reads lower and upper bound from <code>Config</code>, and possible
     * values if available.
     * 
     * @param config To read entries from.
     * @return A new domain object with the read properties.
     * @throws InvalidSettingsException If lower or upper bound are ot defined.
     */
    public static DataColumnDomain load(final Config config)
            throws InvalidSettingsException {
        DataCell lower = config.getDataCell(CFG_LOWER_BOUND);
        DataCell upper = config.getDataCell(CFG_UPPER_BOUND);
        Set<DataCell> values = null;
        if (config.containsKey(CFG_POSS_VALUES)) {
            values = new LinkedHashSet<DataCell>();
            values.addAll(Arrays.asList(config
                    .getDataCellArray(CFG_POSS_VALUES)));
        }
        return new DataColumnDomain(lower, upper, values);
    }

} // DataColumnDomain
