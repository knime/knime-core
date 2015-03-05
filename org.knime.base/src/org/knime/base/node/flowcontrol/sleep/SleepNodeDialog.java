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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.flowcontrol.sleep;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * @author Tim-Oliver Buchholz, Knime.com, Zurich, Switzerland
 */
public class SleepNodeDialog extends NodeDialogPane {

    private FilesHistoryPanel m_fileChooser;

    private JRadioButton m_fileRB;

    private AbstractButton m_toRB;

    private JRadioButton m_forRB;

    private int m_selection;

    private DialogComponentButtonGroup m_events;

    private SpinnerDateModel m_waitToSpinnerModel;

    private SpinnerDateModel m_waitForSpinnerModel;

    private JSpinner m_forSpinner;

    private JSpinner m_toSpinner;

    /**
     *
     */
    public SleepNodeDialog() {
        waitForTimePanel();
        waitToTimePanel();
        waitForFile();
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 4, 2, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;

        p.add(m_forRB, c);

        c.gridx++;

        p.add(m_forSpinner, c);

        c.gridy++;
        c.gridx = 0;

        p.add(m_toRB, c);

        c.gridx++;

        p.add(m_toSpinner, c);

        c.gridy++;
        c.gridx = 0;

        p.add(m_fileRB, c);

        c.gridx++;

        p.add(m_events.getComponentPanel(), c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;

        p.add(m_fileChooser, c);

        ButtonGroup selection = new ButtonGroup();
        selection.add(m_forRB);
        selection.add(m_toRB);
        selection.add(m_fileRB);
        m_fileRB.doClick();
        m_toRB.doClick();
        m_forRB.doClick();

        addTab("Options", p);
    }

    private void waitForTimePanel() {
        m_waitForSpinnerModel = new SpinnerDateModel();
        m_forSpinner = new JSpinner(m_waitForSpinnerModel);
        m_forSpinner.setEditor(new JSpinner.DateEditor(m_forSpinner, "HH:mm:ss"));

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        m_waitForSpinnerModel.setValue(cal.getTime());

        m_forRB = new JRadioButton("Wait for time:");
        m_forRB.doClick();
        m_forRB.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (m_forRB.isSelected()) {
                    m_selection = 0;
                }
                m_forSpinner.setEnabled(m_forRB.isSelected());
            }
        });
    }

    private void waitToTimePanel() {

        m_waitToSpinnerModel = new SpinnerDateModel();
        m_toSpinner = new JSpinner(m_waitToSpinnerModel);
        m_toSpinner.setEditor(new JSpinner.DateEditor(m_toSpinner, "HH:mm:ss"));

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        m_waitToSpinnerModel.setValue(cal.getTime());

        m_toRB = new JRadioButton("Wait to time:");
        m_toRB.doClick();
        m_toRB.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (m_toRB.isSelected()) {
                    m_selection = 1;
                }
                m_toSpinner.setEnabled(m_toRB.isSelected());
            }
        });
    }

    private void waitForFile() {
        m_events =
            new DialogComponentButtonGroup(new SettingsModelString(SleepNodeModel.CFGKEY_FILESTATUS, "Modification"),
                false, null, "Creation", "Modification", "Deletion");

        FlowVariableModel fvm = createFlowVariableModel(SleepNodeModel.CFGKEY_FILEPATH, Type.STRING);

        m_fileChooser = new FilesHistoryPanel(fvm, SleepNodeModel.CFGKEY_FILEPATH, LocationValidation.None);

        m_fileRB = new JRadioButton("Wait for file.. ");
        m_fileRB.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (m_fileRB.isSelected()) {
                    m_selection = 2;
                }
                m_fileChooser.setEnabled(m_fileRB.isSelected());
                m_events.getModel().setEnabled(m_fileRB.isSelected());
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);

        c.set(Calendar.HOUR_OF_DAY, settings.getInt(SleepNodeModel.CFGKEY_FORHOURS, 0));
        c.set(Calendar.MINUTE, settings.getInt(SleepNodeModel.CFGKEY_FORMINUTES, 0));
        c.set(Calendar.SECOND, settings.getInt(SleepNodeModel.CFGKEY_FORSECONDS, 0));

        m_waitForSpinnerModel.setValue(c.getTime());

        c.set(Calendar.HOUR_OF_DAY, settings.getInt(SleepNodeModel.CFGKEY_TOHOURS, 0));
        c.set(Calendar.MINUTE, settings.getInt(SleepNodeModel.CFGKEY_TOMINUTES, 0));
        c.set(Calendar.SECOND, settings.getInt(SleepNodeModel.CFGKEY_TOSECONDS, 0));

        m_waitToSpinnerModel.setValue(c.getTime());

        m_fileChooser.setSelectedFile(settings.getString(SleepNodeModel.CFGKEY_FILEPATH, ""));

        try {
            m_events.loadSettingsFrom(settings, specs);
            m_fileRB.doClick();
        } catch (NotConfigurableException e) {
            // nothing
        }

        m_selection = settings.getInt(SleepNodeModel.CFGKEY_WAITOPTION, 0);
        if (m_selection == 0) {
            m_forRB.doClick();
        } else if (m_selection == 1) {
            m_toRB.doClick();
        } else {
            m_fileRB.doClick();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

        Calendar c = Calendar.getInstance();
        c.setTime((Date)m_waitForSpinnerModel.getValue());

        settings.addInt(SleepNodeModel.CFGKEY_FORHOURS, c.get(Calendar.HOUR_OF_DAY));
        settings.addInt(SleepNodeModel.CFGKEY_FORMINUTES, c.get(Calendar.MINUTE));
        settings.addInt(SleepNodeModel.CFGKEY_FORSECONDS, c.get(Calendar.SECOND));

        c.setTime((Date)m_waitToSpinnerModel.getValue());

        settings.addInt(SleepNodeModel.CFGKEY_TOHOURS, c.get(Calendar.HOUR_OF_DAY));
        settings.addInt(SleepNodeModel.CFGKEY_TOMINUTES, c.get(Calendar.MINUTE));
        settings.addInt(SleepNodeModel.CFGKEY_TOSECONDS, c.get(Calendar.SECOND));

        settings.addString(SleepNodeModel.CFGKEY_FILEPATH, m_fileChooser.getSelectedFile());
        m_events.saveSettingsTo(settings);

        settings.addInt(SleepNodeModel.CFGKEY_WAITOPTION, m_selection);
    }

}
