/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   13.02.2008 (gabriel): created
 */
package org.knime.core.node.port.database;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

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
                       m_spec.getConnectionModel()));
            return load.createTable(cacheNoRows);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        JComponent[] specPanels = m_spec.getViews();
        final JComponent[] panels = new JComponent[specPanels.length + 1];
        final BufferedDataTableView dataView = new BufferedDataTableView(null) {
            @Override
            public String getName() {
                return "Table Preview";
            }            
        };
        JButton b = new JButton("Cache no. of rows: ");
        final JPanel p = new JPanel(new FlowLayout());
        final JTextField cacheRows = new JTextField("100");
        cacheRows.setMinimumSize(new Dimension(50, 20));
        cacheRows.setPreferredSize(new Dimension(50, 20));
        p.add(b);
        p.add(cacheRows);
        panels[0] = new JPanel(new BorderLayout());
        panels[0].setName(dataView.getName());
        panels[0].add(p, BorderLayout.NORTH);
        panels[0].add(dataView, BorderLayout.CENTER);
        b.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(final ActionEvent e) {
                panels[0].removeAll();
                int value = 100;
                try {
                    value = Integer.parseInt(cacheRows.getText().trim());
                } catch (NumberFormatException nfe) {
                    cacheRows.setText("100");
                }
                BufferedDataTableView dataView2 = new BufferedDataTableView(
                        getDataTable(value)) {
                    @Override
                    public String getName() {
                        return "Table Preview";
                    }            
                };
                dataView2.setName("Table Preview");
                panels[0].add(p, BorderLayout.NORTH);
                panels[0].add(dataView2, BorderLayout.CENTER);
                panels[0].setName(dataView2.getName());
                panels[0].repaint();
                panels[0].revalidate();
            }
        });
        for (int i = 1; i < panels.length; i++) {
            panels[i] = specPanels[i - 1];
        }
        return panels;
    }
    
}
