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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on Oct 10, 2013 by Patrick Winter, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.util.filter.column;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.util.filter.InputFilter;

/**
 * Filters based on the DataValues of columns.
 *
 * @author Patrick Winter, KNIME.com AG, Zurich, Switzerland
 * @since 2.9
 */
@SuppressWarnings("serial")
final class TypeFilterPanelImpl extends JPanel {

    @SuppressWarnings("unchecked")
    private static final List<Class<? extends DataValue>> DEFAULT_TYPES = Arrays.asList(BooleanValue.class,
        DateAndTimeValue.class, DoubleValue.class, IntValue.class, LongValue.class, StringValue.class);

    private final Map<String, JCheckBox> m_selections;

    private final JPanel m_selectionPanel;

    private final InputFilter<Class<? extends DataValue>> m_filter;

    /** Creates a DataValue filter panel.
     * @param filter The filter to use, if null no filter will be applied.
     */
    TypeFilterPanelImpl(final InputFilter<Class<? extends DataValue>> filter) {
        m_filter = filter;
        m_selectionPanel = new JPanel(new GridLayout(0, 2));
        m_selections = new LinkedHashMap<String, JCheckBox>();
        add(m_selectionPanel);
    }

    void loadConfiguration(final TypeFilterConfigurationImpl config, final DataTableSpec spec) {
        clearTypes();
        addDataValues(DEFAULT_TYPES);
        addDataValues(getDataValuesFromSpec(spec));
        // copy to remove
        Map<String, Boolean> mapping = new LinkedHashMap<String, Boolean>(config.getSelections());
        for (Map.Entry<String, Boolean> entry : mapping.entrySet()) {
            final String valueClassName = entry.getKey();
            final Boolean valueSelected = entry.getValue();
            JCheckBox box = m_selections.get(valueClassName);
            if (box != null) {
                box.setSelected(valueSelected);
            } else if (valueSelected) {
                // type included by currently not in the input spec
                JCheckBox newCheckBox = addCheckBox(valueClassName, true);
                newCheckBox.setSelected(true);
                m_selections.put(valueClassName, newCheckBox);
            }
        }
    }

    private Set<Class<? extends DataValue>> getDataValuesFromSpec(final DataTableSpec spec) {
        if (spec == null) {
            return Collections.emptySet();
        }
        Set<Class<? extends DataValue>> values = new LinkedHashSet<Class<? extends DataValue>>();
        for (DataColumnSpec colspec : spec) {
            values.add(colspec.getType().getPreferredValueClass());
        }
        return values;
    }


    void saveConfiguration(final TypeFilterConfigurationImpl config) {
        Map<String, Boolean> mapping = new LinkedHashMap<String, Boolean>();
        for (String key : m_selections.keySet()) {
            mapping.put(key, m_selections.get(key).isSelected());
        }
        config.setSelections(mapping);
    }

    /**
     * Add data values to the selection.
     *
     * @param values The data values to add
     */
    void addDataValues(final Collection<Class<? extends DataValue>> values) {
        for (Class<? extends DataValue> value : values) {
            if (m_filter == null || m_filter.include(value)) {
                if (value != null && !m_selections.containsKey(value.getName())) {
                    UtilityFactory utilityFor = DataType.getUtilityFor(value);
                    if (utilityFor instanceof ExtensibleUtilityFactory) {
                        ExtensibleUtilityFactory eu = (ExtensibleUtilityFactory)utilityFor;
                        String label = eu.getName();
                        String key = value.getName();
                        m_selections.put(key, addCheckBox(label, false));
                    }
                }
            }
        }
    }

    private void clearTypes() {
        m_selectionPanel.removeAll();
        m_selections.clear();
    }

    private JCheckBox addCheckBox(final String label, final boolean addRedBorderAsInvalid) {
        JCheckBox checkbox = new JCheckBox(label);
        JComponent c = ViewUtils.getInFlowLayout(FlowLayout.LEFT, 0, 0, checkbox);
        if (addRedBorderAsInvalid) {
            checkbox.setToolTipText("Type no longer exists in input");
            c.setBorder(BorderFactory.createLineBorder(Color.RED));
        }
        m_selectionPanel.add(c);
        return checkbox;
    }

}
