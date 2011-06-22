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
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

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
    private final ColumnFilterPanel m_filterPanel;

    private final ColumnSelectionPanel m_selectionPanel;

    private final LogRegLearnerSettings m_settings;

    /**
     * Create new dialog for linear regression model.
     */
    @SuppressWarnings("unchecked")
    public LogRegLearnerNodeDialogPane() {
        super();
        m_settings = new LogRegLearnerSettings();
        m_filterPanel = new ColumnFilterPanel(true);
        m_selectionPanel = new ColumnSelectionPanel(new EmptyBorder(0, 0, 0, 0),
                NominalValue.class);
        JPanel panel = new JPanel(new BorderLayout());
        JPanel northPanel = new JPanel(new FlowLayout());
        northPanel.setBorder(BorderFactory.createTitledBorder("Target"));
        northPanel.add(m_selectionPanel);
        panel.add(northPanel, BorderLayout.NORTH);

        m_filterPanel.setBorder(BorderFactory.createTitledBorder("Values"));
        panel.add(m_filterPanel, BorderLayout.CENTER);

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

        addTab("Settings", panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        boolean includeAll = m_settings.getIncludeAll();
        String[] includes = m_settings.getIncludedColumns();
        String target = m_settings.getTargetColumn();

        DataTableSpec dts = (DataTableSpec)specs[0];
        m_selectionPanel.update(dts, target);
        m_filterPanel.setKeepAllSelected(includeAll);
        // if includes is not set, put everything into the include list
        if (null != includes) {
            m_filterPanel.update(dts, false, includes);
        } else {
            m_filterPanel.update(dts, true, new String[0]);
        }
        // must hide the target from filter panel
        // updating m_filterPanel first does not work as the first
        // element in the spec will always be in the exclude list.
        String selected = m_selectionPanel.getSelectedColumn();
        if (null == selected) {
            for (DataColumnSpec colSpec : dts) {
                if (colSpec.getType().isCompatible(NominalValue.class)) {
                    selected = colSpec.getName();
                    break;
                }
            }
        }
        if (selected != null) {
            DataColumnSpec colSpec = dts.getColumnSpec(selected);
            m_filterPanel.hideColumns(colSpec);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.setIncludeAll(m_filterPanel.isKeepAllSelected());
        String[] includes = m_filterPanel.getIncludedColumnSet().toArray(
                new String[0]);
        m_settings.setIncludedColumns(includes);
        m_settings.setTargetColumn(m_selectionPanel.getSelectedColumn());

        m_settings.saveSettings(settings);
    }
}
