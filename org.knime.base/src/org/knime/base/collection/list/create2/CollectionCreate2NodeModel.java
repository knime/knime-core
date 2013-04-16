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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 1, 2008 (wiswedel): created
 */
package org.knime.base.collection.list.create2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class CollectionCreate2NodeModel extends NodeModel {

    private SettingsModelColumnFilter2 m_includeModel;

    // if true, a SetCell is created, otherwise a ListCell
    private final SettingsModelBoolean m_createSet;

    private final SettingsModelBoolean m_removeCols;

    private final SettingsModelString m_newColName;

    private final SettingsModelBoolean m_ignoreMissing;

    /**
     *
     */
    public CollectionCreate2NodeModel() {
        super(1, 1);
        m_createSet = createSettingsModelSetOrList();
        m_removeCols = createSettingsModelRemoveCols();
        m_newColName = createSettingsModelColumnName();
        m_ignoreMissing = createSettingsModelIgnoreMissing();
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_includeModel == null) {
            // auto-configure
            m_includeModel = createSettingsModel();
            m_includeModel.loadDefaults(inSpecs[0]);
            setWarningMessage("Auto configuration: Using all suitable "
                    + "columns (in total " + m_includeModel.applyTo(inSpecs[0]).getIncludes().length + ")");
        }
        ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
        try {
            DataTableSpec outspec = rearranger.createSpec();
            return new DataTableSpec[]{outspec};
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }

    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger rearranger =
                createColumnRearranger(inData[0].getDataTableSpec());
        BufferedDataTable out =
                exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        return new BufferedDataTable[]{out};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec in)
            throws InvalidSettingsException {

        FilterResult filterResult = m_includeModel.applyTo(in);
        List<String> includes = Arrays.asList(filterResult.getIncludes());

        if (includes == null || includes.isEmpty()) {
            throw new InvalidSettingsException("Select columns to aggregate");
        }
        String[] names = includes.toArray(new String[includes.size()]);
        final int[] colIndices = new int[names.length];
        for (int i = 0; i < names.length; i++) {
            int index = in.findColumnIndex(names[i]);
            if (index < 0) {
                throw new InvalidSettingsException("No column \"" + names[i]
                        + "\" in input table");
            }
            colIndices[i] = index;
        }
        DataType comType = CollectionCellFactory.getElementType(in, colIndices);
        String newColName = m_newColName.getStringValue();
        DataType type;
        if (m_createSet.getBooleanValue()) {
            type = SetCell.getCollectionType(comType);
        } else {
            type = ListCell.getCollectionType(comType);
        }
        DataColumnSpecCreator newColSpecC =
                new DataColumnSpecCreator(newColName, type);
        newColSpecC.setElementNames(names);
        DataColumnSpec newColSpec = newColSpecC.createSpec();
        CellFactory appendFactory = new SingleCellFactory(newColSpec) {
            /** {@inheritDoc} */
            @Override
            public DataCell getCell(final DataRow row) {
                int[] validColIndices = colIndices;

                // filter out columns indices with missing values
                // iff ignore missing value is switched on
                if (m_ignoreMissing.getBooleanValue()) {
                    List<Integer> vCI = new ArrayList<Integer>();
                    for (int i = 0; i < row.getNumCells(); i++) {
                        if (!row.getCell(i).isMissing()) {
                            vCI.add(i);
                        }
                    }
                    validColIndices = buildIntArray(vCI);
                }

                // create collection cells from row
                // based on given column indices
                if (m_createSet.getBooleanValue()) {
                    return CollectionCellFactory.createSetCell(
                            row, validColIndices);
                } else {
                    return CollectionCellFactory.createListCell(
                            row, validColIndices);
                }
            }
        };
        ColumnRearranger rearranger = new ColumnRearranger(in);
        if (m_removeCols.getBooleanValue()) {
            rearranger.remove(colIndices);
        }
        rearranger.append(appendFactory);
        return rearranger;
    }

    /**
     * Creates an int array from an int list.
     * @param integers The int list to create int array out of.
     * @return an int array created form an int list.
     * @since 2.6
     */
    private int[] buildIntArray(final List<Integer> integers) {
        int[] ints = new int[integers.size()];
        int i = 0;
        for (Integer n : integers) {
            ints[i++] = n;
        }
        return ints;
    }


    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no loads here
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (m_includeModel == null) {
            m_includeModel = createSettingsModel();
        }
        m_includeModel.loadSettingsFrom(settings);
        m_createSet.loadSettingsFrom(settings);
        m_removeCols.loadSettingsFrom(settings);
        m_newColName.loadSettingsFrom(settings);

        try {
            m_ignoreMissing.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            // just catch for the sake of downwards compatibility
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // nothing to reset
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to save here
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_includeModel != null) {
            m_includeModel.saveSettingsTo(settings);
            m_createSet.saveSettingsTo(settings);
            m_removeCols.saveSettingsTo(settings);
            m_newColName.saveSettingsTo(settings);
            m_ignoreMissing.saveSettingsTo(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SettingsModelColumnFilter2 includeModel =
            m_includeModel == null ? createSettingsModel() : m_includeModel;
        includeModel.validateSettings(settings);
        m_createSet.validateSettings(settings);
        m_removeCols.validateSettings(settings);
        m_newColName.validateSettings(settings);

        try {
            m_ignoreMissing.validateSettings(settings);
        } catch (InvalidSettingsException e) {
            // just catch for the sake of downwards compatibility
        }
    }

    /**
     * Create settings model collection create node.
     *
     * @return a new settings object.
     */
    static SettingsModelColumnFilter2 createSettingsModel() {
        return new SettingsModelColumnFilter2("includes");
    }

    /**
     * Create settings model for flag to create SetCell or ListCell.
     *
     * @return a new settings model object
     */
    static SettingsModelBoolean createSettingsModelSetOrList() {
        return new SettingsModelBoolean("createSet", false);
    }

    /**
     * Create settings model for flag to remove aggregated columns.
     *
     * @return a new settings model instance
     */
    static SettingsModelBoolean createSettingsModelRemoveCols() {
        return new SettingsModelBoolean("removeCols", false);
    }

    /**
     * Creates settings model holding the name of the new column.
     *
     * @return a new settings model instance
     */
    static SettingsModelString createSettingsModelColumnName() {
        return new SettingsModelString("newColName", "AggregatedValues");
    }

    /**
     * Creates settings model holding the "ignore missing values" flag.
     *
     * @return a new settings model instance
     * @since 2.6
     */
    static final SettingsModelBoolean createSettingsModelIgnoreMissing() {
        return new SettingsModelBoolean("ignoreMissingValue", false);
    }
}
