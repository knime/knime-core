/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *    27.08.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby.dialogutil;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

import org.knime.base.node.preproc.groupby.GroupByNodeDialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;


/**
 * This class creates the aggregation column panel that allows the user to
 * define the aggregation columns and their aggregation method.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class AggregationColumnPanel {

    private static final int BUTTON_WIDTH = 125;

    private static final int COMPONENT_HEIGHT = 155;

    private final JPanel m_panel = new JPanel();

    private final List<DataColumnSpec> m_avAggrColSpecs =
        new LinkedList<DataColumnSpec>();

    private final DefaultListModel m_avAggrColListModel =
            new DefaultListModel();

    private final JList m_avAggrColList;

    private final AggregationColumnTableModel m_aggrColTableModel
        = new AggregationColumnTableModel();

    private final JTable m_aggrColTable;

    /**Constructor for class AggregationColumnPanel.
     *
     */
    public AggregationColumnPanel() {

        m_avAggrColList = new JList(m_avAggrColListModel);
//        m_avAggrColList.setVisibleRowCount(8);
        m_avAggrColList.setCellRenderer(new DataColumnSpecListCellRenderer());

        m_aggrColTable = new JTable(m_aggrColTableModel);
        m_aggrColTable.setFillsViewportHeight(true);
        m_aggrColTable.getTableHeader().setReorderingAllowed(false);
        m_aggrColTable.getColumnModel().getColumn(0).setCellRenderer(
                new DataColumnSpecTableCellRenderer());
        m_aggrColTable.getColumnModel().getColumn(1).setCellEditor(
                new AggregationMethodTableCellEditor(m_aggrColTableModel));
        m_aggrColTable.getColumnModel().getColumn(1).setCellRenderer(
                new AggregationMethodTableCellRenderer());

        final Dimension dimension = new Dimension(650, 200);
        m_panel.setMinimumSize(dimension);
        m_panel.setPreferredSize(dimension);
        m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.X_AXIS));
        final Box rootBox = new Box(BoxLayout.X_AXIS);
        final Border border = BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), " Aggregation settings ");
        rootBox.setBorder(border);
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(createAggrColBox());
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(createButtonBox());
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(createAggrColTable());
        rootBox.add(Box.createHorizontalGlue());
        m_panel.add(rootBox);
    }

    private Box createAggrColBox() {
        final Box aggrColBox = new Box(BoxLayout.X_AXIS);
        final Border border =
            BorderFactory.createTitledBorder(" Available columns ");
        aggrColBox.setBorder(border);
        aggrColBox.add(Box.createHorizontalGlue());
        final JScrollPane avColList = new JScrollPane(m_avAggrColList);
        final Dimension dimension = new Dimension(150, COMPONENT_HEIGHT);
        avColList.setMinimumSize(dimension);
        avColList.setPreferredSize(dimension);
        aggrColBox.add(avColList);
        aggrColBox.add(Box.createHorizontalGlue());
        return aggrColBox;
    }

    private Component createButtonBox() {
        final Box buttonBox = new Box(BoxLayout.Y_AXIS);
        buttonBox.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        buttonBox.add(Box.createVerticalGlue());

        final JButton addButton = new JButton("add >>");
        addButton.setMaximumSize(new Dimension(BUTTON_WIDTH, 25));
        buttonBox.add(addButton);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddIt();
            }
        });
        buttonBox.add(Box.createVerticalGlue());

        final JButton addAllButton = new JButton("add all >>");
        addAllButton.setMaximumSize(new Dimension(BUTTON_WIDTH, 25));
        buttonBox.add(addAllButton);
        addAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddAll();
            }
        });
        buttonBox.add(Box.createVerticalGlue());

        final JButton remButton = new JButton("<< remove");
        remButton.setMaximumSize(new Dimension(BUTTON_WIDTH, 25));
        buttonBox.add(remButton);
        remButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemIt();
            }
        });
        buttonBox.add(Box.createVerticalGlue());

        final JButton remAllButton = new JButton("<< remove all");
        remAllButton.setMaximumSize(new Dimension(BUTTON_WIDTH, 25));
        buttonBox.add(remAllButton);
        remAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemAll();
            }
        });
        buttonBox.add(Box.createVerticalGlue());
        return buttonBox;
    }

    private Component createAggrColTable() {
        final JScrollPane pane = new JScrollPane(m_aggrColTable);
        final int width =
            (int)Math.ceil(
                    (GroupByNodeDialog.COMPONENT_WIDTH - BUTTON_WIDTH) / 1.5);
        final Dimension dimension =
            new Dimension(width , COMPONENT_HEIGHT);
        pane.setMinimumSize(dimension);
        pane.setPreferredSize(dimension);
        return pane;
    }

    /**
     * Removes all columns from the aggregation column table.
     */
    protected void onRemAll() {
        m_aggrColTableModel.removeAll();
    }

    /**
     * Removes the selected columns from the aggregation column table.
     */
    protected void onRemIt() {
        m_aggrColTableModel.removeColumn(m_aggrColTable.getSelectedRows());
    }

    /**
     *  Adds all columns to the aggregation column table.
     */
    protected void onAddAll() {
        final DataColumnSpec[] specs =
            new DataColumnSpec[m_avAggrColListModel.getSize()];
        for (int i = 0, size = m_avAggrColListModel.getSize(); i < size; i++) {
            specs[i] = (DataColumnSpec)m_avAggrColListModel.get(i);
        }
        m_aggrColTableModel.addColumn(specs);
    }

    /**
     * Adds all selected columns to the aggregation column table.
     */
    protected void onAddIt() {
        final Object[] values = m_avAggrColList.getSelectedValues();
        if (values == null || values.length < 1) {
            return;
        }
        final DataColumnSpec[] specs = new DataColumnSpec[values.length];
        for (int i = 0, length = values.length; i < length; i++) {
            specs[i] = (DataColumnSpec)values[i];
        }
        m_aggrColTableModel.addColumn(specs);
    }

    /**
     * @return the panel in which all sub-components of this component are
     *         arranged. This panel can be added to the dialog pane.
     */
    public JPanel getComponentPanel() {
        return m_panel;
    }

    /**
     * @param groupCols the name of all columns that are used for grouping
     */
    public void groupColsChange(final Collection<String> groupCols) {
        final Set<String> groupColSet =
            new HashSet<String>(groupCols);
        //update the available aggregation column list
        m_avAggrColListModel.removeAllElements();
        for (final DataColumnSpec colSpec : m_avAggrColSpecs) {
            if (!groupColSet.contains(colSpec.getName())) {
                m_avAggrColListModel.addElement(colSpec);
            }
        }
        //remove all group columns from the aggregation column table
        m_aggrColTableModel.removeColumns(groupCols);
    }

    /**
     * @param settings the settings object to write to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_aggrColTableModel.saveSettingsTo(settings);
    }

    /**
     * @param settings the settings to read from
     * @param spec initializes the component
     * @param groupCols the group by columns for compatibility
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec spec, final List<String> groupCols) {
        m_avAggrColSpecs.clear();
        for (final DataColumnSpec colSpec : spec) {
            m_avAggrColSpecs.add(colSpec);
        }
        m_aggrColTableModel.loadSettingsFrom(settings, spec, groupCols);
    }
}
