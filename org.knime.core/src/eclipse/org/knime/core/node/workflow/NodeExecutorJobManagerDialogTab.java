/*
 * ------------------------------------------------------------------------
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   13.10.2008 (ohl): created
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

/**
 * An instance provides the tab that appears in node's dialog to select a {@link NodeExecutionJobManager} and configure
 * the the job manager's settings.
 *
 * @author ohl, University of Konstanz
 */
public class NodeExecutorJobManagerDialogTab extends JPanel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(NodeExecutorJobManagerDialogTab.class);

    /**
     * Pseudo-factory that returns null - the job manager value that indicates that the parent component's or workflow's
     * job manager should be used if applicable or otherwise the appropriate standard job manager provided by
     * {@link NodeExecutionJobManagerPool#getDefaultJobManagerFactory(Class)}.
     *
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     */
    private static final NodeExecutionJobManagerFactory INHERIT_FACTORY = new NodeExecutionJobManagerFactory() {
        @Override
        public String getID() {
            return getClass().getName();
        }

        @Override
        public String getLabel() {
            return "<<default>>";
        }

        @Override
        public NodeExecutionJobManager getInstance() {
            return null;
        }
    };

    /**
     * The user selects the job manager factory that is used to create the node container's job manager. This will then
     * display the job manager specific settings components.
     */
    private final JComboBox<NodeExecutionJobManagerFactory> m_jobManagerSelect;

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

    // its only component is the settings panel from the currently selected job manager
    private JPanel m_settingsPanel;

    // the currently displayed panel of the currently selected manager
    private NodeExecutionJobManagerPanel m_currentPanel = EMPTY_PANEL;

    // maps the selectable factories in the drop down menu to a job manager configuration panel instance
    // we keep previously shown panels in case the manager is re-selected
    private final HashMap<NodeExecutionJobManagerFactory, NodeExecutionJobManagerPanel> m_panels = new HashMap<>();

    // if a job manager panel is displayed for the first time we must give it
    // the sport specs
    private PortObjectSpec[] m_lastPortSpecs;

    private SplitType m_nodeSplitType;

    /**
     * Creates a new selection tab for {@link NodeExecutionJobManager}s. To be
     * added to dialogs if more than the default manager is registered. Displays
     * a selection box and swaps the settings panel corresponding to the current
     * selection.
     * @param splitType indicates the level of splitting this node supports
     */
    public NodeExecutorJobManagerDialogTab(final SplitType splitType) {
        this(splitType, Optional.empty());
    }

    /**
     * Creates a new selection tab for {@link NodeExecutionJobManager}s. To be added to dialogs if more than the default
     * manager is registered. Displays a selection box and swaps the settings panel corresponding to the current
     * selection.
     *
     * @param splitType indicates the level of splitting this node supports
     * @param nc the node container this dialog belongs to. It helps to filter those job managers only that
     *            can handle the given node container (e.g. for those job managers that only work with meta nodes, sub
     *            nodes or native nodes).
     *
     * @since 3.2
     */
    public NodeExecutorJobManagerDialogTab(final SplitType splitType, final Optional<NodeContainer> nc) {
        super(new BorderLayout());
        m_nodeSplitType = splitType;
        // add the selection combo box at the top of the panel
        List<NodeExecutionJobManagerFactory> jobManagerChoices = new LinkedList<>();
        // add default factory as first item
        jobManagerChoices.add(INHERIT_FACTORY);

        // add all applicable job manager factories
        for (String id : NodeExecutionJobManagerPool.getAllJobManagerFactoryIDs()) {
            NodeExecutionJobManagerFactory factory = NodeExecutionJobManagerPool.getJobManagerFactory(id);
            try {
                var manager = Optional.ofNullable(factory.getInstance());
                var canExecute = nc.map(c -> manager.map(m -> m.canExecute(c)).orElse(true)).orElse(true);
                if (canExecute) {
                    jobManagerChoices.add(factory);
                }
            } catch (Throwable e) {
                // seen with SGE job manager throwing NoClassDefFoundError
                LOGGER.warn(String.format("Failed to load job manager of class \"%s\": (%s) %s",
                    factory.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getMessage()), e);
            }
        }
        m_jobManagerSelect = new JComboBox<>(jobManagerChoices.toArray(NodeExecutionJobManagerFactory[]::new));
        m_jobManagerSelect.setRenderer(new JobManagerFactoryRenderer());
        m_jobManagerSelect.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if ((e.getSource() == m_jobManagerSelect) && (e.getStateChange() == ItemEvent.SELECTED)) {
                    jobManagerSelectionChanged();
                }
            }
        });
        Box selectBox = Box.createHorizontalBox();
        selectBox.add(Box.createHorizontalGlue());
        selectBox.add(m_jobManagerSelect, BorderLayout.NORTH);
        selectBox.add(Box.createHorizontalGlue());
        selectBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
            "Select the job manager for this node"));

        add(selectBox, BorderLayout.NORTH);

        // prepare the space for the individual settings panels.
        m_settingsPanel = new JPanel();
        Box settingsBox = Box.createVerticalBox();
        settingsBox.add(Box.createVerticalGlue());
        settingsBox.add(m_settingsPanel);
        settingsBox.add(Box.createVerticalGlue());
        settingsBox.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Settings for selected job manager"));

        add(settingsBox, BorderLayout.CENTER);

        // set the panel of the first selection:
        jobManagerSelectionChanged();
    }

    /**
     * Sets the settings component of the currently selected job manager (and
     * removes the previous one).
     */
    private void jobManagerSelectionChanged() {
        var factory = (NodeExecutionJobManagerFactory)m_jobManagerSelect.getSelectedItem();

        m_panels.computeIfAbsent(factory, f -> {
            var manager = Optional.ofNullable(f.getInstance());
            var panelOrNull = manager.map(m -> m.getSettingsPanelComponent(m_nodeSplitType)).orElse(EMPTY_PANEL);
            var panel = Objects.requireNonNullElse(panelOrNull, EMPTY_PANEL);
            return panel;
        });

        m_currentPanel = m_panels.get(factory);

        // update the inspecs on the new panel
        if (m_lastPortSpecs != null) {
            m_currentPanel.updateInputSpecs(m_lastPortSpecs);
        }

        // remove panel from previously selected manager
        m_settingsPanel.removeAll();
        m_settingsPanel.add(m_currentPanel);

        if (getParent() != null) {
            getParent().invalidate();
            getParent().validate();
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

        // get the stored job manager or use the default factory if none is stored
        final Optional<NodeExecutionJobManager> jobManager = settings.getJobManager();
        var factoryId = jobManager.map(NodeExecutionJobManager::getID);
        var factory = factoryId.map(id -> NodeExecutionJobManagerPool.getJobManagerFactory(id));

        // this triggers jobManagerSelectionChanged, which sets m_currentPanel

        // if no factory is available, fall back to the default factory
        if (factory.isEmpty()) {
            m_jobManagerSelect.setSelectedIndex(0);
            // if the job manager was present but its factory ID couldn't be resolved, issue a warning
            if (factoryId.isPresent()) {
                LOGGER.warn("Unable to create job manager '" + factoryId + "'; using default manager");
            }
        } else {
            // update m_currentPanel
            m_jobManagerSelect.setSelectedItem(factory.get());
            // extract and load settings
            jobManager.ifPresent(m -> {
                NodeSettings s = new NodeSettings("job_manager_settings");
                m_currentPanel.updateInputSpecs(inSpecs);
                m.save(s);
                m_currentPanel.loadSettings(s);
            });
        }
    }

    /**
     * Writes the current settings of the job manager tab into the provided settings object.
     *
     * @param settings the object to write settings into
     * @throws InvalidSettingsException if the settings in the pane are unacceptable
     */
    public void saveSettings(final NodeContainerSettings settings) throws InvalidSettingsException {
        NodeExecutionJobManagerFactory factory = (NodeExecutionJobManagerFactory)m_jobManagerSelect.getSelectedItem();

        // null if default setting (inherit job manager factory) is used
        var manager = Optional.ofNullable(factory.getInstance());
        if (manager.isPresent()) {
            NodeSettings panelSets = new NodeSettings("job_manager_settings");
            m_currentPanel.saveSettings(panelSets);
            manager.get().load(panelSets);
        }
        settings.setJobManager(manager.orElse(null));
    }

    /** Displays {@link NodeExecutionJobManagerFactory#getLabel()} as the combo box item. */
    private static final class JobManagerFactoryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
            final boolean isSelected, final boolean cellHasFocus) {
            Object newValue;
            if (value instanceof NodeExecutionJobManagerFactory) {
                NodeExecutionJobManagerFactory factory = (NodeExecutionJobManagerFactory)value;
                newValue = factory.getLabel();
            } else {
                newValue = value;
            }
            return super.getListCellRendererComponent(list, newValue, index, isSelected, cellHasFocus);
        }
    }

}
