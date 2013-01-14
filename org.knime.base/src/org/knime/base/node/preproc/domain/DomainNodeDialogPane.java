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
 * -------------------------------------------------------------------
 * 
 * History
 *   Aug 12, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.domain;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class DomainNodeDialogPane extends NodeDialogPane {
    
    private final ColumnFilterPanel m_possValuesPanel;
    private final ColumnFilterPanel m_minMaxPanel;
    private final JCheckBox m_maxValuesChecker;
    private final JSpinner m_maxValuesSpinner;
    private final JRadioButton m_possValUnselectedRetainButton;
    private final JRadioButton m_possValUnselectedDropButton;
    private final JRadioButton m_minMaxUnselectedRetainButton;
    private final JRadioButton m_minMaxUnselectedDropButton;
    
    /** Inits members, does nothing else. */
    public DomainNodeDialogPane() {
        m_possValuesPanel = new ColumnFilterPanel();
        m_minMaxPanel = new ColumnFilterPanel();
        m_maxValuesChecker = new JCheckBox(
                "Restrict number of possible values: ");
        SpinnerModel spinModel = 
            new SpinnerNumberModel(60, 1, Integer.MAX_VALUE, 10);
        m_maxValuesSpinner = new JSpinner(spinModel);
        JSpinner.DefaultEditor editor = 
            (JSpinner.DefaultEditor) m_maxValuesSpinner.getEditor();
        editor.getTextField().setColumns(6);
        m_maxValuesChecker.addActionListener(new ActionListener() {
           public void actionPerformed(final ActionEvent e) {
                m_maxValuesSpinner.setEnabled(m_maxValuesChecker.isSelected());
           } 
        });
        m_minMaxUnselectedRetainButton = 
            new JRadioButton("Retain Min/Max Domain");
        m_minMaxUnselectedDropButton = 
            new JRadioButton("Drop Min/Max Domain");
        m_possValUnselectedRetainButton = 
            new JRadioButton("Retain Possible Value Domain");
        m_possValUnselectedDropButton = 
            new JRadioButton("Drop Possible Value Domain");
        addTab("Possible Values", createPossValueTab());
        addTab("Min & Max Values", createMinMaxTab());
    }
    
    private static final String UNSELECTED_LABEL = 
        "Columns in exclude list: ";
    
    private JPanel createMinMaxTab() {
        JPanel minMaxPanel = new JPanel(new BorderLayout());
        minMaxPanel.add(m_minMaxPanel, BorderLayout.CENTER);
        JPanel retainMinMaxPanel = new JPanel(new GridLayout(0, 1));
        retainMinMaxPanel.setBorder(
                BorderFactory.createTitledBorder(UNSELECTED_LABEL));
        ButtonGroup group = new ButtonGroup();
        group.add(m_minMaxUnselectedRetainButton);
        group.add(m_minMaxUnselectedDropButton);
        m_minMaxUnselectedRetainButton.doClick();
        retainMinMaxPanel.add(m_minMaxUnselectedRetainButton);
        retainMinMaxPanel.add(m_minMaxUnselectedDropButton);
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        southPanel.add(retainMinMaxPanel);
        minMaxPanel.add(southPanel, BorderLayout.SOUTH);
        return minMaxPanel;
    }
    
    private JPanel createPossValueTab() {
        JPanel possValPanel = new JPanel(new BorderLayout());
        possValPanel.add(m_possValuesPanel, BorderLayout.CENTER);
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel retainPossValPanel = new JPanel(new GridLayout(0, 1));
        retainPossValPanel.setBorder(
                BorderFactory.createTitledBorder(UNSELECTED_LABEL));
        ButtonGroup group = new ButtonGroup();
        group.add(m_possValUnselectedRetainButton);
        group.add(m_possValUnselectedDropButton);
        m_possValUnselectedRetainButton.doClick();
        retainPossValPanel.add(m_possValUnselectedRetainButton);
        retainPossValPanel.add(m_possValUnselectedDropButton);
        southPanel.add(retainPossValPanel);
        southPanel.add(new JLabel("   "));
        southPanel.add(m_maxValuesChecker);
        southPanel.add(m_maxValuesSpinner);
        possValPanel.add(southPanel, BorderLayout.SOUTH);
        return possValPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        if (specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException("No data at input.");
        }
        String[] stringCols = 
            DomainNodeModel.getAllCols(NominalValue.class, specs[0]); 
        String[] possCols = settings.getStringArray(
                DomainNodeModel.CFG_POSSVAL_COLS, stringCols);
        
        String[] dblCols = 
            DomainNodeModel.getAllCols(BoundedValue.class, specs[0]);
        String[] minMaxCols = settings.getStringArray(
                DomainNodeModel.CFG_MIN_MAX_COLS, dblCols);
        m_possValuesPanel.update(specs[0], false, possCols);
        m_minMaxPanel.update(specs[0], false, minMaxCols);
        int maxPossValues = 
            settings.getInt(DomainNodeModel.CFG_MAX_POSS_VALUES, 60);
        if ((maxPossValues >= 0) != m_maxValuesChecker.isSelected()) {
            m_maxValuesChecker.doClick();
        }
        m_maxValuesSpinner.setValue(maxPossValues >= 0 ? maxPossValues : 60);
        boolean possValRetainUnselected = settings.getBoolean(
                DomainNodeModel.CFG_POSSVAL_RETAIN_UNSELECTED, true);
        boolean minMaxRetainUnselected = settings.getBoolean(
                DomainNodeModel.CFG_MIN_MAX_RETAIN_UNSELECTED, true);
        if (possValRetainUnselected) {
            m_possValUnselectedRetainButton.doClick();
        } else {
            m_possValUnselectedDropButton.doClick();
        }
        if (minMaxRetainUnselected) {
            m_minMaxUnselectedRetainButton.doClick();
        } else {
            m_minMaxUnselectedDropButton.doClick();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        Set<String> possCols = m_possValuesPanel.getIncludedColumnSet();
        Set<String> minMaxCols = m_minMaxPanel.getIncludedColumnSet();
        settings.addStringArray(DomainNodeModel.CFG_POSSVAL_COLS, 
                possCols.toArray(new String[possCols.size()]));
        settings.addStringArray(DomainNodeModel.CFG_MIN_MAX_COLS, 
                minMaxCols.toArray(new String[minMaxCols.size()]));
        int maxPossVals = m_maxValuesChecker.isSelected() 
            ? (Integer)m_maxValuesSpinner.getValue() : -1;
        settings.addInt(DomainNodeModel.CFG_MAX_POSS_VALUES, maxPossVals);
        settings.addBoolean(DomainNodeModel.CFG_POSSVAL_RETAIN_UNSELECTED, 
                m_possValUnselectedRetainButton.isSelected());
        settings.addBoolean(DomainNodeModel.CFG_MIN_MAX_RETAIN_UNSELECTED, 
                m_minMaxUnselectedRetainButton.isSelected());
    }

}
