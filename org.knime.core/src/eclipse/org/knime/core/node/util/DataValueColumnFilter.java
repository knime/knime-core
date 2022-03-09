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
 *    12.03.2007 (Tobias Koetter): created
 */

package org.knime.core.node.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;


/**
 * This {@link ColumnFilter} implementation filters all value which are not
 * compatible to the provided {@link DataValue} classes.
 * @author Tobias Koetter, University of Konstanz
 */
public class DataValueColumnFilter implements ColumnFilter {


    private final List<Class<? extends DataValue>> m_filterClasses;

    /**Constructor for class DataValueColumnFilter.
     * @param filterValueClasses classes derived from DataValue.
     * All other columns will be filtered.
     */
    @SafeVarargs
    public DataValueColumnFilter(
            final Class<? extends DataValue>... filterValueClasses) {
        this(List.of(checkVarArgs(filterValueClasses)));
    }

    private static Class<? extends DataValue>[] checkVarArgs(final Class<? extends DataValue>[] filterValueClasses) {
        if (filterValueClasses == null || filterValueClasses.length == 0) {
            throw new NullPointerException("Classes must not be null");//NOSONAR unfortunately API
        }
        return filterValueClasses;
    }

    /**
     * Constructor for class {@link DataValueColumnFilter}.
     * @param filterValueClasses classes derived from DataValue
     * @since 4.6
     */
    public DataValueColumnFilter(final Collection<Class<? extends DataValue>> filterValueClasses) {
        if (filterValueClasses == null || filterValueClasses.isEmpty()) {
            throw new NullPointerException("Classes must not be null");//NOSONAR unfortunately API
        }
        if (filterValueClasses.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("List of value classes must not contain null elements.");//NOSONAR
        }
        m_filterClasses = List.copyOf(filterValueClasses);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean includeColumn(final DataColumnSpec colSpec) {
        if (colSpec == null) {
            throw new NullPointerException(
                    "Column specification must not be null");
        }
        for (Class<? extends DataValue> cl : m_filterClasses) {
            if (colSpec.getType().isCompatible(cl)) {
               return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String allFilteredMsg() {
        StringBuilder error = new StringBuilder(
        "No column in spec compatible to");
        if (m_filterClasses.size() == 1) {
            error.append(" \"");
            error.append(m_filterClasses.get(0).getSimpleName());
            error.append('"');
        } else {
            for (int i = 0; i < m_filterClasses.size(); i++) {
                error.append(" \"");
                error.append(m_filterClasses.get(i).getSimpleName());
                error.append('"');
                if (i == m_filterClasses.size() - 2) { // second last
                    error.append(" or");
                }
            }
        }
        error.append('.');
        return error.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Filter on " + ConvenienceMethods.getShortStringFrom(m_filterClasses, 3);
    }
}
