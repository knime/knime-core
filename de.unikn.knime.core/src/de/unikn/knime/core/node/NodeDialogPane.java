/*
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.node;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.LinkedHashMap;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicComboPopup;

import de.unikn.knime.core.data.DataTableSpec;

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

    /** Node reference set once which is informed about the dialog's apply. */
    private Node m_node;

    /** The underlying panel which keeps all the tabs. */
    private final JPanel m_panel;

    /**
     * Creates a new dialog with the given title. The pane holds a tabbed pane
     * ready to take additional components needed by the derived class 
     * (<code>#addTab(String,Component)</code>).
     * 
     * @param title The title of this dialog.
     */
    protected NodeDialogPane(final String title) {
        m_panel = new JPanel();
        // init the panel with layout
        m_panel.setLayout(new BorderLayout());
        //m_panel.setBackground(Color.GREEN);
        // set title
        m_panel.setName((title == null || title.length() == 0) 
                ? "Dialog - <no title>" : "Dialog - " + title);
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
     */
    final void setNode(final Node node) {
        assert (m_node == null && node != null);
        m_node = node;
    }

    /**
     * Reads and applies the settings from the XML stream into this dialog's
     * pane.
     * 
     * @param is The XML stream to read the settings from.
     * @throws IOException If the stream is not valid.
     */
    public final void loadSettings(final InputStream is) throws IOException {
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
     *            
     * @see NodeModel#loadSettingsFrom(NodeSettings)
     */
    protected abstract void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs);

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
     * @see NodeModel#loadSettingsFrom(NodeSettings)
     */
    protected abstract void saveSettingsTo(final NodeSettings settings)
            throws InvalidSettingsException;
    
    /** Commit spinners and save settings. It will first call the 
     * commitJSpinners method (which traverses all components and commits 
     * them if they are instance of JSpinner) and finally call 
     * <code>saveSettingsTo(settings)</code>.
     * @param settings The settings object to write into.
     * @throws InvalidSettingsException If the settings are not applicable to
     *             the model.
     * @see NodeModel#saveSettingsTo(NodeSettings)
     */
    void finishEditingAndSaveSettingsTo(final NodeSettings settings) 
        throws InvalidSettingsException {
        commitJSpinners(getPanel());
        saveSettingsTo(settings);
    }

    /**
     * Invoked when the derived dialog needs to apply its settings to the
     * model.
     * 
     * @throws InvalidSettingsException The setting could not be set in the
     *             <code>Node</code>.
     */
    public final void doApply() throws InvalidSettingsException {
        m_node.loadModelSettingsFromDialog();
    }
    
    /**
     * Determines wether the settings in the dialog are the same as 
     * in the model.
     * 
     * @return true if the settings are equal
     */
    public boolean isModelAndDialogSettingsEqual() {

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
    private static void commitJSpinners(final Component c) {
        if (c instanceof JSpinner) {
            try {
                ((JSpinner)c).commitEdit();
            } catch (ParseException e) {
                // ignore
            }
        } else if (c instanceof Container) {
            Component[] cs = ((Container)c).getComponents();
            for (int i = 0; i < cs.length; i++) {
                commitJSpinners(cs[i]);
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
        m_pane.addTab(title, comp);
        // convert all light weight popup components to non-light weight
        noLightWeight(comp);
    }

    /**
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
     * @return The title of this dialog pane.
     */
    public final String getTitle() {
        return m_panel.getName();
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

} // NodeDialogPane
