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
package org.knime.base.node.preproc.domain.editnominal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * Enables the manual managing of possible domain values.
 *
 * @author Marcel Hanser
 */
final class EditNominalDomainNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(EditNominalDomainNodeModel.class);

    private EditNominalDomainConfiguration m_configuration;

    /** One in, one out. */
    EditNominalDomainNodeModel() {
        super(1, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{sortPossibleValues(inSpecs[0])};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        return new BufferedDataTable[]{exec.createSpecReplacerTable(inData[0],
            sortPossibleValues(sortPossibleValues(inData[0].getDataTableSpec())))};
    }

    private DataTableSpec sortPossibleValues(final DataTableSpec orgSpec) throws InvalidSettingsException {

        if (m_configuration == null) {
            throw new InvalidSettingsException("Missing Configuration.");
        }

        Set<String> configuredColumns = new HashSet<String>(m_configuration.getConfiguredColumns());
        String[] columnNames = orgSpec.getColumnNames();

        DataTableSpecCreator creator = new DataTableSpecCreator(orgSpec).dropAllColumns();

        for (int i = 0; i < orgSpec.getNumColumns(); i++) {
            String name = columnNames[i];
            if (configuredColumns.remove(name)) {
                DataColumnSpec orgDataSpec = orgSpec.getColumnSpec(i);
                if (!StringCell.TYPE.equals(orgDataSpec.getType())) {

                    CheckUtils.checkSetting(m_configuration.isIgnoreWrongTypes(),
                        "Column '%s' must be of type '%s' \nbut was of type: '%s'", name, StringCell.TYPE,
                        orgDataSpec.getType());

                    creator.addColumns(orgDataSpec);

                } else {

                    DataColumnDomain domain = orgDataSpec.getDomain();
                    DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(orgDataSpec);
                    DataColumnDomainCreator yetAnotherCreator =
                        new DataColumnDomainCreator(domain.getLowerBound(), domain.getUpperBound());

                    List<DataCell> sorting = new ArrayList<DataCell>(m_configuration.getSorting(name));

                    Set<DataCell> difference = diff(domain.getValues(), sorting);

                    yetAnotherCreator.setValues(resolveNewValues(sorting, difference));

                    dataColumnSpecCreator.setDomain(yetAnotherCreator.createDomain());
                    creator.addColumns(dataColumnSpecCreator.createSpec());
                }
            } else {
                creator.addColumns(orgSpec.getColumnSpec(i));
            }
        }

        if (!configuredColumns.isEmpty()) {
            String missingColumnsString =
                "Following columns are configured but no longer exist: \n"
                    + ConvenienceMethods.getShortStringFrom(configuredColumns, 5);

            CheckUtils.checkSetting(m_configuration.isIgnoreNotExistingColumns(), missingColumnsString);

            setWarningMessage(missingColumnsString);
        }

        return creator.createSpec();
    }

    /**
     * @param sorting
     * @param difference
     * @return
     */
    private Set<DataCell> resolveNewValues(final List<DataCell> sorting, final Set<DataCell> difference) {
        int lastIndexOf = sorting.lastIndexOf(EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL);

        if (lastIndexOf == -1) {
            LOGGER.assertLog(false, "Unkown cell placeholder is not included!");
            return new LinkedHashSet<DataCell>(sorting);
        }

        sorting.remove(lastIndexOf);
        sorting.addAll(lastIndexOf, difference);

        return new LinkedHashSet<DataCell>(sorting);
    }

    /**
     * Returns the relative complement set of the given collections as an immutable set. In other words it computes
     * <code>a\b</code>.
     *
     * @param a the a
     * @param b the b
     * @return returns <code>a\b</code>.
     */
    static Set<DataCell> diff(final Collection<DataCell> a, final Collection<DataCell> b) {
        if (a == null || a.isEmpty()) {
            return Collections.emptySet();
        }
        Set<DataCell> toReturn = new LinkedHashSet<DataCell>();

        for (DataCell val : a) {
            if (!b.contains(val)) {
                toReturn.add(val);
            }
        }
        return Collections.unmodifiableSet(toReturn);
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
        EditNominalDomainConfiguration editNumericDomainConfiguration = new EditNominalDomainConfiguration();
        editNumericDomainConfiguration.loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        EditNominalDomainConfiguration editNumericDomainConfiguration = new EditNominalDomainConfiguration();
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
