/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   13.02.2008 (gabriel): created
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
import javax.swing.SwingWorker;

import org.knime.core.data.DataTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.BufferedDataTableView;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Class used as database port object holding a {@link BufferedDataTable}
 * and a <code>ModelContentRO</code> to create a database connection.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabasePortObject implements PortObject {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            DatabasePortObject.class);

    private final DatabasePortObjectSpec m_spec;

    /**
     * Database port type formed <code>PortObjectSpec.class</code> and
     * <code>PortObject.class</code> from this class.
     */
    public static final PortType TYPE = new PortType(DatabasePortObject.class);

    /** {@inheritDoc} */
    @Override
    public DatabasePortObjectSpec getSpec() {
        return m_spec;
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return "No. of columns: " + m_spec.getDataTableSpec().getNumColumns();
    }

    /**
     * Creates a new database port object.
     * @param spec database port object spec
     * @throws NullPointerException if one of the arguments is null
     */
    public DatabasePortObject(final DatabasePortObjectSpec spec) {
      if (spec == null) {
            throw new NullPointerException(
                    "DatabasePortObjectSpec must not be null!");
        }
        m_spec = spec;
    }

    /**
     * @return underlying data
     */
    private DataTable getDataTable(final int cacheNoRows) {
        try {
            DatabaseReaderConnection load = new DatabaseReaderConnection(
                new DatabaseQueryConnectionSettings(
                       m_spec.getConnectionModel(), m_credentials));
            return load.createTable(cacheNoRows, m_credentials);
        } catch (Throwable t) {
            LOGGER.error("Could not fetch data from database, reason: "
                    + t.getMessage(), t);
            return null;
        }
    }

    /**
     * @return connection model
     */
    public ModelContentRO getConnectionModel() {
        return m_spec.getConnectionModel();
    }

    /**
     * Serializer used to save <code>DatabasePortObject</code>.
     * @return a new database port object serializer
     */
    public static PortObjectSerializer<DatabasePortObject>
            getPortObjectSerializer() {
        return new PortObjectSerializer<DatabasePortObject>() {
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
        };
    }

    /** Credentials to connect to the database while previewing the data. */
    private CredentialsProvider m_credentials;

    /**
     * Override this panel in order to set the CredentialsProvider
     * into this class.
     */
    @SuppressWarnings("serial")
    public final class DatabaseOutPortPanel extends JPanel {
        /**
         * Create new database provider.
         * @param lm using this layout manager
         */
        public DatabaseOutPortPanel(final LayoutManager lm) {
            super(lm);
        }
        /**
         * Set provider.
         * @param cp {@link CredentialsProvider}
         */
        public void setCredentialsProvider(final CredentialsProvider cp) {
            m_credentials = cp;
        }
    }

    /** {@inheritDoc} */
    @Override
    public JComponent[] getViews() {
        JComponent[] specPanels = m_spec.getViews();
        final JComponent[] panels = new JComponent[specPanels.length + 1];
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
        b.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
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
                new SwingWorker<DataTable, Void>() {
                    /** {@inheritDoc} */
                    @Override
                    protected DataTable doInBackground() throws Exception {
                        return getDataTable(value.get());
                    }
                    /** {@inheritDoc} */
                    @Override
                    protected void done() {
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
        });
        for (int i = 1; i < panels.length; i++) {
            panels[i] = specPanels[i - 1];
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
        DatabasePortObject dbPort = (DatabasePortObject) obj;
        return m_spec.equals(dbPort.m_spec);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_spec.hashCode();
    }

}
