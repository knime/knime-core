/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
        m_filterPanel = new ColumnFilterPanel(DoubleValue.class);
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
            final DataTableSpec[] specs) throws NotConfigurableException {
        // must check if there are at least two numeric columns
        int numColsCount = 0;
        for (DataColumnSpec c : specs[0]) {
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
        String[] includes = settings.getStringArray(
                LinRegLearnerNodeModel.CFG_VARIATES, new String[0]);
        String target = settings.getString(LinRegLearnerNodeModel.CFG_TARGET,
                null);
        boolean isCalcError = settings.getBoolean(
                LinRegLearnerNodeModel.CFG_CALC_ERROR, true);
        boolean includeAll = includes.length == 0;
        int first = settings.getInt(LinRegLearnerNodeModel.CFG_FROMROW, 1);
        int count = settings.getInt(LinRegLearnerNodeModel.CFG_ROWCNT, 10000);
        m_selectionPanel.update(specs[0], target);
        m_filterPanel.update(specs[0], includeAll, includes);
        // must hide the target from filter panel
        // updating m_filterPanel first does not work as the first
        // element in the spec will always be in the exclude list.
        String selected = m_selectionPanel.getSelectedColumn();
        if (selected != null) {
            DataColumnSpec colSpec = specs[0].getColumnSpec(selected);
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
        settings.addStringArray(LinRegLearnerNodeModel.CFG_VARIATES, includes);
        settings.addString(LinRegLearnerNodeModel.CFG_TARGET, target);
        settings.addBoolean(LinRegLearnerNodeModel.CFG_CALC_ERROR, isCalcError);
        settings.addInt(LinRegLearnerNodeModel.CFG_FROMROW, first);
        settings.addInt(LinRegLearnerNodeModel.CFG_ROWCNT, count);
    }
}
