/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

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

    private final JFileChooser m_chooser = new JFileChooser();

    private final HashSet<String> m_driverLoaded = new HashSet<String>();

    private final DBSQLTypesPanel m_typePanel;
    
    /**
     * Creates new dialog.
     */
    DBWriterDialogPane() {
        super();
        m_chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        m_chooser.setAcceptAllFileFilterUsed(false);
        m_chooser
                .setFileFilter(new SimpleFileFilter(DBDriverLoader.EXTENSIONS));
        Font font = new Font("Courier", Font.PLAIN, 12);
        m_driver.setEditable(false);
        m_driver.setFont(font);
        m_driver.setPreferredSize(new Dimension(335, 20));
        m_load.setPreferredSize(new Dimension(60, 20));
        m_load.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                int ret = m_chooser.showOpenDialog(getPanel());
                if (ret == JFileChooser.APPROVE_OPTION) {
                    File file = m_chooser.getSelectedFile();
                    try {
                        DBDriverLoader.loadDriver(file);
                        updateDriver();
                        m_driverLoaded.add(file.getAbsolutePath());
                    } catch (Exception exc) {
                        LOGGER.warn("No driver loaded from: " + file);
                        LOGGER.debug("", exc);
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
        m_pass.setText("");
        // save loaded driver
        m_driverLoaded.clear();
        m_driverLoaded.addAll(Arrays.asList(settings.getStringArray(
                "loaded_driver", new String[0])));
        for (String loadedDriver : m_driverLoaded) {
            try {
                DBDriverLoader.loadDriver(new File(loadedDriver));
            } catch (Exception e) {
                LOGGER.info("Could not load driver from: " + loadedDriver);
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
        try {
            settings.addString("password", KnimeEncryption.encrypt(m_pass
                    .getPassword()));
        } catch (Exception e) {
            LOGGER.warn("Could not encrypt password.");
        }
        // save loaded driver
        settings.addStringArray("loaded_driver", m_driverLoaded
                .toArray(new String[0]));
        
        // save sql type for each column
        NodeSettingsWO typeSett = settings.addNodeSettings(
                DBWriterNodeModel.CFG_SQL_TYPES);
        m_typePanel.saveSettingsTo(typeSett);
    }
}
