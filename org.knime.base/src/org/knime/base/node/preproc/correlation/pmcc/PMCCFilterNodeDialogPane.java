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
 *   18.02.2007 (wiswedel): created
 */
package org.knime.base.node.preproc.correlation.pmcc;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class PMCCFilterNodeDialogPane extends NodeDialogPane {
    
    private static final NumberFormat FORMAT = new DecimalFormat("#.#######");
    
    private final JList m_list;
    private final JSlider m_slider;
    private final JFormattedTextField m_textField;
    private final JLabel m_includeLabel;
    private final JLabel m_excludeLabel;
    private final JLabel m_totalLabel;
    private final JButton m_applyButton;
    
    private double m_lastCommittedValue;
    
    private PMCCModel m_model;

    /** Creates GUI. */
    public PMCCFilterNodeDialogPane() {
        m_list = new JList(new DefaultListModel());
        m_list.setCellRenderer(new DataColumnSpecListCellRenderer());
        m_list.setPrototypeCellValue("################");
        m_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_list.setEnabled(false);
        m_list.setMinimumSize(new Dimension(40, 0));
        m_slider = new JSlider(new DefaultBoundedRangeModel(1000, 0, 0, 1000));
        m_slider.setMinorTickSpacing(100);
        m_slider.setMajorTickSpacing(500);
        m_slider.setPaintTicks(true);
        NumberFormatter formatter = new NumberFormatter(FORMAT);
        formatter.setMinimum(0.00);
        formatter.setMaximum(1.0);
        m_lastCommittedValue = -1.0;
        m_applyButton = new JButton("Calculate");
        m_applyButton.addActionListener(new ActionListener() {
           public void actionPerformed(final ActionEvent e) {
                onCalculate();
           } 
        });
        m_textField = new JFormattedTextField(formatter);
        m_textField.setValue(m_slider.getValue() / 1000.0);
        m_textField.setColumns(7);
        m_textField.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "check");
        m_textField.getActionMap().put("check", new AbstractAction() {
           public void actionPerformed(final ActionEvent e) {
               if (!m_textField.isEditValid()) { //The text is invalid.
                   Toolkit.getDefaultToolkit().beep();
                   m_textField.selectAll();
               } else {
                   try {
                       m_textField.commitEdit();
                   } catch (java.text.ParseException exc) {
                       // text not ok, reverting it.
                   }
               }
           } 
        });
        m_textField.addPropertyChangeListener("value", 
                new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                Number value = (Number)evt.getNewValue();
                if (value != null) {
                    m_slider.setValue((int)(1000 * value.doubleValue()));
                    m_applyButton.setEnabled(value.doubleValue() 
                            != m_lastCommittedValue);
                }
            }
        });
        m_slider.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                int val = (int)source.getValue();
                double valDbl = val / 1000.0;
                if (!source.getValueIsAdjusting()) {
                    m_textField.setValue(valDbl); //update field
                    m_applyButton.setEnabled(valDbl != m_lastCommittedValue);
                } else { //value is adjusting; just set the text
                    String valString = FORMAT.format(valDbl);
                    m_textField.setText(valString);
                }
            } 
        });
        m_slider.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "calculate");
        m_slider.getActionMap().put("calculate", new AbstractAction() {
           public void actionPerformed(final ActionEvent e) {
                onCalculate();
           } 
        });
        m_includeLabel = new JLabel("########################");
        m_excludeLabel = new JLabel("########################");
        m_totalLabel = new JLabel("########################");
        setLabels(-1, -1);
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        p.add(new JLabel("Columns from Model"), gbc);
        
        gbc.gridy++;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weighty = 1.0;
        gbc.weightx = 1.0;
        p.add(new JScrollPane(m_list), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        p.add(new JLabel("Correlation Threshold"), gbc);
        
        gbc.gridy++;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.gridwidth = 2;
        p.add(m_slider, gbc);
        
        gbc.gridx = 3;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.0;
        p.add(m_textField, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        p.add(new JLabel("Include columns: "), gbc);
        
        gbc.gridx = 2;
        p.add(m_includeLabel, gbc);
        
        gbc.gridy++;
        gbc.gridx = 1;
        p.add(new JLabel("Exclude columns: "), gbc);

        gbc.gridx = 2;
        p.add(m_excludeLabel, gbc);
        
        gbc.gridy++;
        gbc.gridx = 1;
        p.add(new JLabel("Total columns: "), gbc);
        
        gbc.gridx = 2;
        p.add(m_totalLabel, gbc);
        
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        p.add(m_applyButton, gbc);
        
        addTab("Settings", p);
    }
    
    private void onCalculate() {
        double val = ((Number)m_textField.getValue()).doubleValue(); 
        if (val == m_lastCommittedValue) {
            return;
        }
        String[] includes = m_model.getReducedSet(val);
        HashSet<String> hash = new HashSet<String>(Arrays.asList(includes));
        int[] indices = new int[includes.length];
        DefaultListModel model = (DefaultListModel)m_list.getModel();
        int index = 0;
        int runIndex = 0;
        for (Enumeration<?> e = model.elements(); e.hasMoreElements();) {
            DataColumnSpec s = (DataColumnSpec)e.nextElement();
            if (hash.remove(s.getName())) {
                indices[index++] = runIndex;
            }
            runIndex++;
        }
        m_list.clearSelection();
        m_list.setSelectedIndices(indices);
        setLabels(includes.length, model.size());
        m_slider.requestFocus();
        m_lastCommittedValue = val;
        m_applyButton.setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        NodeSettingsRO subSet;
        try {
            subSet = 
                settings.getNodeSettings(PMCCFilterNodeModel.CFG_MODEL);
            m_model = PMCCModel.load(subSet);
        } catch (InvalidSettingsException ise) {
            throw new NotConfigurableException("No model available.");
        }
        // check if all columns in the model are also present in the spec
        HashSet<String> allColsInModel = new HashSet<String>(
                Arrays.asList(m_model.getColNames()));
        double d = settings.getDouble(PMCCFilterNodeModel.CFG_THRESHOLD, 1.0);
        m_textField.setValue(d);
        DefaultListModel m = (DefaultListModel)m_list.getModel();
        m.removeAllElements();
        int totalCount = 0;
        for (DataColumnSpec s : specs[0]) {
            if (s.getType().isCompatible(NominalValue.class)
                    || s.getType().isCompatible(DoubleValue.class)) {
                if (allColsInModel.remove(s.getName())) {
                    totalCount++;
                    m.addElement(s);
                }
            }
        }
        if (!allColsInModel.isEmpty()) {
            throw new NotConfigurableException("Some columns in the model are "
                    + "not contained in the input table or incompatible: "
                    + allColsInModel.iterator().next());
        }
        m_lastCommittedValue = -1.0;
        m_applyButton.setEnabled(true);
        setLabels(-1, totalCount);
    }
    
    private void setLabels(final int include, final int total) {
        m_totalLabel.setText(Integer.toString(total));
        m_includeLabel.setText(include >= 0 ? Integer.toString(include) : "??");
        m_excludeLabel.setText(include >= 0 
                ? Integer.toString(total - include) : "??");
        m_totalLabel.setSize(m_totalLabel.getPreferredSize());
        m_includeLabel.setSize(m_totalLabel.getPreferredSize());
        m_excludeLabel.setSize(m_totalLabel.getPreferredSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        double val = ((Number)m_textField.getValue()).doubleValue();
        settings.addDouble(PMCCFilterNodeModel.CFG_THRESHOLD, val);
    }

}
