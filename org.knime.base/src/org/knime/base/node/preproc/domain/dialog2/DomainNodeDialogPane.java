/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
package org.knime.base.node.preproc.domain.dialog2;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class DomainNodeDialogPane extends NodeDialogPane {

    private final DataColumnSpecFilterPanel m_filterPanelPossValues;
    private final DataColumnSpecFilterPanel m_filterPanelMinMax;
    private final JCheckBox m_maxValuesChecker;
    private final JSpinner m_maxValuesSpinner;
    private final JRadioButton m_possValUnselectedRetainButton;
    private final JRadioButton m_possValUnselectedDropButton;
    private final JRadioButton m_minMaxUnselectedRetainButton;
    private final JRadioButton m_minMaxUnselectedDropButton;

    /** Inits members, does nothing else. */
    public DomainNodeDialogPane() {
        m_filterPanelPossValues = new DataColumnSpecFilterPanel();
        m_filterPanelMinMax = new DataColumnSpecFilterPanel();
        m_maxValuesChecker = new JCheckBox(
                "Restrict number of possible values: ");
        SpinnerModel spinModel =
            new SpinnerNumberModel(DataContainer.MAX_POSSIBLE_VALUES, 1, Integer.MAX_VALUE, 10);
        m_maxValuesSpinner = new JSpinner(spinModel);
        JSpinner.DefaultEditor editor =
            (JSpinner.DefaultEditor) m_maxValuesSpinner.getEditor();
        editor.getTextField().setColumns(6);
        m_maxValuesChecker.addActionListener(new ActionListener() {
           @Override
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
        minMaxPanel.add(m_filterPanelMinMax, BorderLayout.CENTER);
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
        possValPanel.add(m_filterPanelPossValues, BorderLayout.CENTER);
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
        DataTableSpec spec = specs[0];
        if (spec.getNumColumns() == 0) {
            throw new NotConfigurableException("No data at input.");
        }

        DataColumnSpecFilterConfiguration possConfig = DomainNodeModel.createDCSFilterConfigurationPossVals();
        DataColumnSpecFilterConfiguration minMaxConfig = DomainNodeModel.createDCSFilterConfigurationMinMax();

        possConfig.loadConfigurationInDialog(settings, spec);
        minMaxConfig.loadConfigurationInDialog(settings, spec);
        m_filterPanelPossValues.loadConfiguration(possConfig, spec);
        m_filterPanelMinMax.loadConfiguration(minMaxConfig, spec);

        int maxPossValues =
            settings.getInt(DomainNodeModel.CFG_MAX_POSS_VALUES, DataContainer.MAX_POSSIBLE_VALUES);
        if ((maxPossValues >= 0) != m_maxValuesChecker.isSelected()) {
            m_maxValuesChecker.doClick();
        }
        m_maxValuesSpinner.setValue(maxPossValues >= 0 ? maxPossValues : DataContainer.MAX_POSSIBLE_VALUES);
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
        DataColumnSpecFilterConfiguration possConfig = DomainNodeModel.createDCSFilterConfigurationPossVals();
        DataColumnSpecFilterConfiguration minMaxConfig = DomainNodeModel.createDCSFilterConfigurationMinMax();
        m_filterPanelPossValues.saveConfiguration(possConfig);
        m_filterPanelMinMax.saveConfiguration(minMaxConfig);
        possConfig.saveConfiguration(settings);
        minMaxConfig.saveConfiguration(settings);
        int maxPossVals = m_maxValuesChecker.isSelected()
            ? (Integer)m_maxValuesSpinner.getValue() : -1;
        settings.addInt(DomainNodeModel.CFG_MAX_POSS_VALUES, maxPossVals);
        settings.addBoolean(DomainNodeModel.CFG_POSSVAL_RETAIN_UNSELECTED,
                m_possValUnselectedRetainButton.isSelected());
        settings.addBoolean(DomainNodeModel.CFG_MIN_MAX_RETAIN_UNSELECTED,
                m_minMaxUnselectedRetainButton.isSelected());
    }

}
