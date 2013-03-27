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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.quickform;

import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.node.quickform.in.AbstractQuickFormInElement;

/**
 *
 * @author Thomas Gabriel, KNIME.com, Zurich, Switzerland
 *
 * @param <CFG> type configuration
 */
public abstract class QuickFormConfigurationPanel
    <CFG extends AbstractQuickFormValueInConfiguration> extends JPanel {


    /**
     * Creates a new QuickForm configuration panel.
     */
    public QuickFormConfigurationPanel() {
        super();
    }

    /**
     * @param layout */
    public QuickFormConfigurationPanel(final LayoutManager layout) {
        super(layout);
    }

    /**
     * Save quickform configuration.
     * @param config save into
     * @throws InvalidSettingsException if settings can't be saved
     */
    public abstract void saveSettings(final CFG config)
            throws InvalidSettingsException;

    /**
     * Load quickform configuration.
     * @param config load from
     */
    public abstract void loadSettings(final CFG config);

    /**
     * Creates a new AbstractQuickFormInElement with the current configuration of this panel.
     * @param e the QuickForm element to be filled with the current configuration
     * @throws InvalidSettingsException if settings can't updated
     * @since 2.6
     */
    public abstract void updateQuickFormInElement(final AbstractQuickFormInElement e)
            throws InvalidSettingsException;

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
