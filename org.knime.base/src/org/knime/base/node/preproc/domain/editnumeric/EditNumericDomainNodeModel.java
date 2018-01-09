/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * Created on Mar 17, 2013 by wiswedel
 */
package org.knime.base.node.preproc.domain.editnumeric;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.DoubleValueComparator;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * Enables the manual setting of numeric domains.
 *
 * @author Marcel Hanser
 */
final class EditNumericDomainNodeModel extends NodeModel {

    private static final DataValueComparator DOUBLE_VALUE_COMPARATOR = new DoubleValueComparator();

    private EditNumericDomainConfiguration m_configuration;

    /** One in, one out. */
    EditNumericDomainNodeModel() {
        super(1, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{processDomainSettings(inSpecs[0])};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        //

        final DataTableSpec originalTableSpec = inData[0].getDataTableSpec();
        DataTableSpec editedTableSpec = processDomainSettings(inData[0].getDataTableSpec());

        FilterResult filterResult = m_configuration.getColumnspecFilterConfig().applyTo(originalTableSpec);

        List<String> includedColumnNames = Arrays.asList(filterResult.getIncludes());

        long currentRowIndex = 0;
        long size = inData[0].size();
        String[] tableNames = originalTableSpec.getColumnNames();

        Map<Integer, DataColumnSpec> map = new HashMap<Integer, DataColumnSpec>();
        for (int i = 0; i < originalTableSpec.getNumColumns(); i++) {
            if (includedColumnNames.contains(tableNames[i])) {
                map.put(i, editedTableSpec.getColumnSpec(i));
            } else {
                map.put(i, originalTableSpec.getColumnSpec(i));
            }
        }
        CloseableRowIterator rowIterator = inData[0].iterator();
        long rowIndex = 0;
        try {
            while (rowIterator.hasNext()) {
                DataRow currentRow = rowIterator.next();
                exec.setProgress(currentRowIndex++ / (double)size, //
                    "checking domains of row: " + currentRow.getKey().getString());
                for (String colName : includedColumnNames) {
                    int currIndex = originalTableSpec.findColumnIndex(colName);
                    DataCell currCell = currentRow.getCell(currIndex);
                    if (!currCell.isMissing() && outOfDomain(currCell, map.get(currIndex))) {
                        switch (m_configuration.getDomainOverflowPolicy()) {
                            case CALCULATE_BOUNDS:
                                map.put(currIndex, calculateAndCreateBoundedColumnSpec(currCell, map.get(currIndex)));
                                break;
                            case USE_EXISTING_BOUNDS:
                                map.put(currIndex, originalTableSpec.getColumnSpec(currIndex));
                                break;
                            default:
                                throw new EditNumericDomainOverflowException(tableNames[currIndex],
                                    ((DoubleValue)currCell).getDoubleValue(), m_configuration.getLowerBound(),
                                    m_configuration.getUpperBound(), rowIndex, currentRow.getKey());

                        }

                    }
                }
                exec.checkCanceled();
                rowIndex++;
            }
        } finally {
            rowIterator.close();
        }

        DataTableSpecCreator newTableSpecCreator =
            new DataTableSpecCreator().setName(originalTableSpec.getName()).putProperties(
                originalTableSpec.getProperties());

        for (int i = 0; i < originalTableSpec.getNumColumns(); i++) {
            newTableSpecCreator.addColumns(map.get(i));
        }

        return new BufferedDataTable[]{exec.createSpecReplacerTable(inData[0], newTableSpecCreator.createSpec())};
    }

    /**
     * @param currCell
     * @param dataColumnSpec
     * @return
     */
    private DataColumnSpec calculateAndCreateBoundedColumnSpec(final DataCell currCell,
        final DataColumnSpec dataColumnSpec) {

        DataColumnSpecCreator creator = new DataColumnSpecCreator(dataColumnSpec);
        DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(dataColumnSpec.getDomain());
        if (!dataColumnSpec.getDomain().hasLowerBound()
            || DOUBLE_VALUE_COMPARATOR.compare(currCell, dataColumnSpec.getDomain().getLowerBound()) < 0) {
            domainCreator.setLowerBound(currCell);
        }
        if (!dataColumnSpec.getDomain().hasUpperBound()
            || DOUBLE_VALUE_COMPARATOR.compare(currCell, dataColumnSpec.getDomain().getUpperBound()) > 0) {
            domainCreator.setUpperBound(currCell);
        }

        creator.setDomain(domainCreator.createDomain());
        return creator.createSpec();

    }

    /**
     * @param currCell
     * @param dataColumnSpec
     * @return
     */
    private boolean outOfDomain(final DataCell currCell, final DataColumnSpec dataColumnSpec) {
        DataColumnDomain domain = dataColumnSpec.getDomain();
        return domain.hasBounds()
            && (DOUBLE_VALUE_COMPARATOR.compare(currCell, domain.getLowerBound()) < 0 || DOUBLE_VALUE_COMPARATOR
                .compare(currCell, domain.getUpperBound()) > 0);
    }

    private DataTableSpec processDomainSettings(final DataTableSpec dataTableSpec) throws InvalidSettingsException {
        if (m_configuration == null) {
            throw new InvalidSettingsException("Missing Configuration.");
        }

        EditNumericDomainConfiguration config = m_configuration;
        FilterResult filterResult = config.getColumnspecFilterConfig().applyTo(dataTableSpec);

        List<DataColumnSpec> newColumnSpecs = new ArrayList<DataColumnSpec>(dataTableSpec.getNumColumns());

        String[] columnNames = dataTableSpec.getColumnNames();

        Set<String> includeSet = new HashSet<String>();
        Collections.addAll(includeSet, filterResult.getIncludes());

        for (int i = 0; i < dataTableSpec.getNumColumns(); i++) {
            DataColumnSpec columnSpec = dataTableSpec.getColumnSpec(i);
            String columnName = columnNames[i];
            if (includeSet.contains(columnName)) {
                DataColumnSpecCreator columnSpecCreator = new DataColumnSpecCreator(columnSpec);
                DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(//
                    createCell(columnName, columnSpec.getType(), config.getLowerBound()), //
                    createCell(columnName, columnSpec.getType(), config.getUpperBound()));
                domainCreator.setValues(columnSpec.getDomain().getValues());

                columnSpecCreator.setDomain(domainCreator.createDomain());
                newColumnSpecs.add(columnSpecCreator.createSpec());

            } else {

                newColumnSpecs.add(columnSpec);
            }
        }

        StringBuilder warnings = new StringBuilder();

        if (includeSet.isEmpty()) {
            warnings.append("No columns are included.");
        }
        if (filterResult.getRemovedFromIncludes().length > 0) {
            warnings.append("\nFollowing columns are configured but no longer exist: "
                + ConvenienceMethods.getShortStringFrom(Arrays.asList(filterResult.getRemovedFromIncludes()), 5));
        }
        if (warnings.length() > 0) {
            setWarningMessage(warnings.toString());
        }

        return new DataTableSpecCreator(dataTableSpec).dropAllColumns()
            .addColumns(newColumnSpecs.toArray(new DataColumnSpec[newColumnSpecs.size()])).createSpec();
    }

    /**
     *
     * @param type
     * @param value
     * @return
     */
    private static DataCell createCell(final String columnName, final DataType type, final double value) {
        //
        if (DoubleCell.TYPE.equals(type)) {
            return new DoubleCell(value);
        } else if (IntCell.TYPE.equals(type)) {
            return new IntCell((int)value);
        } else if (LongCell.TYPE.equals(type)) {
            return new LongCell((long)value);
        }
        throw new IllegalArgumentException(String.format("%s has unsupported type: %s", columnName, type));
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.saveSettings(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        EditNumericDomainConfiguration editNumericDomainConfiguration = new EditNumericDomainConfiguration();
        editNumericDomainConfiguration.loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        EditNumericDomainConfiguration editNumericDomainConfiguration = new EditNumericDomainConfiguration();
        editNumericDomainConfiguration.loadConfigurationInModel(settings);
        m_configuration = editNumericDomainConfiguration;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no internals
    }

    /**
     * Defines the behavior if a row value of selected input columns exceeds the defined bounds.
     *
     * @author Marcel Hanser
     */
    public enum DomainOverflowPolicy {
        /**
         * Fails.
         */
        THROW_EXCEPTION("Fail"),
        /**
         * Calculates the bounds.
         */
        CALCULATE_BOUNDS("Calculate Bounds"),
        /**
         * Reuses given information.
         */
        USE_EXISTING_BOUNDS("Use existing Bounds");

        private String m_description;

        /**
         * @param message
         */
        private DomainOverflowPolicy(final String message) {
            this.m_description = message;
        }

        /**
         * @return the message
         */
        public String getDescription() {
            return m_description;
        }

    }
}
