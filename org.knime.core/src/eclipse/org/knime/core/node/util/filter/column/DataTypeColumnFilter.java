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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   29.06.2012 (kilian): created
 */
package org.knime.core.node.util.filter.column;

import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.util.ColumnFilterPanel;
import org.knime.core.node.util.filter.InputFilter;

/**
 * Class that filters all columns based on a given set of compatible
 * <code>DataValue</code> classes.
 *
 * @author Kilian Thiel, KNIME.com AG, Zurich
 * @since 2.6
 */
public final class DataTypeColumnFilter extends InputFilter<DataColumnSpec> {

    /**
     * Show only columns of types that are compatible to one of theses
     * classes.
     */
    private final Class<? extends DataValue>[] m_filterClasses;

    /**
     * Creates a new value class filter.
     * @param filterValueClasses all classes that are compatible with
     *        the type allowed in {@link #include(DataColumnSpec)}
     */
    public DataTypeColumnFilter(
            final Class<? extends DataValue>... filterValueClasses) {
        if (filterValueClasses == null || filterValueClasses.length == 0) {
            throw new NullPointerException("Classes must not be null");
        }
        final List<Class<? extends DataValue>> list = Arrays
                .asList(filterValueClasses);
        if (list.contains(null)) {
            throw new NullPointerException("List of value classes must not "
                    + "contain null elements.");
        }
        m_filterClasses = filterValueClasses;
    }

    /**
     * Checks if the given column type is included in the list of allowed
     * types. If the list is empty, all types are valid.
     * @param cspec {@link ColumnFilterPanel} checked
     * @return true, if given column should be visible in column filter
     */
    @Override
    public final boolean include(final DataColumnSpec cspec) {
        for (final Class<? extends DataValue> cl : m_filterClasses) {
            if (cspec.getType().isCompatible(cl)) {
                return true;
            }
        }
        return false;
    }

}
