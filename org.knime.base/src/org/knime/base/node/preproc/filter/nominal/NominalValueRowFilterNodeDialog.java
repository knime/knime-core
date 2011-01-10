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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.filter.nominal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * <code>NodeDialog</code> for the "PossibleValueRowFilter" Node. Adds a
 * select box for the nominal column and an include and exclude list with the
 * necessar buttons to add, add all, remove, and remove all possible values from
 * one list to the other. The lists are update, when a nominal column is
 * selected.
 *
 * @author KNIME GmbH
 */
public class NominalValueRowFilterNodeDialog extends NodeDialogPane implements
        ItemListener {

    private String m_selectedColumn;

    private String[] m_selectedAttributes;

    private final Map<String, Set<DataCell>> m_colAttributes;

    // models
    private final DefaultComboBoxModel m_columns;

    private final DefaultListModel m_included;

    private final DefaultListModel m_excluded;

    // gui elements
    private final JComboBox m_columnSelection;

    private final JList m_includeList;

    private final JList m_excludeList;

    /** Config key for the selected column. */
    static final String CFG_SELECTED_COL = "selected_column";

    /** Config key for the possible values to include. */
    static final String CFG_SELECTED_ATTR = "selected attributes";

    /**
     * New pane for configuring the PossibleValueRowFilter node.
     */
    protected NominalValueRowFilterNodeDialog() {
        m_colAttributes = new HashMap<String, Set<DataCell>>();
        m_columns = new DefaultComboBoxModel();
        m_included = new DefaultListModel();
        m_excluded = new DefaultListModel();
        m_includeList = new JList(m_included);
        m_excludeList = new JList(m_excluded);

        m_columnSelection = new JComboBox(m_columns);
        m_columnSelection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        // add listener to column selection box
        // to change exclude list
        m_columnSelection.addItemListener(this);

        // create the GUI
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        Box colBox = Box.createHorizontalBox();
        colBox.add(Box.createHorizontalStrut(10));
        colBox.add(new JLabel("Select column:"));
        colBox.add(Box.createHorizontalStrut(10));
        colBox.add(m_columnSelection);
        colBox.add(Box.createHorizontalStrut(20));
        colBox.add(Box.createHorizontalGlue());
        panel.add(colBox);
        panel.add(createAttributeSelectionLists());
        panel.add(Box.createVerticalGlue());
        addTab("Selection", panel);
    }

    private Box createAttributeSelectionLists() {
        Box overall = Box.createHorizontalBox();
        overall.setBorder(new TitledBorder("Nominal value selection:"));
        // left excluded
        Box excluded = Box.createVerticalBox();
        excluded.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createLineBorder(Color.RED), "Excluded:"));
        m_excludeList.setMinimumSize(new Dimension(200, 200));
        // force fixed width for list
        m_excludeList.setFixedCellWidth(200);
        JScrollPane exclScroller = new JScrollPane(m_excludeList);
        exclScroller.setMinimumSize(new Dimension(200, 200));
        excluded.add(exclScroller);
        overall.add(excluded);

        // center buttons
        JButton add = new JButton("Add >");
        add.setMaximumSize(new Dimension(200, 20));
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Object[] o = m_excludeList.getSelectedValues();
                for (int i = 0; i < o.length; i++) {
                    m_included.addElement(o[i]);
                    m_excluded.removeElement(o[i]);
                }
            }
        });
        JButton addAll = new JButton("Add all >>");
        addAll.setMaximumSize(new Dimension(200, 20));
        addAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                for (int i = 0; i < m_excluded.getSize(); i++) {
                    m_included.addElement(m_excluded.getElementAt(i));
                }
                m_excluded.removeAllElements();
            }
        });
        JButton remove = new JButton("< Remove");
        remove.setMaximumSize(new Dimension(200, 20));
        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Object[] o = m_includeList.getSelectedValues();
                for (int i = 0; i < o.length; i++) {
                    m_excluded.addElement(o[i]);
                    m_included.removeElement(o[i]);
                }
            }
        });
        JButton removeAll = new JButton("<< Remove all");
        removeAll.setMaximumSize(new Dimension(200, 20));
        removeAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                for (int i = 0; i < m_included.getSize(); i++) {
                    m_excluded.addElement(m_included.getElementAt(i));
                }
                m_included.removeAllElements();
            }
        });

        Box buttonBox = Box.createVerticalBox();
        buttonBox.setBorder(BorderFactory.createTitledBorder("Select:"));
        buttonBox.setMinimumSize(new Dimension(200, 300));
        buttonBox.add(Box.createVerticalGlue());
        buttonBox.add(Box.createVerticalStrut(20));
        buttonBox.add(add);
        buttonBox.add(Box.createVerticalStrut(20));
        buttonBox.add(addAll);
        buttonBox.add(Box.createVerticalStrut(20));
        buttonBox.add(remove);
        buttonBox.add(Box.createVerticalStrut(20));
        buttonBox.add(removeAll);
        buttonBox.add(Box.createVerticalStrut(20));
        buttonBox.add(Box.createVerticalGlue());
        overall.add(buttonBox);

        // right included
        Box included = Box.createVerticalBox();
        included.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createLineBorder(Color.GREEN), "Included:"));
        m_includeList.setMinimumSize(new Dimension(200, 200));
        // force list to have fixed width
        m_includeList.setFixedCellWidth(200);
        JScrollPane inclScroller = new JScrollPane(m_includeList);
        included.add(inclScroller);
        overall.add(included);
        return overall;
    }

    /**
     * {@inheritDoc}
     *
     * If the nominal column selection changes, include and exclude lists are
     * cleared and all possible values of that column are put into the exclude
     * list.
     */
    @Override
    public void itemStateChanged(final ItemEvent item) {
        m_selectedColumn = (String)item.getItem();
        m_included.removeAllElements();
        m_excluded.removeAllElements();
        if (m_colAttributes.get(m_selectedColumn) != null) {
            for (DataCell dc : m_colAttributes.get(m_selectedColumn)) {
                m_excluded.addElement(dc.toString());
            }
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        if (specs.length == 0 || specs[0] == null) {
            throw new NotConfigurableException("No incoming columns found. "
                    + "Please connect the node with input table!");
        }
        // get selected column
        m_selectedColumn = settings.getString(CFG_SELECTED_COL, "");
        // get included possible values
        m_selectedAttributes = settings.getStringArray(CFG_SELECTED_ATTR, "");
        Set<String> includedAttr = new HashSet<String>();
        for (String s : m_selectedAttributes) {
            includedAttr.add(s);
        }
        // clear old values
        m_colAttributes.clear();
        m_columns.removeAllElements();
        m_included.removeAllElements();
        m_excluded.removeAllElements();
        // disable item state change listener while adding values
        m_columnSelection.removeItemListener(this);
        // fill the models
        for (DataColumnSpec colSpec : specs[0]) {
            if (colSpec.getType().isCompatible(NominalValue.class)
                    && colSpec.getDomain().hasValues()) {
                m_columns.addElement(colSpec.getName());
                // create column - possible values mapping
                m_colAttributes.put(colSpec.getName(), colSpec.getDomain()
                        .getValues());
            }
        }
        // set selection
        m_columnSelection.setSelectedItem(m_selectedColumn);
        // if it is not in the list, use first element
        m_selectedColumn = (String)m_columnSelection.getSelectedItem();
        if (m_colAttributes.get(m_selectedColumn) != null) {
            for (DataCell dc : m_colAttributes.get(m_selectedColumn)) {
                // if possible value was in the settings...
                if (includedAttr.contains(dc.toString())) {
                    // ... put it to included ...
                    m_included.addElement(dc.toString());
                } else {
                    // ... else to excluded
                    m_excluded.addElement(dc.toString());
                }
            }
        }
        // do layout
        m_includeList.revalidate();
        m_excludeList.revalidate();
        // enable item change listener again
        m_columnSelection.addItemListener(this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        settings.addString(CFG_SELECTED_COL, (String)m_columnSelection
                .getSelectedItem());
        String[] selected = new String[m_included.getSize()];
        for (int i = 0; i < selected.length; i++) {
            selected[i] = (String)m_included.getElementAt(i);
        }
        settings.addStringArray(CFG_SELECTED_ATTR, selected);
    }
}
