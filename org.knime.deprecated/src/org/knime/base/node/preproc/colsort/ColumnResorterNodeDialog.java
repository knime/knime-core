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
 *   Nov 23, 2007 (schweize): creaed
 */
package org.knime.base.node.preproc.colsort;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * 
 * @author schweize, University of Konstanz
 */
public class ColumnResorterNodeDialog extends NodeDialogPane {
    

    private static final NodeLogger LOGGER = NodeLogger.
    getLogger(ColumnResorterNodeDialog.class);
    
    private DataColumnSpec[] m_columnNames = new DataColumnSpec[] {};
    
    private JPanel m_contentPanel;
    
    private JList m_columnList;
    
    private DefaultListModel m_model;
    
    private String[] m_order;
    
    
    /**
     * Creates a new dialog for the column resorter node.
     */
    public ColumnResorterNodeDialog() {
        
        LOGGER.debug("Logger begin");
        m_order = new String[] {};
        m_contentPanel = new JPanel();
        m_contentPanel.setLayout(
                new BoxLayout(m_contentPanel, BoxLayout.X_AXIS));
        
        
        Box optionsBox = Box.createVerticalBox();
        optionsBox.add(createChooserBox());
        m_contentPanel.add(Box.createHorizontalGlue());
        m_contentPanel.add(createListBox());
        m_contentPanel.add(optionsBox);
        m_contentPanel.add(Box.createHorizontalGlue());
        this.addTab("Resort columns", m_contentPanel);
    }
    
    
    
    private Box createChooserBox() {
      //Buttons and their listeners
        Box chooserBox = Box.createVerticalBox();
        chooserBox.setBorder(new TitledBorder("Actions"));
        
        JButton buttonAZ = new JButton("A-Z");
        JButton buttonZA = new JButton("Z-A");
        JButton moveFirst = new JButton("Move First");
        JButton moveLast = new JButton("Move Last");
        JButton buttonUp = new JButton("Up");
        JButton buttonDown = new JButton("Down");
        JButton buttonOriginal = new JButton("Reset");
        
        // same size for all buttons
        Dimension allButtons = moveFirst.getPreferredSize();
        buttonAZ.setMinimumSize(allButtons);
        buttonAZ.setMaximumSize(allButtons);
        buttonZA.setMinimumSize(allButtons);
        buttonZA.setMaximumSize(allButtons);
        buttonUp.setMinimumSize(allButtons);
        buttonUp.setMaximumSize(allButtons);
        buttonDown.setMinimumSize(allButtons);
        buttonDown.setMaximumSize(allButtons);
        moveLast.setMinimumSize(allButtons);
        moveLast.setMaximumSize(allButtons);
        // button to restore original order
        buttonOriginal.setMaximumSize(allButtons);
        buttonOriginal.setMinimumSize(allButtons);
        

        chooserBox.add(buttonAZ);
        chooserBox.add(buttonZA);
//        chooserBox.add(Box.createVerticalStrut(20));
        chooserBox.add(buttonUp);
        chooserBox.add(buttonDown);
//        chooserBox.add(Box.createVerticalStrut(20));
        chooserBox.add(moveFirst);
        chooserBox.add(moveLast);
//        chooserBox.add(Box.createVerticalStrut(20));
        chooserBox.add(buttonOriginal);
       
        buttonOriginal.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                m_model.removeAllElements();
                for (DataColumnSpec cur : m_columnNames) {
                    m_model.addElement(cur);
                }
                m_columnList.ensureIndexIsVisible(0);
            }
        });
        
        buttonAZ.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                DataColumnSpec[] tmp = new DataColumnSpec[m_model.getSize()];
                for (int i = 0; i < m_model.getSize(); i++) {
                    tmp[i] = (DataColumnSpec)m_model.get(i);
                }
                Arrays.sort(tmp, new DataColumnSpecComparator());
                m_model.removeAllElements();
                int i = 0;
                for (DataColumnSpec o : tmp) {
                    m_model.insertElementAt(o, i++);
                }
                m_columnList.ensureIndexIsVisible(0);
            }
        });
        
        buttonZA.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                DataColumnSpec[] tmp = new DataColumnSpec[m_model.getSize()];
                for (int i = 0; i < m_model.getSize(); i++) {
                    tmp[i] = (DataColumnSpec)m_model.get(i);
                }
                Arrays.sort(tmp, new DataColumnSpecComparator());
                m_model.removeAllElements();
                int counter = 0;
                for (int i = tmp.length - 1; i >= 0; i--) {
                    m_model.insertElementAt(tmp[i], counter++);
                }
                m_columnList.ensureIndexIsVisible(0);
            }
        });
        
        buttonUp.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (m_columnList.getSelectedIndex() != -1) {
                    int[] indices = m_columnList.getSelectedIndices();
                    moveUp(indices);
                } else {
                    LOGGER.info("Please select a column.");
                }
            }
            
        });
        
        buttonDown.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                
                if (m_columnList.getSelectedIndex() != -1) {
                    int[] indices = m_columnList.getSelectedIndices();
                    moveDown(indices);
                } else {
                    LOGGER.info("Please select a column.");
                }
            }
            
        });
        
        moveFirst.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (m_columnList.getSelectedIndex() != -1) {
                    int[] indices = m_columnList.getSelectedIndices();
                    moveFirst(indices);
                } else {
                    LOGGER.info("Please select a column.");
                }
            }
            
        });
        
        moveLast.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (m_columnList.getSelectedIndex() != -1) {
                    int[] indices = m_columnList.getSelectedIndices();
                    moveLast(indices);
                } else {
                    LOGGER.info("Please select a column.");
                }
            }
            
        });
        return chooserBox;
    }
    
    
    private Box createListBox() {
      //List for the columns
        Box listBox = Box.createVerticalBox();
        listBox.setBorder(new TitledBorder("Columns"));
        m_model = new DefaultListModel();
        m_columnList = new JList(m_model);
        m_columnList.setSelectionMode(
                ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        m_columnList.setCellRenderer(new DataColumnSpecListCellRenderer());
        m_columnList.setBorder(new EtchedBorder());
        
        JScrollPane scrollPane = new JScrollPane(m_columnList);
        listBox.add(scrollPane);
        return listBox;
    }
    
    private void moveFirst(final int[] indices) {
        DataColumnSpec[] values = new DataColumnSpec[indices.length];
        for (int i = indices.length - 1; i >= 0; i--) {
            values[indices.length - 1 - i] 
                   = (DataColumnSpec)m_model.getElementAt(indices[i]);
        }
        for (int i = indices.length - 1; i >= 0; i--) {
            m_model.remove(indices[i]);
        }
        for (DataColumnSpec cur : values) {
            m_model.insertElementAt(cur, 0);
        }
        int[] newSelected = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            newSelected[i] = i;
        }
        m_columnList.setSelectedIndices(newSelected);
        m_columnList.ensureIndexIsVisible(newSelected[0]);
    }
    
    private void moveLast(final int[] indices) {
        DataColumnSpec[] values = new DataColumnSpec[indices.length];
        for (int i = 0; i < indices.length; i++) {
            values[i] = (DataColumnSpec)m_model.getElementAt(indices[i]);
        }
        for (int i = indices.length - 1; i >= 0; i--) {
            m_model.remove(indices[i]);
        }
        for (DataColumnSpec cur : values) {
            m_model.insertElementAt(cur, m_model.size());
        }
        int[] newSelected = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            newSelected[i] = m_model.size() - 1 - i;
        }
        m_columnList.setSelectedIndices(newSelected);
        m_columnList.ensureIndexIsVisible(newSelected[0]);
    }
    
    private void moveUp(final int[] indices) {
        DataColumnSpec[] values = new DataColumnSpec[indices.length];
        for (int i = 0; i < indices.length; i++) {
            values[i] = (DataColumnSpec)m_model.getElementAt(indices[i]);
        }
        for (int i = indices.length - 1; i >= 0; i--) {
            m_model.remove(indices[i]);
        }
        int[] newSelected = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] - 1 >= 0) {
                m_model.insertElementAt(values[i], indices[i] - 1);
                newSelected[i] = indices[i] - 1;
            } else {
                for (DataColumnSpec cur : values) {
                    m_model.addElement(cur);
                }
                for (int k = 0; k < indices.length; k++) {
                    newSelected[k] = m_model.getSize() - 1 - k;
                }
                m_columnList.setSelectedIndices(newSelected);
                break;
            }
        }
        m_columnList.setSelectedIndices(newSelected);
        m_columnList.ensureIndexIsVisible(newSelected[0]);
    }
    
    private void moveDown(final int[] indices) {
        DataColumnSpec[] values = new DataColumnSpec[indices.length];
        for (int i = 0; i < indices.length; i++) {
            values[i] = (DataColumnSpec)m_model.getElementAt(indices[i]);
        }
        for (int i = indices.length - 1; i >= 0; i--) {
            m_model.remove(indices[i]);
        }
        int[] newSelected = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] + 1 <= m_model.getSize()) {
                m_model.insertElementAt(values[i], indices[i] + 1);
                newSelected[i] = indices[i] + 1;
            } else {
                for (int k = 0; k < indices.length; k++) {
                    m_model.insertElementAt(values[values.length - 1 - k], 0);
                }
                for (int k = 0; k < indices.length; k++) {
                    newSelected[k] = k;
                }
                m_columnList.setSelectedIndices(newSelected);
                m_columnList.ensureIndexIsVisible(newSelected[0]);
                break;
            }
        }
        m_columnList.setSelectedIndices(newSelected);
        m_columnList.ensureIndexIsVisible(newSelected[newSelected.length - 1]);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        if (specs[0] == null || specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException(
                    "No input table found or no columns found in input table! "
                    + "Please connect the node first or check input table.");
        }
        try {
            List<DataColumnSpec> columnNames = new ArrayList<DataColumnSpec>();
            for (DataColumnSpec colSpec : specs[0]) {
                    columnNames.add(colSpec);
            }
            m_columnNames = columnNames.toArray(new DataColumnSpec[] {});
            if (m_model.size() > 0) {
                m_model.removeAllElements();
            }
            
            m_order = settings.getStringArray(
                    ColumnResorterNodeModel.CFG_NEW_ORDER);
            Set<String> alreadyInList = new HashSet<String>();
            if (m_order.length > 0) {
                for (String cur : m_order) {
                    //is column in new spec?
                    if (specs[0].containsName(cur)) {
                        alreadyInList.add(cur);
                        m_model.addElement(specs[0].getColumnSpec(cur));
                    }
                }
                // add new columns at the end
                for (DataColumnSpec cur : specs[0]) {
                    if (!alreadyInList.contains(cur.getName())) {
                        m_model.addElement(cur);
                    }
                }
            } else {
                for (DataColumnSpec cur : m_columnNames) {
                    m_model.addElement(cur);
                }
            }
        } catch (InvalidSettingsException is) {
            // do nothing here - its the dialog
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        Object[] tmp = m_model.toArray();
        m_order = new String[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
            m_order[i] = ((DataColumnSpec)tmp[i]).getName();
        }
        settings.addStringArray(ColumnResorterNodeModel.CFG_NEW_ORDER, m_order);
    }
    

    private static class DataColumnSpecComparator 
        implements Comparator<DataColumnSpec> {

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public int compare(final DataColumnSpec o1, final DataColumnSpec o2) {
            return o1.getName().compareTo(o2.getName());
        }
        
    }
}
