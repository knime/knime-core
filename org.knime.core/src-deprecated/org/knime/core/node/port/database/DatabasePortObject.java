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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.port.database;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectView;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.port.database.reader.DBReader;
import org.knime.core.node.workflow.BufferedDataTableView;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.util.SwingWorkerWithContext;

/**
 * Class used as database port object holding a {@link BufferedDataTable}
 * and a <code>ModelContentRO</code> to create a database connection.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
@Deprecated
public class DatabasePortObject extends DatabaseConnectionPortObject {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            DatabasePortObject.class);

    /**
     * Database port type formed <code>PortObjectSpec.class</code> and
     * <code>PortObject.class</code> from this class.
     */
    @SuppressWarnings("hiding")
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(DatabasePortObject.class);

    /**
     * Optional database port type formed <code>PortObjectSpec.class</code> and
     * <code>PortObject.class</code> from this class.
     * @since 2.12
     */
    @SuppressWarnings("hiding")
    public static final PortType TYPE_OPTIONAL =
        PortTypeRegistry.getInstance().getPortType(DatabasePortObject.class, true);

    /** {@inheritDoc} */
    @Override
    public DatabasePortObjectSpec getSpec() {
        return (DatabasePortObjectSpec)m_spec;
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        StringBuilder buf = new StringBuilder();
        buf.append("No. of columns: ").append(((DatabasePortObjectSpec) m_spec).getDataTableSpec().getNumColumns());
        final String dbId = m_spec.getDatabaseIdentifier();
        if (dbId != null) {
            buf.append(" DB: ").append(dbId);
        }
        return buf.toString();
    }

    /**
     * Creates a new database port object.
     * @param spec database port object spec
     * @throws NullPointerException if one of the arguments is null
     */
    public DatabasePortObject(final DatabasePortObjectSpec spec) {
        super(spec);
    }

    /**
     * @return underlying data
     */
    private DataTable getDataTable(final int cacheNoRows) {
        try {
            DatabaseQueryConnectionSettings connSettings = getConnectionSettings(m_credentials);
            DBReader load = connSettings.getUtility().getReader(connSettings);
            return load.getTable(new ExecutionMonitor(), m_credentials, false, cacheNoRows);
        } catch (Throwable t) {
            LOGGER.error("Could not fetch data from database, reason: "
                    + t.getMessage(), t);
            return null;
        }
    }

    /**
     * @return connection model
     *
     * @deprecated use {@link #getConnectionSettings(CredentialsProvider)} instead
     */
    @Deprecated
    public ModelContentRO getConnectionModel() {
        return m_spec.getConnectionModel();
    }


    /**
     * {@inheritDoc}
     * @since 2.10
     */
    @Override
    public DatabaseQueryConnectionSettings getConnectionSettings(final CredentialsProvider credProvider)
        throws InvalidSettingsException {
        return ((DatabasePortObjectSpec) m_spec).getConnectionSettings(credProvider);
    }

    /**
     * Serializer used to save <code>DatabasePortObject</code>.
     *
     * @noreference This class is not intended to be referenced by clients.
     * @since 3.0
     */
    public static final class Serializer extends PortObjectSerializer<DatabasePortObject> {
        /** {@inheritDoc} */
        @Override
        public void savePortObject(final DatabasePortObject portObject,
                final PortObjectZipOutputStream out,
                final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
            // nothing to save
        }

        /** {@inheritDoc} */
        @Override
        public DatabasePortObject loadPortObject(
                final PortObjectZipInputStream in,
                final PortObjectSpec spec,
                final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
            return new DatabasePortObject((DatabasePortObjectSpec) spec);
        }
    }

    /** Credentials to connect to the database while previewing the data. */
    private CredentialsProvider m_credentials;

    /**
     * Override this panel in order to set the CredentialsProvider
     * into this class.
     */
    @SuppressWarnings("serial")
    public final class DatabaseOutPortPanel extends JPanel implements PortObjectView {
        /**
         * Create new database provider.
         * @param lm using this layout manager
         */
        public DatabaseOutPortPanel(final LayoutManager lm) {
            super(lm);
        }

        @Override
        public void setCredentialsProvider(final CredentialsProvider cp) {
            m_credentials = cp;
        }

        @Override
        public void dispose() {
            m_credentials = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public JComponent[] getViews() {
        JComponent[] superViews = super.getViews();

        final JComponent[] panels = new JComponent[superViews.length + 1];
        @SuppressWarnings("serial")
        final BufferedDataTableView dataView = new BufferedDataTableView(null) {
            @Override
            public String getName() {
                return "Table Preview";
            }
        };
        final JButton b = new JButton("Cache no. of rows: ");
        final JPanel p = new JPanel(new FlowLayout());
        final JTextField cacheRows = new JTextField("100");
        cacheRows.setMinimumSize(new Dimension(50, 20));
        cacheRows.setPreferredSize(new Dimension(50, 20));
        p.add(b);
        p.add(cacheRows);
        panels[0] = new DatabaseOutPortPanel(new BorderLayout());
        panels[0].setName(dataView.getName());
        panels[0].add(p, BorderLayout.NORTH);
        panels[0].add(dataView, BorderLayout.CENTER);
        //store the NodeContext to explicitly set the NodeContext when the fetch rows button is pressed
        final NodeContext nodeContext = NodeContext.getContext();
        b.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                //explicitly set the NodeContext to get the current workflow user that is used in Kerberos secured
                //db connection
                NodeContext.pushContext(nodeContext);
                try{
                    loadTablePreview(panels, p, cacheRows);
                } finally {
                    //remove the previously set NodeContext
                    NodeContext.removeLastContext();
                }
            }
        });
        for (int i = 1; i < panels.length; i++) {
            panels[i] = superViews[i - 1];
        }
        return panels;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DatabasePortObject)) {
            return false;
        }
        return super.equals(obj);
    }

    private void loadTablePreview(final JComponent[] panels, final JPanel p, final JTextField cacheRows) {
        final AtomicInteger value = new AtomicInteger(100);
        try {
            int v = Integer.parseInt(cacheRows.getText().trim());
            value.set(v);
        } catch (NumberFormatException nfe) {
            cacheRows.setText(Integer.toString(value.get()));
        }
        panels[0].removeAll();
        panels[0].add(new JLabel("Fetching " + value.get()
                + " rows from database..."), BorderLayout.NORTH);
        panels[0].repaint();
        panels[0].revalidate();
        new SwingWorkerWithContext<DataTable, Void>() {
            /** {@inheritDoc} */
            @Override
            protected DataTable doInBackgroundWithContext() throws Exception {
                return getDataTable(value.get());
            }
            /** {@inheritDoc} */
            @Override
            protected void doneWithContext() {
                DataTable dt = null;
                try {
                    dt = super.get();
                } catch (ExecutionException ee) {
                    LOGGER.warn("Error during fetching data from "
                        + "database, reason: " + ee.getMessage(), ee);
                } catch (InterruptedException ie) {
                    LOGGER.warn("Error during fetching data from "
                        + "database, reason: " + ie.getMessage(), ie);
                }
                @SuppressWarnings("serial")
                final BufferedDataTableView dataView2 = new BufferedDataTableView(dt) {
                    /** {@inheritDoc} */
                    @Override
                    public String getName() {
                        return "Table Preview";
                    }
                };
                dataView2.setName("Table Preview");
                panels[0].removeAll();
                panels[0].add(p, BorderLayout.NORTH);
                panels[0].add(dataView2, BorderLayout.CENTER);
                panels[0].setName(dataView2.getName());
                panels[0].repaint();
                panels[0].revalidate();
            }
        }.execute();
    }
}
