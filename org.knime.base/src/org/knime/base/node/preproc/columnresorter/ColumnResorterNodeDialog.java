/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   28.05.2012 (kilian): created
 */
package org.knime.base.node.preproc.columnresorter;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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

/**
 * The dialog of the column resorter node.
 * 
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class ColumnResorterNodeDialog extends NodeDialogPane {

    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(ColumnResorterNodeDialog.class);
    
    private JList m_jlist;
    
    private DefaultListModel m_listModel;
    
    private String[] m_order;
    
    private List<DataColumnSpec> m_origColOrder = 
        new ArrayList<DataColumnSpec>();
    
    /**
     * Creates new instance of <code>ColumnResorterNodeDialog</code>.
     */
    ColumnResorterNodeDialog() {
        addTab("Resort columns", initPanel());
    }
    
    /**
     * @return Creates and returns the main panel.
     */
    private JPanel initPanel() {
        JPanel jp = new JPanel();
        jp.setLayout(new BoxLayout(jp, BoxLayout.X_AXIS));
        
        // add column list
        jp.add(Box.createHorizontalGlue());
        jp.add(initColumnListBox());
        
        // add buttons
        jp.add(initButtonBox());
        
        jp.add(Box.createHorizontalGlue());
        return jp;
    }
    
    /**
     * @return Creates and returns the panel containing the column list.
     */
    private Box initColumnListBox() {
        Box columnListBox = Box.createVerticalBox();
        columnListBox.setBorder(new TitledBorder("Columns"));
        m_listModel = new DefaultListModel();
        m_jlist = new JList(m_listModel);
        m_jlist.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        m_jlist.setCellRenderer(new DataColumnSpecListDummyCellRenderer());
        m_jlist.setBorder(new EtchedBorder());

        columnListBox.add(new JScrollPane(m_jlist));
        return columnListBox;
    }
    
    /**
     * @return Creates and returns the panel containing the buttons.
     */
    private Box initButtonBox() {
        Box buttonBox = Box.createVerticalBox();
        buttonBox.setBorder(new TitledBorder("Actions"));

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
        buttonOriginal.setMaximumSize(allButtons);
        buttonOriginal.setMinimumSize(allButtons);

        buttonBox.add(buttonAZ);
        buttonBox.add(buttonZA);
        buttonBox.add(buttonUp);
        buttonBox.add(buttonDown);
        buttonBox.add(moveFirst);
        buttonBox.add(moveLast);
        buttonBox.add(buttonOriginal);

        buttonOriginal.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_listModel.removeAllElements();
                for (DataColumnSpec col : m_origColOrder) {
                    m_listModel.addElement(col);
                }
                // add dummy element "<any unknown new column>".
                m_listModel.addElement(
                        DataColumnSpecListDummyCellRenderer.UNKNOWN_COL_DUMMY);
                m_jlist.ensureIndexIsVisible(0);
            }
        });
        
        buttonAZ.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            @Override            
            public void actionPerformed(final ActionEvent e) {
                DataColumnSpec[] tmp = getSortedCols();
                m_listModel.removeAllElements();
                for (int i = 0; i < tmp.length; i++) {
                    m_listModel.insertElementAt(tmp[i], i);
                }
                m_jlist.ensureIndexIsVisible(0);
            }
        });
        
        buttonZA.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            @Override            
            public void actionPerformed(final ActionEvent e) {
                DataColumnSpec[] tmp = getSortedCols();
                m_listModel.removeAllElements();
                for (int i = tmp.length - 1; i >= 0; i--) {
                    m_listModel.insertElementAt(tmp[i], tmp.length - i - 1);
                }
                m_jlist.ensureIndexIsVisible(0);
            }
        });        
        
        buttonUp.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            @Override            
            public void actionPerformed(final ActionEvent e) {
                if (m_jlist.getSelectedIndex() != -1) {
                    int[] indices = m_jlist.getSelectedIndices();
                    moveUp(indices);
                } else {
                    LOGGER.info("Please select a column.");
                }
            }
        });
        
        buttonDown.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            @Override            
            public void actionPerformed(final ActionEvent e) {
                if (m_jlist.getSelectedIndex() != -1) {
                    int[] indices = m_jlist.getSelectedIndices();
                    moveDown(indices);
                } else {
                    LOGGER.info("Please select a column.");
                }
            }
        });        
        
        moveFirst.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            @Override            
            public void actionPerformed(final ActionEvent e) {
                if (m_jlist.getSelectedIndex() != -1) {
                    int[] indices = m_jlist.getSelectedIndices();
                    moveFirst(indices);
                } else {
                    LOGGER.info("Please select a column.");
                }
            }
        });
        
        moveLast.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            @Override            
            public void actionPerformed(final ActionEvent e) {
                if (m_jlist.getSelectedIndex() != -1) {
                    int[] indices = m_jlist.getSelectedIndices();
                    moveLast(indices);
                } else {
                    LOGGER.info("Please select a column.");
                }
            }
        });        
        
        return buttonBox;
    }
    
    /**
     * @return Returns an array of sorted column specs.
     */
    private DataColumnSpec[] getSortedCols() {
        DataColumnSpec[] tmp = new DataColumnSpec[m_listModel.getSize()];
        for (int i = 0; i < m_listModel.getSize(); i++) {
            tmp[i] = (DataColumnSpec)m_listModel.get(i);
        }
        Arrays.sort(tmp, new DataColumnSpecComparator());
        return tmp;
    }
    
    /**
     * Moves elements at given indices one position up in list.
     * @param indices to move up.
     */
    private void moveUp(final int[] indices) {
        DataColumnSpec[] values = new DataColumnSpec[indices.length];
        for (int i = 0; i < indices.length; i++) {
            values[i] = (DataColumnSpec)m_listModel.getElementAt(indices[i]);
        }
        for (int i = indices.length - 1; i >= 0; i--) {
            m_listModel.remove(indices[i]);
        }
        int[] newSelected = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] - 1 >= 0) {
                m_listModel.insertElementAt(values[i], indices[i] - 1);
                newSelected[i] = indices[i] - 1;
            } else {
                for (DataColumnSpec cur : values) {
                    m_listModel.addElement(cur);
                }
                for (int k = 0; k < indices.length; k++) {
                    newSelected[k] = m_listModel.getSize() - 1 - k;
                }
                m_jlist.setSelectedIndices(newSelected);
                break;
            }
        }
        m_jlist.setSelectedIndices(newSelected);
        m_jlist.ensureIndexIsVisible(newSelected[0]);
    }
    
    /**
     * Moves elements at given indices one position down in list.
     * @param indices to move down.
     */
    private void moveDown(final int[] indices) {
        DataColumnSpec[] values = new DataColumnSpec[indices.length];
        for (int i = 0; i < indices.length; i++) {
            values[i] = (DataColumnSpec)m_listModel.getElementAt(indices[i]);
        }
        for (int i = indices.length - 1; i >= 0; i--) {
            m_listModel.remove(indices[i]);
        }
        int[] newSelected = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] + 1 <= m_listModel.getSize()) {
                m_listModel.insertElementAt(values[i], indices[i] + 1);
                newSelected[i] = indices[i] + 1;
            } else {
                for (int k = 0; k < indices.length; k++) {
                    m_listModel.insertElementAt(
                            values[values.length - 1 - k], 0);
                }
                for (int k = 0; k < indices.length; k++) {
                    newSelected[k] = k;
                }
                m_jlist.setSelectedIndices(newSelected);
                m_jlist.ensureIndexIsVisible(newSelected[0]);
                break;
            }
        }
        m_jlist.setSelectedIndices(newSelected);
        m_jlist.ensureIndexIsVisible(newSelected[newSelected.length - 1]);
    }    
    
    /**
     * Moves elements at given indices to first position.
     * @param indices to move to top.
     */
    private void moveFirst(final int[] indices) {
        DataColumnSpec[] values = new DataColumnSpec[indices.length];
        for (int i = indices.length - 1; i >= 0; i--) {
            values[indices.length - 1 - i] 
                   = (DataColumnSpec)m_listModel.getElementAt(indices[i]);
        }
        for (int i = indices.length - 1; i >= 0; i--) {
            m_listModel.remove(indices[i]);
        }
        for (DataColumnSpec cur : values) {
            m_listModel.insertElementAt(cur, 0);
        }
        int[] newSelected = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            newSelected[i] = i;
        }
        m_jlist.setSelectedIndices(newSelected);
        m_jlist.ensureIndexIsVisible(newSelected[0]);
    }
    
    /**
     * Moves elements at given indices to last position.
     * @param indices to move to bottom.
     */
    private void moveLast(final int[] indices) {
        DataColumnSpec[] values = new DataColumnSpec[indices.length];
        for (int i = 0; i < indices.length; i++) {
            values[i] = (DataColumnSpec)m_listModel.getElementAt(indices[i]);
        }
        for (int i = indices.length - 1; i >= 0; i--) {
            m_listModel.remove(indices[i]);
        }
        for (DataColumnSpec cur : values) {
            m_listModel.insertElementAt(cur, m_listModel.size());
        }
        int[] newSelected = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            newSelected[i] = m_listModel.size() - 1 - i;
        }
        m_jlist.setSelectedIndices(newSelected);
        m_jlist.ensureIndexIsVisible(newSelected[0]);
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_order = new String[m_listModel.size()];
        for (int i = 0; i < m_listModel.size(); i++) {
            m_order[i] = ((DataColumnSpec)m_listModel.get(i)).getName();
        }
        settings.addStringArray(ColumnResorterConfigKeys.COLUMN_ORDER, m_order);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // Check input spec
        if (specs[0] == null || specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException(
                    "No input table found or no columns found in input table! "
                    + "Please connect the node first or check input table.");
        }
        DataTableSpec spec = specs[0];
        boolean addedDummy = false;
        
        try {
            // get settings
            m_order = settings.getStringArray(
                    ColumnResorterConfigKeys.COLUMN_ORDER);
            m_listModel.removeAllElements();
            m_origColOrder.clear();
            
            // get available col names
            List<String> colNames = new ArrayList<String>();
            List<String> additionalColNames = new ArrayList<String>();
            for (int i = 0; i < spec.getNumColumns(); i++) {
                colNames.add(spec.getColumnSpec(i).getName());
                additionalColNames.add(spec.getColumnSpec(i).getName());
                m_origColOrder.add(spec.getColumnSpec(i));
            }
            // Get all column names which are in spec but not in order
            additionalColNames.removeAll(Arrays.asList(m_order));
                        
            // cols are already sorter
            if (m_order.length > 0) {
                for (String col : m_order) {
                    if (DataColumnSpecListDummyCellRenderer.UNKNOWN_COL_DUMMY
                            .getName().equals(col)) {
                        // add place holder
                        m_listModel.addElement(
                                DataColumnSpecListDummyCellRenderer
                                .UNKNOWN_COL_DUMMY);
                        addedDummy = true;
                        
                        // now all new columns need to be added
                        for (String newCol : additionalColNames) {
                            m_listModel.addElement(spec.getColumnSpec(newCol));
                            colNames.remove(newCol);
                        }

                    // add "old" columns    
                    } else {
                        DataColumnSpec colSpec = spec.getColumnSpec(col);
                        if (colSpec != null) {
                            m_listModel.addElement(colSpec);
                            colNames.remove(col);
                        }
                    }
                }
                // any cols left?
                for (String col : colNames) {
                    m_listModel.addElement(spec.getColumnSpec(col));
                }
            
            // cols are not sorted, available cols have to be added initially
            } else {
                for (String col : colNames) {
                    m_listModel.addElement(spec.getColumnSpec(col));
                }
            }
            
            if (!addedDummy) {
                // add dummy element "<any unknown new column>".
                m_listModel.addElement(
                        DataColumnSpecListDummyCellRenderer.UNKNOWN_COL_DUMMY);
            }
            
        } catch (InvalidSettingsException e) {
            // Nothing to do ...
        }
    }
    
    private static class DataColumnSpecComparator implements
            Comparator<DataColumnSpec> {

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
