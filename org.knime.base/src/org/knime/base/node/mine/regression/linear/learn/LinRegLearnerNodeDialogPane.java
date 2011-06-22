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
 *   Feb 22, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.learn;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilterPanel;
import org.knime.core.node.util.ColumnSelectionPanel;

/**
 * Dialog for the linear regression learner.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LinRegLearnerNodeDialogPane extends NodeDialogPane {
    private final ColumnFilterPanel m_filterPanel;

    private final ColumnSelectionPanel m_selectionPanel;

    private final JCheckBox m_isCalcErrorChecker;

    private final JSpinner m_firstSpinner;

    private final JSpinner m_countSpinner;

    /**
     * Create new dialog for linear regression model.
     */
    @SuppressWarnings("unchecked")
    public LinRegLearnerNodeDialogPane() {
        super();
        m_filterPanel = new ColumnFilterPanel(true, DoubleValue.class);
        m_selectionPanel = new ColumnSelectionPanel((Border)null,
                DoubleValue.class);
        JPanel panel = new JPanel(new BorderLayout());
        JPanel northPanel = new JPanel(new FlowLayout());
        northPanel.setBorder(BorderFactory.createTitledBorder("Target"));
        northPanel.add(m_selectionPanel);
        panel.add(northPanel, BorderLayout.NORTH);

        m_filterPanel.setBorder(BorderFactory.createTitledBorder("Values"));
        panel.add(m_filterPanel, BorderLayout.CENTER);

        m_isCalcErrorChecker = new JCheckBox("Calculate error in input data");
        m_isCalcErrorChecker.setToolTipText("Prints also the squared error "
                + "in the view (needs extra scan over data)");
        Box southPanel = Box.createHorizontalBox();
        southPanel.setBorder(BorderFactory
                .createTitledBorder("Statistics & View"));
        southPanel.add(m_isCalcErrorChecker);
        m_firstSpinner = new JSpinner(new SpinnerNumberModel(1, 1,
                Integer.MAX_VALUE, 10));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)m_firstSpinner
                .getEditor();
        editor.getTextField().setColumns(8);
        JPanel temp = new JPanel(new FlowLayout());
        temp.add(new JLabel("First row: "));
        temp.add(m_firstSpinner);
        southPanel.add(temp);
        m_countSpinner = new JSpinner(new SpinnerNumberModel(10000, 1,
                Integer.MAX_VALUE, 10));
        editor = (JSpinner.DefaultEditor)m_countSpinner.getEditor();
        editor.getTextField().setColumns(8);
        temp = new JPanel(new FlowLayout());
        temp.add(new JLabel("Row count: "));
        temp.add(m_countSpinner);
        southPanel.add(temp);

        panel.add(southPanel, BorderLayout.SOUTH);

        m_selectionPanel.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                Object selected = e.getItem();
                if (selected instanceof DataColumnSpec) {
                    // TODO use update mechanism on the filter panel instead of
                    // hiding columns
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
        // must check if there are at least two numeric columns
        int numColsCount = 0;
        DataTableSpec dts = (DataTableSpec)specs[0];
        for (DataColumnSpec c : dts) {
            if (c.getType().isCompatible(DoubleValue.class)) {
                numColsCount++;
                if (numColsCount >= 2) {
                    break;
                }
            }
        }
        if (numColsCount < 2) {
            throw new NotConfigurableException("Too few numeric columns "
                    + "(need at least 2): " + numColsCount);
        }
        boolean includeAll = settings.getBoolean(
                LinRegLearnerNodeModel.CFG_VARIATES_USE_ALL, false);
        String[] includes = settings.getStringArray(
                LinRegLearnerNodeModel.CFG_VARIATES, new String[0]);
        String target = settings.getString(LinRegLearnerNodeModel.CFG_TARGET,
                null);
        boolean isCalcError = settings.getBoolean(
                LinRegLearnerNodeModel.CFG_CALC_ERROR, true);
        int first = settings.getInt(LinRegLearnerNodeModel.CFG_FROMROW, 1);
        int count = settings.getInt(LinRegLearnerNodeModel.CFG_ROWCNT, 10000);
        m_selectionPanel.update(dts, target);
        m_filterPanel.setKeepAllSelected(includeAll);
        // if includes list is empty, put everything into the include list
        m_filterPanel.update(dts, includes.length == 0, includes);
        // must hide the target from filter panel
        // updating m_filterPanel first does not work as the first
        // element in the spec will always be in the exclude list.
        String selected = m_selectionPanel.getSelectedColumn();
        if (selected != null) {
            DataColumnSpec colSpec = dts.getColumnSpec(selected);
            m_filterPanel.hideColumns(colSpec);
        }
        m_isCalcErrorChecker.setSelected(isCalcError);

        m_firstSpinner.setValue(first);
        m_countSpinner.setValue(count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        String[] includes = m_filterPanel.getIncludedColumnSet().toArray(
                new String[0]);
        String target = m_selectionPanel.getSelectedColumn();
        boolean isCalcError = m_isCalcErrorChecker.isSelected();
        int first = (Integer)m_firstSpinner.getValue();
        int count = (Integer)m_countSpinner.getValue();
        boolean includeAll = m_filterPanel.isKeepAllSelected();
        if (includeAll) {
            settings.addBoolean(
                    LinRegLearnerNodeModel.CFG_VARIATES_USE_ALL, true);
        } else {
            settings.addStringArray(
                    LinRegLearnerNodeModel.CFG_VARIATES, includes);
        }
        settings.addString(LinRegLearnerNodeModel.CFG_TARGET, target);
        settings.addBoolean(LinRegLearnerNodeModel.CFG_CALC_ERROR, isCalcError);
        settings.addInt(LinRegLearnerNodeModel.CFG_FROMROW, first);
        settings.addInt(LinRegLearnerNodeModel.CFG_ROWCNT, count);
    }
}
