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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.filter.nominal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.NameFilterConfiguration.EnforceOption;
import org.knime.core.node.util.filter.nominal.NominalValueFilterConfiguration;
import org.knime.core.node.util.filter.nominal.NominalValueFilterPanel;

/**
 * <code>NodeDialog</code> for the "PossibleValueRowFilter" Node. Adds a
 * select box for the nominal column and an include and exclude list with the
 * necessary buttons to add, add all, remove, and remove all possible values from
 * one list to the other. The lists are update, when a nominal column is
 * selected.
 *
 * @author KNIME GmbH
 */
public class NominalValueRowFilterNodeDialog extends NodeDialogPane implements
        ItemListener {

    private String m_selectedColumn;

    private final Map<String, Set<DataCell>> m_colAttributes;

    // models
    private final DefaultComboBoxModel<String> m_columns;

    // gui elements
    private final JComboBox<String> m_columnSelection;

    /** Config key for the selected column. */
    static final String CFG_SELECTED_COL = "selected_column";

    /** Config key for the possible values to be included. */
    static final String CFG_SELECTED_ATTR = "selected attributes";

    /** Config key for filter configuration. */
    static final String CFG_CONFIGROOTNAME = "filter config";

    private NominalValueFilterPanel m_filterPanel;

    /**
     * New pane for configuring the NominalValueRowFilter node.
     * @param splitter whether this dialog is for the splitter or the filter
     * @since 3.4
     */
    protected NominalValueRowFilterNodeDialog(final boolean splitter) {
        m_colAttributes = new LinkedHashMap<String, Set<DataCell>>();
        m_columns = new DefaultComboBoxModel<>();

        m_columnSelection = new JComboBox<>(m_columns);
        m_columnSelection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        // add listener to column selection box to change exclude list
        m_columnSelection.addItemListener(this);

        // create the GUI
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Select column:"));
        JPanel columnSelectionPanel = new JPanel(new BorderLayout());
        columnSelectionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        columnSelectionPanel.add(m_columnSelection, BorderLayout.CENTER);
        panel.add(columnSelectionPanel, BorderLayout.NORTH);
        Box attributeSelectionBox = Box.createHorizontalBox();
        m_filterPanel = new NominalValueFilterPanel();
        if (splitter) {
            m_filterPanel.setIncludeTitle("Top");
            m_filterPanel.setExcludeTitle("Bottom");
            m_filterPanel.setAdditionalCheckboxText("<html>Incl. Missing<br/>Values (Top)</html>");
            m_filterPanel.setPatternFilterBorderTitles("Mismatch (Bottom)", "Match (Top)");
            m_filterPanel.setAdditionalPatternCheckboxText("Include Missing Values (Top)");
        }
        attributeSelectionBox.add(m_filterPanel);
        panel.add(attributeSelectionBox, BorderLayout.CENTER);
        addTab("Selection", new JScrollPane(panel));
    }

    /**
     * New pane for configuring the PossibleValueRowFilter node.
     */
    protected NominalValueRowFilterNodeDialog() {
        this(false);
    }

    /**
     * {@inheritDoc}
     *
     * If the nominal column selection changes, include and exclude lists are
     * cleared and all possible values of that column are put into the exclude
     * list.
     */
    @Override
    public void itemStateChanged(final ItemEvent item) {
        if (item.getStateChange() == ItemEvent.SELECTED) {
            m_selectedColumn = (String)item.getItem();
            ArrayList<String> names = new ArrayList<>();
            if (m_colAttributes.get(m_selectedColumn) != null) {
                for (DataCell dc : m_colAttributes.get(m_selectedColumn)) {
                    names.add(dc.toString());
                }
            }
            String[] namesArray = names.toArray(new String[names.size()]);
            NominalValueFilterConfiguration config = new NominalValueFilterConfiguration(CFG_CONFIGROOTNAME);
            config.loadDefaults(null, namesArray, EnforceOption.EnforceInclusion);
            m_filterPanel.loadConfiguration(config, namesArray);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        if (specs.length == 0 || specs[0] == null) {
            throw new NotConfigurableException("No incoming columns found. "
                    + "Please connect the node with input table!");
        }
        // get selected column
        m_selectedColumn = settings.getString(CFG_SELECTED_COL, "");
        // clear old values
        m_colAttributes.clear();
        m_columns.removeAllElements();
        // disable item state change listener while adding values
        m_columnSelection.removeItemListener(this);
        // fill the models
        for (DataColumnSpec colSpec : specs[0]) {
            if (colSpec.getType().isCompatible(NominalValue.class) && colSpec.getDomain().hasValues()) {
                m_columns.addElement(colSpec.getName());
                // create column - possible values mapping
                m_colAttributes.put(colSpec.getName(), colSpec.getDomain().getValues());
            }
        }
        // set selection
        if (m_selectedColumn != null) {
            m_columnSelection.setSelectedItem(m_selectedColumn);
            // enable item change listener again
            m_columnSelection.addItemListener(this);
        } else {
            m_columnSelection.addItemListener(this);
            m_columnSelection.setSelectedIndex(-1);
            m_columnSelection.setSelectedItem(m_columnSelection.getItemAt(0));
        }

        NominalValueFilterConfiguration config = new NominalValueFilterConfiguration(CFG_CONFIGROOTNAME);
        Set<DataCell> domain = m_colAttributes.get(m_selectedColumn);
        if (settings.containsKey(CFG_CONFIGROOTNAME)) {
            config.loadConfigurationInDialog(settings, domain);
        } else {
            // backwards compatibility
            String[] selectedAttributes = settings.getStringArray(CFG_SELECTED_ATTR, "");
            Set<String> includedAttr = new HashSet<String>();
            for (String s : selectedAttributes) {
                includedAttr.add(s);
            }

            ArrayList<String> m_included = new ArrayList<String>();
            ArrayList<String> m_excluded = new ArrayList<String>();
            if (domain != null) {
                for (DataCell dc : domain) {
                    // if possible value was in the settings...
                    if (includedAttr.contains(dc.toString())) {
                        // ... put it to included ...
                        m_included.add(dc.toString());
                    } else {
                        // ... else to excluded
                        m_excluded.add(dc.toString());
                    }
                }
            }
            config.loadDefaults(m_included.toArray(new String[m_included.size()]),
                m_excluded.toArray(new String[m_excluded.size()]), EnforceOption.EnforceInclusion);
        }
        m_filterPanel.loadConfiguration(config, domain);


    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        String selectedColumn = (String)m_columnSelection.getSelectedItem();
        CheckUtils.checkSettingNotNull(selectedColumn, "No nominal column selected (there is no nominal column in "
            + "the input or the upstream node is not executed)");
        settings.addString(CFG_SELECTED_COL, selectedColumn);
        NominalValueFilterConfiguration config = new NominalValueFilterConfiguration(CFG_CONFIGROOTNAME);
        m_filterPanel.saveConfiguration(config);
        config.saveConfiguration(settings);
    }

}
