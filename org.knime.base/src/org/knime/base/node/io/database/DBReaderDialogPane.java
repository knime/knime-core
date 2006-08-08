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
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.util.SimpleFileFilter;


/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DBReaderDialogPane extends NodeDialogPane {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DBReaderDialogPane.class);

    private final JComboBox m_driver = new JComboBox();

    private final JButton m_load = new JButton("Load");

    private final JEditorPane m_statmnt = new JEditorPane("text", "");

    private final JTextField m_db = new JTextField();

    private final JTextField m_user = new JTextField("");

    private final JPasswordField m_pass = new JPasswordField();

    private final JFileChooser m_chooser = new JFileChooser();

    private final HashSet<String> m_driverLoaded = new HashSet<String>();

    /**
     * Creates new dialog.
     */
    DBReaderDialogPane() {
        super();
        m_chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        m_chooser.setAcceptAllFileFilterUsed(false);
        m_chooser
                .setFileFilter(new SimpleFileFilter(DBDriverLoader.EXTENSIONS));
        Font font = new Font("Courier", Font.PLAIN, 12);
        JPanel parentPanel = new JPanel(new GridLayout(4, 1));
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
        parentPanel.add(driverPanel);
        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dbPanel.setBorder(BorderFactory.createTitledBorder(" Database Name "));
        m_db.setEditable(true);
        m_db.setFont(font);
        m_db.setPreferredSize(new Dimension(400, 20));
        dbPanel.add(m_db);
        parentPanel.add(dbPanel);
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userPanel.setBorder(BorderFactory.createTitledBorder(" User Name "));
        m_user.setPreferredSize(new Dimension(400, 20));
        m_user.setFont(font);
        userPanel.add(m_user);
        parentPanel.add(userPanel);
        JPanel passPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        passPanel.setBorder(BorderFactory.createTitledBorder(" Password "));
        m_pass.setPreferredSize(new Dimension(400, 20));
        m_pass.setFont(font);
        passPanel.add(m_pass);
        parentPanel.add(passPanel);
        JPanel allPanel = new JPanel(new BorderLayout());
        allPanel.add(parentPanel, BorderLayout.NORTH);
        m_statmnt.setFont(font);
        m_statmnt.setText("SELECT * FROM");
        JScrollPane scrollPane = new JScrollPane(m_statmnt,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory
                .createTitledBorder(" SQL Statement "));
        scrollPane.setPreferredSize(new Dimension(400, 100));
        allPanel.add(scrollPane, BorderLayout.CENTER);
        super.addTab("Settings", allPanel);
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
        // statement
        m_statmnt.setText(settings.getString("statement", ""));
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
    }

    private void updateDriver() {
        m_driver.removeAllItems();
        HashSet<String> driverHash = new HashSet<String>();
        Enumeration<Driver> driver = DriverManager.getDrivers();
        while (driver.hasMoreElements()) {
            Driver d = driver.nextElement();
            if (d instanceof WrappedDriver) {
                String driverName = d.toString();
                if (driverHash.add(driverName)) {
                    m_driver.addItem(driverName);
                }
            }
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
        try {
            settings.addString("password", DBReaderConnection.encrypt(m_pass
                    .getPassword()));
        } catch (Exception e) {
            LOGGER.warn("Could not encrypt password.");
        }
        settings.addString("statement", m_statmnt.getText().trim());
        // save loaded driver
        settings.addStringArray("loaded_driver", m_driverLoaded
                .toArray(new String[0]));
    }
}
