/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
