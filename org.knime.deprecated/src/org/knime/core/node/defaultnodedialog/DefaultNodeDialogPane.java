/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
package org.knime.core.node.defaultnodedialog;




import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * Default implementation for a NodeDialogPane that allows to register
 * standard DialogComponents which will be displayed in a standard
 * way and automatically stored and retrieved in the node settings
 * objects.
 * 
 * @author M. Berthold, University of Konstanz
 * @deprecated use classes in org.knime.core.node.defaultnodesettings instead
 */
public class DefaultNodeDialogPane extends NodeDialogPane {
    private final List<DialogComponent> m_dialogComponents;
    private final JPanel m_compositePanel;
    private JPanel m_currentPanel;

    /* The tabs' names. */
    private static final String TAB_TITLE = "Default Options";
    
    /**
     * Constructor for DefaultNodeDialogPane.
     */
    public DefaultNodeDialogPane() {
        super();
        m_dialogComponents = new ArrayList<DialogComponent>();
        m_compositePanel = new JPanel();
        m_compositePanel.setLayout(new BoxLayout(m_compositePanel, 
                BoxLayout.Y_AXIS));
        m_currentPanel = m_compositePanel;
        super.addTab(TAB_TITLE, m_compositePanel);
    }
    
    /**
     * Creates a new dialog component group and closes the current one. From now
     * on the dialog components added with the addDialogComponent method are
     * added to the current group. The group is a titled panel.
     * 
     * @param title - the title of the new group.
     */
    public void createNewGroup(final String title) {
        m_currentPanel = createSubPanel(title);
    }
    
    private JPanel createSubPanel(final String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title));
        m_compositePanel.add(panel);
        return panel;
    }
    
    /**
     * Closes the current group. Further added dialog components are added to 
     * the default panel.
     *
     */
    public void closeCurrentGroup() {
        if (m_currentPanel.getComponentCount() == 0) {
            m_compositePanel.remove(m_currentPanel);
        }
        m_currentPanel = m_compositePanel;
    }

    /**
     * Add a new DialogComponent to the underlying dialog. It will
     * automatically be added in the dialog and saved/loaded from/to
     * the config.
     * 
     * @param diaC component to be added
     */
    public void addDialogComponent(final DialogComponent diaC) {
        m_dialogComponents.add(diaC);
        m_currentPanel.add(diaC);
    }

    /**
     * Load settings for all registered components.
     * 
     * @param settings the <code>NodeSettings</code> to read from
     * @param specs the input specs
     * @throws NotConfigurableException if the node can currently not be
     * configured
     */
    @Override
    public final void loadSettingsFrom(
            final NodeSettingsRO settings, final DataTableSpec[] specs)
    throws NotConfigurableException {
        assert (settings != null && specs != null);
        try {
            for (DialogComponent comp : m_dialogComponents) {
                comp.loadSettingsFrom(settings, specs);
            }
        } catch (InvalidSettingsException ise) {
            // ignore, should be handled in each component
            // assert false : ise.getMessage();
        }
    }
    
    
    
    
    
    /**
     * Save settings of all registered <code>DialogComponents</code> into
     * the configuration object.
     * 
     * @param settings the <code>NodeSettings</code> to write into
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     * @throws InvalidSettingsException if the user has entered wrong
     * values
     */
    @Override
    public final void saveSettingsTo(final NodeSettingsWO settings) 
                                throws InvalidSettingsException {
        for (DialogComponent comp : m_dialogComponents) {
            comp.saveSettingsTo(settings);
        }
//        saveAdditionalSettingsTo(settings);
    }

//    public void saveAdditionalSettingsTo(final NodeSettings settings) {
//    }
}
