/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   18.04.2006 (mb): created
 */
package de.unikn.knime.core.node.defaultnodesettings;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.defaultnodedialog.DialogComponent;

/** Default implementation for a NodeDialogPane that allows to register
 * standard DialogComponents which will be displayed in a standard
 * way and automatically stored and retrieved in the node settings
 * objects.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class DefaultNodeDialog extends NodeDialogPane {
    private List<DialogComponent> m_dialogComponents;
    private JPanel m_panel;

    /** Constructor for DefaultNodeDialogPane.
     * 
     * @param title dialog title
     */
    public DefaultNodeDialog(final String title) {
        super(title);
        m_dialogComponents = new ArrayList<DialogComponent>();
        m_panel = new JPanel();
        m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.Y_AXIS));
        super.addTab("Default Options", m_panel);
    }

    /** add a new DialogComponent to the underlying dialog. It will
     * automatically be added in the dialog and saved/loaded from/to
     * the config.
     * @param diaC component to be added
     */
    public void addDialogComponent(final DialogComponent diaC) {
        m_dialogComponents.add(diaC);
        m_panel.add(diaC);
    }

    /** Load Settings for all registered components.
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     */
    @Override
    public final void loadSettingsFrom(
            final NodeSettings settings, final DataTableSpec[] specs) throws InvalidSettingsException {
        assert (settings != null && specs != null);
        try {
            for (DialogComponent comp : m_dialogComponents) {
                comp.loadSettingsFrom(settings, specs);
            }
        } catch (InvalidSettingsException ise) {
            assert false : ise.getMessage();
        }
    } 
    
    /**
     * save settings of all registered <code>DialogComponents</code> into
     * the configuration object.
     * 
     * @param settings The <code>NodeSettings</code> to write into.
     * @see NodeDialogPane#saveSettingsTo(NodeSettings)
     * @throws InvalidSettingsException if the user has entered wrong
     * values.
     */
    @Override
    public final void saveSettingsTo(final NodeSettings settings) 
                                throws InvalidSettingsException {
        for (DialogComponent comp : m_dialogComponents) {
            comp.saveSettingsTo(settings);
        }
    }

}
