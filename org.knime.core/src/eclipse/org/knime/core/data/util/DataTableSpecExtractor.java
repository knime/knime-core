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
package org.knime.core.data.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
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
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ShapeHandler;
import org.knime.core.data.property.SizeHandler;

/**
 * This utility class provides means to extract meta information from a specific data table spec and to return the
 * extracted data as data table. The extracted information consists of the column names, type, and indices, the lower
 * and upper bounds, if available (otherwise missing values), the information whether there are color, size, or shape
 * handler associated with the columns and the possible values as collection cell. It can be set whether the information
 * of property handlers as well as the possible values are extracted or not.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.8
 * @noextend This class is not intended to be subclassed by clients.
 */
public class DataTableSpecExtractor {

    private PropertyHandlerOutputFormat m_propertyHandlerOutputFormat = PropertyHandlerOutputFormat.Hide;

    private PossibleValueOutputFormat m_possibleValueOutputFormat = PossibleValueOutputFormat.Hide;

    private boolean m_extractColumnNameAsColumn = true;

    /** How the color, shape, size handler are shown in the table. */
    public enum PropertyHandlerOutputFormat {
        /** Don't show. */
        Hide,
        /** Show as flag (true if present, false if not). */
        Boolean,
        /** Use property handlers toString() method, use "" if not set. */
        ToString;
    }

    /** Defines how possible values will be represented. */
    public enum PossibleValueOutputFormat {
        /** Don't show. */
        Hide,
        /** As single collection column. */
        Collection,
        /** As sequence of columns. */
        Columns
    }

    /** ...
     * @param f ...
     */
    public final void setPropertyHandlerOutputFormat(final PropertyHandlerOutputFormat f) {
        if (f == null) {
            throw new NullPointerException();
        }
        m_propertyHandlerOutputFormat = f;
    }

    /** ...
     * @param f ...
     */
    public final void setPossibleValueOutputFormat(final PossibleValueOutputFormat f) {
        if (f == null) {
            throw new NullPointerException();
        }
        m_possibleValueOutputFormat = f;
    }

    /**
     * @param value the extractColumnNameAsColumn to set (default is true)
     */
    public void setExtractColumnNameAsColumn(final boolean value) {
        m_extractColumnNameAsColumn = value;
    }

    private static final DataType GENERIC_DATA_TYPE = DataType.getType(DataCell.class);

    /**
     * Creates and returns a data table containing the meta information of the given spec. The meta information is
     * referred to as the table data specification and contains information such as column names, types, domain values
     * (list of possible values for categorical columns) and lower and upper bounds. It also contains the information
     * which of the columns have a view handler associated, as well the possible values, if specified. Each column in
     * the given table spec is represented as a row in the returned table.
     *
     * @param spec The spec to extract the meta information from.
     * @return The data table containing the meta information of the given spec.
     */
    public DataTable extract(final DataTableSpec spec) {
        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();

        if (m_extractColumnNameAsColumn) {
            colSpecs.add(new DataColumnSpecCreator("Column Name", StringCell.TYPE).createSpec());
        }
        colSpecs.add(new DataColumnSpecCreator("Column Type", StringCell.TYPE).createSpec());
        colSpecs.add(new DataColumnSpecCreator("Column Index", IntCell.TYPE).createSpec());
        switch (m_propertyHandlerOutputFormat) {
            case Hide:
                break;
            case Boolean:
                colSpecs.add(new DataColumnSpecCreator("Color Handler", BooleanCell.TYPE).createSpec());
                colSpecs.add(new DataColumnSpecCreator("Size Handler", BooleanCell.TYPE).createSpec());
                colSpecs.add(new DataColumnSpecCreator("Shape Handler", BooleanCell.TYPE).createSpec());
                break;
            default:
                colSpecs.add(new DataColumnSpecCreator("Color Handler", StringCell.TYPE).createSpec());
                colSpecs.add(new DataColumnSpecCreator("Size Handler", StringCell.TYPE).createSpec());
                colSpecs.add(new DataColumnSpecCreator("Shape Handler", StringCell.TYPE).createSpec());
        }
        DataType lowerBoundColType = null; // likely number (important for sorting)
        DataType upperBoundColType = null;
        for (DataColumnSpec c : spec) {
            DataColumnDomain domain = c.getDomain();
            if (domain.hasLowerBound()) {
                DataType curLowerType = domain.getLowerBound().getType();
                if (lowerBoundColType == null) {
                    lowerBoundColType = curLowerType;
                } else {
                    lowerBoundColType = DataType.getCommonSuperType(lowerBoundColType, curLowerType);
                }
            }
            if (domain.hasUpperBound()) {
                DataType curUpperType = domain.getUpperBound().getType();
                if (upperBoundColType == null) {
                    upperBoundColType = curUpperType;
                } else {
                    upperBoundColType = DataType.getCommonSuperType(upperBoundColType, curUpperType);
                }
            }
        }
        lowerBoundColType = lowerBoundColType == null ? GENERIC_DATA_TYPE : lowerBoundColType;
        upperBoundColType = upperBoundColType == null ? GENERIC_DATA_TYPE : upperBoundColType;
        colSpecs.add(new DataColumnSpecCreator("Lower Bound", lowerBoundColType).createSpec());
        colSpecs.add(new DataColumnSpecCreator("Upper Bound", upperBoundColType).createSpec());
        int maxPossValues = 0;
        switch (m_possibleValueOutputFormat) {
            case Hide:
                break;
            case Collection:
                colSpecs.add(new DataColumnSpecCreator("Possible Values",
                           SetCell.getCollectionType(GENERIC_DATA_TYPE)).createSpec());
                break;
            default:
                for (DataColumnSpec c : spec) {
                    DataColumnDomain domain = c.getDomain();
                    if (domain.hasValues()) {
                        maxPossValues = Math.max(domain.getValues().size(), maxPossValues);
                    }
                }
                for (int i = 0; i < maxPossValues; i++) {
                    colSpecs.add(new DataColumnSpecCreator("Value " + i, GENERIC_DATA_TYPE).createSpec());
                }
        }


        /* fill it */
        DataContainer dc = new DataContainer(new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()])));
        for (int i = 0; i < spec.getNumColumns(); i++) {
            DataColumnSpec colSpec = spec.getColumnSpec(i);

            List<DataCell> cells = new ArrayList<DataCell>();
            if (m_extractColumnNameAsColumn) {
                cells.add(new StringCell(colSpec.getName()));
            }
            cells.add(new StringCell(colSpec.getType().toString()));
            cells.add(new IntCell(i));

            ColorHandler colorHandler = colSpec.getColorHandler();
            SizeHandler sizeHandler = colSpec.getSizeHandler();
            ShapeHandler shapeHandler = colSpec.getShapeHandler();
            switch (m_propertyHandlerOutputFormat) {
                case Hide:
                    break;
                case Boolean:
                    cells.add(BooleanCell.get(colorHandler != null));
                    cells.add(BooleanCell.get(sizeHandler != null));
                    cells.add(BooleanCell.get(shapeHandler != null));
                    break;
                default:
                    cells.add(new StringCell(colorHandler == null ? "" : colorHandler.toString()));
                    cells.add(new StringCell(sizeHandler == null ? "" : sizeHandler.toString()));
                    cells.add(new StringCell(shapeHandler == null ? "" : shapeHandler.toString()));
            }

            DataColumnDomain domain = colSpec.getDomain();
            DataCell lb = domain.getLowerBound();
            if (lb != null) {
                cells.add(lb);
            } else {
                cells.add(DataType.getMissingCell());
            }
            DataCell ub = domain.getUpperBound();
            if (ub != null) {
                cells.add(ub);
            } else {
                cells.add(DataType.getMissingCell());
            }

            switch (m_possibleValueOutputFormat) {
                case Hide:
                    break;
                case Collection:
                    if (domain.hasValues()) {
                        cells.add(CollectionCellFactory.createSetCell(domain.getValues()));
                    } else {
                        cells.add(DataType.getMissingCell());
                    }
                    break;
                default:
                    Set<DataCell> set = domain.hasValues() ? domain.getValues() : Collections.EMPTY_SET;
                    int nrColsToWrite = maxPossValues;
                    for (DataCell c: set) {
                        cells.add(c);
                        nrColsToWrite -= 1;
                    }
                    while (nrColsToWrite > 0) {
                        cells.add(DataType.getMissingCell());
                        nrColsToWrite -= 1;
                    }
            }
            dc.addRowToTable(new DefaultRow(new RowKey(colSpec.getName()), cells));
        }
        dc.close();
        return dc.getTable();
    }


}
