/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Jul 2, 2012 (wiswedel): created
 */
package org.knime.core.node.util.filter.column;

import java.util.ArrayList;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/** Represents a column filtering. Classes of this object are used as member in
 * the NodeModel and as underlying model to a {@link DataColumnSpecFilterPanel}.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.6
 */
public final class DataColumnSpecFilterConfiguration
    extends NameFilterConfiguration {

    private final InputFilter<DataColumnSpec> m_filter;

    /** New instance with hard coded root name.
     * @param configRootName Non null name that is used as identifier when
     * saved to a NodeSettings object during save (and load). */
    public DataColumnSpecFilterConfiguration(final String configRootName) {
        this(configRootName, null);
    }

    /** New instance with hard coded root name.
     * @param configRootName Non null name that is used as identifier when
     * saved to a NodeSettings object during save (and load).
     * @param filter A (type) filter applied to the input spec. */
    public DataColumnSpecFilterConfiguration(final String configRootName,
            final InputFilter<DataColumnSpec> filter) {
        super(configRootName);
        m_filter = filter;
    }

    /** Loads the configuration in the dialog (no exception thrown)
     * and maps it to the input spec.
     * @param settings The settings to load from.
     * @param spec The non-null spec.
     */
    public void loadConfigurationInDialog(final NodeSettingsRO settings,
            final DataTableSpec spec) {
        String[] names = toFilteredStringArray(spec);
        super.loadConfigurationInDialog(settings, names);
    }

    private String[] toFilteredStringArray(final DataTableSpec spec) {
        ArrayList<String> acceptedInNames = new ArrayList<String>();
        for (DataColumnSpec col : spec) {
            if (m_filter == null || m_filter.include(col)) {
                String name = col.getName();
                acceptedInNames.add(name);
            }
        }
        return acceptedInNames.toArray(new String[acceptedInNames.size()]);
    }

    /** Applies the settings to the input spec and returns an
     * object representing the included, excluded and unknown
     * names.
     * @param spec The input spec.
     * @return The filter result object.
     */
    public FilterResult applyTo(final DataTableSpec spec) {
        String[] names = toFilteredStringArray(spec);
        return super.applyTo(names);
    }

    /** {@inheritDoc} */
    @Override
    public DataColumnSpecFilterConfiguration clone() {
        return (DataColumnSpecFilterConfiguration)super.clone();
    }
}
