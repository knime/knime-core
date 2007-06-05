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
import java.util.HashSet;
import java.util.Set;

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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.KnimeEncryption;
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

    private JFileChooser m_chooser = null;

    private boolean m_passwordChanged = false;
    
    /**
     * Keeps the history of all loaded driver and its order.
     */
    static final StringHistory DRIVER_ORDER = StringHistory.getInstance(
            "database_drivers");
    
    /**
     * Keeps the history of all URL used to create a Database connection.
     */
    static final StringHistory DRIVER_URLS = StringHistory.getInstance(
            "driver_urls");
    
    /**
     * Creates new dialog.
     */
    DBReaderDialogPane() {
        super();
        Font font = new Font("Monospaced", Font.PLAIN, 12);
        JPanel parentPanel = new JPanel(new GridLayout(4, 1));
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
                .createTitledBorder(" Database driver "));
        driverPanel.add(m_driver, BorderLayout.CENTER);
        driverPanel.add(m_load, BorderLayout.EAST);
        parentPanel.add(driverPanel);
        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dbPanel.setBorder(BorderFactory.createTitledBorder(
                " Database URL "));
        m_db.setEditable(true);
        m_db.setFont(font);
        m_db.setPreferredSize(new Dimension(400, 20));
        dbPanel.add(m_db);
        parentPanel.add(dbPanel);
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userPanel.setBorder(BorderFactory.createTitledBorder(" User name "));
        m_user.setPreferredSize(new Dimension(400, 20));
        m_user.setFont(font);
        userPanel.add(m_user);
        parentPanel.add(userPanel);
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
        String dbName = settings.getString("database", null);
        m_db.setText(dbName == null ? "jdbc:odbc:<database_name>" : dbName);
        // statement
        String statement = settings.getString("statement", null); 
        m_statmnt.setText(statement == null 
                ? "SELECT * FROM <table>" : statement);
        // user
        String user = settings.getString("user", null);
        m_user.setText(user == null ? "<user>" : user);
        // password
        String password = settings.getString("password", null);
        m_pass.setText(password == null ? "" : password);
        m_passwordChanged = false;
        // loaded driver: need to load settings before 1.2
        String[] loadedDriver = settings.getStringArray("loaded_driver",
                new String[0]);
        for (String driver : loadedDriver) {
            try {
                DBDriverLoader.loadDriver(new File(driver));
            } catch (Exception e) {
                LOGGER.warn("Could not load driver: " + driver, e);
            }
        }
        updateDriver();
        String select = settings.getString("driver", 
                m_driver.getSelectedItem().toString());
        m_driver.setSelectedItem(select);
    }

    private void updateDriver() {
        m_driver.removeAllItems();
        Set<String> driverNames = new HashSet<String>(
                DBDriverLoader.getLoadedDriver());
        for (String driverName : DRIVER_ORDER.getHistory()) {
            if (driverNames.contains(driverName)) {
                m_driver.addItem(driverName);
                driverNames.remove(driverName);
            }
        }
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
        String url = m_db.getText().trim();
        settings.addString("database", url);
        try {
            if (!DBDriverLoader.getWrappedDriver(driverName).acceptsURL(url)) {
                throw new InvalidSettingsException("Driver \"" + driverName 
                        + "\" does not accept URL: " + url);
            }
        } catch (Exception e) {
            InvalidSettingsException ise = new InvalidSettingsException(
                    "Couldn't test connection to URL \"" + url + "\" "
                            + " with driver: " + driverName);
            ise.initCause(e);
            throw ise;
        }        
        settings.addString("user", m_user.getText().trim());
        if (m_passwordChanged) {
            try {
                settings.addString("password", KnimeEncryption.encrypt(
                        m_pass.getPassword()));
            } catch (Exception e) {
                LOGGER.warn("Could not encrypt password.", e);
                throw new InvalidSettingsException(
                        "Could not encrypt password.");
            }
        } else {
            settings.addString("password", new String(m_pass.getPassword()));
        }
        settings.addString("statement", m_statmnt.getText().trim());
    }
}
