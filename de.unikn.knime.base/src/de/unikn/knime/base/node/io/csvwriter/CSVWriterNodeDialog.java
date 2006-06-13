/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.base.node.io.csvwriter;

import java.awt.Dimension;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeSettings;

/**
 * Dialog to choose a file for csv output.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class CSVWriterNodeDialog extends NodeDialogPane {
    
    /** textfield to enter file name. */
    private final CSVFilesHistoryPanel m_textBox;
    /** Checkbox for writing column header. */
    private final JCheckBox m_colHeaderChecker;
    /** Checkbox for writing column header. */
    private final JCheckBox m_rowHeaderChecker;
    /** text field for missing pattern. */
    private final JTextField m_missingField;
    

    /**
     * Creates a new CSV writer dialog.
     */
    public CSVWriterNodeDialog() {
        super("CSV File Writer");
        m_textBox = new CSVFilesHistoryPanel();
        m_colHeaderChecker = new JCheckBox("Write column header");
        m_rowHeaderChecker = new JCheckBox("Write row header");
        m_missingField = new JTextField(3);
        m_missingField.setMaximumSize(new Dimension(40, 20));
        m_missingField.setToolTipText(
                "Pattern for missing values. If unsure, simply leave empty");
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        final JPanel missingPanel = new JPanel();
        missingPanel.setLayout(new BoxLayout(missingPanel, BoxLayout.X_AXIS));
        missingPanel.add(m_missingField);
        missingPanel.add(new JLabel(" Missing Pattern"));
        missingPanel.add(Box.createHorizontalGlue());
        
        final JPanel colHeaderPane = new JPanel();
        colHeaderPane.setLayout(new BoxLayout(colHeaderPane, BoxLayout.X_AXIS));
        colHeaderPane.add(m_colHeaderChecker);
        colHeaderPane.add(Box.createHorizontalGlue());
        final JPanel rowHeaderPane = new JPanel();
        rowHeaderPane.setLayout(new BoxLayout(rowHeaderPane, BoxLayout.X_AXIS));
        rowHeaderPane.add(m_rowHeaderChecker);
        rowHeaderPane.add(Box.createHorizontalGlue());
        
        panel.add(m_textBox);
        panel.add(Box.createVerticalStrut(5));
        panel.add(missingPanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(colHeaderPane);
        panel.add(Box.createVerticalStrut(5));
        panel.add(rowHeaderPane);
        panel.add(Box.createVerticalStrut(5));
        panel.add(Box.createVerticalGlue());
        addTab("File Chooser", panel);
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettings, DataTableSpec[])
     */
    protected void loadSettingsFrom(
            final NodeSettings settings, final DataTableSpec[] specs) throws InvalidSettingsException {
        String fileName = 
            settings.getString(CSVWriterNodeModel.CFGKEY_FILE, null);
        boolean writeColHeader = 
            settings.getBoolean(CSVWriterNodeModel.CFGKEY_COLHEADER, true);
        boolean writeRowHeader = 
            settings.getBoolean(CSVWriterNodeModel.CFGKEY_ROWHEADER, true);
        String missing = settings.getString(
                CSVWriterNodeModel.CFGKEY_MISSING, "");
        m_textBox.updateHistory();
        m_textBox.setSelectedFile(fileName);
        m_missingField.setText(missing);
        m_colHeaderChecker.setSelected(writeColHeader);
        m_rowHeaderChecker.setSelected(writeRowHeader);
    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettings)
     */
    protected void saveSettingsTo(final NodeSettings settings)
            throws InvalidSettingsException {
        String fileName = m_textBox.getSelectedFile();
        if (!fileName.equals("")) {
            File file = CSVFilesHistoryPanel.getFile(fileName);
            settings.addString(
                    CSVWriterNodeModel.CFGKEY_FILE, file.getAbsolutePath());
        }
        boolean writeColHeader = m_colHeaderChecker.isSelected();
        boolean writeRowHeader = m_rowHeaderChecker.isSelected();
        String missing = m_missingField.getText();
        settings.addString(CSVWriterNodeModel.CFGKEY_MISSING, missing);
        settings.addBoolean(
                CSVWriterNodeModel.CFGKEY_COLHEADER, writeColHeader);
        settings.addBoolean(
                CSVWriterNodeModel.CFGKEY_ROWHEADER, writeRowHeader);
    }
    
}
