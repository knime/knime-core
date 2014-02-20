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

import java.util.Map;
import java.util.Set;

import javax.swing.ListCellRenderer;

import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.NameFilterPanel;
import org.knime.core.node.workflow.FlowVariable;

/**
 * A panel to filter {@link FlowVariable}s.
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 * @since 2.9
 */
@SuppressWarnings("serial")
public class FlowVariableFilterPanel extends NameFilterPanel<FlowVariable> {

    private Map<String, FlowVariable> m_variables;

    /**
     * Create a new panel to filter {@link FlowVariable}s.
     */
    public FlowVariableFilterPanel() {
        super();
    }

    /**
     * Create a new panel to filter {@link FlowVariable}s.
     *
     * @param showSelectionListsOnly if true, the panel shows no additional options like search box,
     *            force-include-option, etc.
     */
    public FlowVariableFilterPanel(final boolean showSelectionListsOnly) {
        super(showSelectionListsOnly);
    }

    /**
     * Create a new panel to filter {@link FlowVariable}s. The given FlowVariable.Types specify the type of the
     * variables which are shown and can be included or excluded.
     *
     * @param types The FlowVariable.Type of the flow variables to show.
     */
    public FlowVariableFilterPanel(final FlowVariable.Type... types) {
        super(false, new FlowVariableTypeFilter(types));
    }

    /**
     * Create a new panel to filter {@link FlowVariable}s. The given FlowVariable.Types specify the type of the
     * variables which are shown and can be included or excluded.
     *
     * @param showSelectionListsOnly if true, the panel shows no additional options like search box,
     *            force-include-option, etc.
     * @param types The FlowVariable.Type of the flow variables to show.
     */
    public FlowVariableFilterPanel(final boolean showSelectionListsOnly, final FlowVariable.Type... types) {
        super(showSelectionListsOnly, new FlowVariableTypeFilter(types));
    }

    /**
     * Create a new panel to filter {@link FlowVariable}s. The given filter handles which variables are shown and can be
     * included or excluded and which not, based on the underlying type of the variable.
     *
     * @param showSelectionListsOnly if true, the panel shows no additional options like search box,
     *            force-include-option, etc.
     * @param filter The filter specifying which variables are shown and which not.
     */
    public FlowVariableFilterPanel(final boolean showSelectionListsOnly, final InputFilter<FlowVariable> filter) {
        super(showSelectionListsOnly, filter);
    }

    /**
     * Create a new panel to filter {@link FlowVariable}s. The given filter handles which variables are shown and can be
     * included or excluded and which not, based on the underlying type of the variable.
     *
     * @param filter The filter specifying which variables are shown and which not.
     */
    public FlowVariableFilterPanel(final InputFilter<FlowVariable> filter) {
        super(false, filter);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected ListCellRenderer getListCellRenderer() {
        return new FlowVariableListCellRenderer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FlowVariable getTforName(final String name) {
        return m_variables.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getNameForT(final FlowVariable t) {
        return t.getName();
    }

    /**
     * Load configuration.
     *
     * @param config the configuration to read to settings from.
     * @param flowVariables the {@link FlowVariable}s to validate the settings on
     */
    public void loadConfiguration(final FlowVariableFilterConfiguration config,
        final Map<String, FlowVariable> flowVariables) {
        m_variables = flowVariables;
        Set<String> keys = flowVariables.keySet();
        super.loadConfiguration(config, keys.toArray(new String[keys.size()]));
    }

}
