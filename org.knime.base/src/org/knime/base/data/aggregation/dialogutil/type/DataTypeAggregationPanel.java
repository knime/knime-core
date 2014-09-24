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
package org.knime.base.data.aggregation.dialogutil.type;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.dialogutil.AbstractAggregationPanel;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionAndRowTableCellRenderer;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRowTableCellRenderer;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRowTableCellRenderer.ValueRenderer;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
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
    /**The default title of the panel to display in a dialog.*/
    public static final String DEFAULT_TITLE = "Type Based Aggregation";

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
        initialize(getTypeList(null), Collections.EMPTY_LIST, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {
        columnModel.getColumn(0).setCellRenderer(
            new AggregationFunctionRowTableCellRenderer<>(new ValueRenderer<DataTypeAggregator>() {
                @Override
                public void renderComponent(final DefaultTableCellRenderer c, final DataTypeAggregator row) {
                    final DataType dataType = row.getDataType();
                    c.setText(dataType.toString());
                    c.setIcon(dataType.getIcon());
                }
            }, true, "Double click to remove data type. Right mouse click for context menu."));
        columnModel.getColumn(1).setCellEditor(new DataTypeAggregatorTableCellEditor());
        columnModel.getColumn(1).setCellRenderer(new AggregationFunctionAndRowTableCellRenderer());
        columnModel.getColumn(0).setPreferredWidth(170);
        columnModel.getColumn(1).setPreferredWidth(150);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JPopupMenu createTablePopupMenu() {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem invalidRowsMenu = createInvalidRowsSelectionMenu();
        if (invalidRowsMenu != null) {
            menu.add(invalidRowsMenu);
            menu.addSeparator();
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
                    selectAllRows();
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
    protected DataTypeAggregator createRow(final DataType selectedListElement) {
        List<AggregationMethod> compatibleMethods = AggregationMethods.getCompatibleMethods(selectedListElement);
        return new DataTypeAggregator(selectedListElement, compatibleMethods.iterator().next());
    }

    /**
     * @param spec
     * @return the {@link List} of {@link DataType}s the user can choose from
     */
    private List<DataType> getTypeList(final DataTableSpec spec) {
        final Set<DataType> types = new HashSet<>();
        final DataType generalType = DataType.getType(DataCell.class);
        types.add(generalType);
        types.add(BooleanCell.TYPE);
        types.add(IntCell.TYPE);
        types.add(LongCell.TYPE);
        types.add(DoubleCell.TYPE);
        types.add(StringCell.TYPE);
        types.add(DateAndTimeCell.TYPE);
        types.add(ListCell.getCollectionType(generalType));
        types.add(SetCell.getCollectionType(generalType));
        if (spec != null) {
            for (DataColumnSpec colSpec : spec) {
                types.add(colSpec.getType());
            }
        }
        final List<DataType> typeList = new ArrayList<>(types);
        Collections.sort(typeList, DataTypeNameSorter.getInstance());
        return typeList;
    }

    /**
     * @param settings {@link NodeSettingsRO}
     * @param spec {@link DataTableSpec}
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
    throws InvalidSettingsException {
        final List<DataType> typeList = getTypeList(spec);
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
