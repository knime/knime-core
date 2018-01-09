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
 * Created on Mar 17, 2013 by wiswedel
 */
package org.knime.base.node.preproc.domain.editnumeric;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.base.node.preproc.domain.editnumeric.EditNumericDomainNodeModel.DomainOverflowPolicy;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * Dialog to node.
 *
 * @author Marcel Hanser
 */
final class EditNumericDomainNodeDialogPane extends NodeDialogPane {

    /**
     *
     */
    private static final String UPPER_B = "Upper Bound";

    /**
     *
     */
    private static final String LOWER_B = "Lower Bound";

    private DataColumnSpecFilterPanel m_filterPanel;

    private JTextField m_lowerBField;

    private JTextField m_upperBField;

    private DomainOverflowPolicy m_handler;

    private ButtonGroup m_buttonGrp;

    private static final InputVerifier DOUBLE_VERFIER = new InputVerifier() {

        @Override
        public boolean verify(final JComponent input) {
            try {
                Double.valueOf(((JTextField)input).getText());
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    };

    /** Inits members, does nothing else. */
    public EditNumericDomainNodeDialogPane() {
        createMinMaxTab();
    }

    private void createMinMaxTab() {

        m_filterPanel = new DataColumnSpecFilterPanel();
        m_filterPanel.setIncludeTitle(" Include ");
        m_filterPanel.setExcludeTitle(" Exclude ");

        m_lowerBField = createTextField("0.0");
        m_upperBField = createTextField("1.0");

        JPanel retainMinMaxPanel = new JPanel(new GridLayout(2, 2));
        retainMinMaxPanel.setBorder(BorderFactory.createTitledBorder("Domain"));

        createBoundedBox(retainMinMaxPanel, m_lowerBField, LOWER_B + ": ");
        createBoundedBox(retainMinMaxPanel, m_upperBField, UPPER_B + ": ");

        Box outOfDomainBox = Box.createVerticalBox();
        outOfDomainBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
            "Out of Domain Policy"));

        m_buttonGrp = new ButtonGroup();

        //        ViewUtils

        for (final DomainOverflowPolicy handler : DomainOverflowPolicy.values()) {
            JRadioButton jRadioButton = new JRadioButton(handler.getDescription());
            jRadioButton.setActionCommand(handler.toString());
            jRadioButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    m_handler = handler;
                }
            });
            m_buttonGrp.add(jRadioButton);
            outOfDomainBox.add(jRadioButton, Component.LEFT_ALIGNMENT);
        }

        JPanel tabpanel = new JPanel();
        tabpanel.setLayout(new BorderLayout());
        tabpanel.add(m_filterPanel, BorderLayout.NORTH);
        tabpanel.add(retainMinMaxPanel, BorderLayout.WEST);
        outOfDomainBox.add(Box.createHorizontalGlue());
        tabpanel.add(outOfDomainBox, BorderLayout.SOUTH);
        addTab("Edit Domain", tabpanel);
    }

    /**
     * @param retainMinMaxPanel
     */
    private void createBoundedBox(final JPanel retainMinMaxPanel, final JTextField field, final String label) {
        retainMinMaxPanel.add(ViewUtils.getInFlowLayout(new JLabel(label)));
        retainMinMaxPanel.add(ViewUtils.getInFlowLayout(field));
    }

    /**
     * @param string
     * @return
     */
    private JTextField createTextField(final String defaultVal) {
        JTextField field = new JTextField(defaultVal, 8);
        field.setInputVerifier(DOUBLE_VERFIER);
        return field;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        if (specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException("No data at input.");
        }
        EditNumericDomainConfiguration forConfig = new EditNumericDomainConfiguration();

        forConfig.loadConfigurationInDialog(settings, specs[0]);
        m_filterPanel.loadConfiguration(forConfig.getColumnspecFilterConfig(), specs[0]);

        m_upperBField.setText(Double.isNaN(forConfig.getUpperBound()) ? "1.0" : Double.toString(forConfig
            .getUpperBound()));
        m_lowerBField.setText(Double.isNaN(forConfig.getLowerBound()) ? "0.0" : Double.toString(forConfig
            .getLowerBound()));

        m_handler = forConfig.getDomainOverflowPolicy();
        Enumeration<AbstractButton> elements = m_buttonGrp.getElements();
        while (elements.hasMoreElements()) {
            AbstractButton abstractButton = elements.nextElement();
            if (abstractButton.getActionCommand().equals(m_handler.toString())) {
                abstractButton.setSelected(true);
            }
        }
    }

    private static double getDouble(final String name, final JTextField inputField) throws InvalidSettingsException {
        try {
            return Double.valueOf(inputField.getText());
        } catch (Exception e) {
            CheckUtils.checkSetting(false, "%s must be a valid double value", name);
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        EditNumericDomainConfiguration configuration = new EditNumericDomainConfiguration();

        double max = getDouble(UPPER_B, m_upperBField);
        double min = getDouble(LOWER_B, m_lowerBField);

        CheckUtils.checkSetting(max >= min, "%s must be >= %s", UPPER_B, LOWER_B);

        configuration.setMax(max);
        configuration.setMin(min);
        configuration.setColumnspecFilterCofig(m_filterPanel);
        configuration.setDomainOverflowPolicy(m_handler);

        configuration.saveSettings(settings);
    }
}
