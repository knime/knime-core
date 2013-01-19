/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   03.06.2012 (kilian): created
 */
package org.knime.base.data.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

/**
 * This utility class provides means to extract meta information from a
 * specific data table spec and to return the extracted data as data table.
 * The extracted information consists of the column names, type, and indices,
 * the lower and upper bounds, if available (otherwise missing values), the
 * information whether there are color, size, or shape handler associated with
 * the columns and the possible values as collection cell. It can be set
 * whether the information of property handlers as well as the possible values
 * are extracted or not.
 *
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 * @since 2.6
 */
public class DataTableSpecExtractor {
    private boolean m_extractPropertyHanlders = false;

    private boolean m_possibleValuesAsCollection = false;

    /**
     * Empty constructor of <code>DataTableSpecExtractor</code>.
     */
    public DataTableSpecExtractor() { /* empty */ }

    /**
     * @param extractPropertyHanlders The option whether the property handlers
     * will be extracted (<code>true</code>) or not (<code>false</code>).
     */
    public void setExtractPropertyHandlers(
            final boolean extractPropertyHanlders) {
        m_extractPropertyHanlders = extractPropertyHanlders;
    }

    /**
     * @param extractPossibleValuesAsCollection The option whether the
     * possible values will be extracted (<code>true</code>) or not
     * (<code>false</code>).
     */
    public void setExtractPossibleValuesAsCollection(
            final boolean extractPossibleValuesAsCollection) {
        m_possibleValuesAsCollection = extractPossibleValuesAsCollection;
    }

    /**
     * Creates and returns a data table containing the meta information of the
     * given spec. The meta information is referred to as the table data
     * specification and contains information such as column names, types,
     * domain values (list of possible values for categorical columns) and
     * lower and upper bounds. It also contains the information which of the
     * columns have a view handler associated, as well the possible values, if
     * specified. Each column in the given table spec is represented as a row
     * in the returned table.
     *
     * @param spec The spec to extract the meta information from.
     * @return The data table containing the meta information of the given spec.
     */
    public DataTable extract(final DataTableSpec spec) {
        DataContainer dc = new DataContainer(createSpec(spec));

        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);

            List<DataCell> cells = new ArrayList<DataCell>(8);
            cells.add(new StringCell(colSpec.getName()));
            cells.add(new StringCell(colSpec.getType().toString()));
            cells.add(new IntCell(i));

            if (m_extractPropertyHanlders) {
                if (colSpec.getColorHandler() != null) {
                    cells.add(BooleanCell.TRUE);
                } else {
                    cells.add(BooleanCell.FALSE);
                }
                if (colSpec.getSizeHandler() != null) {
                    cells.add(BooleanCell.TRUE);
                } else {
                    cells.add(BooleanCell.FALSE);
                }

                if (colSpec.getShapeHandler() != null) {
                    cells.add(BooleanCell.TRUE);
                } else {
                    cells.add(BooleanCell.FALSE);
                }
            }

            DataCell lb = colSpec.getDomain().getLowerBound();
            if (lb != null) {
                cells.add(lb);
            } else {
                cells.add(DataType.getMissingCell());
            }
            DataCell ub = colSpec.getDomain().getUpperBound();
            if (ub != null) {
                cells.add(ub);
            } else {
                cells.add(DataType.getMissingCell());
            }

            if (m_possibleValuesAsCollection) {
                Set<DataCell> vals = colSpec.getDomain().getValues();
                if (vals != null) {
                    cells.add(CollectionCellFactory.createSetCell(vals));
                } else {
                    cells.add(CollectionCellFactory.createSetCell(
                            new HashSet<DataCell>()));
                }
            }

            dc.addRowToTable(new DefaultRow(
                    new RowKey(colSpec.getName()), cells));
        }
        dc.close();
        return dc.getTable();
    }


    /**
     * Creates and returns the spec of the data table containing the meta
     * information of the given spec.
     * @param spec The spec containing the meta information to extract.
     * @return The spec of the data table containing the extracted meta
     * information.
     */
    private DataTableSpec createSpec(final DataTableSpec spec) {
        int cols = 5;
        if (m_extractPropertyHanlders) {
            cols += 3;
        }
        if (m_possibleValuesAsCollection) {
            cols += 1;
        }
        DataColumnSpec[] colSpecs = new DataColumnSpec[cols];
        int indexAdd = 0;

        colSpecs[0] = new DataColumnSpecCreator("Column Name",
                StringCell.TYPE).createSpec();
        colSpecs[1] = new DataColumnSpecCreator("Column Type",
                StringCell.TYPE).createSpec();
        colSpecs[2] = new DataColumnSpecCreator("Column Index",
                IntCell.TYPE).createSpec();
        if (m_extractPropertyHanlders) {
            indexAdd = 3;
            colSpecs[3] = new DataColumnSpecCreator("Color Handler",
                    BooleanCell.TYPE).createSpec();
            colSpecs[4] = new DataColumnSpecCreator("Size Handler",
                    BooleanCell.TYPE).createSpec();
            colSpecs[5] = new DataColumnSpecCreator("Shape Handler",
                    BooleanCell.TYPE).createSpec();
        }
        colSpecs[3 + indexAdd] = new DataColumnSpecCreator("Lower Bound",
                DoubleCell.TYPE).createSpec();
        colSpecs[4 + indexAdd] = new DataColumnSpecCreator("Upper Bound",
                DoubleCell.TYPE).createSpec();
        if (m_possibleValuesAsCollection) {
            colSpecs[5 + indexAdd] = new DataColumnSpecCreator(
                    "Possible Values",
                    SetCell.getCollectionType(StringCell.TYPE)).createSpec();
        }

        return new DataTableSpec(colSpecs);
    }
}
