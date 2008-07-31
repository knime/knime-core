/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   27.09.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.learner;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.kernel.KernelFactory;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Dialog for the SVM Learner. Lets the user choose the overlapping penalty,
 * class column and the kernel with its parameters.
 * 
 * @author cebron, University of Konstanz
 */
public class SVMLearnerNodeDialog extends NodeDialogPane {

    private JPanel m_panel;

    private ArrayList<DialogComponent> m_components;

    private ArrayList<JRadioButton> m_kernels;

    private ArrayList<KernelPanel> m_kernelPanels;

    /**
     * Constructor.
     */
    @SuppressWarnings("unchecked")
    public SVMLearnerNodeDialog() {
        super();
        m_components = new ArrayList<DialogComponent>();
        m_panel = new JPanel();
        m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.Y_AXIS));
        super.addTab("Options", m_panel);
        this.addDialogComponent(new DialogComponentColumnNameSelection(
                new SettingsModelString(SVMLearnerNodeModel.CFG_CLASSCOL, ""),
                "Class column", 0, StringValue.class));

        this.addDialogComponent(new DialogComponentNumber(
                new SettingsModelDouble(SVMLearnerNodeModel.CFG_PARAMC,
                        SVMLearnerNodeModel.DEFAULT_PARAMC),
                "Overlapping penalty: ", .1));

        JPanel kernelsettingsPanel = new JPanel();
        kernelsettingsPanel.setLayout(new BoxLayout(kernelsettingsPanel,
                BoxLayout.Y_AXIS));
        Border titleborder =
                BorderFactory.createTitledBorder("Choose your kernel"
                        + " and parameters:");
        kernelsettingsPanel.setBorder(titleborder);
        ButtonGroup group = new ButtonGroup();
        m_kernels = new ArrayList<JRadioButton>();
        m_kernelPanels = new ArrayList<KernelPanel>();
        HashMap<String, Vector<SettingsModelDouble>> kernelparams =
                SVMLearnerNodeModel.createKernelParams();
        for (Map.Entry<String, Vector<SettingsModelDouble>> entry : kernelparams
                .entrySet()) {
            KernelPanel kernelpanel = new KernelPanel();
            kernelpanel.setLayout(new GridLayout(3, 1));
            Border paramborder =
                    BorderFactory.createTitledBorder("");
            kernelpanel.setBorder(paramborder);
            final JRadioButton kernelbutton = new JRadioButton(entry.getKey());

            group.add(kernelbutton);
            m_kernels.add(kernelbutton);
            kernelpanel.add(kernelbutton, BorderLayout.WEST);
            Vector<SettingsModelDouble> kernelsettings = entry.getValue();
            Kernel kernel = KernelFactory.getKernel(entry.getKey());
            int i = 0;
            for (SettingsModelDouble smd : kernelsettings) {
                DialogComponent comp =
                        new DialogComponentNumber(smd, kernel
                                .getParameterName(i), .1);
                m_components.add(comp);
                kernelpanel.addComponent(comp);
                i++;
            }
            m_kernelPanels.add(kernelpanel);
            kernelbutton.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    if (kernelbutton.isSelected()) {
                        for (int j = 0; j < m_kernels.size(); j++) {
                            JRadioButton button = m_kernels.get(j);
                            if (!button.equals(kernelbutton)) {
                                m_kernelPanels.get(j).setAllEnabled(false);
                            } else {
                                m_kernelPanels.get(j).setAllEnabled(true);
                            }
                        }
                    }
                }

            });
            kernelsettingsPanel.add(kernelpanel);
        }
        m_panel.add(kernelsettingsPanel);
    }

    /**
     * add component.
     * 
     * @param component which one
     */
    private void addDialogComponent(final DialogComponent component) {
        m_components.add(component);
        m_panel.add(component.getComponentPanel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        for (DialogComponent comp : m_components) {
            comp.loadSettingsFrom(settings, specs);
        }
        String selected =
                settings.getString(SVMLearnerNodeModel.CFG_KERNELTYPE,
                        KernelFactory.getDefaultKernelType());
        for (JRadioButton button : m_kernels) {
            if (button.getText().equals(selected)) {
                button.setSelected(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        for (DialogComponent comp : m_components) {
            comp.saveSettingsTo(settings);
        }
        for (JRadioButton button : m_kernels) {
            if (button.isSelected()) {
                String s = button.getText();
                settings.addString(SVMLearnerNodeModel.CFG_KERNELTYPE, s);
            }
        }
    }

}
