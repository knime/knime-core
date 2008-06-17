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
 * History
 *   06.12.2005 (dill): created
 */
package org.knime.base.node.mine.subgroupminer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;


/**
 * The dialog for the BitvectorGeneratorNode. Simply provides a threshold above
 * all incoming values are presented with a bit set to one, items below that
 * threshold are presented as a bit set to zero.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class BitVectorGeneratorNodeDialog extends NodeDialogPane {
    private JSpinner m_threshold;

    private DialogComponentColumnNameSelection m_stringColumn;

    private JComboBox m_stringType;

    private JCheckBox m_useMean;

    private JSpinner m_meanPercentage;

    private JRadioButton m_numericRadio;

    private JRadioButton m_stringRadio;

    private boolean m_hasStringCol = false;

    private JCheckBox m_replaceBox;


    private static final int COMP_HEIGHT = 20;

    /**
     * Creates an instance of the BitVectorGeneratorNodeDialog, containing an
     * adjustable threshold. All values above or equal to that threshold are
     * represented by a bit set to 1.
     */
    @SuppressWarnings("unchecked")
    public BitVectorGeneratorNodeDialog() {
        super();

        m_stringType = new JComboBox(BitVectorGeneratorNodeModel.STRING_TYPES
                .values());
        m_stringType.setEnabled(false);
        m_useMean = new JCheckBox();
        m_useMean.setSelected(false);
        m_useMean.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent arg0) {
                m_meanPercentage.setEnabled(m_useMean.isSelected());
                m_threshold.setEnabled(!m_useMean.isSelected());
                if (m_useMean.isSelected()) {
                    m_threshold.setEnabled(false);
                } else {
                    m_threshold.setEnabled(true);
                }
            }
        });
        m_meanPercentage = new JSpinner(new SpinnerNumberModel(100, 0,
                Integer.MAX_VALUE, 10));
        m_meanPercentage.setEnabled(false);
        m_stringColumn = new DialogComponentColumnNameSelection(
                BitVectorGeneratorNodeModel.createStringColModel(),
                "String column to be parsed", 0, false,
                StringValue.class);
        m_stringColumn.getModel().setEnabled(false);
        m_threshold = new JSpinner(new SpinnerNumberModel(1.0, 0.0,
                Integer.MAX_VALUE, 0.1));

        ButtonGroup btnGroup = new ButtonGroup();

        // do here the layout and the composition
        // if the input is numeric
        m_numericRadio = new JRadioButton("Numeric input (many columns)");
        m_numericRadio.addChangeListener(new ChangeListener() {

            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent arg0) {
                if (m_numericRadio.isSelected()) {
                    // disable
                    m_stringColumn.getModel().setEnabled(false);
                    m_stringType.setEnabled(false);
                    // enable
                    m_threshold.setEnabled(!m_useMean.isSelected());
                    m_useMean.setEnabled(true);
                    m_meanPercentage.setEnabled(m_useMean.isSelected());
                }
            }

        });
        // m_numericRadio.setSelected(true);
        m_stringRadio = new JRadioButton(
                "Parse bitvectors from strings (one column)");
        m_stringRadio.addChangeListener(new ChangeListener() {

            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent arg0) {
                if (m_stringRadio.isSelected()) {
                    // disable
                    m_threshold.setEnabled(false);
                    m_useMean.setEnabled(false);
                    m_meanPercentage.setEnabled(false);
                    // enable
                    m_stringColumn.getModel().setEnabled(true);
                    m_stringType.setEnabled(true);
                }
            }

        });
        btnGroup.add(m_numericRadio);
        btnGroup.add(m_stringRadio);

        JPanel numericPanel = createNumericInputPanel();
        JPanel stringPanel = createStringInputPanel();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(numericPanel);
        panel.add(stringPanel);
        panel.add(createReplacePanel());

        addTab("Default Settings", panel);
    }

    private JPanel createReplacePanel() {
        JPanel panel = new JPanel();
        m_replaceBox = new JCheckBox("Replace column(s)", false);
        panel.add(m_replaceBox);
        return panel;
    }

    private JPanel createNumericInputPanel() {
        Box dataTableInput = Box.createVerticalBox();
        // the threshold
        Box threshBox = Box.createHorizontalBox();
        JLabel threshLabel = new JLabel("Threshold:");
        m_threshold.setMinimumSize(new Dimension(100, COMP_HEIGHT));
        m_threshold.setMaximumSize(new Dimension(100, COMP_HEIGHT));

        threshBox.add(Box.createHorizontalGlue());
        threshBox.add(threshLabel);
        threshBox.add(m_threshold);

        Box meanBox = Box.createHorizontalBox();
        JLabel meanLabel = new JLabel("Use percentage of the mean:");
        meanLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        m_useMean.setAlignmentX(Component.RIGHT_ALIGNMENT);
        meanBox.add(Box.createHorizontalGlue());
        meanBox.add(meanLabel);
        meanBox.add(m_useMean);

        Box percentageBox = Box.createHorizontalBox();
        JLabel percentageLabel = new JLabel("Percentage:");
        m_meanPercentage.setMinimumSize(new Dimension(100, COMP_HEIGHT));
        m_meanPercentage.setMaximumSize(new Dimension(100, COMP_HEIGHT));
        percentageBox.add(Box.createHorizontalGlue());
        percentageBox.add(percentageLabel);
        percentageBox.add(m_meanPercentage);

        Box numericRadioBox = Box.createHorizontalBox();
        numericRadioBox.add(m_numericRadio);
        numericRadioBox.add(Box.createHorizontalGlue());

        dataTableInput.add(numericRadioBox);
        dataTableInput.add(Box.createVerticalGlue());
        dataTableInput.add(threshBox);
        dataTableInput.add(Box.createVerticalGlue());
        dataTableInput.add(meanBox);
        dataTableInput.add(Box.createVerticalGlue());
        dataTableInput.add(percentageBox);
        dataTableInput.add(Box.createVerticalGlue());

        JPanel numericPanel = new JPanel();
        numericPanel.setLayout(new BoxLayout(numericPanel, BoxLayout.Y_AXIS));
        numericPanel.setBorder(BorderFactory.createEmptyBorder());
        numericPanel.add(dataTableInput);
        return numericPanel;
    }

    private JPanel createStringInputPanel() {
        // if the input is a string
        Box stringBox = Box.createVerticalBox();

        Box colBox = Box.createHorizontalBox();
        colBox.add(m_stringColumn.getComponentPanel());
        Box methodBox = Box.createHorizontalBox();
        JLabel methodLabel = new JLabel("Kind of string representation: ");
        m_stringType.setMinimumSize(new Dimension(100, COMP_HEIGHT));
        m_stringType.setMaximumSize(new Dimension(100, COMP_HEIGHT));
        methodBox.add(Box.createHorizontalGlue());
        methodBox.add(methodLabel);
        methodBox.add(m_stringType);

        Box stringRadioBox = Box.createHorizontalBox();
        stringRadioBox.add(m_stringRadio);
        stringRadioBox.add(Box.createHorizontalGlue());

        stringBox.add(stringRadioBox);
        stringBox.add(Box.createVerticalGlue());
        stringBox.add(colBox);
        stringBox.add(Box.createVerticalGlue());
        stringBox.add(methodBox);
        stringBox.add(Box.createVerticalGlue());

        JPanel stringPanel = new JPanel();
        stringPanel.setLayout(new BoxLayout(stringPanel, BoxLayout.Y_AXIS));
        stringPanel.setBorder(BorderFactory.createEmptyBorder());
        stringPanel.add(stringBox);
        return stringPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {

        if ((specs[0] == null) || (specs[0].getNumColumns() < 1)) {
            throw new NotConfigurableException("Need DataTableSpec at input "
                    + "port. Connect node and/or execute predecessor");
        }
        for (DataColumnSpec spec : specs[0]) {
            if (spec.getType().isCompatible(StringValue.class)) {
                m_hasStringCol = true;
                break;
            }
        }
        if (!m_hasStringCol) {
            m_stringRadio.setEnabled(false);
        }

        m_stringRadio.setSelected(settings.getBoolean(
                BitVectorGeneratorNodeModel.CFG_FROM_STRING, false));
        m_numericRadio.setSelected(!m_stringRadio.isSelected());
        String typeString = settings.getString(
                BitVectorGeneratorNodeModel.CFG_STRING_TYPE,
                BitVectorGeneratorNodeModel.STRING_TYPES.BIT.name());
        BitVectorGeneratorNodeModel.STRING_TYPES type
            = BitVectorGeneratorNodeModel.STRING_TYPES.valueOf(typeString);
        m_stringType.setSelectedItem(type);
        m_useMean.setSelected(settings.getBoolean(
                BitVectorGeneratorNodeModel.CFG_USE_MEAN, false));
        m_threshold.setValue(settings.getDouble(
                BitVectorGeneratorNodeModel.CFG_THRESHOLD, 1.0));
        m_meanPercentage.setValue(settings.getInt(
                BitVectorGeneratorNodeModel.CFG_MEAN_THRESHOLD, 100));
        m_replaceBox.setSelected(settings.getBoolean(
                BitVectorGeneratorNodeModel.CFG_REPLACE, false));
        m_stringColumn.loadSettingsFrom(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        settings.addBoolean(BitVectorGeneratorNodeModel.CFG_FROM_STRING,
                m_stringRadio.isSelected());
        settings.addBoolean(BitVectorGeneratorNodeModel.CFG_USE_MEAN, m_useMean
                .isSelected());
        settings.addString(BitVectorGeneratorNodeModel.CFG_STRING_TYPE,
                ((BitVectorGeneratorNodeModel.STRING_TYPES)m_stringType
                        .getModel().getSelectedItem()).name());
        settings.addDouble(BitVectorGeneratorNodeModel.CFG_THRESHOLD,
                (Double)m_threshold.getValue());
        settings.addInt(BitVectorGeneratorNodeModel.CFG_MEAN_THRESHOLD,
                (Integer)m_meanPercentage.getValue());
        settings.addBoolean(BitVectorGeneratorNodeModel.CFG_REPLACE,
                m_replaceBox.isSelected());
        m_stringColumn.saveSettingsTo(settings);
    }
}
