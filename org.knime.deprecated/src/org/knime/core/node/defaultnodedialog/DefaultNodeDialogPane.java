/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
