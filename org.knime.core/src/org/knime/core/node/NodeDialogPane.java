/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   12.09.2007 (mb): created
 */
package org.knime.core.node;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboPopup;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.Node.SettingsLoaderAndWriter;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigEditJTree;
import org.knime.core.node.config.ConfigEditTreeEvent;
import org.knime.core.node.config.ConfigEditTreeEventListener;
import org.knime.core.node.config.ConfigEditTreeModel;
import org.knime.core.node.config.ConfigEditTreeModel.ConfigEditTreeNode;
import org.knime.core.node.defaultnodesettings.SettingsModelFlowVariableCompatible;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.ICredentials;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.NodeExecutorJobManagerDialogTab;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;
import org.knime.core.util.MutableInteger;


/**
 * The base class for all node dialogs. It provides a tabbed pane to which the
 * derived dialog can add its own components (method
 * {@link #addTab(String, Component)}. Subclasses will also override
 * {@link #saveSettingsTo(NodeSettingsWO)} and either
 * {@link #loadSettingsFrom(NodeSettingsRO, DataTableSpec[])} or
 * {@link #loadSettingsFrom(NodeSettingsRO, PortObjectSpec[])}, whereby the
 * latter is only of interest when dealing with custom port types. Failing to
 * override one of the two load methods will result in errors during runtime.
 *
 * @author Thomas Gabriel, University of Konstanz
 * @see org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane
 */
public abstract class NodeDialogPane {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            NodeDialogPane.class);

    // This listener needs to be static and not an anonymous inner class
    // because it stays registered in some static Swing classes. Thus a long
    // reference chain will prevent quite a lot of memory to get garbage
    // collected even if all workflows are closed.
    private static final HierarchyListener HIERARCHY_LISTENER =
        new HierarchyListener() {
        /** {@inheritDoc} */
        @Override
        public void hierarchyChanged(final HierarchyEvent e) {
            noLightWeight(e.getComponent());
        }
    };

    /** Logger "personalized" for this dialog instance. */
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private static final String TAB_NAME_VARIABLES = "Flow Variables";

    /**
     * Tabbed pane in the center of the dialog, which holds the components added
     * by the derived class via <code>#addTab(String,Component)</code>.
     */
    private final JTabbedPane m_pane;

    /** Status bar below the config tabs indicating that some settings are
     * overwritten using flow variables. */
    private final JLabel m_statusBarLabel;

    /**
     * Keeps all components which are added to the tabbed pane above by its
     * string title.
     */
    private final LinkedHashMap<String, Component> m_tabs;

    /** The additional tab in which the user can set the memory options.
     * This field is null when m_node has no data outports.
     */
    private MiscSettingsTab m_memPolicyTab;

    /**
     * the additional tab in which the user can select the job manager and set
     * its options. This field is null, if there is only one job manager with
     * no options.
     */
    private NodeExecutorJobManagerDialogTab m_jobMgrTab;

    /** List of registered models. (Model for little buttons attached to a
     * selected set of components -- used to customize variables in place.)
     */
    private final List<FlowVariableModel> m_flowVariablesModelList;

    /** Flag whether the settings in any of the flow variable models have
     * changed. If so, we need to update the flow variable tab before saving
     * the settings. We can't directly modify the tab during the event because
     * it might not show the current settings tree. */
    private boolean m_flowVariablesModelChanged;

    /** The tab containing the flow variables. */
    private final FlowVariablesTab m_flowVariableTab;

    /** The flow object stack, it's also used when the variables tab get's
     * activated. */
    private FlowObjectStack m_flowObjectStack;

    /** The credentials available in the workflow. */
    private CredentialsProvider m_credentialsProvider;

    /** The specs that were provided to the most recent internalLoadSettingsFrom
     * invocation. Ideally this member should not be kept as field but we need
     * it when the wrapped node dialog pane calls loadSettings on user request
     * (from the menu). */
    private PortObjectSpec[] m_specs;

    /** Same as m_specs -- we keep the last data in order to allow for loading
     * settings from the dialog's file menu. */
    private PortObject[] m_data;

    /** The underlying panel which keeps all the tabs. */
    private final JPanel m_panel;

    private boolean m_isWriteProtected;

    /**
     * Creates a new dialog with the given title. The pane holds a tabbed pane
     * ready to take additional components needed by the derived class
     * (<code>#addTab(String,Component)</code>).
     */
    protected NodeDialogPane() {
        m_panel = new JPanel();
        // init the panel with layout
        m_panel.setLayout(new BorderLayout());
        // status bar label - mostly hidden unless flow var tab is in use
        m_statusBarLabel = new JLabel(
                "", NodeView.WARNING_ICON, SwingConstants.LEFT);
        Font font = m_statusBarLabel.getFont().deriveFont(Font.BOLD);
        m_statusBarLabel.setFont(font);
        m_statusBarLabel.setBorder(
                BorderFactory.createBevelBorder(BevelBorder.RAISED));
        m_statusBarLabel.setBackground(Color.WHITE);
        m_statusBarLabel.setOpaque(true);
        m_statusBarLabel.setVisible(false);
        m_panel.add(m_statusBarLabel, BorderLayout.SOUTH);

        // init map for tabs
        m_tabs = new LinkedHashMap<String, Component>();
        // init tabbed pane and at it to the underlying panel
        m_pane = new JTabbedPane();
        m_pane.addChangeListener(new ChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateFlowVariablesOverwriteWarning();
            }
        });
        m_panel.add(m_pane, BorderLayout.CENTER);
        m_flowVariablesModelList =
            new CopyOnWriteArrayList<FlowVariableModel>();
        m_flowVariableTab = new FlowVariablesTab();
    }

    /** A logger initialized with the concrete runtime class.
     * @return the logger (not null).
     * @since 2.6*/
    protected final NodeLogger getLogger() {
        return m_logger;
    }

    /**
     * creates and adds the miscellaneous tab that is contained in each dialog
     * of nodes with output ports.
     */
    void addMiscTab() {
        m_memPolicyTab = new MiscSettingsTab();
        addTab(m_memPolicyTab.getTabName(), m_memPolicyTab);
    }

    /**
     * Creates and adds the job manager selection tab.
     * @param splitType indicates how table splitting is supported in this node
     */
    public void addJobMgrTab(final SplitType splitType) {
        m_jobMgrTab = new NodeExecutorJobManagerDialogTab(splitType);
        addTab(m_jobMgrTab.getTabName(), m_jobMgrTab);
    }

    /** Returns the panel holding the different tabs shown in the dialog. Note,
     * don't modify this panel.
     * @return The underlying dialog panel which keeps the tabbed pane.
     */
    public final JPanel getPanel() {
        return m_panel;
    }

    /** Returns true if the underlying node is write protected. A node is write
     * protected if it is part of a linked meta node (i.e. referencing a
     * template). Client code usually does not need to evaluate this flag, the
     * framework will disable the OK/Apply button for write protected nodes.
     * @return If this node is write protected. */
    public final boolean isWriteProtected() {
        return m_isWriteProtected;
    }

    /** @return available flow variables in a non-modifiable map
     *           (ensured to be not null) . */
    public final Map<String, FlowVariable> getAvailableFlowVariables() {
        Map<String, FlowVariable> result = null;
        if (m_flowObjectStack != null) {
            result = m_flowObjectStack.getAvailableFlowVariables();
        }
        if (result == null) {
            result = Collections.emptyMap();
        }
        return result;
    }

    /** Get a list of available credentials identifiers. This is important for,
     * e.g. nodes connecting to password-secured systems (e.g. data bases). The
     * user will select a credentials object from the list of available
     * credentials and the node will store the credentials identifier as part
     * of the node configuration.
     * @return a list of credentials variables defined on the workflow.
     */
    public final Collection<String> getCredentialsNames() {
        if (m_credentialsProvider == null) {
            m_logger.warn("No credentials provider set, using empty list");
            return Collections.emptyList();
        }
        return m_credentialsProvider.listNames();
    }

    /** Accessor to credentials defined on a workflow. The method will return
     * a non-null provider. Sub-classes can read out credentials here.
     * Any invocation of the {@link CredentialsProvider#get(String)} method will
     * register this node as client to the requested credentials.
     * The credentials identifier (its {@link ICredentials#getName()} should be
     * part of the configuration, i.e. chosen by the user in the configuration
     * dialog.
     *
     * @return A provider for credentials available in this workflow.
     */
    protected final CredentialsProvider getCredentialsProvider() {
        return m_credentialsProvider;
    }

    /** Method being called from the node when the dialog shall load the
     * settings from a NodeSettingsRO object. This method will call the
     * abstract loadSettingsFrom method and finally load internals
     * (i.e. memory policy of outports, if any).
     * @param settings To load from.
     * @param specs Specs at input ports
     * @param data Data from input ports
     * @param foStack Flow object stack (contains flow variables)
     * @param credentialsProvider The credentials available in the flow
     * @param isWriteProtected Whether ok/apply should be disabled
     *        (write protected meta node)
     * @throws NotConfigurableException
     * If loadSettingsFrom throws this exception.
     * @see #loadSettingsFrom(NodeSettingsRO, PortObjectSpec[])
     */
    void internalLoadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs, final PortObject[] data,
            final FlowObjectStack foStack,
            final CredentialsProvider credentialsProvider,
            final boolean isWriteProtected)
        throws NotConfigurableException {
        NodeSettings modelSettings = null;
        NodeSettings flowVariablesSettings = null;
        m_flowObjectStack = foStack;
        m_credentialsProvider = credentialsProvider;
        m_specs = specs;
        m_data = data;
        m_isWriteProtected = isWriteProtected;
        try {
            SettingsLoaderAndWriter l = SettingsLoaderAndWriter.load(settings);
            modelSettings = l.getModelSettings();
            flowVariablesSettings = l.getVariablesSettings();
        } catch (InvalidSettingsException e) {
            // silently ignored here, variables get assigned default values
            // if they are null
        }
        if (modelSettings == null) {
            modelSettings = new NodeSettings("empty");
        }
        try {
            callDerivedLoadSettingsFrom(modelSettings, specs, data);
        } catch (NotConfigurableException nce) {
            throw nce;
        } catch (Throwable e) {
            m_logger.error("Error loading model settings", e);
        }
        // add the flow variables tab
        addFlowVariablesTab();
        m_flowVariablesModelChanged = false;
        initFlowVariablesTab(modelSettings, flowVariablesSettings);

        // output memory policy and job manager (stored in NodeContainer)
        if (m_memPolicyTab != null || m_jobMgrTab != null) {
            NodeContainerSettings ncSettings;
            SingleNodeContainerSettings sncSettings;
            try {
                ncSettings = new NodeContainerSettings();
                ncSettings.load(settings);
            } catch (InvalidSettingsException ise) {
                ncSettings = new NodeContainerSettings();
            }
            try {
                sncSettings = new SingleNodeContainerSettings(settings);
            } catch (InvalidSettingsException ise) {
                sncSettings = new SingleNodeContainerSettings();
            }
            if (m_memPolicyTab != null) {
                MemoryPolicy memoryPolicy = sncSettings.getMemoryPolicy();
                if (memoryPolicy == null) {
                    memoryPolicy = MemoryPolicy.CacheSmallInMemory;
                }
                m_memPolicyTab.setStatus(memoryPolicy);
            }
            if (m_jobMgrTab != null) {
                m_jobMgrTab.loadSettings(ncSettings, specs);
            }
        }
        updateFlowVariablesOverwriteWarning();
    }

    /** Calls abstract method
     * {@link #loadSettingsFrom(NodeSettingsRO, PortObjectSpec[])}. Overwritten
     * by {@link DataAwareNodeDialogPane} to forward the input data.
     * @param settings ...
     * @param specs ...
     * @param data ...
     * @throws NotConfigurableException ... */
    void callDerivedLoadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs, final PortObject[] data)
    throws NotConfigurableException {
        loadSettingsFrom(settings, specs);
    }

    /**
     * Called from the node when the current settings shall be written to
     * a NodeSettings object. It will call the abstract saveSettingsTo method
     * and finally write "misc" settings to the argument object.
     * @param settings To write to. Forwarded to abstract saveSettings method.
     * @throws InvalidSettingsException If any of the writing fails.
     */
    void internalSaveSettingsTo(final NodeSettingsWO settings)
    throws InvalidSettingsException {
        SettingsLoaderAndWriter l = new SettingsLoaderAndWriter();
        NodeSettings model = new NodeSettings("field_ignored");
        try {
            saveSettingsTo(model);
        } catch (InvalidSettingsException ise) {
            throw ise;
        } catch (Throwable e) {
            m_logger.coding("Wrong exception type thrown "
                    + "while saving dialog settings", e);
            throw new InvalidSettingsException(e);
        }
        if (m_flowVariablesModelChanged) {
            updateFlowVariablesTab();
        }
        NodeSettings variables = m_flowVariableTab.getVariableSettings();
        l.setModelSettings(model);
        l.setVariablesSettings(variables);
        l.save(settings);
        SingleNodeContainerSettings s = new SingleNodeContainerSettings();
        NodeContainerSettings ncSet = new NodeContainerSettings();

        if (m_memPolicyTab != null) {
            s.setMemoryPolicy(m_memPolicyTab.getStatus());
        }
        if (m_jobMgrTab != null) {
            m_jobMgrTab.saveSettings(ncSet);
        }
        ncSet.save(settings);
        s.save(settings);
    }

    /**
     * Invoked before the dialog window is opened. The settings object passed,
     * contains the current settings of the corresponding node model. The model
     * and the dialog must agree on a mutual contract on how settings are stored
     * in the spec. I.e. they must able to read each other's settings.
     * <p>
     * The implementation must be able to handle invalid or incomplete settings
     * as the model may not have any reasonable values yet (for example when the
     * dialog is opened for the first time). When an empty/invalid settings
     * object is passed the dialog should set default values in its components.
     *
     * @param settings The settings to load into the dialog. Could be an empty
     *            object or contain invalid settings. But will never be null.
     * @param specs The input data table specs. Items of the array could be null
     *            if no spec is available from the corresponding input port
     *            (i.e. not connected or upstream node does not produce an
     *            output spec). If a port is of type
     *            {@link BufferedDataTable#TYPE} and no spec is available the
     *            framework will replace null by an empty {@link DataTableSpec}
     *            (no columns) unless the port is marked as optional.
     * @throws NotConfigurableException if the dialog cannot be opened because
     * of real invalid settings or if any preconditions are not fulfilled, e.g.
     * no predecessor node, no nominal column in input table, etc.
     * @see NodeModel#loadSettingsFrom(NodeSettingsRO)
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        // default implementation: the standard version needs to hold: all
        // ports are data ports!

        // (1) case PortObjectSpecs to DataTableSpecs
        DataTableSpec[] inDataSpecs = new DataTableSpec[specs.length];
        for (int i = 0; i < specs.length; i++) {
            try {
                inDataSpecs[i] = (DataTableSpec)specs[i];
            } catch (ClassCastException cce) {
                throw new NotConfigurableException("Input Port " + i
                        + " does not hold data table specs. "
                        + "Likely reason: wrong version"
                        + " of loadSettingsFrom() overwritten!");
            }
        }
        // (2) call old-fashioned, data-only loadSettingsFrom
        loadSettingsFrom(settings, inDataSpecs);
    }

    /**
     * Invoked before the dialog window is opened. The settings object passed,
     * contains the current settings of the corresponding node model. The model
     * and the dialog must agree on a mutual contract on how settings are stored
     * in the spec. I.e. they must able to read each other's settings.
     * <p>
     * The implementation must be able to handle invalid or incomplete settings
     * as the model may not have any reasonable values yet (for example when the
     * dialog is opened for the first time). When an empty/invalid settings
     * object is passed the dialog should set default values in its components.
     *
     * @param settings The settings to load into the dialog. Could be an empty
     *            object or contain invalid settings. But will never be null.
     * @param specs The input data table specs. If no spec is available for any
     *            given port (because the port is not connected or the previous
     *            node does not produce a spec) the framework will pass an
     *            empty {@link DataTableSpec} (no columns) unless the port is
     *            marked as {@link PortType#isOptional() optional} (in which
     *            case the array element is null).
     * @throws NotConfigurableException if the dialog cannot be opened because
     * of real invalid settings or if any preconditions are not fulfilled, e.g.
     * no predecessor node, no nominal column in input table, etc.
     * @see NodeModel#loadSettingsFrom(NodeSettingsRO)
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        throw new NotConfigurableException(
        "NodeDialogPane.loadSettingsFrom() implementation missing!");
    }

    /**
     * Override this method in order to react on events induced by the Cancel
     * button from the surrounding dialog.
     */
    public void onCancel() {
        // default implementation does nothing.
    }

    /** Does some cleanup and finally calls the {@link #onClose()} method.
     * @since 2.6
     * @noreference This method is not intended to be referenced by clients. */
    public final void callOnClose() {
        m_data = null;
        m_specs = null;
        onClose();
    }

    /**
     * Override this method in order to react on events if the surrounding
     * dialog is supposed to be closed.
     */
    public void onClose() {
        // default implementation does nothing.
    }

    /**
     * Override this method in order to react on events if the surrounding
     * dialog is supposed to be opened.
     */
    public void onOpen() {
        // default implementation does nothing.
    }

    /**
     * Invoked when the settings need to be applied. The implementation should
     * write the current user settings from its components into the passed
     * object. It should not check consistency or completeness of the settings -
     * this is part of the model's load method. The only situation this method
     * would throw an exception is when a component contains an invalid value
     * that can't be stored in the settings object. <br>
     * The settings must be written in a way the model is able to load in, i.e.
     * with the model's keys.
     *
     * @param settings The settings object to write into.
     *
     * @throws InvalidSettingsException If the settings are not applicable to
     *             the model.
     * @see NodeModel#loadSettingsFrom(NodeSettingsRO)
     */
    protected abstract void saveSettingsTo(final NodeSettingsWO settings)
    throws InvalidSettingsException;

    /** Commit spinners and save settings. It will first call the
     * commitJSpinners method (which traverses all components and commits
     * them if they are instance of JSpinner) and finally call
     * <code>saveSettingsTo(settings)</code>.
     *
     * <p>Derived classes should not be required to call this method. It may
     * change in future versions without prior notice.
     * @param settings The settings object to write into.
     * @throws InvalidSettingsException If the settings are not applicable to
     *             the model.
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    public final void finishEditingAndSaveSettingsTo(
            final NodeSettingsWO settings) throws InvalidSettingsException {
        commitComponentsRecursively(getPanel());

        /* Workaround that makes sure that the last modified Swing component
         * looses focus and that its values are stored (Bug 1949). Otherwise
         * the last changed values might get lost if a user simply clicks on
         * the ok or apply button without changing the focus first. */
        JTabbedPane pane = m_pane;
        int index = pane.getTabCount() - 1;
        if (index > 0) {
            if (TAB_NAME_VARIABLES.equals(pane.getTitleAt(index))) {
                LOGGER.coding("Configuration dialog tab order has"
                        + " changed. The assumption that the flow variables tab"
                        + " is not the last tab was violated.");
            }
            int prevIndex = pane.getSelectedIndex();
            pane.setSelectedIndex(index);
            pane.setSelectedIndex(prevIndex);
        }

        internalSaveSettingsTo(settings);
    }


    /** Saves current settings to an output stream (in xml format).
     *
     * <p>Derived classes should not be required to call this method. It may
     * change in future versions without prior notice.
     * @param out To save to.
     * @throws InvalidSettingsException If the settings can't be save since
     *         they are invalid
     * @throws IOException If problems writing to the stream occur.
     * @see #loadSettingsFrom(InputStream)
     */
    public final void saveSettingsTo(final OutputStream out)
    throws InvalidSettingsException, IOException {
        NodeSettings settings = new NodeSettings("dialog");
        finishEditingAndSaveSettingsTo(settings);
        settings.saveToXML(out);
    }

    /** Loads settings from an input stream (in xml format).
     *
     * <p>Derived classes should not be required to call this method. It may
     * change in future versions without prior notice.
     * @param in to load from.
     * @throws NotConfigurableException If settings can't be loaded since the
     * most recent input spec does not match the settings (or is not available)
     * @throws IOException If problems reading the stream occur.
     * @see #saveSettingsTo(OutputStream)
     */
    public final void loadSettingsFrom(final InputStream in)
    throws NotConfigurableException, IOException {
        NodeSettingsRO settings = NodeSettings.loadFromXML(in);
        internalLoadSettingsFrom(settings, m_specs, m_data, m_flowObjectStack,
                m_credentialsProvider, m_isWriteProtected);
    }

    /**
     * JSpinner seem to have the "feature" that their value is not committed
     * when* they are being edited (by hand, not with the arrows) and someone
     * presses an button. This method traverse all components recursively and
     * commits the values if it finds components that are JSpinners.
     *
     * @param c Component to find JSpinner in.
     */
    private static void commitComponentsRecursively(final Component c) {
        if (c instanceof JSpinner) {
            JSpinner spin = (JSpinner)c;
            try {
                spin.commitEdit();
            } catch (ParseException e) {
                // reset the value also in the GUI
                JComponent editor = spin.getEditor();
                if (editor instanceof DefaultEditor) {
                    ((DefaultEditor)editor).getTextField().setValue(
                            spin.getValue());
                }
            }
        } else if (c instanceof JFormattedTextField) {
            try {
                ((JFormattedTextField)c).commitEdit();
            } catch (ParseException e) {
                // ignore
            }
        } else if (c instanceof Container) {
            Component[] cs = ((Container)c).getComponents();
            for (int i = 0; i < cs.length; i++) {
                commitComponentsRecursively(cs[i]);
            }
        }
    }

    /** Add the flow variables tab. Override to disable this tab. */
    protected void addFlowVariablesTab() {
        if (getTab(TAB_NAME_VARIABLES) == null) {
            addTab(TAB_NAME_VARIABLES, m_flowVariableTab);
            m_pane.addChangeListener(new ChangeListener() {
                /** {@inheritDoc} */
                @Override
                public void stateChanged(final ChangeEvent e) {
                    if (m_pane.getSelectedComponent() == m_flowVariableTab) {
                        updateFlowVariablesTab();
                    }
                }
            });
        }
    }

    /**
     * Adds a new component in a new tab to the node's dialog. Tabs are
     * referenced by their title (to remove or replace them). The tab is added
     * at the right most position (for nodes with outputs this is left of
     * KNIME's default tab though).
     *<p>
     * If the specified title already exists, this method creates a new unique
     * title for the new tab (and issues a coding problem warning). If the same
     * title with the same component was added before, this method does nothing
     * (but issues a coding problem warning). Also, the same component with
     * a different title is accepted (again with a coding problem warning), the
     * tab that was added before with this component is removed before adding
     * the component again to the dialog.

     * @param title The title of the tab for the given component. Must be unique
     *            for this dialog.
     * @param comp The component to add to this dialog's tabbed pane.
     * @throws NullPointerException If either the title or the component is
     *             <code>null</code>.
     */
    protected final void addTab(final String title, final Component comp) {
        if (title == null) {
            throw new NullPointerException("The title of a tab in the dialog"
                    + " can't be null");
        }
        if (comp == null) {
            throw new NullPointerException("The component in the dialog's"
                    + " tab can't be null");
        }
        String titleToUse = title;

        // check if the title was used before
        Component existComp = m_tabs.get(title);
        if (existComp != null) {
            if (existComp == comp) {
                // this tab is already in the dialog. Do nothing. Warn them.
                m_logger.coding("The exact same tab component is added to "
                        + "the dialog twice. Ignoring it.");
                return;
            } else {
                // there is already a (different component) with the same
                // title: change the title to be unique
                int idx = 2;
                while (m_tabs.containsKey(titleToUse)) {
                    titleToUse = title + " (#" + idx + ")";
                    idx++;
                }
                m_logger.coding("The title of a tab in the dialog must be "
                        + "unique. Title '" + title + "' changed to unique '"
                        + titleToUse + "'.");
            }
        }

        // see if they are reusing the component (with a different title)
        if (m_tabs.containsValue(comp)) {
            // Not good. Rename that tab!
            int compIdx = m_pane.indexOfComponent(comp);
            assert compIdx >= 0;
            String oldTitle = m_pane.getTitleAt(compIdx);
            renameTab(oldTitle, titleToUse);
            m_logger.coding("The component was already added to the dialog with"
                    + " a different tab title (old title: '" + oldTitle
                    + "', new title: '" + title + "'). The old tab is"
                    + " renamed to '" + titleToUse + "'.");
            return;
        }

        insertNewTabAt(Integer.MAX_VALUE, titleToUse, comp);
    }

    /**
     * Adds a new tab at a certain position in the tabbed pane of the node's
     * dialog. Tabs are referenced by their title (to remove or replace them).
     * If the specified title already exists or if the specified component is
     * already used by another tab this method throws an exception. If the
     * specified index is greater than the number of existing tabs, the tab is
     * added at the right most position. For nodes with outputs this is left of
     * KNIME's "General Node Settings" tab though. The actual index of the new
     * tab after insertion is returned.
     * <p>
     * NOTE: This method is more restrictive than the
     * {@link #addTab(String, Component)} method, in that it does not accept
     * duplicate titles or components.
     *
     * @param index the index of the new tab after insertion. Must be greater
     *            than or equal to zero.
     * @param title The title of the new tab for the given component. Must be
     *            unique for this dialog.
     * @param comp The component to add to this dialog's tabbed pane.
     * @return the index where the new tab actually has been placed (might be
     *         different from the specified argument).
     * @throws NullPointerException If either the title or the component is
     *             <code>null</code>.
     * @throws IllegalArgumentException if another tab with the same title or
     *             the same component exists already
     * @throws IndexOutOfBoundsException if the index is smaller than zero.
     */
    protected final int addTabAt(final int index, final String title,
            final Component comp) {

        if (title == null) {
            throw new NullPointerException("The title of a tab in the dialog"
                    + " can't be null");
        }
        if (comp == null) {
            throw new NullPointerException("The component in the dialog's"
                    + " tab can't be null");
        }
        if (m_tabs.containsKey(title)) {
            throw new IllegalArgumentException("Can't use a tab title twice "
                    + "in the same dialog.");
        }
        if (m_tabs.containsValue(comp)) {
            throw new IllegalArgumentException("Can't register the same "
                    + "component twice in the same dialog");
        }

        if (index < 0) {
            throw new IndexOutOfBoundsException("Index must be greater than"
                    + " or equal to zero");
        }
        return insertNewTabAt(index, title, comp);
    }

    /**
     * Changes the name of an existing tab. Doesn't modify the tab's component
     * or position. It throws an exception, if the specified old name doesn't
     * exist, a tab with the new name already exists, or one of the arguments is
     * null.
     *
     * @param oldName the current name of the tab to be renamed
     * @param newName the new name. Must be unique for this dialog. Must not be
     *            null.
     * @throws NullPointerException if one of the arguments is null
     */
    protected final void renameTab(final String oldName, final String newName) {

        if ((oldName == null) || (newName == null)) {
            throw new NullPointerException("The title of a tab in the dialog"
                    + " can't be null");
        }
        if (!m_tabs.containsKey(oldName)) {
            throw new IllegalArgumentException(
            "Tab with specified name doesn't exist.");
        }
        if (m_tabs.containsKey(newName)) {
            throw new IllegalArgumentException(
            "Another tab with the specified new name already exists");
        }

        Component tabComp = getTab(oldName);
        int index = m_pane.indexOfTab(oldName);

        m_tabs.remove(oldName);
        m_tabs.put(newName, tabComp);

        m_pane.setTitleAt(index, newName);

    }

    /*
     * Adds a new tab with the specified title at the specified position. The
     * tab component and the title must not exist in the tabbed pane. The
     * component is added to the hash and the tabbed pane. If the index is too
     * big, or attempts to insert the tab right of the misc tab, the index is
     * adjusted. It returns the actual index of the new tab, where it is placed
     * after insertion.
     */
    private int insertNewTabAt(final int idx, final String title,
            final Component newTab) {
        assert idx >= 0;
        assert title != null;
        assert newTab != null;
        assert !m_tabs.containsKey(title);
        assert !m_tabs.containsValue(newTab);

        // the index where the tab is actually inserted
        final MutableInteger insertIdx = new MutableInteger(idx);

        ViewUtils.invokeAndWaitInEDT(new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                int varTabIdx = m_pane.indexOfComponent(m_flowVariableTab);
                int memIndex = m_pane.indexOfComponent(m_memPolicyTab);
                int jobMgrIdx = m_pane.indexOfComponent(m_jobMgrTab);

                if (memIndex >= 0) {
                    // make sure the miscellaneous tab is the last tab
                    if (insertIdx.intValue() > memIndex) {
                        insertIdx.setValue(memIndex);
                    }
                }
                if (varTabIdx >= 0) {
                    // make sure the variables tab is the second last tab
                    if (insertIdx.intValue() > varTabIdx) {
                        insertIdx.setValue(varTabIdx);
                    }

                }
                if (jobMgrIdx >= 0) {
                    // make sure the job manager tab is the third last tab
                    if (insertIdx.intValue() > jobMgrIdx) {
                        insertIdx.setValue(jobMgrIdx);
                    }

                }
                if (insertIdx.intValue() > m_pane.getTabCount()) {
                    insertIdx.setValue(m_pane.getTabCount());
                }

                // add it to the tabbed pane and the hash map
                m_pane.insertTab(title, null, newTab, null,
                        insertIdx.intValue());
                m_tabs.put(title, newTab);

                // listens to components added/removed from this dialog
                newTab.addHierarchyListener(HIERARCHY_LISTENER);

                // convert all light weight popup components to non-light weight
                noLightWeight(newTab);
            }
        });

        return insertIdx.intValue();

    }

    /**
     * Sets the enable status of a certain tab in the dialog. This does not
     * affect any component placed in the tab - just the tab itself. Disabled
     * tabs can't be brought to front - but if the tab which is currently
     * visible is disabled it works as before, i.e. all components in the
     * tab can be used/clicked/changed.
     *
     * @param enabled set to true to enable the specified tab, or to false to
     *            disable it.
     * @param tabTitle the title of the tab to en/disable
     * @throws IllegalArgumentException if a tab with the specified tabTitle
     *             does not exist.
     */
    protected final void setEnabled(final boolean enabled,
            final String tabTitle) {

        final Component pane = m_tabs.get(tabTitle);
        if (pane == null) {
            throw new IllegalArgumentException("Tab with the specified title '"
                    + tabTitle + "' doesn't exist.");
        }

        ViewUtils.invokeAndWaitInEDT(new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                int tabIdx = m_pane.indexOfComponent(pane);
                if (tabIdx < 0) {
                    // Ooops. Our hash map is out of sync...
                    m_tabs.remove(pane);
                    throw new IllegalArgumentException(
                            "Tab with the specified title '" + tabTitle
                            + "' doesn't exist.");
                }

                m_pane.setEnabledAt(tabIdx, enabled);
            }
        });

    }

    /**
     * Selects the tab with the specified title. Selected tabs are in front of
     * all the other tabs and their components are shown.
     *
     * @param tabTitle the title to bring to front,
     * @return true, if it selected the specified tab, false, if a tab with this
     *         title doesn't exist.
     */
    protected final boolean setSelected(final String tabTitle) {
        int index = getTabIndex(tabTitle);
        if (index >= 0) {
            m_pane.setSelectedIndex(index);
        }
        return index >= 0;
    }

    /*
     * Converts the specified and all its parent Component object to no-light
     * weight components if they are of type JComboBox, JPopupMenu, or
     * BasicComboPopup.
     */
    private static void noLightWeight(final Component c) {
        if (c instanceof Container) {
            if (c instanceof JComboBox) {
                JComboBox cb = (JComboBox)c;
                cb.setLightWeightPopupEnabled(false);
            }
            if (c instanceof JPopupMenu) {
                JPopupMenu pm = (JPopupMenu)c;
                pm.setLightWeightPopupEnabled(false);
            }
            if (c instanceof BasicComboPopup) {
                BasicComboPopup cp = (BasicComboPopup)c;
                cp.setLightWeightPopupEnabled(false);
            }
            Container o = (Container)c;
            for (int cnt = o.getComponentCount(); --cnt >= 0;) {
                Component newComponent = o.getComponent(cnt);
                noLightWeight(newComponent);
            }
        }
    }

    /**
     * Returns the component of the tab with the specified title.
     *
     * @param title The name of tab to return the component from.
     * @return the component of the tab with the given <code>title</code>.
     */
    protected final Component getTab(final String title) {
        return m_tabs.get(title);
    }

    /**
     * Removes the tab and its component specified by the <code>title</code>
     * from the tabbed pane. Does nothing if a tab with the specified title
     * doesn't exist.
     *
     * @param name The title of the tab to remove.
     * @see #getTabIndex(String)
     * @see #addTabAt(int, String, Component)
     */
    protected final void removeTab(final String name) {
        final Component comp = getTab(name);

        if (comp != null) {
            ViewUtils.invokeAndWaitInEDT(new Runnable() {
                /** {@inheritDoc} */
                @Override
                public void run() {
                    comp.removeHierarchyListener(HIERARCHY_LISTENER);
                    m_pane.remove(comp);
                    m_tabs.remove(name);
                }

            });
        }

    }

    /**
     * Returns the current index of the specified tab. Or -1 if no tab with the
     * given title exists.
     * @param title of the tab to return the index for.
     * @return the index of the tab with the specified title.
     */
    protected final int getTabIndex(final String title) {
        return m_pane.indexOfTab(title);
    }

    /**
     * Controls the behavior of the dialog when the ESC key is pressed. By
     * default it allows the dialog to cancel and close. Dialogs that consume
     * the ESC event can overwrite and return false, if the dialog should not
     * be canceled on ESC.
     *
     * @return true, if the dialog should cancel on ESC, false if it should stay
     *         open.
     */
    public boolean closeOnESC() {
        return true;
    }

    /** Create model and register a new variable for a specific settings entry
     * (in a non-hierarchical settings object).
     * This can serve two purposes:
     * 1) replace the actual value in the settings object by the value of
     *    the variable
     * 2) and/or put the current value of the settings object into the
     *    specified variable.
     *
     * @param key of corresponding settings object
     * @param type of variable/settings object
     * @return new FlowVariableModel which is already registered
     */
    public FlowVariableModel createFlowVariableModel(
            final String key, final FlowVariable.Type type) {
        return createFlowVariableModel(new String[] {key}, type);
    }

    /** Create model and register a new variable for a specific settings entry
     * for a hierarchical settings object.
     * This can serve two purposes:
     * 1) replace the actual value in the settings object by the value of
     *    the variable
     * 2) and/or put the current value of the settings object into the
     *    specified variable.
     *
     * @param keys hierarchy of keys of corresponding settings object
     * @param type of variable/settings object
     * @return new FlowVariableModel which is already registered
     */
    public FlowVariableModel createFlowVariableModel(
            final String[] keys, final FlowVariable.Type type) {
        FlowVariableModel wvm = new FlowVariableModel(this, keys, type);
        m_flowVariablesModelList.add(wvm);
        wvm.addChangeListener(new ChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_flowVariablesModelChanged = true;
                updateFlowVariablesOverwriteWarning();
            }
        });
        return wvm;
    }

    /** Create model and register a new variable for a specific settings
     * object.
     *
     * @param dc settings object of corresponding DialogComponent
     * @return new FlowVariableModel which is already registered
     */
    protected FlowVariableModel createFlowVariableModel(
            final SettingsModelFlowVariableCompatible dc) {
        return createFlowVariableModel(dc.getKey(),
                dc.getFlowVariableType());
    }

    /** Sets the settings from the second argument into the flow variables tab.
     * The parameter tree is supposed to follow the node settings argument.
     * @param nodeSettings The (user) settings of the node.
     * @param variableSettings The flow variable settings.
     */
    @SuppressWarnings("unchecked")
    private void initFlowVariablesTab(final NodeSettingsRO nodeSettings,
            final NodeSettingsRO variableSettings) {
        m_flowVariableTab.setErrorLabel("");
        m_flowVariableTab.setVariableSettings(nodeSettings, variableSettings,
                m_flowObjectStack, Collections.EMPTY_SET);
        for (FlowVariableModel m : m_flowVariablesModelList) {
            ConfigEditTreeNode configNode = m_flowVariableTab
            .findTreeNodeForChild(m.getKeys());
            if (configNode != null) {
                m.setInputVariableName(configNode.getUseVariableName());
                m.setOutputVariableName(configNode.getExposeVariableName());
            }
        }
        m_flowVariablesModelChanged = false;
    }

    /** Updates the flow variable tab to reflect the current parameter tree.
     * It also includes the config of the flow variable models (the little
     * buttons attached to some control elements). */
    private void updateFlowVariablesTab() {
        m_flowVariableTab.setErrorLabel("");
        NodeSettings settings = new NodeSettings("save");
        NodeSettings variableSettings;
        commitComponentsRecursively(getPanel());
        try {
            saveSettingsTo(settings);
            variableSettings = m_flowVariableTab.getVariableSettings();
        } catch (Throwable e) {
            if (!(e instanceof InvalidSettingsException)) {
                m_logger.error("Saving intermediate settings failed with "
                        + e.getClass().getSimpleName(), e);
            }
            String error = "Panel does not reflect current settings; failed to "
                + "save intermediate settings:<br/>" + e.getMessage();
            m_flowVariableTab.setErrorLabel(error);
            return;
        }
        m_flowVariableTab.setVariableSettings(settings, variableSettings,
                m_flowObjectStack, m_flowVariablesModelList);
    }

    /** Updates the warning message below the panel to inform the user
     * whether any parameter is overwritten using flow variables. */
    private void updateFlowVariablesOverwriteWarning() {
        Component activeTab = m_pane.getSelectedComponent();
        Set<String> overwrittenParams =
            m_flowVariableTab.getVariableControlledParameters();
        if (m_flowVariablesModelChanged) {
            for (FlowVariableModel f : m_flowVariablesModelList) {
                String[] keyPath = f.getKeys();
                String key = keyPath[keyPath.length - 1];
                if (f.getInputVariableName() == null) {
                    overwrittenParams.remove(key);
                } else {
                    overwrittenParams.add(key);
                }
            }
        }
        if (activeTab != m_flowVariableTab && !overwrittenParams.isEmpty()) {
            String msg = buildVariableOverwriteWarning(overwrittenParams);
            m_statusBarLabel.setVisible(true);
            m_statusBarLabel.setText(msg);
            m_statusBarLabel.setToolTipText(msg);
        } else {
            m_statusBarLabel.setVisible(false);
        }
    }

    private static String buildVariableOverwriteWarning(
            final Collection<String> params) {
        StringBuilder b = new StringBuilder();
        if (params.size() == 1) {
            String key = params.iterator().next();
            b.append("The \"").append(key);
            b.append("\" parameter is controlled by a variable.");
        } else {
            int size = params.size();
            int i = 0;
            for (String s : params) {
                b.append("\"").append(s).append("\"");
                if (i == size - 2) {
                    b.append(" and ");
                } else if (i == size - 1) {
                    // last key, nothing to separate
                } else {
                    b.append(", ");
                }
                i++;
            }
            b.append(" are controlled by variables.");
        }
        return b.toString();
    }

    /** The tab currently called "Flow Variables". It allows the user to mask
     * certain settings of the dialog (for instance to use variables instead
     * of hard-coded values.) */
    private class FlowVariablesTab extends JPanel
    implements ConfigEditTreeEventListener {

        private final ConfigEditJTree m_tree;
        private final JLabel m_errorLabel;

        /** Creates new tab. */
        public FlowVariablesTab() {
            super(new BorderLayout());
            m_tree = new ConfigEditJTree();
            m_errorLabel = new JLabel();
            m_errorLabel.setForeground(Color.RED);
            // nesting m_tree directly into the scrollpane causes the dialog
            // to take oversized dimensions
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(m_tree, BorderLayout.CENTER);
            JScrollPane scrPanel = new JScrollPane(panel);
            scrPanel.setPreferredSize(new Dimension(150, 100));
            add(scrPanel, BorderLayout.CENTER);
            add(m_errorLabel, BorderLayout.NORTH);
        }

        /** Find the tree node that is associated with the given key path.
         * @param keyPath The path in the tree (single root is omitted).
         * @return The config node or null if not present.
         */
        ConfigEditTreeNode findTreeNodeForChild(final String[] keyPath) {
            return m_tree.getModel().findChildForKeyPath(keyPath);
        }

        /** Update the panel to reflect new properties.
         * @param nodeSettings Settings of the node (or currently entered in
         *  the remaining tabs of the dialog.
         * @param varSettings  The variable mask.
         * @param stack the stack to get the variables from.
         * @param variableModels The models that may be used in the main panels
         * of the dialog to overwrite settings in place. */
        void setVariableSettings(final NodeSettingsRO nodeSettings,
                final NodeSettingsRO varSettings,
                final FlowObjectStack stack,
                final Collection<FlowVariableModel> variableModels) {
            if (nodeSettings != null && !(nodeSettings instanceof Config)) {
                m_logger.debug("Node settings object not instance of "
                        + Config.class + " -- disabling flow variables tab");
                return;
            }
            if (varSettings != null && !(varSettings instanceof Config)) {
                m_logger.debug("Variable settings object not instance of "
                        + Config.class + " -- disabling flow variables tab");
                return;
            }
            Config nodeSetsCopy = nodeSettings == null
            ? new NodeSettings("variables") : (Config)nodeSettings;
            ConfigEditTreeModel model;
            try {
                if (varSettings == null) {
                    model = ConfigEditTreeModel.create(nodeSetsCopy);
                } else {
                    model = ConfigEditTreeModel.create(
                            nodeSetsCopy, (Config)varSettings);
                }
            } catch (InvalidSettingsException e) {
                JOptionPane.showMessageDialog(this, "Errors reading variable "
                        + "configuration: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                model = ConfigEditTreeModel.create(nodeSetsCopy);
            }
            final ConfigEditTreeModel newModel = model;
            ViewUtils.invokeAndWaitInEDT(new Runnable() {
                @Override
                public void run() {
                    newModel.update(variableModels);
                    m_tree.setFlowObjectStack(stack);
                    FlowVariablesTab lis = FlowVariablesTab.this;
                    m_tree.getModel().removeConfigEditTreeEventListener(lis);
                    m_tree.setModel(newModel);
                    newModel.addConfigEditTreeEventListener(lis);
                    // if this isn't run after initialization it will not paint
                    // the very first row in the table correctly
                    // (label slightly off & combo box not visible)
                    SwingUtilities.invokeLater(new Runnable() {
                        /** {@inheritDoc} */
                        @Override
                        public void run() {
                            m_tree.repaint();
                        }
                    });
                };
            });
        }

        /**
         * @param error the errorLabel to set
         */
        public void setErrorLabel(final String error) {
            m_errorLabel.setText("<html><body>" + error + "</body></html>");
        }

        /** @return the variables mask as node settings object. */
        public NodeSettings getVariableSettings() {
            m_tree.getCellEditor().cancelCellEditing();
            ConfigEditTreeModel model = m_tree.getModel();
            if (model.hasConfiguration()) {
                NodeSettings settings = new NodeSettings("variables");
                model.writeVariablesTo(settings);
                return settings;
            }
            return null;
        }

        /** @return list of params that are controlled by a variable (then
         * shown in status bar).
         */
        Set<String> getVariableControlledParameters() {
            return m_tree.getModel().getVariableControlledParameters();
        }

        /** {@inheritDoc} */
        @Override
        public void configEditTreeChanged(final ConfigEditTreeEvent event) {
            String[] keyPath = event.getKeyPath();
            for (FlowVariableModel m : m_flowVariablesModelList) {
                if (Arrays.equals(m.getKeys(), keyPath)) {
                    m.setInputVariableName(event.getUseVariable());
                    m.setOutputVariableName(event.getExposeVariableName());
                }
            }
        }
    }

}

