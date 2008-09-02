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
 *   06.10.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.learner;

import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.DialogComponent;

/**
 * Panel for kernel parameter values. Allows for enabling/disabling
 * the components on the panel.
 *
 * @author cebron, University of Konstanz
 */
public class KernelPanel extends JPanel {
    /**
     *
     */
    private static final long serialVersionUID = -5032637076823894980L;

    /*
     * DialogComponents in the panel.
     */
    private ArrayList<DialogComponent> m_components;

    /**
     * Constructor.
     */
    KernelPanel() {
        super();
        super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        m_components = new ArrayList<DialogComponent>();
    }

    /**
     * @param comp {@link DialogComponent} to add  to the panel.
     */
    void addComponent(final DialogComponent comp) {
        m_components.add(comp);
        super.add(comp.getComponentPanel());
    }

    /**
     * Sets all components in the panel in the enabled state.
     * @param enabled the state to set.
     */
    void setAllEnabled(final boolean enabled) {
        for (DialogComponent comp : m_components) {
           comp.getModel().setEnabled(enabled);
        }
    }
}
