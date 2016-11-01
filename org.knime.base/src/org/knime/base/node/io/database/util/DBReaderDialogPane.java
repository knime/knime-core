/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
package org.knime.base.node.io.database.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.DatabaseMetaData;
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
import javax.swing.text.JTextComponent;

import org.knime.base.node.io.database.DBTreeBrowser;
import org.knime.base.util.flowvariable.FlowVariableResolver;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionPortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.port.database.reader.DBReader;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.SwingWorkerWithContext;

/**
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class DBReaderDialogPane extends NodeDialogPane {

    private boolean m_showConnectionPanel;

    private final DBDialogPane m_connectionPane = new DBDialogPane(false);

    private DatabaseConnectionSettings m_upstreamConnectionSettings;

    private final JEditorPane m_statmnt = new JEditorPane("text", "");

    private final DefaultListModel<FlowVariable> m_listModelVars;
    private final JList<FlowVariable> m_listVars;

    private final JCheckBox m_configureBox = new JCheckBox(
            "Run SQL query only during execute, skips configure");

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DBReaderDialogPane.class);

    private SwingWorker<Void, Void> m_worker;

    /**
     * Creates a new dialog.
     *
     * @param showConnectionPanel <code>true</code>, if a panel for defining the connection should be shown,
     *            <code>false</code> otherwise
     * @param showDBBrowser <code>true</code> if the database browser should be shown, <code>false</code> otherwise
     */
    public DBReaderDialogPane(final boolean showConnectionPanel, final boolean showDBBrowser) {
// init SQL statement component
        m_statmnt.setPreferredSize(new Dimension(350, 200));
        m_statmnt.setFont(DBDialogPane.FONT);
        m_statmnt.setText("SELECT * FROM "
                + DatabaseQueryConnectionSettings.TABLE_PLACEHOLDER);

// init database table browser
        final JPanel browserPanel = new JPanel(new BorderLayout());
        if (showDBBrowser) {
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
                        final DatabaseConnectionSettings settings = getConnectionSettings();
                        final DBReader conn = settings.getUtility().getReader(
                                    new DatabaseQueryConnectionSettings(settings, ""));
                        browser.update((DatabaseMetaData) null);
                        m_worker = new SwingWorkerWithContext<Void, Void>() {
                             /** {@inheritDoc} */
                             @Override
                             protected Void doInBackgroundWithContext() throws Exception {
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
                             protected void doneWithContext() {
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

        allPanel.add(m_connectionPane, BorderLayout.NORTH);
        showConnectionPanel(showConnectionPanel);

// init variable list
        m_listModelVars = new DefaultListModel<FlowVariable>();
        m_listVars = new JList<FlowVariable>(m_listModelVars);
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
                        m_statmnt.replaceSelection(FlowVariableResolver.getPlaceHolderForVariable(var));
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
        if (showDBBrowser) {
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
        super.addTab("Settings", allPanel, false);
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
        if (m_showConnectionPanel) {
            m_connectionPane.loadSettingsFrom(settings, specs, getCredentialsProvider());
        }

        DatabaseQueryConnectionSettings s = new DatabaseQueryConnectionSettings();
        try {
            s.loadValidatedConnection(settings, getCredentialsProvider());
        } catch (InvalidSettingsException ex) {
            // use settings as they are
        }

        // statement
        String statement = s.getQuery();
        m_statmnt.setText(statement == null
                ? "SELECT * FROM " + DatabaseQueryConnectionSettings.TABLE_PLACEHOLDER
                : statement);
        // select the table placeholder statement for easier replacements
        selectPlaceHolder(m_statmnt, DatabaseQueryConnectionSettings.TABLE_PLACEHOLDER);
        // update list of flow/workflow variables
        m_listModelVars.removeAllElements();
        for (Map.Entry<String, FlowVariable> e
                : getAvailableFlowVariables().entrySet()) {
            m_listModelVars.addElement(e.getValue());
        }
        // read execute without configure checkbox
        if (runWithoutConfigure()) {
            m_configureBox.setSelected(!s.getValidateQuery());
        }

        m_upstreamConnectionSettings = null;
        for (PortObjectSpec pos : specs) {
            if (pos instanceof DatabaseConnectionPortObjectSpec) {
                try {
                    m_upstreamConnectionSettings =
                        ((DatabaseConnectionPortObjectSpec)pos).getConnectionSettings(getCredentialsProvider());
                } catch (InvalidSettingsException ex) {
                    LOGGER.warn("Could not load database connection from upstream node: " + ex.getMessage(), ex);
                }
            }
        }
        if (m_upstreamConnectionSettings != null || !m_showConnectionPanel) {
            m_connectionPane.setVisible(false);
        } else {
            m_connectionPane.setVisible(true);
        }
    }

    /**
     * @param statmnt the {@link JTextComponent} to select the place holder in
     * @param placeHolder the place holder to select
     * @since 2.10
     */
    private static void selectPlaceHolder(final JTextComponent statmnt, final String placeHolder) {
        if (statmnt == null || placeHolder == null) {
            return;
        }
        final String text = statmnt.getText();
        final int startIdx = text.indexOf(placeHolder);
        if (startIdx < 0) {
            return;
        }
        statmnt.select(startIdx, startIdx + placeHolder.length());
    }

    private DatabaseConnectionSettings getConnectionSettings() {
        if (m_showConnectionPanel && m_upstreamConnectionSettings == null) {
            return m_connectionPane.getConnectionSettings();
        } else {
            return m_upstreamConnectionSettings;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        DatabaseQueryConnectionSettings s = new DatabaseQueryConnectionSettings();
        s.setQuery(m_statmnt.getText().trim());
        if (runWithoutConfigure()) {
            s.setValidateQuery(!m_configureBox.isSelected());
        }
        s.saveConnection(settings);

        if (m_showConnectionPanel) {
            m_connectionPane.saveSettingsTo(settings, getCredentialsProvider());
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

    /**
     * Shows or hides the connection information panel.
     *
     * @param b <code>true</code> if the connection info panel should be shown, <code>false</code> otherwise
     */
    public void showConnectionPanel(final boolean b) {
        m_showConnectionPanel = b;
        m_connectionPane.setVisible(b);
    }
}
