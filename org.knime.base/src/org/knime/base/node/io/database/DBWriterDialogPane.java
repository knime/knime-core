/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   16.11.2005 (gabriel): created
 */
package org.knime.base.node.io.database;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.util.KnimeEncryption;
import org.knime.core.util.SimpleFileFilter;

/**
 * Dialog pane of the database writer.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DBWriterDialogPane extends NodeDialogPane {
    
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DBWriterDialogPane.class);

    private final JComboBox m_driver = new JComboBox();

    private final JButton m_load = new JButton("Load");

    private final JTextField m_db = new JTextField();

    private final JTextField m_user = new JTextField("");
    
    private final JTextField m_table = new JTextField("<table_name>");

    private final JPasswordField m_pass = new JPasswordField();

    private JFileChooser m_chooser = null;

    private final DBSQLTypesPanel m_typePanel;
    
    private boolean m_passwordChanged = false;
    
    /**
     * Creates new dialog.
     */
    DBWriterDialogPane() {
        super();
        Font font = new Font("Monospaced", Font.PLAIN, 12);
        m_driver.setEditable(false);
        m_driver.setFont(font);
        m_driver.setPreferredSize(new Dimension(320, 20));
        m_load.setPreferredSize(new Dimension(75, 20));
        m_load.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                JFileChooser chooser = createFileChooser();
                int ret = chooser.showOpenDialog(getPanel());
                if (ret == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    try {
                        DBDriverLoader.loadDriver(file);
                        updateDriver();
                    } catch (Exception exc) {
                        LOGGER.warn("No driver loaded from: " + file, exc);
                    }
                }
            }
        });
        JPanel driverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        driverPanel.setBorder(BorderFactory
                .createTitledBorder(" Database Driver "));
        driverPanel.add(m_driver, BorderLayout.CENTER);
        driverPanel.add(m_load, BorderLayout.EAST);
        
        JPanel settPanel = new JPanel(new GridLayout(5, 1));
        settPanel.add(driverPanel);
        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dbPanel.setBorder(BorderFactory.createTitledBorder(" Database Name "));
        m_db.setEditable(true);
        m_db.setFont(font);
        m_db.setPreferredSize(new Dimension(400, 20));
        dbPanel.add(m_db);
        settPanel.add(dbPanel);
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userPanel.setBorder(BorderFactory.createTitledBorder(" User Name "));
        m_user.setPreferredSize(new Dimension(400, 20));
        m_user.setFont(font);
        userPanel.add(m_user);
        settPanel.add(userPanel);
        JPanel passPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        passPanel.setBorder(BorderFactory.createTitledBorder(" Password "));
        m_pass.setPreferredSize(new Dimension(400, 20));
        m_pass.setFont(font);
        m_pass.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(final DocumentEvent e) {
                m_passwordChanged = true;
            }
            public void insertUpdate(final DocumentEvent e) {
                m_passwordChanged = true;
            }
            public void removeUpdate(final DocumentEvent e) {
                m_passwordChanged = true;
            }
        });

        passPanel.add(m_pass);
        settPanel.add(passPanel);
        JPanel tablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tablePanel.setBorder(BorderFactory.createTitledBorder(" Table Name "));
        m_table.setPreferredSize(new Dimension(400, 20));
        m_table.setFont(font);
        tablePanel.add(m_table);
        settPanel.add(tablePanel);
        super.addTab("Settings", settPanel);
        
        // add SQL type panel
        m_typePanel = new DBSQLTypesPanel();
        JScrollPane scroll = new JScrollPane(m_typePanel);
        scroll.setPreferredSize(settPanel.getPreferredSize());
        super.addTab("SQL Types", scroll);
    }

    private JFileChooser createFileChooser() {
        if (m_chooser == null) {
            m_chooser = new JFileChooser();
            m_chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            m_chooser.setAcceptAllFileFilterUsed(false);
            m_chooser.setFileFilter(
                    new SimpleFileFilter(DBDriverLoader.EXTENSIONS));
        }
        return m_chooser;
    }
    
    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // database driver and name
        m_driver.removeAllItems();
        m_db.setText(settings.getString("database", ""));
        // user
        m_user.setText(settings.getString("user", ""));
        // password
        m_pass.setText(settings.getString("password", ""));
        m_passwordChanged = false;
        // loaded driver: need to load settings before 1.2
        String[] driverLoaded = settings.getStringArray("loaded_driver", 
                new String[0]);
        for (String driver : driverLoaded) {
            try {
                DBDriverLoader.loadDriver(new File(driver));
            } catch (Exception e) {
                LOGGER.warn("Could not load driver from: " + driver, e);
            }
        }
        updateDriver();
        m_driver.setSelectedItem(settings.getString("driver", ""));
        // table name
        m_table.setText(settings.getString("table", ""));
        
        // load sql type for each column
        try {
            NodeSettingsRO typeSett = settings.getNodeSettings(
                    DBWriterNodeModel.CFG_SQL_TYPES);
            m_typePanel.loadSettingsFrom(typeSett, specs);
        } catch (InvalidSettingsException ise) {
            m_typePanel.loadSettingsFrom(null, specs);
        }
    }

    private void updateDriver() {
        m_driver.removeAllItems();
        Set<String> driverNames = DBDriverLoader.getLoadedDriver();
        for (String driverName : driverNames) {
            m_driver.addItem(driverName);
        }
    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        String driverName = m_driver.getSelectedItem().toString();
        settings.addString("driver", driverName);
        settings.addString("database", m_db.getText().trim());
        settings.addString("user", m_user.getText().trim());
        settings.addString("table", m_table.getText().trim());
        if (m_passwordChanged) {
            try {
                settings.addString("password", 
                        KnimeEncryption.encrypt(m_pass.getPassword()));
            } catch (Exception e) {
                LOGGER.warn("Could not encrypt password.", e);
                throw new InvalidSettingsException(
                        "Could not encrypt password.");
            }
        } else {
            settings.addString("password", new String(m_pass.getPassword())); 
        }
        // save sql type for each column
        NodeSettingsWO typeSett = settings.addNodeSettings(
                DBWriterNodeModel.CFG_SQL_TYPES);
        m_typePanel.saveSettingsTo(typeSett);
    }
}
