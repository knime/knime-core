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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.base.node.io.csvwriter.FileWriterNodeSettings.FileOverwritePolicy;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.FlowVariable;

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

    private final JRadioButton m_overwritePolicyAbortButton;
    
    private final JRadioButton m_overwritePolicyAppendButton;
    
    private final JRadioButton m_overwritePolicyOverwriteButton;

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
        m_textBox = new CSVFilesHistoryPanel(createFlowVariableModel(
              FileWriterNodeSettings.CFGKEY_FILE,
              FlowVariable.Type.STRING));
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
        ActionListener al = new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                checkCheckerState();
            }
        };
        m_colHeaderChecker = new JCheckBox("Write column header");
        m_colHeaderChecker.addItemListener(l);
        m_colHeaderWriteSkipOnAppend =
                new JCheckBox("Don't write column headers if file exists");
        m_rowHeaderChecker = new JCheckBox("Write row ID");
        
        ButtonGroup bg = new ButtonGroup();
        m_overwritePolicyAppendButton = new JRadioButton("Append");
        bg.add(m_overwritePolicyAppendButton);
        m_overwritePolicyAppendButton.addActionListener(al);
        m_overwritePolicyOverwriteButton = new JRadioButton("Overwrite");
        bg.add(m_overwritePolicyOverwriteButton);
        m_overwritePolicyOverwriteButton.addActionListener(al);
        m_overwritePolicyAbortButton = new JRadioButton("Abort");
        bg.add(m_overwritePolicyAbortButton);
        m_overwritePolicyAbortButton.addActionListener(al);
        m_overwritePolicyAbortButton.doClick();

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
        final JPanel overwriteFileLabelPane = new JPanel();
        overwriteFileLabelPane.setLayout(
                new BoxLayout(overwriteFileLabelPane, BoxLayout.X_AXIS));
        overwriteFileLabelPane.add(new JLabel(" If file exists... "));
        overwriteFileLabelPane.add(Box.createHorizontalGlue());
        final JPanel overwriteFilePane = new JPanel();
        overwriteFilePane.setLayout(
                new BoxLayout(overwriteFilePane, BoxLayout.X_AXIS));
        overwriteFilePane.add(m_overwritePolicyOverwriteButton);
        overwriteFilePane.add(Box.createGlue());
        overwriteFilePane.add(m_overwritePolicyAppendButton);
        overwriteFilePane.add(Box.createGlue());
        overwriteFilePane.add(m_overwritePolicyAbortButton);
        overwriteFilePane.add(Box.createHorizontalGlue());

        optionsPanel.add(colHeaderPane);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(colHeaderPane2);
        optionsPanel.add(Box.createVerticalStrut(5));
        optionsPanel.add(rowHeaderPane);
        optionsPanel.add(Box.createVerticalStrut(15));
        optionsPanel.add(overwriteFileLabelPane);
        optionsPanel.add(Box.createVerticalStrut(3));
        optionsPanel.add(overwriteFilePane);
        optionsPanel.add(Box.createVerticalGlue());

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
                && m_overwritePolicyAppendButton.isSelected());
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
        
        switch (newValues.getFileOverwritePolicy()) {
        case Append:
            m_overwritePolicyAppendButton.doClick();
            break;
        case Overwrite:
            m_overwritePolicyOverwriteButton.doClick();
            break;
        default: // Fail
            m_overwritePolicyAbortButton.doClick();
        }

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
        FileOverwritePolicy overwritePolicy;
        if (m_overwritePolicyAppendButton.isSelected()) {
            overwritePolicy = FileOverwritePolicy.Append;
        } else if (m_overwritePolicyOverwriteButton.isSelected()) {
            overwritePolicy = FileOverwritePolicy.Overwrite;
        } else {
            overwritePolicy = FileOverwritePolicy.Abort;
        }
        values.setFileOverwritePolicy(overwritePolicy);

        m_quotePanel.saveValuesFromPanelInto(values);
        m_advancedPanel.saveValuesFromPanelInto(values);
        m_commentPanel.saveValuesFromPanelInto(values);
        m_decSeparatorPanel.saveValuesFromPanelInto(values);

        values.saveSettingsTo(settings);
    }
}
