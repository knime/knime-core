/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 * History
 *   18.02.2007 (wiswedel): created
 */
package org.knime.base.node.preproc.correlation.filter;

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

import org.knime.base.node.preproc.correlation.pmcc.PMCCPortObjectAndSpec;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class CorrelationFilterNodeDialogPane extends NodeDialogPane {
    
    private static final NumberFormat FORMAT = new DecimalFormat("#.#######");
    
    private final JList m_list;
    private final JSlider m_slider;
    private final JFormattedTextField m_textField;
    private final JLabel m_includeLabel;
    private final JLabel m_excludeLabel;
    private final JLabel m_totalLabel;
    private final JButton m_calcButton;
    private final JLabel m_errorLabel;
    
    private double m_lastCommittedValue;
    
    private PMCCPortObjectAndSpec m_model;

    /** Creates GUI. */
    public CorrelationFilterNodeDialogPane() {
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
        m_calcButton = new JButton("Calculate");
        m_calcButton.addActionListener(new ActionListener() {
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
                    m_calcButton.setEnabled(m_model.hasData() 
                            && value.doubleValue() != m_lastCommittedValue);
                }
            }
        });
        m_slider.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                int val = source.getValue();
                double valDbl = val / 1000.0;
                if (!source.getValueIsAdjusting()) {
                    m_textField.setValue(valDbl); //update field
                    m_calcButton.setEnabled(m_model.hasData() 
                            && valDbl != m_lastCommittedValue);
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
        m_errorLabel = new JLabel("########################");
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
        p.add(m_calcButton, gbc);
        
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.gridy++;
        p.add(m_errorLabel, gbc);
        
        addTab("Settings", p);
    }
    
    private void onCalculate() {
        if (!m_model.hasData()) {
            // can enter here from an event fired by the slider, silently ignore
            assert !m_calcButton.isEnabled() : "No data for preview";
            m_calcButton.setEnabled(false);  
            return;
        }
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
        m_calcButton.setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_model = (PMCCPortObjectAndSpec)specs[0];
        DataTableSpec spec = (DataTableSpec)specs[1];
        if (m_model == null || spec == null) {
            throw new NotConfigurableException("No input available");
        }
        // check if all columns in the model are also present in the spec
        HashSet<String> allColsInModel = new HashSet<String>(
                Arrays.asList(m_model.getColNames()));
        double d = settings.getDouble(CorrelationFilterNodeModel.CFG_THRESHOLD, 1.0);
        m_textField.setValue(d);
        DefaultListModel m = (DefaultListModel)m_list.getModel();
        m.removeAllElements();
        int totalCount = 0;
        for (DataColumnSpec s : spec) {
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
        if (m_model.hasData()) {
            m_errorLabel.setText(" ");
            m_calcButton.setEnabled(true);
        } else {
            m_errorLabel.setText("No correlation in input available");
            m_calcButton.setEnabled(false);
        }
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
        settings.addDouble(CorrelationFilterNodeModel.CFG_THRESHOLD, val);
    }

}
