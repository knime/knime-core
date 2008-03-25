/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.io.database;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.PortObjectSpec;


/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class DBReaderDialogPane extends GenericNodeDialogPane {
    
    private final DBDialogPane m_loginPane = new DBDialogPane();
  
    private final JEditorPane m_statmnt = new JEditorPane("text", "");
    
    /**
     * Creates new dialog.
     */
    DBReaderDialogPane() {
        super();
        m_statmnt.setFont(DBDialogPane.FONT);
        m_statmnt.setText("SELECT * FROM " 
                + DBQueryConnection.TABLE_PLACEHOLDER);
        final JScrollPane scrollPane = new JScrollPane(m_statmnt,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory
                .createTitledBorder(" SQL statement "));
        JPanel allPanel = new JPanel(new BorderLayout());
        allPanel.add(m_loginPane, BorderLayout.NORTH);
        allPanel.add(scrollPane, BorderLayout.CENTER);
        super.addTab("Settings", allPanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_loginPane.loadSettingsFrom(settings, specs);
        // statement
        String statement = 
            settings.getString(DBConnection.CFG_STATEMENT, null); 
        m_statmnt.setText(statement == null 
                ? "SELECT * FROM " + DBQueryConnection.TABLE_PLACEHOLDER 
                : statement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_loginPane.saveSettingsTo(settings);
        settings.addString(DBConnection.CFG_STATEMENT, 
                m_statmnt.getText().trim());
    }
}
