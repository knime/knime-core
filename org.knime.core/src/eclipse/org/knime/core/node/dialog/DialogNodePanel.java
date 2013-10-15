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
 * ---------------------------------------------------------------------
 *
 * Created on 08.10.2013 by Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 * @param <VAL> The node value implementation of the dialog node.
 * @since 2.9
 */
@SuppressWarnings("serial")
public abstract class DialogNodePanel<VAL extends DialogNodeValue>
        extends JPanel {

    /**
     * Creates a new DialogNode panel.
     */
    public DialogNodePanel() {
        super();
    }

    /**
     * Create a new buffered DialogNodePanel with the specified layout manager.
     * @param layout the LayoutManager to use
     */
    public DialogNodePanel(final LayoutManager layout) {
        super(layout);
    }

    /**
     * Save DialogNode value.
     * @param value save into
     * @throws InvalidSettingsException if settings can't be saved
     */
    public abstract void saveNodeValue(final VAL value) throws InvalidSettingsException;

    /**
     * Load DialogNode value.
     * @param value load from
     */
    public abstract void loadNodeValue(final VAL value);

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBackground(final Color bg) {
        int max = getComponentCount();
        for (int i = 0; i < max; i++) {
            Component c = getComponent(i);
            if ((c instanceof AbstractButton) || (c instanceof JPanel) || (c instanceof JLabel)) {
                c.setBackground(bg);
            }
        }
        super.setBackground(bg);
    }

}
