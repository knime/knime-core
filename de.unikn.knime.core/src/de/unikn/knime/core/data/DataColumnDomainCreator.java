/*
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
package de.unikn.knime.core.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Column domain creator used to initialize possible values, lower and upper
 * bounds using <code>DataCell</code> objects.
 * 
 * @author Thomas Gabriel, Konstanz University
 */
public class DataColumnDomainCreator {

    /** Keeps all possible nominal values or null if not available. */
    private Set<DataCell> m_values;

    /** Keeps the lower bound or null if not available. */
    private DataCell m_lowerBound;

    /** Keeps the upper bound or null if not available. */
    private DataCell m_upperBound;

    /**
     * Creates a new domain object with no meta-info defined.
     */
    public DataColumnDomainCreator() {
        this((Set<DataCell>)null, null, null);
    }

    /**
     * Creates a new domain object with a Set of possible nominal values.
     * 
     * @param values The Set can be null.
     */
    public DataColumnDomainCreator(final Set<DataCell> values) {
        this(values, null, null);
    }

    /**
     * Creates a new domain object with the given array of values.
     * 
     * @param values The array can be null, null elements are ignored.
     */
    public DataColumnDomainCreator(final DataCell[] values) {
        this(values, null, null);
    }

    /**
     * Creates a new domain object with the defined lower and upper bound.
     * 
     * @param lowerBound The lower bound can be null.
     * @param upperBound The upper bound can be null.
     */
    public DataColumnDomainCreator(final DataCell lowerBound,
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
    public DataColumnDomainCreator(final DataCell[] values, 
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
    public DataColumnDomainCreator(final Set<DataCell> values,
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
     * Creates a read-only column domain based on the internal values.
     * @return Column domain object.
     */
    public DataColumnDomain createDomain() {
        return new DataColumnDomain(m_lowerBound, m_upperBound, m_values);
    }
    
    /**
     * Sets a new set of nominal values which can be null.
     * @param values Set of nominal values.
     */
    public void setValues(final Set<DataCell> values) {
        m_values = values;
    }

    /**
     * Sets a new lower bound which can be null.
     * @param lower The new lower bound.
     */
    public void setLowerBound(final DataCell lower) {
        m_lowerBound = lower;
    }

    /**
     * Sets new upper bound which can be null.
     * @param upper The new upper bound.
     */
    public void setUpperBound(final DataCell upper) {
        m_upperBound = upper;
    }
    
}
