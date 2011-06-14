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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.columnmerge;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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

/**
 * Model to column merger.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ColumnMergerNodeModel extends NodeModel {

    private ColumnMergerConfiguration m_configuration;

    /** One in, one out. */
    public ColumnMergerNodeModel() {
        super(1, 1);
    }

    /** Creates column rearranger doing all the work.
     * @param spec The input spec.
     * @return The rearranger creating the output table/spec. */
    private ColumnRearranger createColumnRearranger(
            final DataTableSpec spec) throws InvalidSettingsException {
        ColumnMergerConfiguration cfg = m_configuration;
        if (cfg == null) {
            throw new InvalidSettingsException("No settings available");
        }
        final int primColIndex = spec.findColumnIndex(cfg.getPrimaryColumn());
        final int secColIndex = spec.findColumnIndex(cfg.getSecondaryColumn());
        if (primColIndex < 0) {
            throw new InvalidSettingsException(
                    "No such primary column: " + cfg.getPrimaryColumn());
        }
        if (secColIndex < 0) {
            throw new InvalidSettingsException(
                    "No such secondary column: " + cfg.getSecondaryColumn());
        }
        DataColumnSpec c1 = spec.getColumnSpec(primColIndex);
        DataColumnSpec c2 = spec.getColumnSpec(secColIndex);
        DataType commonType = DataType.getCommonSuperType(
                c1.getType(), c2.getType());
        String name;
        switch (cfg.getOutputPlacement()) {
        case ReplacePrimary:
        case ReplaceBoth:
            name = c1.getName();
            break;
        case ReplaceSecondary:
            name = c2.getName();
            break;
        case AppendAsNewColumn:
            name = DataTableSpec.getUniqueColumnName(spec, cfg.getOutputName());
            break;
        default:
            throw new InvalidSettingsException(
                    "Coding problem: unhandled case");
        }
        DataColumnSpec outColSpec =
            new DataColumnSpecCreator(name, commonType).createSpec();
        SingleCellFactory fac = new SingleCellFactory(outColSpec) {
            /** {@inheritDoc} */
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell1 = row.getCell(primColIndex);
                DataCell cell2 = row.getCell(secColIndex);
                return !cell1.isMissing() ? cell1 : cell2;
            }
        };
        ColumnRearranger result = new ColumnRearranger(spec);
        switch (cfg.getOutputPlacement()) {
        case ReplacePrimary:
            result.replace(fac, primColIndex);
            break;
        case ReplaceBoth:
            result.replace(fac, primColIndex);
            result.remove(secColIndex);
            break;
        case ReplaceSecondary:
            result.replace(fac, secColIndex);
            break;
        case AppendAsNewColumn:
            result.append(fac);
            break;
        default:
            throw new InvalidSettingsException(
                    "Coding problem: unhandled case");
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[] {rearranger.createSpec()};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec spec = inData[0].getDataTableSpec();
        ColumnRearranger rearranger = createColumnRearranger(spec);
        BufferedDataTable out = exec.createColumnRearrangeTable(
                inData[0], rearranger, exec);
        return new BufferedDataTable[] {out};
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
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ColumnMergerConfiguration c = new ColumnMergerConfiguration();
        c.loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ColumnMergerConfiguration c = new ColumnMergerConfiguration();
        c.loadConfigurationInModel(settings);
        m_configuration = c;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

}
