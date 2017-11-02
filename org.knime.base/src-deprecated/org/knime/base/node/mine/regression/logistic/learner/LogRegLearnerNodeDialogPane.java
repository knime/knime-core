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
 * -------------------------------------------------------------------
 *
 * History
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.logistic.learner;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilterPanel;
import org.knime.core.node.util.ColumnSelectionPanel;

/**
 * Dialog for the logistic regression learner.
 *
 * @author Heiko Hofer
 */
public final class LogRegLearnerNodeDialogPane extends NodeDialogPane {
    private ColumnFilterPanel m_filterPanel;
    private JComboBox m_targetReferenceCategory;
    private JCheckBox m_notSortTarget;


    private ColumnSelectionPanel m_selectionPanel;
    private JCheckBox m_notSortIncludes;
    private DataTableSpec m_inSpec;


    /**
     * Create new dialog for linear regression model.
     */
    public LogRegLearnerNodeDialogPane() {
        super();

        JPanel panel = new JPanel(new BorderLayout());
        JPanel northPanel = createTargetOptionsPanel();
        northPanel.setBorder(BorderFactory.createTitledBorder("Target"));
        panel.add(northPanel, BorderLayout.NORTH);

        JPanel centerPanel = createIncludesPanel();
        centerPanel.setBorder(BorderFactory.createTitledBorder("Values"));
        panel.add(centerPanel, BorderLayout.CENTER);

        addTab("Settings", panel);
    }


    /**
     * Create options panel for the target.
     */
    private JPanel createTargetOptionsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        c.insets = new Insets(5, 5, 0, 0);


        p.add(new JLabel("Target Column:"), c);

        c.gridx++;
        m_selectionPanel = new ColumnSelectionPanel(new EmptyBorder(0, 0, 0, 0), NominalValue.class);
        m_selectionPanel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                updateTargetCategories((DataCell)m_targetReferenceCategory.getSelectedItem());
            }
        });
        p.add(m_selectionPanel, c);

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Reference Category:"), c);

        c.gridx++;
        m_targetReferenceCategory = new JComboBox();
        p.add(m_targetReferenceCategory, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.weightx = 1;
        m_notSortTarget =
                new JCheckBox("Use order from target column domain (only relevant for output representation)");
        p.add(m_notSortTarget, c);


        m_selectionPanel.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                Object selected = e.getItem();
                if (selected instanceof DataColumnSpec) {
                    m_filterPanel.resetHiding();
                    m_filterPanel.hideColumns((DataColumnSpec)selected);
                }
            }
        });


        return p;
    }

    /**
     * Create options panel for the included columns.
     */
    private JPanel createIncludesPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        c.insets = new Insets(5, 5, 0, 0);


        m_filterPanel = new ColumnFilterPanel(true);
        p.add(m_filterPanel, c);

        c.gridy++;
        m_notSortIncludes = new JCheckBox("Use order from column domain (applies only to nominal columns). "
            + "First value is chosen as reference for dummy variables.");
        p.add(m_notSortIncludes, c);

        return p;
    }



    /**
     * Update list of target categories.
     */
    private void updateTargetCategories(final DataCell selectedItem) {
        m_targetReferenceCategory.removeAllItems();

        String selectedColumn = m_selectionPanel.getSelectedColumn();
        if (selectedColumn != null) {
            DataColumnSpec colSpec = m_inSpec.getColumnSpec(selectedColumn);
            if (null != colSpec) {
                // select last as default
                DataCell newSelectedItem = null;
                DataCell lastItem = null;
                final DataColumnDomain domain = colSpec.getDomain();
                if (domain.hasValues()) {
                    for (DataCell cell : domain.getValues()) {
                        m_targetReferenceCategory.addItem(cell);
                        lastItem = cell;
                        if (cell.equals(selectedItem)) {
                            newSelectedItem = selectedItem;
                        }
                    }
                    if (newSelectedItem == null) {
                        newSelectedItem = lastItem;
                    }
                    m_targetReferenceCategory.getModel().setSelectedItem(newSelectedItem);
                }
            }
        }
        m_targetReferenceCategory.setEnabled(m_targetReferenceCategory.getModel().getSize() > 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO s,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        LogRegLearnerSettings settings = new LogRegLearnerSettings();
        settings.loadSettingsForDialog(s);

        boolean includeAll = settings.getIncludeAll();
        String[] includes = settings.getIncludedColumns();
        String target = settings.getTargetColumn();

        m_inSpec = (DataTableSpec)specs[0];
        m_selectionPanel.update(m_inSpec, target);
        m_filterPanel.setKeepAllSelected(includeAll);
        // if includes is not set, put everything into the include list
        if (null != includes) {
            m_filterPanel.update(m_inSpec, false, includes);
        } else {
            m_filterPanel.update(m_inSpec, true, new String[0]);
        }
        // must hide the target from filter panel
        // updating m_filterPanel first does not work as the first
        // element in the spec will always be in the exclude list.
        String selected = m_selectionPanel.getSelectedColumn();
        if (null == selected) {
            for (DataColumnSpec colSpec : m_inSpec) {
                if (colSpec.getType().isCompatible(NominalValue.class)) {
                    selected = colSpec.getName();
                    break;
                }
            }
        }
        if (selected != null) {
            DataColumnSpec colSpec = m_inSpec.getColumnSpec(selected);
            m_filterPanel.hideColumns(colSpec);
        }


        updateTargetCategories(settings.getTargetReferenceCategory());

        m_notSortTarget.setSelected(!settings.getSortTargetCategories());
        m_notSortIncludes.setSelected(!settings.getSortIncludesCategories());
    }




    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO s)
            throws InvalidSettingsException {
        LogRegLearnerSettings settings = new LogRegLearnerSettings();
        settings.setIncludeAll(m_filterPanel.isKeepAllSelected());
        String[] includes = m_filterPanel.getIncludedColumnSet().toArray(
                new String[0]);
        settings.setIncludedColumns(includes);
        settings.setTargetColumn(m_selectionPanel.getSelectedColumn());
        settings.setTargetReferenceCategory((DataCell)m_targetReferenceCategory.getSelectedItem());
        settings.setSortTargetCategories(!m_notSortTarget.isSelected());
        settings.setSortIncludesCategories(!m_notSortIncludes.isSelected());

        settings.saveSettings(s);
    }
}
