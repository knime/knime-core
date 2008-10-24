/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
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
 *   13.10.2008 (ohl): created
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;

/**
 * Implements the tab that appears in the node dialog if a
 * {@link NodeExecutionJobManager} is available (besides the default one) and
 * shows the settings panel of the job manager(s).
 *
 * @author ohl, University of Konstanz
 */
public class NodeExecutorJobManagerDialogTab extends JPanel {

    private static NodeLogger LOGGER =
            NodeLogger.getLogger(NodeExecutorJobManagerDialogTab.class);

    private final JComboBox m_jobManagerSelect;

    private final static NodeExecutionJobManagerPanel EMPTY_PANEL;
    static {
        EMPTY_PANEL = new NodeExecutionJobManagerPanel() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void loadSettings(final NodeSettingsRO settings,
                    final PortObjectSpec[] inSpecs) {
                // nothing to do here
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void saveSettings(final NodeSettingsWO settings)
                    throws InvalidSettingsException {
                // nothing to do here
            }
        };
        EMPTY_PANEL
                .add(new JLabel("no settings to adjust for this job manager"));
    }

    // its only component is the settings panel from the current selection
    private JPanel m_settingsPanel;

    // the currently displayed panel of the currently selected manager
    private NodeExecutionJobManagerPanel m_currentPanel;

    // we keep previously shown panels in case the manager is re-selected
    private final HashMap<String, NodeExecutionJobManagerPanel> m_panels =
            new HashMap<String, NodeExecutionJobManagerPanel>();

    /**
     * Creates a new selection tab for {@link NodeExecutionJobManager}s. To be
     * added to dialogs if more than the default manager is registered. Displays
     * a selection box and swaps the settings panel corresponding to the current
     * selection.
     */
    public NodeExecutorJobManagerDialogTab() {
        super(new BorderLayout());

        // add the selection combo box at the top of the panel
        m_jobManagerSelect =
                new JComboBox(NodeExecutionJobManagerPool
                        .getAllJobManagersAsArray());
        m_jobManagerSelect.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if ((e.getSource() == m_jobManagerSelect)
                        && (e.getStateChange() == ItemEvent.SELECTED)) {
                    jobManagerSelectionChanged();
                }
            }
        });
        Box selectBox = Box.createHorizontalBox();
        selectBox.add(Box.createHorizontalGlue());
        selectBox.add(m_jobManagerSelect, BorderLayout.NORTH);
        selectBox.add(Box.createHorizontalGlue());
        selectBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Select the job manager for this node"));

        add(selectBox, BorderLayout.NORTH);

        // prepare the space for the individual settings panels.
        m_settingsPanel = new JPanel();
        Box settingsBox = Box.createVerticalBox();
        settingsBox.add(Box.createVerticalGlue());
        settingsBox.add(m_settingsPanel);
        settingsBox.add(Box.createVerticalGlue());
        settingsBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Settings for selected job manager"));

        add(settingsBox, BorderLayout.CENTER);

        // set the panel of the first selection:
        jobManagerSelectionChanged();

    }

    /**
     * Sets the settings component of the currently selected job manager (and
     * removes the previous one).
     */
    private void jobManagerSelectionChanged() {
        assert m_settingsPanel != null;

        // remove panel from previously selected manager
        m_settingsPanel.removeAll();
        m_currentPanel = null; // don't dispose it, we may reuse it

        // get a new current panel from the newly selected job manager
        NodeExecutionJobManager selJobMgr =
                (NodeExecutionJobManager)m_jobManagerSelect.getSelectedItem();
        if (selJobMgr != null) {
            // see if we already have a panel for this manager
            m_currentPanel = m_panels.get(selJobMgr.getID());
            if (m_currentPanel == null) {
                m_currentPanel = selJobMgr.getSettingsPanelComponent();
                if (m_currentPanel != null) {
                    // store new panels in the map
                    m_panels.put(selJobMgr.getID(), m_currentPanel);
                } else {
                    m_currentPanel = EMPTY_PANEL;
                }
            }
        } else {
            // no job manager selected - that is almost impossible
            m_currentPanel = EMPTY_PANEL;
        }

        m_settingsPanel.add(m_currentPanel);

        if (getParent() != null) {
            getParent().invalidate();
            getParent().repaint();
        }

    }

    /**
     * Returns a name of this tab.
     *
     * @return a name of this tab.
     */
    public String getTabName() {
        return "Job Manager Selection";
    }

    /**
     * Takes over the settings from the argument and displays them in the panel.
     *
     * @param settings the settings to load into the components
     * @param inSpecs the specs of the input port objects
     */
    public void loadSettings(final SingleNodeContainerSettings settings,
            final PortObjectSpec[] inSpecs) {

        // select the job manager in the combo box
        NodeExecutionJobManager newMgr =
                NodeExecutionJobManagerPool.getJobManager(settings
                        .getJobManagerID());

        m_jobManagerSelect.setSelectedItem(newMgr);
        if (m_jobManagerSelect.getSelectedItem() == newMgr) {

            // if the job manager exists in the list apply the settings
            m_currentPanel.loadSettings(settings.getJobManagerSettings(),
                    inSpecs);

        } else {
            // seems we got a manager we currently don't have
            LOGGER.warn("Unable to find job manager '"
                    + settings.getJobManagerID()
                    + "' falling back to default job manager");

            m_jobManagerSelect.setSelectedItem(NodeExecutionJobManagerPool
                    .getDefaultJobManager());
        }

        // show the proper panel
        jobManagerSelectionChanged();
    }

    /**
     * Writes the current settings of the job manager tab into the provided
     * settings object.
     *
     * @param settings the object to write settings into
     * @throws InvalidSettingsException if the settings in the pane are
     *             unacceptable
     */
    public void saveSettings(final SingleNodeContainerSettings settings)
            throws InvalidSettingsException {

        NodeExecutionJobManager selJobMgr =
                (NodeExecutionJobManager)m_jobManagerSelect.getSelectedItem();
        if (selJobMgr == null) {
            throw new InvalidSettingsException("Select a job manager");
        }

        // create a new/empty settings object for the panel to write into
        NodeSettings panelSettings =
                new NodeSettings(settings.getJobManagerID());
        m_currentPanel.saveSettings(panelSettings);

        settings.setJobManager(selJobMgr.getID(), panelSettings);
    }
}