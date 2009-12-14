/*
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
