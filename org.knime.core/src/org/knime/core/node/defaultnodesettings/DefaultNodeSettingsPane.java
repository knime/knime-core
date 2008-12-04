/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * -------------------------------------------------------------------
 *
 * History
 *   20.09.2005 (mb): created
 *   2006-05-24 (tm): reviewed
 */
package org.knime.core.node.defaultnodesettings;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Default implementation for a NodeDialogPane that allows to add standard
 * DialogComponents which will be displayed in a standard way and automatically
 * stored and retrieved in the node settings objects.
 *
 * @author M. Berthold, University of Konstanz
 */
public class DefaultNodeSettingsPane extends NodeDialogPane {

    /* The tabs' names. */
    private static final String TAB_TITLE = "Options";

    private final List<DialogComponent> m_dialogComponents;

    private JPanel m_compositePanel;

    private JPanel m_currentPanel;

    private String m_defaultTabTitle = TAB_TITLE;

    private Box m_currentBox;

    private boolean m_horizontal = false;

    /**
     * Constructor for DefaultNodeDialogPane.
     */
    public DefaultNodeSettingsPane() {
        super();
        m_dialogComponents = new ArrayList<DialogComponent>();
        createNewPanels();
        super.addTab(m_defaultTabTitle, m_compositePanel);
    }

    private void createNewPanels() {
        m_compositePanel = new JPanel();
        m_compositePanel.setLayout(new BoxLayout(m_compositePanel,
                BoxLayout.Y_AXIS));
        m_currentPanel = m_compositePanel;
        m_currentBox = createBox(m_horizontal);
        m_currentPanel.add(m_currentBox);
    }

    /**
     * Sets the title of the default tab that is created and used until you call
     * {@link #createNewTab}.
     *
     * @param tabTitle the new title of the first tab. Can't be null or empty.
     * @throws IllegalArgumentException if the title is already used by another
     *             tab, or if the specified title is null or empty.
     */
    public void setDefaultTabTitle(final String tabTitle) {
        if ((tabTitle == null) || (tabTitle.length() == 0)) {
            throw new IllegalArgumentException("The title of a tab can't be "
                    + "null or empty.");
        }
        if (tabTitle.equals(m_defaultTabTitle)) {
            return;
        }
        // check if we already have a tab with the new title
        if (super.getTab(tabTitle) != null) {
            throw new IllegalArgumentException("A tab with the specified new"
                    + " name (" + tabTitle + ") already exists.");
        }
        super.renameTab(m_defaultTabTitle, tabTitle);
        m_defaultTabTitle = tabTitle;
    }

    /**
     * Creates a new tab in the dialog. All components added from now on are
     * placed in that new tab. After creating a new tab the previous tab is no
     * longer accessible. If a tab with the same name was created before an
     * Exception is thrown. The new panel in the new tab has no group set (i.e.
     * has no border). The new tab is placed at the specified position (or at
     * the right most position, if the index is too big).
     *
     * @param tabTitle the title of the new tab to use from now on. Can't be
     *            null or empty.
     * @param index the index to place the new tab at. Can't be negative.
     * @throws IllegalArgumentException if you specify a title that is already
     *             been used by another tab. Or if the specified title is null
     *             or empty.
     * @see #setDefaultTabTitle(String)
     */
    public void createNewTabAt(final String tabTitle, final int index) {
        if ((tabTitle == null) || (tabTitle.length() == 0)) {
            throw new IllegalArgumentException("The title of a tab can't be "
                    + "null nor empty.");
        }
        // check if we already have a tab with the new title
        if (super.getTab(tabTitle) != null) {
            throw new IllegalArgumentException("A tab with the specified new"
                    + " name (" + tabTitle + ") already exists.");
        }
        createNewPanels();
        super.addTabAt(index, tabTitle, m_compositePanel);
    }

    /**
     * Creates a new tab in the dialog. All components added from now on are
     * placed in that new tab. After creating a new tab the previous tab is no
     * longer accessible. If a tab with the same name was created before an
     * Exception is thrown. The new panel in the new tab has no group set (i.e.
     * has no border). The tab is placed at the right most position.
     *
     * @param tabTitle the title of the new tab to use from now on. Can't be
     *            null or empty.
     * @throws IllegalArgumentException if you specify a title that is already
     *             been used by another tab. Or if the specified title is null
     *             or empty.
     * @see #setDefaultTabTitle(String)
     */
    public void createNewTab(final String tabTitle) {
        createNewTabAt(tabTitle, Integer.MAX_VALUE);
    }

    /**
     * Brings the specified tab to front and shows its components.
     *
     * @param tabTitle the title of the tab to select. If the specified title
     *            doesn't exist, this method does nothing.
     */
    public void selectTab(final String tabTitle) {
        setSelected(tabTitle);
    }

    /**
     * Creates a new dialog component group and closes the current one. From now
     * on the dialog components added with the addDialogComponent method are
     * added to the current group. The group is a bordered and titled panel.
     *
     * @param title - the title of the new group.
     */
    public void createNewGroup(final String title) {
        checkForEmptyBox();
        m_currentPanel = createSubPanel(title);
        m_currentBox = createBox(m_horizontal);
        m_currentPanel.add(m_currentBox);
    }

    private JPanel createSubPanel(final String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), title));
        m_compositePanel.add(panel);
        return panel;
    }

    /**
     * Closes the current group. Further added dialog components are added to
     * the default panel outside any border.
     *
     */
    public void closeCurrentGroup() {
        checkForEmptyBox();
        if (m_currentPanel.getComponentCount() == 0) {
            m_compositePanel.remove(m_currentPanel);
        }
        m_currentPanel = m_compositePanel;
        m_currentBox = createBox(m_horizontal);
        m_currentPanel.add(m_currentBox);
    }

    /**
     * Add a new DialogComponent to the underlying dialog. It will automatically
     * be added in the dialog and saved/loaded from/to the config.
     *
     * @param diaC component to be added
     */
    public void addDialogComponent(final DialogComponent diaC) {
        m_dialogComponents.add(diaC);
        m_currentBox.add(diaC.getComponentPanel());
        addGlue(m_currentBox, m_horizontal);
    }

    /**
     * Changes the orientation the components get placed in the dialog.
     * @param horizontal <code>true</code> if the next components should be
     * placed next to each other or <code>false</code> if the next components
     * should be placed below each other.
     */
    public void setHorizontalPlacement(final boolean horizontal) {
        if (m_horizontal != horizontal) {
            m_horizontal = horizontal;
            checkForEmptyBox();
            m_currentBox = createBox(m_horizontal);
            m_currentPanel.add(m_currentBox);
        }
    }

    /**
     * Load settings for all registered components.
     *
     * @param settings the <code>NodeSettings</code> to read from
     * @param specs the input specs
     * @throws NotConfigurableException if the node can currently not be
     *             configured
     */
    @Override
    public final void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        assert settings != null;
        assert specs != null;

        for (DialogComponent comp : m_dialogComponents) {
            comp.loadSettingsFrom(settings, specs);
        }

        loadAdditionalSettingsFrom(settings, specs);
    }

    /**
     * Save settings of all registered <code>DialogComponents</code> into the
     * configuration object.
     *
     * @param settings the <code>NodeSettings</code> to write into
     * @throws InvalidSettingsException if the user has entered wrong values
     */
    @Override
    public final void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        for (DialogComponent comp : m_dialogComponents) {
            comp.saveSettingsTo(settings);
        }

        saveAdditionalSettingsTo(settings);
    }

    /**
     * This method can be overridden to load additional settings.
     *
     * @param settings the <code>NodeSettings</code> to read from
     * @param specs the input specs
     * @throws NotConfigurableException if the node can currently not be
     *             configured
     */
    @SuppressWarnings("unused")
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        assert settings != null;
        assert specs != null;
    }

    /**
     * This method can be overridden to save additional settings to the given
     * settings object.
     *
     * @param settings the <code>NodeSettings</code> to write into
     * @throws InvalidSettingsException if the user has entered wrong values
     */
    @SuppressWarnings("unused")
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        assert settings != null;
    }

    private void checkForEmptyBox() {
        if (m_currentBox.getComponentCount() == 0) {
            m_currentPanel.remove(m_currentBox);
        }
    }

    /**
     * @param currentBox
     * @param horizontal
     */
    private static void addGlue(final Box box, final boolean horizontal) {
        if (horizontal) {
            box.add(Box.createVerticalGlue());
        } else {
            box.add(Box.createHorizontalGlue());
        }
    }

    /**
     * @param horizontal <code>true</code> if the layout is horizontal
     * @return the box
     */
    private static Box createBox(final boolean horizontal) {
        final Box box;
        if (horizontal) {
            box = new Box(BoxLayout.X_AXIS);
            box.add(Box.createVerticalGlue());
        } else {
            box = new Box(BoxLayout.Y_AXIS);
            box.add(Box.createHorizontalGlue());
        }
         return box;
    }
}
