/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 * ---------------------------------------------------------------------
 *
 * Created on Oct 1, 2013 by Patrick Winter
 */
package org.knime.core.node.util.filter.variable;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Represents a FlowVariable filtering. Classes of this object are used as member in the NodeModel and as underlying
 * model to a {@link FlowVariableFilterPanel}.
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 * @since 2.9
 */
public class FlowVariableFilterConfiguration extends NameFilterConfiguration {

    private final InputFilter<FlowVariable.Type> m_filter;

    /**
     * New instance with hard coded root name.
     *
     * @param configRootName Non null name that is used as identifier when saved to a NodeSettings object during save
     *            (and load).
     */
    public FlowVariableFilterConfiguration(final String configRootName) {
        this(configRootName, null);
    }

    /**
     * New instance with hard coded root name.
     *
     * @param configRootName Non null name that is used as identifier when saved to a NodeSettings object during save
     *            (and load).
     * @param filter A (type) filter applied to the input spec.
     */
    public FlowVariableFilterConfiguration(final String configRootName, final InputFilter<FlowVariable.Type> filter) {
        super(configRootName);
        m_filter = filter;
    }

    /**
     * Loads the configuration in the dialog (no exception thrown) and maps it to the flow variables.
     *
     * @param settings The settings to load from.
     * @param flowVars The available flow variables.
     */
    public void loadConfigurationInDialog(final NodeSettingsRO settings, final Map<String, FlowVariable> flowVars) {
        String[] names = toFilteredStringArray(flowVars);
        super.loadConfigurationInDialog(settings, names);
    }

    private String[] toFilteredStringArray(final Map<String, FlowVariable> flowVariables) {
        ArrayList<String> acceptedInNames = new ArrayList<String>();
        Set<Entry<String, FlowVariable>> entries = flowVariables.entrySet();
        for (Entry<String, FlowVariable> var : entries) {
            if (m_filter == null || m_filter.include(var.getValue().getType())) {
                String name = var.getKey();
                acceptedInNames.add(name);
            }
        }
        return acceptedInNames.toArray(new String[acceptedInNames.size()]);
    }

    /**
     * Applies the settings to the flow variables and returns an object representing the included, excluded and unknown
     * names.
     *
     * @param flowVariables The available flow variables.
     * @return The filter result object.
     */
    public FilterResult applyTo(final Map<String, FlowVariable> flowVariables) {
        String[] names = toFilteredStringArray(flowVariables);
        return super.applyTo(names);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlowVariableFilterConfiguration clone() {
        return (FlowVariableFilterConfiguration)super.clone();
    }

    /**
     * Guess default settings on the given flow variables. If the flag is set all appropriate columns will be put into
     * the include list, otherwise they are put into the exclude list.
     *
     * @param flowVariables The available flow variables.
     * @param includeByDefault See above.
     */
    public void loadDefaults(final Map<String, FlowVariable> flowVariables, final boolean includeByDefault) {
        String[] names = toFilteredStringArray(flowVariables);
        super.loadDefaults(names, includeByDefault);
    }

    /**
     * Returns the filter used by this configuration.
     *
     * @return a reference to the filter. Don't modify.
     */
    public InputFilter<FlowVariable.Type> getFilter() {
        return m_filter;
    }

}
