/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   25.10.2006 (tg): cleanup
 *   02.11.2006 (tm, cs): reviewed
 */
package org.knime.core.data;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * Final <code>DataColumnDomain</code> object holding meta infos about one
 * column, that are, possible values and/or upper and lower bounds - if
 * available. This object can only be created by a
 * {@link DataColumnDomainCreator} within this package. The
 * <code>DataColumnDomain</code> is read-only.
 *
 * <p>
 * Note: It is assumed that the domain - if available - contains reliable data.
 * It is crucial that the creator of a column domain ensures that no value in
 * the data table is outside the provided bounds and no other value appears in
 * that column than the values listed in the set returned by
 * {@link #getValues()}. If you are not sure about the data to come in your
 * column, don't provide domain infos (<code>null</code>).<br>
 * Also noteworthy: domain information describes the source of the data (the
 * domain), not the data itself. I.e. the created domain object could contain
 * more possible values than actually appear in the data table, or a range
 * bigger than needed for the data in that particular data table. But it must
 * always include all values appearing in the table.
 *
 * @see DataColumnDomainCreator
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DataColumnDomain {

    /** Config key for the lower bound. */
    private static final String CFG_LOWER_BOUND = "lower_bound";

    /** Config key for the upper bound. */
    private static final String CFG_UPPER_BOUND = "upper_bound";

    /** Config key for possible values. */
    private static final String CFG_POSS_VALUES = "possible_values";

    /** Lower bound value or <code>null</code>. */
    private final DataCell m_lowerBound;

    /** Upper bound value or <code>null</code>. */
    private final DataCell m_upperBound;

    /** Set of possible values or <code>null</code>. */
    private final Set<DataCell> m_values;

    /**
     * Create new column domain with lower and upper bounds, and set of possible
     * values. All arguments can be <code>null</code> in case none of these
     * properties are available.
     *
     * @param lower the lower bound value or <code>null</code>
     * @param upper the upper bound value or <code>null</code>
     * @param values a set of nominal values or <code>null</code>
     */
    DataColumnDomain(final DataCell lower, final DataCell upper,
            final Set<DataCell> values) {
        m_lowerBound = lower;
        m_upperBound = upper;
        m_values = values;
    }

    /**
     * Returns all possible values in this column. Note, that this set can be
     * <code>null</code> if this information is not available (for continuous
     * double values, for example) and that the {@link DataCell}s in the
     * returned set do not have to be of the same type (but the column type
     * should be compatible to all of their values).
     *
     * <p>
     * If the returned set is not <code>null</code>, the corresponding column
     * does not contain any value other than the ones contained in the set. The
     * set can contain a superset of the values in the table though.
     *
     * @return a {@link Set} of possible {@link DataCell} values or
     *         <code>null</code>
     *
     * @see #hasValues()
     */
    public Set<DataCell> getValues() {
        return m_values;
    }

    /**
     * Returns <code>true</code> if the values are not <code>null</code>.
     *
     * @return <code>true</code>, if this column has possible values defined (
     *         i.e. the {@link #getValues()} method returns a non-null set)
     *
     * @see #getValues()
     */
    public boolean hasValues() {
        return m_values != null;
    }

    /**
     * Return the lower bound of the domain of this column, if available. Note
     * that this value does not necessarily need to actually occur in the
     * corresponding {@link DataTable} but it is describing the range of the
     * domain of this attribute.
     *
     * <p>
     * Usually this value is compatible with type {@link DoubleValue}
     * corresponding to an numeric left interval border.
     *
     * @return a {@link DataCell} with the lowest possible value or
     *         <code>null</code>
     *
     * @see #hasLowerBound()
     */
    public DataCell getLowerBound() {
        return m_lowerBound;
    }

    /**
     * Returns <code>true</code>, if a lower bound is defined.
     *
     * @return <code>true</code>, if the lower bound value has been defined
     *
     * @see #getLowerBound()
     */
    public boolean hasLowerBound() {
        return m_lowerBound != null;
    }

    /**
     * Return the upper bound of the domain of this column, if available. Note
     * that this value does not necessarily need to actually occur in the
     * corresponding {@link DataTable} but it describes the range of the domain
     * of this attribute.
     *
     * <p>
     * Usually this value is compatible with type {@link DoubleValue}
     * corresponding to an numeric right interval border.
     *
     * @return a {@link DataCell} with the largest possible value or
     *         <code>null</code>
     *
     * @see #hasUpperBound()
     */
    public DataCell getUpperBound() {
        return m_upperBound;
    }

    /**
     * Returns <code>true</code>, if an upper bound is defined.
     *
     * @return <code>true</code>, if the upper bound value has been defined
     *
     * @see #getUpperBound()
     */
    public boolean hasUpperBound() {
        return m_upperBound != null;
    }

    /**
     * Returns <code>true</code> if both, lower and upper bound, are defined.
     *
     * @return <code>true</code>, if lower and upper bound are defined
     */
    public boolean hasBounds() {
        return hasLowerBound() && hasUpperBound();
    }

    /**
     * Compares this domain with the other one by the possible values and lower
     * and upper bounds and returns <code>true</code> if both are the same.
     *
     * @param obj The other domain to compare with.
     * @return <code>true</code> if all possible values, and lower and upper
     *         bounds are the same
     *
     * @see Set#equals(Object)
     * @see DataCell#equals(Object)
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

        // check if properties are available in both domains
        DataColumnDomain domain = (DataColumnDomain)obj;
        // check if one or the other has possible values defined
        if (hasValues() ^ domain.hasValues()) {
            return false;
        }
        // check if one or the other has a lower bound defined
        if (hasLowerBound() ^ domain.hasLowerBound()) {
            return false;
        }
        // check if one or the other has a upper bound defined
        if (hasUpperBound() ^ domain.hasUpperBound()) {
            return false;
        }

        // if both domains have possible values defined
        if (hasValues() && domain.hasValues()) {
            // compare them
            if (!getValues().equals(domain.getValues())) {
                return false;
            }
        }
        // if both domains have lower bounds defined
        if (hasLowerBound() && domain.hasLowerBound()) {
            if (!getLowerBound().equals(domain.getLowerBound())) {
                return false;
            }
        }
        // if both domains have upper bounds defined
        if (hasUpperBound() && domain.hasUpperBound()) {
            if (!getUpperBound().equals(domain.getUpperBound())) {
                return false;
            }
        }

        // done, both domains are equal
        return true;
    }

    /**
     * Returns the hash code of this domain, based on the hash codes of the
     * lower, upper bound, and each possible value - if available.
     *
     * {@inheritDoc}
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
     * Returns string representation of this domain including lower and upper
     * bounds, and possible values.
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

    /**
     * Save this domain to the given {@link ConfigWO} including lower and upper
     * bound, and possible values or null - if not available.
     *
     * @param config the {@link ConfigWO} to write into
     */
    public void save(final ConfigWO config) {
        if (hasLowerBound()) {
            config.addDataCell(CFG_LOWER_BOUND, m_lowerBound);
        }
        if (hasUpperBound()) {
            config.addDataCell(CFG_UPPER_BOUND, m_upperBound);
        }
        if (hasValues()) {
            DataCell[] values = m_values.toArray(new DataCell[0]);
            config.addDataCellArray(CFG_POSS_VALUES, values);
        }
    }

    /**
     * Reads lower and upper bounds as well as the possible values - if
     * available - from {@link ConfigRO}.
     *
     * @param config to read entries from
     * @return a new domain object with the read properties
     */
    public static DataColumnDomain load(final ConfigRO config) {
        DataCell lower = null;
        if (config.containsKey(CFG_LOWER_BOUND)) {
            lower = config.getDataCell(CFG_LOWER_BOUND, null);
        }
        DataCell upper = null;
        if (config.containsKey(CFG_UPPER_BOUND)) {
            upper = config.getDataCell(CFG_UPPER_BOUND, null);
        }
        Set<DataCell> values = null;
        if (config.containsKey(CFG_POSS_VALUES)) {
            DataCell[] valArray =
                    config.getDataCellArray(CFG_POSS_VALUES, (DataCell[])null);
            if (valArray != null) {
                values = new LinkedHashSet<DataCell>();
                values.addAll(Arrays.asList(valArray));
            }
        }
        return new DataColumnDomain(lower, upper, values);
    }

} // DataColumnDomain
