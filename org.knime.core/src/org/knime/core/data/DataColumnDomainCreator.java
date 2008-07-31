/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   25.10.2006 (tg): cleanup
 *   02.11.2006 (tm, cs): reviewed
 */
package org.knime.core.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A column domain creator is used to initialize possible values and lower and
 * upper bounds using {@link DataCell} objects.
 * 
 * @see DataColumnDomain
 * @see #createDomain()
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DataColumnDomainCreator {

    /** Keeps all possible nominal values or null if not available. */
    private Set<DataCell> m_values;

    /** Keeps the lower bound or null if not available. */
    private DataCell m_lowerBound;

    /** Keeps the upper bound or null if not available. */
    private DataCell m_upperBound;

    /**
     * Creates a new domain creator by copying the information from an other
     * column domain.
     * 
     * @param copyFrom the column domain to copy from
     */
    public DataColumnDomainCreator(final DataColumnDomain copyFrom) {
        this(copyFrom.getValues(), copyFrom.getLowerBound(), copyFrom
                .getUpperBound());
    }

    /**
     * Creates a new domain creator with no meta-info defined.
     */
    public DataColumnDomainCreator() {
        this((Set<DataCell>)null, null, null);
    }

    /**
     * Creates a new domain creator with a {@link Set} of possible values.
     * 
     * @param values the {@link Set} of possible values (can be
     *            <code>null</code>)
     */
    public DataColumnDomainCreator(final Set<DataCell> values) {
        this(values, null, null);
    }

    /**
     * Creates a new domain creator with the given array of values.
     * 
     * @param values the array can be null, whereas null elements are ignored
     */
    public DataColumnDomainCreator(final DataCell[] values) {
        this(values, null, null);
    }

    /**
     * Creates a new domain creator with the given lower and upper bound.
     * 
     * @param lowerBound the lower bound (can be <code>null</code>)
     * @param upperBound the upper bound (can be <code>null</code>)
     */
    public DataColumnDomainCreator(final DataCell lowerBound,
            final DataCell upperBound) {
        this((Set<DataCell>)null, lowerBound, upperBound);
    }

    /**
     * Creates a new domain creator with an array of possible values, and a
     * lower and upper bound. All parameters can be null.
     * 
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @param values the array of possible values
     */
    public DataColumnDomainCreator(final DataCell[] values,
            final DataCell lowerBound, final DataCell upperBound) {
        this(lowerBound, upperBound);
        if (values != null) {
            // store a unmodifiable copy of the value set in the same order
            Set<DataCell> set =
                    new LinkedHashSet<DataCell>(Arrays.asList(values));
            m_values = Collections.unmodifiableSet(set);
        } else {
            m_values = null;
        }
    }

    /**
     * Creates a new domain creator with a {@link Set} of possible values, and a
     * lower and upper bound. All parameters can be <code>null</code>.
     * 
     * @param values the Set of possible values
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     */
    public DataColumnDomainCreator(final Set<DataCell> values,
            final DataCell lowerBound, final DataCell upperBound) {
        setValues(values);
        setLowerBound(lowerBound);
        setUpperBound(upperBound);
    }

    /**
     * Creates a read-only {@link DataColumnDomain} based on the internal
     * values.
     * 
     * @return a new instance of a {@link DataColumnDomain}
     */
    public DataColumnDomain createDomain() {
        return new DataColumnDomain(m_lowerBound, m_upperBound, m_values);
    }

    /**
     * Sets a (new) {@link Set} of possible values which can be
     * <code>null</code>. The values are copied into a unmodifiable set.
     * 
     * @param values {@link Set} of possible values as {@link DataCell} objects
     */
    public void setValues(final Set<DataCell> values) {
        if (values != null) {
            // store a unmodifiable copy of the value set in the same order
            Set<DataCell> set = new LinkedHashSet<DataCell>(values);
            m_values = Collections.unmodifiableSet(set);
        } else {
            m_values = null;
        }
    }

    /**
     * Sets a (new) lower bound which can be <code>null</code>.
     * 
     * @param lower the (new) lower bound
     */
    public void setLowerBound(final DataCell lower) {
        m_lowerBound = lower;
    }

    /**
     * Sets (new) upper bound which can be <code>null</code>.
     * 
     * @param upper the (new) upper bound
     */
    public void setUpperBound(final DataCell upper) {
        m_upperBound = upper;
    }

}
