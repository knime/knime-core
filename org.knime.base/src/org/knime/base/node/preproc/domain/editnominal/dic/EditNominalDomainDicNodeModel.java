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
package org.knime.base.node.preproc.domain.editnominal.dic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CloseableRowIterator;
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
 * Enables the manual adding of nominal values to a domains.
 *
 * @author Marcel Hanser
 */
final class EditNominalDomainDicNodeModel extends NodeModel {

    private EditNominalDomainDicConfiguration m_configuration;

    /** Two in, one out. */
    EditNominalDomainDicNodeModel() {
        super(2, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec orgSpec = inSpecs[0];
        DataTableSpec valueSpec = inSpecs[1];

        return new DataTableSpec[]{mergeTableSpecs(orgSpec,
            createNewSpec(orgSpec, valueSpec, new NewDomainValuesAdder<InvalidSettingsException>())).createSpec()};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        final BufferedDataTable additionalValues = inData[1];

        final DataTableSpec orgSpec = inData[0].getDataTableSpec();
        final DataTableSpec valueSpec = additionalValues.getDataTableSpec();

        Map<Integer, Set<DataCell>> doCreateSpec =
            createNewSpec(orgSpec, valueSpec, new NewDomainValuesAdder<CanceledExecutionException>() {

                private long m_currentRowIndex = 0;

                private final long m_size = additionalValues.size();

                @Override
                void addNewDomainValues(final Map<Integer, String> orgIndexToColNameMap,
                    final Map<Integer, Set<DataCell>> orgIndexToNewDomainValuesMap) throws CanceledExecutionException {

                    CloseableRowIterator addValuesIterator = additionalValues.iterator();

                    try {
                        Collection<String> values = orgIndexToColNameMap.values();
                        while (addValuesIterator.hasNext()) {

                            DataRow currRow = addValuesIterator.next();

                            exec.setProgress(m_currentRowIndex++ / (double)m_size, //
                                "adding values to domain of row: " + currRow.getKey().getString());

                            for (String addValRowId : values) {
                                DataCell cell = currRow.getCell(valueSpec.findColumnIndex(addValRowId));
                                if (!cell.isMissing()) {
                                    orgIndexToNewDomainValuesMap.get(orgSpec.findColumnIndex(addValRowId)).add(cell);
                                }
                            }
                            exec.checkCanceled();
                        }

                    } finally {
                        addValuesIterator.close();
                    }
                }
            });

        return new BufferedDataTable[]{exec.createSpecReplacerTable(inData[0], mergeTableSpecs(orgSpec, doCreateSpec)
            .createSpec())};
    }

    private <E extends Exception> Map<Integer, Set<DataCell>> createNewSpec(final DataTableSpec origSpec,
        final DataTableSpec valueSpec, final NewDomainValuesAdder<E> adder) throws InvalidSettingsException, E {

        // create a default configuration if it does not exist.
        EditNominalDomainDicConfiguration configuration = createDefaultConfigIfNotExist(origSpec, valueSpec);

        // receive the matching columns and check for existence of these in the input data table.
        Map<Integer, String> findMatchingColumns =
            checkAndFindMatchingColumns(configuration.getFilterConfiguration().applyTo(valueSpec), origSpec, valueSpec,
                m_configuration.isIgnoreDomainColumns());

        // creates the new data spec.
        Map<Integer, Set<DataCell>> newDataSpec = createColNewDomainValsMap(findMatchingColumns);

        if (!configuration.isAddNewValuesFirst()) {
            addExistingDomainValues(newDataSpec, findMatchingColumns, origSpec, valueSpec);
        }

        checkTypesAndAddValueTableDomainValuesContainedInSpec(origSpec, valueSpec, findMatchingColumns, newDataSpec);

        // now add the actual values of the domain value table [2]
        adder.addNewDomainValues(findMatchingColumns, newDataSpec);

        if (configuration.isAddNewValuesFirst()) {
            addExistingDomainValues(newDataSpec, findMatchingColumns, origSpec, valueSpec);
        }

        checkSize(newDataSpec, origSpec);

        return newDataSpec;
    }

    /**
     * Checks the size of each entry and throws an Exception if the amount of possible values is bigger than the defined
     * maximum.
     *
     * @param newDataSpec
     * @throws InvalidSettingsException
     */
    private void checkSize(final Map<Integer, Set<DataCell>> newDataSpec, final DataTableSpec orgSpec)
        throws InvalidSettingsException {
        for (Map.Entry<Integer, Set<DataCell>> entry : newDataSpec.entrySet()) {
            if (entry.getValue().size() > m_configuration.getMaxDomainValues()) {
                throw new InvalidSettingsException(String.format("Amount of possible values: %d of column '%s' "
                    + "\nis greater than the defined maximum: %d", entry.getValue().size(),
                    orgSpec.getColumnNames()[entry.getKey()], m_configuration.getMaxDomainValues()));
            }
        }
    }

    /**
     * @param inSpecs
     * @param config
     * @param origSpec
     * @param valueSpec
     * @param orgIndexToNewValMap
     * @throws InvalidSettingsException
     */
    private void checkTypesAndAddValueTableDomainValuesContainedInSpec(final DataTableSpec origSpec,
        final DataTableSpec valueSpec, final Map<Integer, String> orgColIndexToColName,
        final Map<Integer, Set<DataCell>> orgColIndexToDomainVals) throws InvalidSettingsException {

        EditNominalDomainDicConfiguration config = createDefaultConfigIfNotExist(origSpec, valueSpec);
        FilterResult filterResult = config.getFilterConfiguration().applyTo(valueSpec);

        List<Integer> toRemoveBecauseTypeIncompatible = new ArrayList<Integer>();
        // check the correct data type and add the additional domain values if there already some.
        for (Map.Entry<Integer, String> entry : orgColIndexToColName.entrySet()) {
            DataType orgType = origSpec.getColumnSpec(entry.getKey()).getType();
            DataType valueType = valueSpec.getColumnSpec(entry.getValue()).getType();

            if (!orgType.equals(valueType)) {

                if (!config.isIgnoreWrongTypes()) {
                    throw new InvalidSettingsException(String.format(Locale.US,
                        "matching column '%s' has incompatible types: "
                            + "input table [1] type: %s, domain value table type [2]: %s",
                        origSpec.getColumnNames()[entry.getKey()], orgType, valueType));
                } else {
                    //  ignore this column on continue
                    toRemoveBecauseTypeIncompatible.add(entry.getKey());
                }

            } else {

                Set<DataCell> values = valueSpec.getColumnSpec(entry.getValue()).getDomain().getValues();

                if (values != null) {
                    orgColIndexToDomainVals.get(entry.getKey()).addAll(values);
                }
            }
        }

        for (Integer i : toRemoveBecauseTypeIncompatible) {
            orgColIndexToDomainVals.remove(i);
            orgColIndexToColName.remove(i);
        }

        StringBuilder warnings = new StringBuilder();

        if (filterResult.getIncludes().length == 0) {
            warnings.append("No columns in value table [2] are included.");
        }
        if (filterResult.getRemovedFromIncludes().length > 0) {
            warnings.append("\nFollowing columns are configured but no longer exist: "
                + ConvenienceMethods.getShortStringFrom(Arrays.asList(filterResult.getRemovedFromIncludes()), 5));
        }
        if (warnings.length() > 0) {
            setWarningMessage(warnings.toString());
        }
    }

    /**
     * @param findMatchingColumns
     * @return
     */
    private static Map<Integer, Set<DataCell>>
        createColNewDomainValsMap(final Map<Integer, String> findMatchingColumns) {
        Map<Integer, Set<DataCell>> toReturn = new HashMap<Integer, Set<DataCell>>();
        for (Integer i : findMatchingColumns.keySet()) {
            toReturn.put(i, new LinkedHashSet<DataCell>());
        }
        return toReturn;
    }

    /**
     * Called when its time to add the domain values to the table spec. <code>E</code> determines some generics magic to
     * get rid of unnecessary try/catch blocks.
     *
     *
     * @author Marcel Hanser
     */
    private static class NewDomainValuesAdder<E extends Exception> {

        /**
         * Called when its time to add the domain values to the table spec.
         *
         * @param orgIndexToColNameMap
         * @param orgIndexToNewDomainValuesMap
         */
        void addNewDomainValues(final Map<Integer, String> orgIndexToColNameMap,
            final Map<Integer, Set<DataCell>> orgIndexToNewDomainValuesMap) throws E {

        }
    }

    /**
     * @param orgSpec
     * @param orgIndexToNewDomainValuesMap
     * @return
     */
    private static DataTableSpecCreator mergeTableSpecs(final DataTableSpec orgSpec,
        final Map<Integer, Set<DataCell>> orgIndexToNewDomainValuesMap) {
        DataTableSpecCreator newSpecCreator = new DataTableSpecCreator(orgSpec).dropAllColumns();

        for (int i = 0; i < orgSpec.getNumColumns(); i++) {
            if (orgIndexToNewDomainValuesMap.containsKey(i)) {
                DataColumnSpec orgDataSpec = orgSpec.getColumnSpec(i);
                DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(orgDataSpec);
                DataColumnDomainCreator yetAnotherCreator = new DataColumnDomainCreator(orgDataSpec.getDomain());
                yetAnotherCreator.setValues(orgIndexToNewDomainValuesMap.get(i));
                dataColumnSpecCreator.setDomain(yetAnotherCreator.createDomain());
                newSpecCreator.addColumns(dataColumnSpecCreator.createSpec());
            } else {
                newSpecCreator.addColumns(orgSpec.getColumnSpec(i));
            }
        }
        return newSpecCreator;
    }

    /**
     * Returns a mapping between the orgSpec column to the corresponding column name and checks if the columns exist in
     * the original table, if not either a EditNominalDomainDicColumnNotExistsException is thrown or the column is
     * ignored.
     *
     * @param orgSpec
     * @param valueSpec
     * @param includedCols
     * @param matchingCols
     * @throws InvalidSettingsException
     */
    private static Map<Integer, String> checkAndFindMatchingColumns(final FilterResult filterResult,
        final DataTableSpec orgSpec, final DataTableSpec valueSpec, final boolean ignoreMissingCol)
        throws InvalidSettingsException {

        Map<Integer, String> toReturn = new HashMap<Integer, String>();
        for (String includedCol : filterResult.getIncludes()) {

            int orgColumnIndex = orgSpec.findColumnIndex(includedCol);
            if (orgColumnIndex != -1) {
                toReturn.put(orgSpec.findColumnIndex(includedCol), includedCol);
            } else {
                if (!ignoreMissingCol) {
                    throw new InvalidSettingsException(String.format(Locale.US,
                        "Configured column: '%s', does exist in domain value table [2] but not in the input table [1]",
                        includedCol));
                }
            }
        }

        return toReturn;
    }

    private static void addExistingDomainValues(final Map<Integer, Set<DataCell>> toFill,
        final Map<Integer, String> orgIndexToColName, final DataTableSpec orgSpec, final DataTableSpec valueSpec) {

        for (Integer i : orgIndexToColName.keySet()) {
            Set<DataCell> values = orgSpec.getColumnSpec(i).getDomain().getValues();
            if (values != null) {
                toFill.get(i).addAll(values);
            }
        }
    }

    /**
     * @param addValSpec
     * @return
     */
    private EditNominalDomainDicConfiguration createDefaultConfigIfNotExist(final DataTableSpec origSpec,
        final DataTableSpec addValSpec) {
        if (m_configuration == null) {
            m_configuration = new EditNominalDomainDicConfiguration();
            m_configuration.guessDefaultColumnFilter(origSpec, addValSpec);
            StringBuilder builder = new StringBuilder("Guessing default settings.");
            String[] includes = m_configuration.getFilterConfiguration().applyTo(addValSpec).getIncludes();
            if (includes != null && includes.length > 0) {
                builder.append("\nThe included columns are:\n");
                builder.append(ConvenienceMethods.getShortStringFrom(Arrays.asList(includes), 10));
            }
            setWarningMessage(builder.toString());
        }
        return m_configuration;
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
            m_configuration.saveConfiguration(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        EditNominalDomainDicConfiguration editNumericDomainConfiguration = new EditNominalDomainDicConfiguration();
        editNumericDomainConfiguration.loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        EditNominalDomainDicConfiguration editNumericDomainConfiguration = new EditNominalDomainDicConfiguration();
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
}
