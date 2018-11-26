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
 * ---------------------------------------------------------------------
 *
 * Created on Oct 10, 2013 by Patrick Winter, KNIME AG, Zurich, Switzerland
 */
package org.knime.core.node.util.filter.column;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.util.filter.FilterIncludeExcludePreview;
import org.knime.core.node.util.filter.InputFilter;

/**
 * Filters based on the DataValues of columns.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @since 2.9
 */
@SuppressWarnings("serial")
final class TypeFilterPanelImpl extends JPanel {

    @SuppressWarnings("unchecked")
    private static final List<Class<? extends DataValue>> DEFAULT_TYPES = Arrays.asList(BooleanValue.class,
        IntValue.class, DoubleValue.class, LongValue.class, StringValue.class, DateAndTimeValue.class);

    private final Map<String, JCheckBox> m_selections;

    private final JPanel m_selectionPanel;

    private final InputFilter<DataColumnSpec> m_filter;

    private List<ChangeListener> m_listeners;

    private Map<String, Boolean> m_selectionValues = new LinkedHashMap<String, Boolean>();

    private DataColumnSpecFilterPanel m_parent;

    private DataTableSpec m_tableSpec;

    private FilterIncludeExcludePreview<DataColumnSpec> m_preview;

    /**
     * Creates a DataValue filter panel.
     *
     * @param parent The filter that is parent to this type filter
     * @param filter The filter to use, if null no filter will be applied.
     */
    @SuppressWarnings("unchecked")
    TypeFilterPanelImpl(final DataColumnSpecFilterPanel parent, final InputFilter<DataColumnSpec> filter) {
        setLayout(new GridBagLayout());
        m_parent = parent;
        m_filter = filter;
        m_selectionPanel = new JPanel(new GridBagLayout());
        m_selections = new LinkedHashMap<String, JCheckBox>();

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 4, 0);
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        add(m_selectionPanel, c);
        m_preview = new FilterIncludeExcludePreview<DataColumnSpec>(m_parent.getListCellRenderer());
        m_preview.setListSize(new Dimension(365, 195));
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        add(m_preview, c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        for (JCheckBox checkBox : m_selections.values()) {
            checkBox.setEnabled(enabled);
        }
        m_preview.setEnabled(enabled);
    }

    /** @param config to load from
     * @param spec specs of the available columns that will be shown in the selection preview */
    void loadConfiguration(final TypeFilterConfigurationImpl config, final DataTableSpec spec) {
        m_tableSpec = spec;
        clearTypes();
        // ArrayList holding all the JPanels with the type checkboxes
        ArrayList<JPanel> checkboxes = new ArrayList<JPanel>();
        // add checkboxes from the DataTableSpec
        checkboxes.addAll(getCheckBoxPanelsForDataColumns(spec));
        // add checkboxes for the default data values
        checkboxes.addAll(getCheckBoxPanelsForDataValues(DEFAULT_TYPES, spec));
        // add checkboxes from the configuration
        Map<String, Boolean> mapping = new LinkedHashMap<String, Boolean>(config.getSelections());
        for (Map.Entry<String, Boolean> entry : mapping.entrySet()) {
            final String valueClassName = entry.getKey();
            final Boolean valueSelected = entry.getValue();
            JCheckBox box = m_selections.get(valueClassName);
            if (box != null) {
                box.setSelected(valueSelected);
            } else if (valueSelected) {
                // type included by currently not in the input spec
                checkboxes.add(getCheckBoxPanel(Optional.empty(), valueClassName, true, isEnabled(), valueClassName, true, false));
            }
        }
        initTypeSelectionPanel(checkboxes);
        update();
    }

    /**
     * Initializes the type selection panel with the provided CheckBoxes
     * @param checkboxes List of JPanels containing the JCheckBoxes, an Icon depicting the type and a Label
     */
    private void initTypeSelectionPanel(final ArrayList<JPanel> checkboxes) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        // number of columns
        int columns = 3;

        for (JPanel jp : checkboxes) {
            // add CheckBox Panel to selectionPanel
            m_selectionPanel.add(jp, gbc);
            // if CheckBox was the last in entry in the row
            if (gbc.gridx == (columns - 1)) {
                gbc.gridx = 0;
                gbc.gridy++;
            } else {
                gbc.gridx++;
            }
        }
    }

    /** @param config to save to */
    void saveConfiguration(final TypeFilterConfigurationImpl config) {
        LinkedHashMap<String, Boolean> mapping = new LinkedHashMap<String, Boolean>();
        for (String key : m_selections.keySet()) {
            mapping.put(key, m_selections.get(key).isSelected());
        }
        config.setSelections(mapping);
    }

    /**
     * Returns the type checkboxes (each in a JPanel) columns.
     *
     * @param columns The columns contained in the current data table.
     */
    private ArrayList<JPanel> getCheckBoxPanelsForDataColumns(final Iterable<DataColumnSpec> columns) {
        ArrayList<JPanel> panels = new ArrayList<JPanel>();
        for (DataColumnSpec column : columns) {
            if (m_filter == null || m_filter.include(column)) {
                Class<? extends DataValue> prefValueClass = column.getType().getPreferredValueClass();
                if (!m_selections.containsKey(prefValueClass.getName())) {
                    UtilityFactory utilityFor = DataType.getUtilityFor(prefValueClass);
                    if (utilityFor instanceof ExtensibleUtilityFactory) {
                        ExtensibleUtilityFactory eu = (ExtensibleUtilityFactory)utilityFor;
                        String label = eu.getName();
                        String key = prefValueClass.getName();
                        panels.add(getCheckBoxPanel(Optional.of(utilityFor.getIcon()), label, false, isEnabled(), key, false, true));
                    }
                }
            }
        }
        return panels;
    }

    /**
     * Returns the type checkboxes (each in a JPanel) for the  data values.
     *
     * @param values The data values to add
     */
    private ArrayList<JPanel> getCheckBoxPanelsForDataValues(final Iterable<Class<? extends DataValue>> values, final DataTableSpec spec) {
        ArrayList<JPanel> panels = new ArrayList<JPanel>();
        for (Class<? extends DataValue> value : values) {
            if (isIncludedByFilter(value)) {
                if (value != null && !m_selections.containsKey(value.getName())) {
                    UtilityFactory utilityFor = DataType.getUtilityFor(value);
                    if (utilityFor instanceof ExtensibleUtilityFactory) {
                        ExtensibleUtilityFactory eu = (ExtensibleUtilityFactory)utilityFor;
                        String label = eu.getName();
                        String key = value.getName();
                        panels.add(getCheckBoxPanel(Optional.of(utilityFor.getIcon()), label, false, isEnabled(), key, false, false));
                    }
                }
            }
        }
        return panels;
    }


    /**
     * @param value
     * @return
     */
    private boolean isIncludedByFilter(final Class<? extends DataValue> valueClass) {
        if (m_filter instanceof DataTypeColumnFilter) {
            Class<? extends DataValue>[] filterClasses = ((DataTypeColumnFilter)m_filter).getFilterClasses();
            final List<Class<? extends DataValue>> filterClassesAsList = Arrays.asList(filterClasses);
            if (filterClassesAsList.contains(valueClass)) {
                return true;
            }
            if (StringValue.class.equals(valueClass) && filterClassesAsList.contains(NominalValue.class)) {
                // Often the filter is configured to include NominalValue, which is implemented by StringCell.
                // Since the default type list only contains StringValue, which is often used as synonym for nominal
                // values, we treat them as if they were the same
                return true;
            }
            return false;
        }
        return true;
    }

    private void clearTypes() {
        m_selectionPanel.removeAll();
        m_selections.clear();
    }

    /**
     * Returns a CheckBox with an icon and label representing the type.
     * A red border is painted around the label if the type no longer exists in the input. The checkbox label is written italic if the type is not present in the DataTableSpec.
     *
     * @param label label of the type
     * @param addRedBorderAsInvalid if a red border should be painted around the label
     * @param isEnabled if the checkbox is enabled
     * @param icon type icon (see getIcon() in {@link DataValue.UtilityFactory})
     * @param setSelected if the checkbox should be selected
     * @param inDataTableSpec whether the type of this checkbox is contained in the DataTableSpec
     * @return
     */
    private JPanel getCheckBoxPanel(final Optional<Icon> icon, final String label, final boolean addRedBorderAsInvalid, final boolean isEnabled, final String key,  final boolean setSelected, final boolean inDataTableSpec) {
        JPanel jp = new JPanel(new GridBagLayout());
        // Checkbox - no text
        final JCheckBox checkbox = new JCheckBox();
        // has to be executed before the ChangeListener is registered
        if(setSelected) {
            checkbox.setSelected(true);
        }
        checkbox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (m_selectionValues.get(label).booleanValue() != checkbox.isSelected()) {
                    m_selectionValues.put(label, checkbox.isSelected());
                    fireFilteringChangedEvent();
                }
            }
        });
        if (addRedBorderAsInvalid) {
            checkbox.setToolTipText("Type no longer exists in input");
            jp.setBorder(BorderFactory.createLineBorder(Color.RED));
        }
        // JLabel with type icon and text
        JLabel type = new JLabel(label);
        if (icon.isPresent()) {
            type.setIcon(icon.get());
        }
        if (!inDataTableSpec) {
            type.setFont(getFont().deriveFont(Font.ITALIC));
        }
        type.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                checkbox.setSelected(!checkbox.isSelected());
            }
        });
        // add to JPanel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.anchor = GridBagConstraints.WEST;
        jp.add(checkbox, gbc);
        gbc.gridx++;
        gbc.weightx = 0.1;
        jp.add(type, gbc);

        m_selectionValues.put(label, checkbox.isSelected());
        m_selections.put(key, checkbox);
        return jp;
    }

    /**
     * Adds a listener which gets informed whenever the filtering changes.
     *
     * @param listener the listener
     */
    public void addChangeListener(final ChangeListener listener) {
        if (m_listeners == null) {
            m_listeners = new ArrayList<ChangeListener>();
        }
        m_listeners.add(listener);
    }

    /**
     * Removes the given listener from this filter panel.
     *
     * @param listener the listener.
     */
    public void removeChangeListener(final ChangeListener listener) {
        if (m_listeners != null) {
            m_listeners.remove(listener);
        }
    }

    private void fireFilteringChangedEvent() {
        update();
        if (m_listeners != null) {
            for (ChangeListener listener : m_listeners) {
                listener.stateChanged(new ChangeEvent(this));
            }
        }
    }

    /**
     * Update the preview.
     */
    void update() {
        if (m_tableSpec == null) {
            return; // nothing to render (yet)
        }
        List<DataColumnSpec> includes = new ArrayList<DataColumnSpec>();
        List<DataColumnSpec> excludes = new ArrayList<DataColumnSpec>();
        for (DataColumnSpec spec : m_tableSpec) {
            // Check if type is filtered out by filter of the node
            if (m_filter != null) {
                if (!m_filter.include(spec)) {
                    continue;
                }
            }
            if (m_parent.getHiddenNames().contains(spec)) {
                continue;
            }
            final Class<? extends DataValue> preferredValueClass = spec.getType().getPreferredValueClass();
            // Check if type would be included with the current settings
            String key = preferredValueClass.getName();
            if (m_selections.containsKey(key) && m_selections.get(key).isSelected()) {
                includes.add(spec);
            } else {
                excludes.add(spec);
            }
        }
        m_preview.update(includes, excludes);
    }

}
