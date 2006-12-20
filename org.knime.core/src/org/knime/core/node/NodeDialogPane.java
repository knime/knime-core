/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.LinkedHashMap;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.plaf.basic.BasicComboPopup;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.Node.MemoryPolicy;


/**
 * The base class for all node dialogs. It provides a tabbed pane to which the
 * derived dialog can add its own components (method
 * <code>addTab(String,Component)</code>). Methods <code>#onOpen()</code>
 * and <code>#onClose()</code> are to be implemented, to setup and cleanup the
 * dialog. A method <code>#onApply()</code> must be provided by the derived
 * class to transfer the user choices into the node's model.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class NodeDialogPane {
    
    private static final String TAB_NAME_MISCELLANEOUS = 
        "General Node Settings";

    /**
     * Tabbed pane in the center of the dialog, which holds the components added
     * by the derived class via <code>#addTab(String,Component)</code>.
     */
    private final JTabbedPane m_pane;

    /**
     * Keeps all components which are added to the tabbed pane above by its
     * string title.
     */
    private final LinkedHashMap<String, Component> m_tabs;

    /** Node reference set once which is informed about the dialog's apply. 
     * @deprecated This member as NodeContainer will be moved into the 
     *             {@link NodeDialog}. */
    private Node m_node;
    
    /** The additional tab in which the user can set the memory options. 
     * This field is null when m_node has no data outports. 
     */
    private MiscSettingsTab m_miscTab;

    /** The underlying panel which keeps all the tabs. */
    private final JPanel m_panel;
    
    /**
     * Creates a new dialog with the given title. The pane holds a tabbed pane
     * ready to take additional components needed by the derived class 
     * (<code>#addTab(String,Component)</code>).
     */
    protected NodeDialogPane() {
        m_panel = new JPanel();
        // init the panel with layout
        m_panel.setLayout(new BorderLayout());
        // init map for tabs
        m_tabs = new LinkedHashMap<String, Component>();
        // init tabbed pane and at it to the underlying panel
        m_pane = new JTabbedPane();
        m_panel.add(m_pane, BorderLayout.CENTER);
    }

    /**
     * Sets the node for this dialog. Can only be called once.
     * 
     * @param node The underlying node.
     * @deprecated The <code>Node</code> member inside the dialog pane is
     *             obsolete.
     */
    final void setNode(final Node node) {
        assert (m_node == null && node != null);
        m_node = node;
        if (m_node.getNrDataOutPorts() > 0) {
            m_miscTab = new MiscSettingsTab();
            addTab(TAB_NAME_MISCELLANEOUS, m_miscTab);
        }
    }

    /**
     * Reads and applies the settings from the XML stream into this dialog's
     * pane.
     * 
     * @param is The XML stream to read the settings from.
     * @throws IOException If the stream is not valid.
     * @throws NotConfigurableException if the dialog cannot be opened because
     * of real invalid settings or if any predconditions are not fulfilled, e.g.
     * no predecessor node, no nominal column in input table, etc.
     */
    public final void loadSettings(final InputStream is)
    throws IOException, NotConfigurableException {
        DataTableSpec[] specs = m_node.getInDataTableSpecs();
        loadSettingsFrom(NodeSettings.loadFromXML(is), specs);
    }

    /**
     * Saves this dialog's settings into the given output stream by the
     * specified name.
     * 
     * @param os The stream to write to.
     * @param name The name of this settings.
     * @throws InvalidSettingsException If the current dialog settings are
     *             invalid.
     * @throws IOException If the stream could not be written.
     */
    public final void saveSettings(final OutputStream os, final String name)
            throws InvalidSettingsException, IOException {
        NodeSettings sett = new NodeSettings(name);
        saveSettingsTo(sett);
        sett.saveToXML(os);
    }

    /**
     * @return The underlying dialog panel which keeps the tabbed pane.
     */
    public final JPanel getPanel() {
        return m_panel;
    }
    
    /** Method being called from the node when the dialog shall load the
     * settings from a NodeSettingsRO object. This method will call the
     * abstract loadSettingsFrom method and finally load internals
     * (i.e. memory policy of outports, if any).
     * @param settings To load from.
     * @param specs The DTSs from the inports.
     * @throws NotConfigurableException 
     * If loadSettingsFrom throws this exception.
     * @see #loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    final void internalLoadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        NodeSettingsRO modelSettings;
        try {
            modelSettings = settings.getNodeSettings(Node.CFG_MODEL);
        } catch (InvalidSettingsException ise) {
            modelSettings = new NodeSettings("empty");
        }
        loadSettingsFrom(modelSettings, specs);
        try {
            NodeSettingsRO subSettings = 
                settings.getNodeSettings(Node.CFG_MISC_SETTINGS);
            if (m_node.getNrDataOutPorts() > 0) {
                String memoryPolicy = subSettings.getString(
                        Node.CFG_MEMORY_POLICY, 
                        MemoryPolicy.CacheSmallInMemory.toString());
                MemoryPolicy policy;
                try {
                    policy = MemoryPolicy.valueOf(memoryPolicy);
                } catch (IllegalArgumentException iae) {
                    policy = MemoryPolicy.CacheInMemory;
                }
                m_miscTab.setStatus(policy);
            }
        } catch (InvalidSettingsException ise) {
            m_miscTab.setStatus(MemoryPolicy.CacheInMemory);
        }
    }
    
    /**
     * Called from the node when the current settings shall be writting to 
     * a NodeSettings object. It will call the abstract saveSettingsTo method
     * and finally write misc settings to the argument object. Misc settings 
     * @param settings To write to. Forwarded to abstract saveSettings method.
     * @throws InvalidSettingsException If any of the writing fails.
     */
    final void internalSaveSettingsTo(final NodeSettingsWO settings)
        throws InvalidSettingsException {
        saveSettingsTo(settings.addNodeSettings(Node.CFG_MODEL));
        NodeSettingsWO subSettings = 
            settings.addNodeSettings(Node.CFG_MISC_SETTINGS);
        if (m_node.getNrDataOutPorts() > 0) {
            MemoryPolicy pol = m_miscTab.getStatus();
            subSettings.addString(Node.CFG_MEMORY_POLICY, pol.toString());
        }
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
     *            if no spec is available from the corresponding input port.
     * @throws NotConfigurableException if the dialog cannot be opened because
     * of real invalid settings or if any predconditions are not fulfilled, e.g.
     * no predecessor node, no nominal column in input table, etc.
     * @see NodeModel#loadSettingsFrom(NodeSettingsRO)
     */
    protected abstract void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException;

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
     * @param settings The settings object to write into.
     * @throws InvalidSettingsException If the settings are not applicable to
     *             the model.
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    void finishEditingAndSaveSettingsTo(final NodeSettingsWO settings) 
        throws InvalidSettingsException {
        commitComponentsRecursively(getPanel());
        internalSaveSettingsTo(settings);
    }

    /**
     * Invoked when the derived dialog needs to apply its settings to the
     * model. The node is reset and configured after settings
     * were applied successfully - only if the model and dialog settings
     * are not identical.
     * @throws InvalidSettingsException If the settings could not be set in the
     *             <code>Node</code>.
     * @deprecated Normally the workflow manager should be involved when
     * resetting nodes. This method does it behind the workflow manager and
     * should therefore not be used
     */
    @Deprecated
    public final void doApply() throws InvalidSettingsException {
        // try to load dialog settings to the model
        m_node.loadModelSettingsFromDialog();
        // reset node and configure
        m_node.resetAndConfigure();
    }
    
    /**
     * Determines wether the settings in the dialog are the same as 
     * in the model.
     * 
     * @return true if the settings are equal
     */
    public final boolean isModelAndDialogSettingsEqual() {
        return m_node.isModelAndDialogSettingsEqual();
    }

    /**
     * @return <code>true</code> if this node has been executed.
     */
    final boolean isNodeExecuted() {
        return m_node.isExecuted();
    }

    /**
     * Validates the settings from the dialog inside the <code>Node</code>.
     * 
     * @throws InvalidSettingsException <code>true</code> if the settings are
     *             not valid.
     */
    public final void validateSettings() throws InvalidSettingsException {
        m_node.validateModelSettingsFromDialog();
    }

    /**
     * JSpinner seem to have the "feature" that their value is not commited when
     * they are being edited (by hand, not with the arrows) and someone presses
     * an button. This method traverse all components recursively and commits
     * the values if it finds components that are JSpinners.
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

    /**
     * Sets the panel of the view's content pane center area placed in the
     * center.
     * 
     * @param title The title inside the tabbed pane for the given component.
     *            Must be unique for this dialog.
     * @param comp The component to add to this dialog's tabbed pane.
     * @throws NullPointerException If either the title or the component is
     *             <code>null</code>.
     */
    protected final void addTab(final String title, final Component comp) {
        if (title == null) {
            throw new NullPointerException();
        }
        if (comp == null) {
            throw new NullPointerException();
        }
        // listens to components which are added/removed from this dialog
        comp.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(final HierarchyEvent e) {
                noLightWeight(e.getComponent());
            }
        });
        m_tabs.put(title, comp);
        int miscIndex = m_pane.indexOfComponent(m_miscTab);
        // make sure the Miscallaneous tab is always the last tab.
        if (miscIndex >= 0) {
            m_pane.insertTab(title, null, comp, null, miscIndex);
        } else {
            m_pane.addTab(title, comp);
        }
        // convert all light weight popup components to non-light weight
        noLightWeight(comp);
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
     * Returns a component from the tabbed pane for the given 
     * <code>title</code>.
     * 
     * @param title The name of this component.
     * @return Component in the tabbed pane with the given <code>title</code>.
     */
    protected final Component getTab(final String title) {
        return m_tabs.get(title);
    }

    /**
     * Removes a component given by the <code>title</code> from the tabbed
     * pane.
     * 
     * @param name The name of the component to remove.
     * 
     * @see #getTab(String)
     */
    protected final void removeTab(final String name) {
        m_pane.remove(getTab(name));
        m_tabs.remove(name);
    }
    
    private static class MiscSettingsTab extends JPanel {
        private final ButtonGroup m_group;
        
        /** Inits GUI. */
        public MiscSettingsTab() {
            super(new BorderLayout());
            m_group = new ButtonGroup();
            JRadioButton cacheAll = new JRadioButton("Keep all in memory.");
            cacheAll.setActionCommand(MemoryPolicy.CacheInMemory.toString());
            m_group.add(cacheAll);
            cacheAll.setToolTipText(
                    "All generated output data is kept in main memory, " 
                    + "resulting in faster execution of successing nodes but "
                    + "also in more memory usage.");
            JRadioButton cacheSmall = new JRadioButton(
                    "Keep only small tables in memory.", true);
            cacheSmall.setActionCommand(
                    MemoryPolicy.CacheSmallInMemory.toString());
            m_group.add(cacheSmall);
            cacheSmall.setToolTipText("Tables with less than "
                    + DataContainer.MAX_CELLS_IN_MEMORY + " cells are kept in "
                    + "main memory, otherwise swapped to disc.");
            JRadioButton cacheOnDisc = new JRadioButton(
                    "Write tables to disc.");
            cacheOnDisc.setActionCommand(MemoryPolicy.CacheOnDisc.toString());
            m_group.add(cacheOnDisc);
            cacheOnDisc.setToolTipText("All output is immediately " 
                    + "written to disc to save main memory usage.");
            final int s = 15;
            JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, s, s));
            north.add(new JLabel("Select memory policy for data outport(s)"));
            add(north, BorderLayout.NORTH);
            JPanel bigCenter = 
                new JPanel(new FlowLayout(FlowLayout.LEFT, s, s));
            JPanel center = new JPanel(new GridLayout(0, 1));
            center.add(cacheAll);
            center.add(cacheSmall);
            center.add(cacheOnDisc);
            bigCenter.add(center);
            add(bigCenter, BorderLayout.CENTER);
        }
        
        /** Get the memory policy for the currently selected radio button.
         * @return The corresponding policy.
         */
        MemoryPolicy getStatus() {
            String memoryPolicy = m_group.getSelection().getActionCommand();
            return MemoryPolicy.valueOf(memoryPolicy);
        }
        
        /** Select the radio button for the given policy.
         * @param policy The one to use.
         */
        void setStatus(final MemoryPolicy policy) {
            for (Enumeration<AbstractButton> e = m_group.getElements(); 
                e.hasMoreElements();) {
                AbstractButton m = e.nextElement();
                if (m.getActionCommand().equals(policy.toString())) {
                    m.setSelected(true);
                    return;
                }
            }
            assert false;
        }
    } // class MiscSettingsTab
    
    /**
     * <code>NodeDialogPane</code> that only keeps a 
     * <i>General Node Settings</i> tab. Load and save methods are left blank.
     * 
     * @author Thomas Gabriel, University of Konstanz
     */
    static class MiscNodeDialogPane extends NodeDialogPane {
        
        /**
         * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, 
         *      DataTableSpec[])
         */
        @Override
        protected void loadSettingsFrom(final NodeSettingsRO settings, 
                final DataTableSpec[] specs) throws NotConfigurableException {

        }

        /**
         * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
         */
        @Override
        protected void saveSettingsTo(final NodeSettingsWO settings)
                throws InvalidSettingsException {
            
        }
        
    }
        

} // NodeDialogPane
