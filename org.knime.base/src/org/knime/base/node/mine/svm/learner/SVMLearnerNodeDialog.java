/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.kernel.KernelFactory;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;

/**
 * 
 * @author cebron, University of Konstanz
 */
public class SVMLearnerNodeDialog extends NodeDialogPane 
        implements ItemListener {
    
    private ArrayList<DialogComponent> m_components;

    private DialogComponentNumber[] m_kernelParameters;

    private JComboBox m_comboKernelTypes;
    
    private JPanel m_panel;

    /**
     * Constructor.
     */
    public SVMLearnerNodeDialog() {
        super();

        m_components = new ArrayList<DialogComponent>();

        m_kernelParameters = null;

        m_panel = new JPanel();
        m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.Y_AXIS));

        super.addTab("Options", m_panel);

        this.addDialogComponent(new DialogComponentNumber(
                new SettingsModelDouble(SVMLearnerNodeModel.CFG_PARAMC,
                        SVMLearnerNodeModel.DEFAULT_PARAMC),
                "Overlapping penalty: ", .1));
        
        m_comboKernelTypes = new JComboBox(KernelFactory.getKernelNames());
        m_comboKernelTypes.addItemListener(this);

        JPanel kernelTypePanel = new JPanel();
        kernelTypePanel.setLayout(new BoxLayout(kernelTypePanel,
                BoxLayout.X_AXIS));
        kernelTypePanel.add(new JLabel("Kernel type: "));
        kernelTypePanel.add(m_comboKernelTypes);

        m_panel.add(kernelTypePanel);

        int count = KernelFactory.getMaximalParameters();
        m_kernelParameters = new DialogComponentNumber[count];
        for (int i = 0; i < count; ++i) {
            m_kernelParameters[i] =
                    new DialogComponentNumber(new SettingsModelDouble(
                            SVMLearnerNodeModel.CFG_KERNELPARAM + i, 0),
                            "Parameter " + i, .1);
            this.addDialogComponent(m_kernelParameters[i]);
            m_kernelParameters[i].getModel().setEnabled(false);
        }
    }

    /**
     * Did something change?
     * 
     * {@inheritDoc}
     */
    public void itemStateChanged(final ItemEvent e) {
        if (m_kernelParameters != null) {
            for (int i = 0; i < m_kernelParameters.length; ++i) {
                delDialogComponent(m_kernelParameters[i]);
            }
            m_kernelParameters = null;
        }

        String s = m_comboKernelTypes.getSelectedItem().toString();
        Kernel kernel = KernelFactory.getKernel(s);
        int count = kernel.getNumberParameters();
        for (int i = 0; i < count; ++i) {
            m_kernelParameters[i] =
                    new DialogComponentNumber(new SettingsModelDouble(
                            SVMLearnerNodeModel.CFG_KERNELPARAM + i, 0),
                            "Parameter " + i, .1);
            this.addDialogComponent(m_kernelParameters[i]);
            m_kernelParameters[i].getComponentPanel().invalidate();
        }

        super.removeTab("Options");
        super.addTab("Options", m_panel);
    }

    /**
     * add component.
     * @param component which one
     */
    private void addDialogComponent(final DialogComponent component) {
        m_components.add(component);
        m_panel.add(component.getComponentPanel());
    }

    /**
     * remove component.
     * @param component which one
     */
    private void delDialogComponent(final DialogComponent component) {
        m_components.remove(component);
        m_panel.remove(component.getComponentPanel());
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, 
                final DataTableSpec[] specs) throws NotConfigurableException {
        try {
            for (DialogComponent comp : m_components) {
                comp.loadSettingsFrom(settings, specs);
            }

            String kernelType = settings
                    .getString(SVMLearnerNodeModel.CFG_KERNELTYPE);
            for (int i = 0; i < m_comboKernelTypes.getItemCount(); ++i) {
                String s = m_comboKernelTypes.getItemAt(i).toString();
                if (s.equals(kernelType)) {
                    m_comboKernelTypes.setSelectedIndex(i);
                    break;
                }
            }
        } catch (InvalidSettingsException ise) {
            // leave everything as it is. 
        }
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        for (DialogComponent comp : m_components) {
            comp.saveSettingsTo(settings);
        }
        String s = m_comboKernelTypes.getSelectedItem().toString();
        settings.addString(SVMLearnerNodeModel.CFG_KERNELTYPE, s);
    }
}
