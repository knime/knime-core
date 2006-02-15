/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
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
package de.unikn.knime.core.data.def;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnDomain;

/**
 * 
 * Default column damain holds a set of possible values, and lower and upper
 * bound as DataCell object.
 * 
 * @author Thomas Gabriel, Konstanz University
 */
public class DefaultDataColumnDomain implements DataColumnDomain {

    /** Keeps all possible nominal values or null if not available. */
    private final Set<DataCell> m_values;

    /** Keeps the lower bound or null if not available. */
    private final DataCell m_lowerBound;

    /** Keeps the upper bound or null if not available. */
    private final DataCell m_upperBound;

    /**
     * Creates a new domain object with no meta-info defined.
     */
    public DefaultDataColumnDomain() {
        this((Set<DataCell>)null, null, null);
    }

    /**
     * Creates a new domain object with a Set of possible nominal values.
     * 
     * @param values The Set can be null.
     */
    public DefaultDataColumnDomain(final Set<DataCell> values) {
        this(values, null, null);
    }

    /**
     * Creates a new domain object with the given array of values.
     * 
     * @param values The array can be null, null elements are ignored.
     */
    public DefaultDataColumnDomain(final DataCell[] values) {
        this(values, null, null);
    }

    /**
     * Creates a new domain object with the defined lower and upper bound.
     * 
     * @param lowerBound The lower bound can be null.
     * @param upperBound The upper bound can be null.
     */
    public DefaultDataColumnDomain(final DataCell lowerBound,
            final DataCell upperBound) {
        this((Set<DataCell>)null, lowerBound, upperBound);
    }

    /**
     * Creates a new domain object with a array of possible nominal values, and
     * a lower and upper bound. All parameters can be null.
     * 
     * @param lowerBound The lower bound.
     * @param upperBound The upper bound.
     * @param values The array of possible values.
     */
    public DefaultDataColumnDomain(final DataCell[] values, 
            final DataCell lowerBound, final DataCell upperBound) {
        this((values == null ? null : new LinkedHashSet<DataCell>(Arrays
                .asList(values))), lowerBound, upperBound);
    }

    /**
     * Creates a new domain object with a Set of possible nominal values, and a
     * lower and upper bound. All parameters can be null.
     * 
     * @param values The Set of possible values.
     * @param lowerBound The lower bound.
     * @param upperBound The upper bound.
     */
    public DefaultDataColumnDomain(final Set<DataCell> values,
            final DataCell lowerBound, final DataCell upperBound) {
        if (values != null) {
            // store a unmodifable copy of the value set in the same order
            m_values = Collections.unmodifiableSet(values);
        } else {
            m_values = null;
        }
        m_lowerBound = lowerBound;
        m_upperBound = upperBound;
    }

    /**
     * @see de.unikn.knime.core.data.DataColumnDomain#getValues() Do not modify
     *      the returned Set. Any attempt in doing so will cause a runtime
     *      exception to fly.
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
     * @see de.unikn.knime.core.data.DataColumnDomain#getLowerBound()
     */
    public DataCell getLowerBound() {
        return m_lowerBound;
    }

    /**
     * @see de.unikn.knime.core.data.DataColumnDomain#getUpperBound()
     */
    public DataCell getUpperBound() {
        return m_upperBound;
    }

    /**
     * @see de.unikn.knime.core.data.DataColumnDomain#hasValues()
     */
    public boolean hasValues() {
        return (m_values != null);
    }

    /**
     * @see de.unikn.knime.core.data.DataColumnDomain#hasLowerBound()
     */
    public boolean hasLowerBound() {
        return (m_lowerBound != null);
    }

    /**
     * @see de.unikn.knime.core.data.DataColumnDomain#hasUpperBound()
     */
    public boolean hasUpperBound() {
        return (m_upperBound != null);
    }
    
    /**
     * @see de.unikn.knime.core.data.DataColumnDomain#hasBounds()
     */
    public boolean hasBounds() {
        return hasLowerBound() && hasUpperBound();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
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
     * Compares this domain with the other one by the possible values and lower
     * and upper bound.
     * 
     * @param obj The other domain to compare with.
     * @return true if possible values, and lower and upper bounds are equal.
     */
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

}
