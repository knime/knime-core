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
import org.knime.base.node.mine.svm.kernel.KernelFactory.KernelType;
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
import org.knime.core.node.port.PortObjectSpec;

/**
 * Dialog for the SVM Learner. Lets the user choose the overlapping penalty,
 * class column and the kernel with its parameters.
 *
 * @author cebron, University of Konstanz
 */
public class SVMLearnerNodeDialog extends NodeDialogPane {

    private final JPanel m_panel;

    private final ArrayList<DialogComponent> m_components;

    private final ArrayList<JRadioButton> m_kernels;

    private final ArrayList<KernelPanel> m_kernelPanels;

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
        HashMap<KernelType, Vector<SettingsModelDouble>> kernelparams =
                SVMLearnerNodeModel.createKernelParams();
        for (Map.Entry<KernelType, Vector<SettingsModelDouble>>
        entry : kernelparams
                .entrySet()) {
            KernelPanel kernelpanel = new KernelPanel();
            kernelpanel.setLayout(new GridLayout(3, 1));
            Border paramborder =
                    BorderFactory.createTitledBorder("");
            kernelpanel.setBorder(paramborder);
            final JRadioButton kernelbutton = new JRadioButton(
                    entry.getKey().toString());

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
                @Override
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
            final PortObjectSpec[] specs) throws NotConfigurableException {
        for (DialogComponent comp : m_components) {
            comp.loadSettingsFrom(settings, specs);
        }
        String selected =
                settings.getString(SVMLearnerNodeModel.CFG_KERNELTYPE,
                        KernelFactory.getDefaultKernelType().toString());
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
