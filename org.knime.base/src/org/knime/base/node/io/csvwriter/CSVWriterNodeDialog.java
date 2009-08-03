/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
package org.knime.base.node.io.csvwriter;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.ScopeVariable;

/**
 * Dialog to choose a file for csv output.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class CSVWriterNodeDialog extends NodeDialogPane {

    /** textfield to enter file name. */
    private final CSVFilesHistoryPanel m_textBox;

    /** Checkbox for writing column header. */
    private final JCheckBox m_colHeaderChecker;

    /** Checkbox for writing column header even if file exits. */
    private final JCheckBox m_colHeaderWriteSkipOnAppend;

    /** Checkbox for writing column header. */
    private final JCheckBox m_rowHeaderChecker;

    /** Checkbox if append to output file (if exists). */
    private final JCheckBox m_appendChecker;

    private final QuotePanel m_quotePanel;

    private final AdvancedPanel m_advancedPanel;

    private final CommentPanel m_commentPanel;

    private final DecimalSeparatorPanel m_decSeparatorPanel;


    /**
     * Creates a new CSV writer dialog.
     */
    public CSVWriterNodeDialog() {
        super();

        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Output file location:"));
        m_textBox = new CSVFilesHistoryPanel(createScopeVariableModel(
              FileWriterNodeSettings.CFGKEY_FILE,
              ScopeVariable.Type.STRING));
//        m_textBox = new CSVFilesHistoryPanel();
        filePanel.add(m_textBox);
        filePanel.add(Box.createHorizontalGlue());

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Writer options:"));

        ItemListener l = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                checkCheckerState();
            }
        };
        m_colHeaderChecker = new JCheckBox("Write column header");
        m_colHeaderChecker.addItemListener(l);
        m_colHeaderWriteSkipOnAppend =
                new JCheckBox("Don't write column headers if file exists");
        m_rowHeaderChecker = new JCheckBox("Write row ID");
        m_appendChecker = new JCheckBox("Append to output file");
        m_appendChecker.addItemListener(l);

        final JPanel colHeaderPane = new JPanel();
        colHeaderPane.setLayout(new BoxLayout(colHeaderPane, BoxLayout.X_AXIS));
        colHeaderPane.add(m_colHeaderChecker);
        colHeaderPane.add(Box.createHorizontalGlue());
        final JPanel colHeaderPane2 = new JPanel();
        colHeaderPane2
                .setLayout(new BoxLayout(colHeaderPane2, BoxLayout.X_AXIS));
        colHeaderPane2.add(m_colHeaderWriteSkipOnAppend);
        colHeaderPane2.add(Box.createHorizontalGlue());
        final JPanel rowHeaderPane = new JPanel();
        rowHeaderPane.setLayout(new BoxLayout(rowHeaderPane, BoxLayout.X_AXIS));
        rowHeaderPane.add(m_rowHeaderChecker);
        rowHeaderPane.add(Box.createHorizontalGlue());
        final JPanel appendPane = new JPanel();
        appendPane.setLayout(new BoxLayout(appendPane, BoxLayout.X_AXIS));
        appendPane.add(m_appendChecker);
        appendPane.add(Box.createHorizontalGlue());

        optionsPanel.add(colHeaderPane);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(colHeaderPane2);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(rowHeaderPane);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(appendPane);
        optionsPanel.add(Box.createVerticalStrut(5));

        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(filePanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(optionsPanel);
        panel.add(Box.createVerticalGlue());

        addTab("Settings", panel);

        m_advancedPanel = new AdvancedPanel();
        addTab("Advanced", m_advancedPanel);

        m_quotePanel = new QuotePanel();
        addTab("Quotes", m_quotePanel);

        m_commentPanel = new CommentPanel();
        addTab("Comment Header", m_commentPanel);

        m_decSeparatorPanel = new DecimalSeparatorPanel();
        addTab("Decimal Separator", m_decSeparatorPanel);
    }

    /** Checks whether or not the "on file exists" check should be enabled. */
    private void checkCheckerState() {
        m_colHeaderWriteSkipOnAppend.setEnabled(m_colHeaderChecker.isSelected()
                && m_appendChecker.isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {

        FileWriterNodeSettings newValues;
        try {
            newValues = new FileWriterNodeSettings(settings);
        } catch (InvalidSettingsException ise) {
            // use default settings if we didn't get any useful ones
            newValues = new FileWriterNodeSettings();
        }

        m_textBox.updateHistory();
        m_textBox.setSelectedFile(newValues.getFileName());
        m_colHeaderChecker.setSelected(newValues.writeColumnHeader());
        m_colHeaderWriteSkipOnAppend.setSelected(newValues
                .skipColHeaderIfFileExists());
        m_rowHeaderChecker.setSelected(newValues.writeRowID());
        m_appendChecker.setSelected(newValues.appendToFile());
        checkCheckerState();

        m_quotePanel.loadValuesIntoPanel(newValues);
        m_advancedPanel.loadValuesIntoPanel(newValues);
        m_commentPanel.loadValuesIntoPanel(newValues);
        m_decSeparatorPanel.loadValuesIntoPanel(newValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {

        FileWriterNodeSettings values = new FileWriterNodeSettings();

        String fileName = m_textBox.getSelectedFile();
        if (!fileName.equals("")) {
            File file = CSVFilesHistoryPanel.getFile(fileName);
            fileName = file.getAbsolutePath();
        }
        values.setFileName(fileName);

        values.setWriteColumnHeader(m_colHeaderChecker.isSelected());
        values.setSkipColHeaderIfFileExists(m_colHeaderWriteSkipOnAppend
                .isSelected());
        values.setWriteRowID(m_rowHeaderChecker.isSelected());
        values.setAppendToFile(m_appendChecker.isSelected());

        m_quotePanel.saveValuesFromPanelInto(values);
        m_advancedPanel.saveValuesFromPanelInto(values);
        m_commentPanel.saveValuesFromPanelInto(values);
        m_decSeparatorPanel.saveValuesFromPanelInto(values);

        values.saveSettingsTo(settings);
    }
}
