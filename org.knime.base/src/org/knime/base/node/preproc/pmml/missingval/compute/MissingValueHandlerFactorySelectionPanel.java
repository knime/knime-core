/*
 * ------------------------------------------------------------------------
 *
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
 *   17.12.2014 (Alexander): created
 */
package org.knime.base.node.preproc.pmml.missingval.compute;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.node.preproc.pmml.missingval.DefaultMissingValueHandlerPanel;
import org.knime.base.node.preproc.pmml.missingval.MVIndividualSettings;
import org.knime.base.node.preproc.pmml.missingval.MissingCellHandlerFactory;
import org.knime.base.node.preproc.pmml.missingval.MissingCellHandlerFactoryManager;
import org.knime.base.node.preproc.pmml.missingval.MissingValueHandlerPanel;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * A panel that shows the user a selection of available missing value handler factories and possible options for them.
 * The user can select a factory and make adjustments to its settings.
 *
 * @author Alexander Fillbrunn
 * @since 3.5
 * @noreference This class is not intended to be referenced by clients.
 */
public class MissingValueHandlerFactorySelectionPanel extends JPanel implements ActionListener {

    private static final int ALPHA_MASK = -16777216;

    /**
     * Event identifier for the event that is fired when the selected factory changes.
     */
    public static final String SELECTED_FACTORY_CHANGE = "selectedFactoryChanged";

    private final JComboBox<MissingCellHandlerFactory> m_comboBox;

    private final JPanel m_argumentsPanel;

    private final HashMap<String, MissingValueHandlerPanel> m_valueHandlerPanels;

    private final MissingCellHandlerFactoryManager m_handlerFactoryManager;

    /**
     * Constructor for the MissingValueHandlerFactorySelectionPanel.
     * @param dt the data type this panel is for
     * @param s the current settings for this data type
     * @param factoryManager manager keeping the missing value factories
     * @param specs the input specs
     */
    public MissingValueHandlerFactorySelectionPanel(final DataType dt, final MVIndividualSettings s,
        final MissingCellHandlerFactoryManager factoryManager, final PortObjectSpec[] specs) {

        this(new DataType[]{dt}, s, factoryManager, specs);
    }

    /**
     * Constructor for the MissingValueHandlerFactorySelectionPanel.
     * @param dt the data types this panel is for
     * @param s the current settings for this data type
     * @param factoryManager manager keeping the missing value factories
     * @param specs the input specs
     */
    public MissingValueHandlerFactorySelectionPanel(final DataType[] dt, final MVIndividualSettings s,
        final MissingCellHandlerFactoryManager factoryManager, final PortObjectSpec[] specs) {

        // Each settings panel for the different factories is a card in a card layout.
        // The change of the cards is triggered by the combo box.
        m_argumentsPanel = new JPanel(new DynamicCardLayout());
        m_valueHandlerPanels = new HashMap<>();
        m_comboBox = new JComboBox<MissingCellHandlerFactory>();
        for (MissingCellHandlerFactory fac : factoryManager.getFactoriesSorted(dt)) {
            m_comboBox.addItem(fac);
            if (fac.hasSettingsPanel()) {
                MissingValueHandlerPanel panel = fac.getSettingsPanel();
                try {
                    panel.loadSettingsFrom(s.getSettings(), specs);
                    m_argumentsPanel.add(panel, fac.getID());
                    m_valueHandlerPanels.put(fac.getID(), panel);
                } catch (Exception e) {
                    m_argumentsPanel.add(new LoadingErrorMissingValueHandlerPanel(e.getMessage()), fac.getID());
                }
            } else {
                m_argumentsPanel.add(new EmptyMissingValueHandlerPanel(), fac.getID());
            }
        }
        // Add listener to respond to a changing selection so the settings panel cards can be updated
        m_comboBox.addActionListener(this);
        // Now set the selected item. The change action event is triggered and the correct panel shown.
        m_comboBox.setSelectedItem(s.getFactory());

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        // Add components
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.ipadx = 10;
        gbc.ipady = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        add(m_comboBox, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        add(m_argumentsPanel, gbc);

        m_handlerFactoryManager = factoryManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        MissingCellHandlerFactory fac = getSelectedFactory();
        if (fac.hasSettingsPanel()) {
            // Show card layout panel and activate the correct settings panel
            m_argumentsPanel.setVisible(true);
            String key = fac.getID();
            ((CardLayout)m_argumentsPanel.getLayout()).show(m_argumentsPanel, key);
        } else {
            // If the factory has no settings panel, we can hide the card layout panel.
            m_argumentsPanel.setVisible(false);
        }
        firePropertyChange(SELECTED_FACTORY_CHANGE, null, null);
    }

    /**
     * @return the factory that was selected in the combo box
     */
    public MissingCellHandlerFactory getSelectedFactory() {
        return (MissingCellHandlerFactory)m_comboBox.getSelectedItem();
    }

    /**
     * Creates and returns a new settings object for the settings made by the user.
     * @return the settings made by the user
     * @throws InvalidSettingsException if a user defined panel throws an error while saving its settings.
     */
    public MVIndividualSettings getSettings() throws InvalidSettingsException {
        MissingCellHandlerFactory currentFactory = getSelectedFactory();
        MVIndividualSettings settings = new MVIndividualSettings(currentFactory, m_handlerFactoryManager);
        if (m_valueHandlerPanels.containsKey(currentFactory.getID())) {
            m_valueHandlerPanels.get(currentFactory.getID()).saveSettingsTo(settings.getSettings());
        }
        return settings;
    }

    /**
     * Dummy panel for when a factory has no panel.
     */
    private class EmptyMissingValueHandlerPanel extends DefaultMissingValueHandlerPanel {
    }

    /**
     * Panel that shows an error message informing the user that the settings for the panel could not be loaded.
     */
    private class LoadingErrorMissingValueHandlerPanel extends MissingValueHandlerPanel {

        public LoadingErrorMissingValueHandlerPanel(final String text) {
            JLabel error = new JLabel("Error loading settings");
            error.setToolTipText(text);
            error.setForeground(Color.red);
            add(error);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
                                                                    throws NotConfigurableException {
        }
    }
}
