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
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.util.Pair;

/**
 * Implements the tab that appears in the node dialog if a
 * {@link NodeExecutionJobManager} is available (besides the default one) and
 * shows the settings panel of the job manager(s).
 *
 * @author ohl, University of Konstanz
 */
public class NodeExecutorJobManagerDialogTab extends JPanel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(NodeExecutorJobManagerDialogTab.class);

    private final JComboBox m_jobManagerSelect;

    private static final String DEFAULT_ENTRY = "<<default>>";

    private static final NodeExecutionJobManagerPanel EMPTY_PANEL;
    static {
        EMPTY_PANEL = new NodeExecutionJobManagerPanel() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void loadSettings(final NodeSettingsRO settings) {
                // nothing to do here
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void updateInputSpecs(final PortObjectSpec[] inSpecs) {
                // we don't care
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
    private final HashMap<String,
        Pair<NodeExecutionJobManager, NodeExecutionJobManagerPanel>> m_panels =
            new HashMap<String,
            Pair<NodeExecutionJobManager, NodeExecutionJobManagerPanel>>();

    // if a job manager panel is displayed for the first time we must give it
    // the sport specs
    private PortObjectSpec[] m_lastPortSpecs;

    private SplitType m_nodeSplitType;

    /**
     * Creates a new selection tab for {@link NodeExecutionJobManager}s. To be
     * added to dialogs if more than the default manager is registered. Displays
     * a selection box and swaps the settings panel corresponding to the current
     * selection.
     */
    public NodeExecutorJobManagerDialogTab() {
        super(new BorderLayout());
        // add the selection combo box at the top of the panel
        Vector<Object> jobManagerChoices = new Vector<Object>();
        jobManagerChoices.add(DEFAULT_ENTRY);
        jobManagerChoices.addAll(
                NodeExecutionJobManagerPool.getAllJobManagerFactoryIDs());
        m_jobManagerSelect = new JComboBox(jobManagerChoices);
        m_jobManagerSelect.setRenderer(new ComboRenderer());
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
        String sel = (String)m_jobManagerSelect.getSelectedItem();
        NodeExecutionJobManager selJobMgr;
        if (sel == DEFAULT_ENTRY) {
            selJobMgr = null;
        } else if (m_panels.containsKey(sel)) {
            selJobMgr = m_panels.get(sel).getFirst();
            m_currentPanel = m_panels.get(sel).getSecond();
        } else {
            NodeExecutionJobManagerFactory fac =
                NodeExecutionJobManagerPool.getJobManagerFactory(sel);
            selJobMgr = fac.getInstance();
            m_currentPanel = selJobMgr.getSettingsPanelComponent(m_nodeSplitType);
            if (m_currentPanel != null) {
                m_currentPanel.loadSettings(new NodeSettings("empty"));
            }
            m_panels.put(sel, new Pair<NodeExecutionJobManager,
                    NodeExecutionJobManagerPanel>(selJobMgr, m_currentPanel));
        }
        if (m_currentPanel == null) {
            m_currentPanel = EMPTY_PANEL;
        }

        // update the inspecs on the new panel
        if (m_lastPortSpecs != null) {
            m_currentPanel.updateInputSpecs(m_lastPortSpecs);
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
    public void loadSettings(final NodeContainerSettings settings,
            final PortObjectSpec[] inSpecs) {

        // we must store the port specs in case job manager selection changes
        m_lastPortSpecs = inSpecs;
        // we store the node split type
        m_nodeSplitType = settings.getSplitType();

        // select the job manager in the combo box
        NodeExecutionJobManager newMgr = settings.getJobManager();
        String id = newMgr == null ? DEFAULT_ENTRY : newMgr.getID();
        m_jobManagerSelect.setSelectedItem(id);
        if (DEFAULT_ENTRY.equals(id)) {
            // must also have selected this entry
            assert DEFAULT_ENTRY.equals(m_jobManagerSelect.getSelectedItem());
        } else if (m_jobManagerSelect.getSelectedItem().equals(id)) {
            // if the job manager exists in the list apply the settings
            NodeSettings s = new NodeSettings("job_manager_settings");
            m_currentPanel.updateInputSpecs(inSpecs);
            newMgr.save(s);
            m_currentPanel.loadSettings(s);
        } else {
            // seems we got a manager we currently don't have
            LOGGER.warn("Unable to find job manager '"
                    + id + "'; using parent manager");
            m_jobManagerSelect.setSelectedItem(DEFAULT_ENTRY);
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
    public void saveSettings(final NodeContainerSettings settings)
            throws InvalidSettingsException {
        String selected = (String)m_jobManagerSelect.getSelectedItem();
        NodeExecutionJobManager selMgr = null;
        if (!DEFAULT_ENTRY.equals(selected)) {
            // any "real" node execution manager was selected
            selMgr = m_panels.get(selected).getFirst();
            NodeSettings panelSets = new NodeSettings("job_manager_settings");
            m_currentPanel.saveSettings(panelSets);
            selMgr.load(panelSets);
        }
        settings.setJobManager(selMgr);
    }

    private static final class ComboRenderer extends DefaultListCellRenderer {

        /** {@inheritDoc} */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            Object newValue;
            if (DEFAULT_ENTRY.equals(value)) {
                newValue = value;
            } else if (value instanceof String) {
                String id = (String)value;
                NodeExecutionJobManagerFactory jobMgrFac =
                    NodeExecutionJobManagerPool.getJobManagerFactory(id);
                if (jobMgrFac != null) {
                    newValue = jobMgrFac.getLabel();
                } else {
                    newValue = value;
                }
            } else {
                newValue = value;
            }
            return super.getListCellRendererComponent(list, newValue, index,
                    isSelected, cellHasFocus);
        }
    }
}