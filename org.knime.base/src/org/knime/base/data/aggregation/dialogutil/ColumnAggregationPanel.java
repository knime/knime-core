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
 *    27.08.2008 (Tobias Koetter): created
 */

package org.knime.base.data.aggregation.dialogutil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableColumnModel;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.NamedAggregationOperator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;


/**
 * This class creates the aggregation column panel that allows the user to
 * define the aggregation columns and their aggregation method.
 *
 * @author Tobias Koetter, University of Konstanz
 * @since 2.6
 */
public class ColumnAggregationPanel extends AbstractAggregationPanel<
    ColumnAggregationTableModel, NamedAggregationOperator, AggregationMethod> {

    private DataType m_type = null;

    /**
     * @return the number of compatible methods
     */
    public int getCompatibleMethodsCount() {
        return getListModel().getSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JPopupMenu createTablePopupMenu() {
        final JPopupMenu menu = new JPopupMenu();
        if (getNoOfTableRows() == 0) {
            //the table contains no rows
            final JMenuItem item =
                new JMenuItem("No method(s) available");
            item.setEnabled(false);
            menu.add(item);
            return menu;
        }
        if (getNoOfSelectedRows() < getNoOfTableRows()) {
            //add this option only if at least one row is not selected
            final JMenuItem item =
                new JMenuItem("Select all");
            item.addActionListener(new ActionListener() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    selectAllSelectedMethods();
                }
            });
            menu.add(item);
        }
        if (getNoOfSelectedRows() > 0) {
            final JMenuItem nameItem =
                new JMenuItem("Revert selected names");
            nameItem.addActionListener(new ActionListener() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    revertSelectedMethodsNames();
                }
            });
            menu.add(nameItem);
        }
        appendMissingValuesEntry(menu);
        return menu;
    }

    /**
     * Reverts the name of all selected methods to their default name.
     */
    protected void revertSelectedMethodsNames() {
        final int[] selectedRows = getTable().getSelectedRows();
        getTableModel().revertOperatorNames(selectedRows);
    }

    /**Constructor for class AggregationColumnPanel.
     * @param title the title of the surrounding border or <code>null</code> if
     * no border should be used
     */
    public ColumnAggregationPanel(final String title) {
        super(title, " Aggregation methods ",
                new AggregationMethodListCellRenderer(), " To change multiple "
                + "columns use right mouse click for context menu. ",
                new ColumnAggregationTableModel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {
        columnModel.getColumn(0).setCellRenderer(
                new NamedAggregationMethodNameTableCellRenderer());
        columnModel.getColumn(0).setCellEditor(
                new NamedAggregationMethodNameTableCellEditor());
        columnModel.getColumn(0).setPreferredWidth(120);
        columnModel.getColumn(1).setPreferredWidth(45);
    }

    /**
     * Calling this method updates the panel to accept only methods that are
     * compatible with the given {@link DataType}.
     * @param newType the new {@link DataType} the {@link AggregationMethod}s
     * should be compatible with
     */
    public void updateType(final DataType newType) {
        if (m_type == newType) {
            return;
        }
        initialize(newType, getTableModel().getRows(), getInputTableSpec());
    }

    /**
     * @param cnfg the {@link Config} to write to
     * @see #saveSettingsTo(NodeSettingsWO)
     */
    @Deprecated
    public void saveSettingsTo(final Config cnfg) {
        NamedAggregationOperator.saveMethods((NodeSettingsWO)cnfg,
                getTableModel().getRows());
    }

    /**
     * @param cnfg the {@link Config} to read from
     * @param type the {@link DataType} the methods should support
     * @throws InvalidSettingsException if the settings are invalid
     * @see #loadSettingsFrom(NodeSettingsRO, DataType, DataTableSpec)
     */
    @Deprecated
    public void loadSettingsFrom(final Config cnfg, final DataType type)
    throws InvalidSettingsException {
        initialize(type, NamedAggregationOperator.loadOperators(
                      (NodeSettingsRO)cnfg, null), null);
    }

    /**
     * @param settings the {@link NodeSettingsRO} to write to
     * @since 2.7
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        NamedAggregationOperator.saveMethods(settings,
                getTableModel().getRows());
    }

    /**
     * @param settings the {@link NodeSettingsRO} to read from
     * @param type the {@link DataType} the methods should support
     * @param spec the input {@link DataTableSpec}
     * @throws InvalidSettingsException if the settings are invalid
     * @since 2.7
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
             final DataType type, final DataTableSpec spec)
    throws InvalidSettingsException {
        initialize(type, NamedAggregationOperator.loadOperators(settings, spec), spec);
    }

    /**
     * Initializes the panel.
     * @param type the {@link DataType} the methods should support
     * @param methods the {@link List} of {@link NamedAggregationOperator}s
     * that are initially used
     * @param spec input {@link DataTableSpec}
     * @since 2.7
     */
    public void initialize(final DataType type,
            final List<NamedAggregationOperator> methods,
            final DataTableSpec spec) {
        m_type = type;
        //update the compatible methods list
        final List<NamedAggregationOperator> methods2Use =
            new ArrayList<NamedAggregationOperator>(methods.size());
        if (m_type != null) {
            //remove selected methods that are not compatible with the new type
            for (final NamedAggregationOperator method : methods) {
                if (method.isCompatible(m_type)) {
                    methods2Use.add(method);
                }
            }
            super.initialize(AggregationMethods.getCompatibleMethods(m_type),
                    methods2Use, spec);
        }
    }

    /**
     * @return the {@link DataType} that defines the content of the panel
     */
    protected DataType getType() {
        return m_type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NamedAggregationOperator getOperator(
            final AggregationMethod method) {
        //create a new instance to guarantee that each aggregation operator
        //uses its own instance
        final AggregationMethod clonedMethod =
            AggregationMethods.getMethod4Id(method.getId());
        return new NamedAggregationOperator(clonedMethod);
    }
}
