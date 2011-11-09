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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.database;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.DatabaseMetaData;
import java.util.Collection;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.DatabaseReaderConnection;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;

/**
 *
 * @author Thomas Gabriel, University of Konstanz
 */
class DBReaderDialogPane extends NodeDialogPane {

    private final boolean m_hasLoginPane;
    private final DBDialogPane m_loginPane = new DBDialogPane();

    private final JEditorPane m_statmnt = new JEditorPane("text", "");

    private final DefaultListModel m_listModelVars;
    private final JList m_listVars;

    /** Config key for the option to run the SQL query during configure. */
    static final String EXECUTE_WITHOUT_CONFIGURE = "execute_without_configure";

    private final JCheckBox m_configureBox = new JCheckBox(
            "Run SQL query only during execute, skips configure");
    
    private static final NodeLogger LOGGER = 
            NodeLogger.getLogger(DBReaderDialogPane.class);
    
    private SwingWorker<Void, Void> m_worker;

    /**
     * Creates new dialog.
     * @param hasLoginPane true, if a login pane is visible, otherwise false
     */
    DBReaderDialogPane(final boolean hasLoginPane) {
        super();
        
// init SQL statement component
        m_statmnt.setPreferredSize(new Dimension(350, 200));
        m_statmnt.setFont(DBDialogPane.FONT);
        m_statmnt.setText("SELECT * FROM "
                + DatabaseQueryConnectionSettings.TABLE_PLACEHOLDER);
        
// init database table browser
        final JPanel browserPanel = new JPanel(new BorderLayout());
        if (hasLoginPane) {
            browserPanel.setBorder(
                    BorderFactory.createTitledBorder(" Database Browser "));
            final JButton updateButton = new JButton("Fetch Metadata");
            browserPanel.add(updateButton, BorderLayout.NORTH);
            final DBTreeBrowser browser = new DBTreeBrowser(m_statmnt);
            browserPanel.add(browser, BorderLayout.CENTER);
            // init database update button
            updateButton.addActionListener(new ActionListener() {
                /** {@inheritDoc} */
                @Override
                public void actionPerformed(final ActionEvent ae) {
                    updateButton.setEnabled(false);
                    updateButton.setText("Fetching...");
                    try {
                        final DatabaseConnectionSettings settings = 
                                m_loginPane.getConnectionSettings();
                        final DatabaseReaderConnection conn = 
                                new DatabaseReaderConnection(
                                    new DatabaseQueryConnectionSettings(
                                        settings, ""));
                        browser.update((DatabaseMetaData) null);
                        m_worker = new SwingWorker<Void, Void>() {
                             /** {@inheritDoc} */
                             @Override
                             protected Void doInBackground() throws Exception {
                                 try {
                                     final DatabaseMetaData meta = 
                                          conn.getDatabaseMetaData(
                                             getCredentialsProvider());
                                     browser.update(meta);
                                 } catch (Exception e) {
                                     LOGGER.warn("Error during fetching "
                                         + "metadata from database, reason: " 
                                         + e.getMessage());
                                 }
                                 return null;
                             }
                             /** {@inheritDoc} */
                             @Override
                             protected void done() {
                                 updateButton.setText("Fetch Metadata");
                                 updateButton.setEnabled(true);
                             }
                         };
                         m_worker.execute();
                    } catch (final Exception e) {
                        browser.update((DatabaseMetaData) null);
                        updateButton.setText("Fetch Metadata");
                        updateButton.setEnabled(true);
                        LOGGER.warn("Error during fetching metadata "
                            + "from database, reason: " + e.getMessage(), e);
                    }
                }
            });
        }
        
        final JScrollPane scrollPane = new JScrollPane(m_statmnt,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory
                .createTitledBorder(" SQL Statement "));

        JPanel configurePanel = new JPanel(new BorderLayout());
        configurePanel.add(scrollPane, BorderLayout.CENTER);
        if (runWithoutConfigure()) {
            m_configureBox.setToolTipText(
                    "Returns an empty spec during configure.");
            configurePanel.add(m_configureBox, BorderLayout.SOUTH);
        }
        JPanel allPanel = new JPanel(new BorderLayout());

        m_hasLoginPane = hasLoginPane;
        if (hasLoginPane) {
            allPanel.add(m_loginPane, BorderLayout.NORTH);
        }

// init variable list
        m_listModelVars = new DefaultListModel();
        m_listVars = new JList(m_listModelVars);
        m_listVars.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_listVars.setCellRenderer(new FlowVariableListCellRenderer());
        m_listVars.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public final void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Object o = m_listVars.getSelectedValue();
                    if (o != null) {
                        FlowVariable var = (FlowVariable) o;
                        m_statmnt.replaceSelection(
                            DBVariableSupportNodeModel.Resolver.wrap(var));
                        m_listVars.clearSelection();
                        m_statmnt.requestFocus();
                    }
                }
            }
        });
        final JScrollPane scrollVars = new JScrollPane(m_listVars,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollVars.setBorder(BorderFactory.createTitledBorder(
            " Flow Variable List "));
        
        final JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        jsp.setResizeWeight(0.4);
        if (hasLoginPane) {
            final JSplitPane browserTables = new JSplitPane(
                    JSplitPane.VERTICAL_SPLIT);
            browserTables.setResizeWeight(0.5);
            browserTables.setTopComponent(browserPanel);
            browserTables.setBottomComponent(scrollVars);
            jsp.setLeftComponent(browserTables);
        } else {
            jsp.setLeftComponent(scrollVars);
        }
        jsp.setRightComponent(configurePanel);
        allPanel.add(jsp, BorderLayout.CENTER);
        super.addTab("Settings", allPanel);
    }
    
    /**
     * @return false (default), or true if the option to run the SQL query
     *         without configure should be visible.
     */
    protected boolean runWithoutConfigure() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        if (m_hasLoginPane) {
            Collection<String> creds = super.getCredentialsNames();
            m_loginPane.loadSettingsFrom(settings, specs, creds);
        }
        // statement
        String statement =
            settings.getString(DatabaseConnectionSettings.CFG_STATEMENT, null);
        m_statmnt.setText(statement == null
                ? "SELECT * FROM "
                        + DatabaseQueryConnectionSettings.TABLE_PLACEHOLDER
                : statement);
        // update list of flow/workflow variables
        m_listModelVars.removeAllElements();
        for (Map.Entry<String, FlowVariable> e
                : getAvailableFlowVariables().entrySet()) {
            m_listModelVars.addElement(e.getValue());
        }
        // read execute without configure checkbox
        if (runWithoutConfigure()) {
            m_configureBox.setSelected(
                settings.getBoolean(EXECUTE_WITHOUT_CONFIGURE, false));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        if (m_hasLoginPane) {
            m_loginPane.saveSettingsTo(settings);
        }
        settings.addString(DatabaseConnectionSettings.CFG_STATEMENT,
                m_statmnt.getText().trim());
        if (runWithoutConfigure()) {
            settings.addBoolean(EXECUTE_WITHOUT_CONFIGURE,
                m_configureBox.isSelected());
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void onClose() {
        super.onClose();
        if (m_worker != null && !m_worker.isDone()) {
            m_worker.cancel(true);
        }
    }
}
