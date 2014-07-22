/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   06.07.2014 (koetter): created
 */
package org.knime.base.data.aggregation.dialogutil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethodDecorator;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.dialogutil.AggregationMethodDecoratorTableCellRenderer.ValueRenderer;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.DataTypeListCellRenderer;

/**
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public class DataTypeAggregationPanel
extends AbstractAggregationPanel<DataTypeAggregationTableModel, DataTypeAggregator, DataType> {

    private final String m_key;

    /**
     * Constructor.
     * @param key the unique settings key
     */
    public DataTypeAggregationPanel(final String key) {
        super("Aggregation Settings", "Data types", new DataTypeListCellRenderer(), "Aggregation methods",
            new DataTypeAggregationTableModel());
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty");
        }
        m_key = key;
        initialize(getTypeList(), Collections.EMPTY_LIST, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {
        columnModel.getColumn(0).setCellRenderer(
            new AggregationMethodDecoratorTableCellRenderer(new ValueRenderer() {
                @Override
                public void renderComponent(final DefaultTableCellRenderer c,
                                            final AggregationMethodDecorator method) {
                    if (method instanceof DataTypeAggregator) {
                        final DataTypeAggregator aggregator = (DataTypeAggregator)method;
                        final DataType dataType = aggregator.getDataType();
                        c.setText(dataType.toString());
                        c.setIcon(dataType.getIcon());
                    }
                }
            }, true));
        columnModel.getColumn(1).setCellEditor(new DataTypeAggregatorTableCellEditor());
        columnModel.getColumn(1).setCellRenderer(
                new AggregationMethodDecoratorTableCellRenderer(new ValueRenderer() {
                    @Override
                    public void renderComponent(final DefaultTableCellRenderer c,
                                                final AggregationMethodDecorator method) {
                        c.setText(method.getLabel());
                    }
                }, false));
        columnModel.getColumn(0).setPreferredWidth(170);
        columnModel.getColumn(1).setPreferredWidth(150);
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
        appendMissingValuesEntry(menu);
        return menu;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTypeAggregator getOperator(final DataType selectedListElement) {
        List<AggregationMethod> compatibleMethods = AggregationMethods.getCompatibleMethods(selectedListElement);
        return new DataTypeAggregator(selectedListElement, compatibleMethods.iterator().next());
    }

    /**
     * @return the {@link List} of {@link DataType}s the user can choose from
     */
    private List<DataType> getTypeList() {
        final DataType generalType = DataType.getType(DataCell.class);
        final List<DataType> typeList = new LinkedList<>();
        typeList.add(generalType);
        typeList.add(BooleanCell.TYPE);
        typeList.add(IntCell.TYPE);
        typeList.add(LongCell.TYPE);
        typeList.add(DoubleCell.TYPE);
        typeList.add(StringCell.TYPE);
        typeList.add(DateAndTimeCell.TYPE);
        typeList.add(ListCell.getCollectionType(generalType));
        typeList.add(SetCell.getCollectionType(generalType));
        return typeList;
    }

    /**
     * @param settings {@link NodeSettingsRO}
     * @param spec {@link DataTableSpec}
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
    throws InvalidSettingsException {
        final List<DataType> typeList = getTypeList();
        final List<DataTypeAggregator> aggregators = DataTypeAggregator.loadAggregators(settings, m_key, spec);
        initialize(typeList, aggregators, spec);
    }

    /**
     * @param settings {@link NodeSettingsWO}
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        DataTypeAggregator.saveAggregators(settings, m_key, getTableModel().getRows());
    }
}
