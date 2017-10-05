/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   07.01.2015 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Default panel for a missing value handler's settings to be used with DialogComponents.
 * @author Alexander Fillbrunn
 */
public class DefaultMissingValueHandlerPanel extends MissingValueHandlerPanel {

    private List<DialogComponent> m_comp;

    /**
     * Default constructor for DefaultMissingValueHandlerPanel.
     */
    public DefaultMissingValueHandlerPanel() {
        m_comp = new ArrayList<DialogComponent>();
        this.setLayout(new GridBagLayout());
    }

    /**
     * Adds a dialog component to this panel.
     * @param c the component
     */
    public final void addDialogComponent(final DialogComponent c) {
        m_comp.add(c);
        JPanel p = c.getComponentPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = m_comp.size() - 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        this.add(p, gbc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        for (DialogComponent dc : m_comp) {
            dc.saveSettingsTo(settings);
        }
        saveAdditionalSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
                                            throws NotConfigurableException, InvalidSettingsException {
        for (DialogComponent dc : m_comp) {
            dc.loadSettingsFrom(settings, specs);
        }
        loadAdditionalSettingsFrom(settings);
    }

    /**
     * Saves additional settings in extending classes.
     * @param settings the settings to save to
     * @throws InvalidSettingsException when the settings cannot be saved
     */
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        // Nothing to do here
    }

    /**
     * Loads additional settings in extending classes.
     * @param settings the settings to load from
     * @throws NotConfigurableException when the loading fails
     */
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings) throws NotConfigurableException {
        // Nothing to do here
    }
}
