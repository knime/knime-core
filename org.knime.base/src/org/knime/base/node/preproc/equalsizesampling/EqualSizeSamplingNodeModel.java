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
package org.knime.base.node.preproc.equalsizesampling;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class EqualSizeSamplingNodeModel extends NodeModel {

    private EqualSizeSamplingConfiguration m_configuration;

    /**  */
    public EqualSizeSamplingNodeModel() {
        super(1, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_configuration == null) {
            EqualSizeSamplingConfiguration c = new EqualSizeSamplingConfiguration();
            try {
                c.loadConfigurationInDialog(
                        new NodeSettings("empty"), inSpecs[0]);
            } catch (NotConfigurableException e) {
                throw new InvalidSettingsException("Can't auto-guess settings: "
                        + "No nominal column in input");
            }
            m_configuration = c;
            setWarningMessage("Auto-guessing \"" + c.getClassColumn()
                    + "\" as selected nominal class column");
        }
        String classColumn = m_configuration.getClassColumn();
        DataColumnSpec col = inSpecs[0].getColumnSpec(classColumn);
        if (col == null) {
            throw new InvalidSettingsException("No column \"" + classColumn
                    + "\" in input data");
        }
        if (!col.getType().isCompatible(NominalValue.class)) {
            throw new InvalidSettingsException("Class column \"" + classColumn
                    + "\" is not a nominal attribute");
        }
        return new DataTableSpec[] {inSpecs[0]};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable in = inData[0];
        final Map<DataCell, OccurrenceCounter> countMap =
            new LinkedHashMap<DataCell, OccurrenceCounter>();
        ExecutionMonitor countExec = exec.createSubProgress(0.5);
        ExecutionMonitor createExec = exec.createSubProgress(0.5);

        // first step: count occurrences
        exec.setMessage("Counting occurrences");
        final int colIndex = in.getDataTableSpec().findColumnIndex(
                m_configuration.getClassColumn());
        int rowIndex = 0;
        final int rowCount = in.getRowCount();
        for (DataRow r : in) {
            countExec.setProgress(rowIndex / (double)rowCount,
                    "Row \"" + r.getKey() + "\" (" + (rowIndex++)
                    + "/" + rowCount + ")");
            countExec.checkCanceled();
            DataCell value = r.getCell(colIndex);
            OccurrenceCounter oc = countMap.get(value);
            if (oc == null) {
                if (countMap.size() > 100000) {
                    throw new Exception("Too many different values in column \""
                            + m_configuration.getClassColumn() + "\"");
                }
                countMap.put(value, new OccurrenceCounter(value));
            } else {
                oc.incrementCount();
            }
        }

        int minOccurrence = Integer.MAX_VALUE;
        for (OccurrenceCounter oc : countMap.values()) {
            minOccurrence = Math.min(minOccurrence, oc.getCount());
        }

        // second step
        exec.setMessage("Assemble Output");
        Long s = m_configuration.getSeed();
        Random rand = new Random(s == null ? System.currentTimeMillis() : s);
        final Map<DataCell, OccurrenceCounter.Sampler> samplerMap =
            new LinkedHashMap<DataCell, OccurrenceCounter.Sampler>();
        for (Map.Entry<DataCell, OccurrenceCounter> e : countMap.entrySet()) {
            samplerMap.put(e.getKey(), e.getValue().createSampler(
                    minOccurrence, m_configuration.getSamplingMethod(), rand));
        }

        BufferedDataContainer cont = exec.createDataContainer(in.getSpec());
        rowIndex = 0;
        for (DataRow r : in) {
            createExec.setProgress(rowIndex / (double)rowCount,
                    "Row \"" + r.getKey() + "\" (" + (rowIndex++)
                    + "/" + rowCount + ")");
            createExec.checkCanceled();
            DataCell value = r.getCell(colIndex);
            OccurrenceCounter.Sampler sampler = samplerMap.get(value);
            if (sampler.includeNext()) {
                cont.addRowToTable(r);
            }
        }
        cont.close();
        return new BufferedDataTable[] {cont.getTable()};
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new EqualSizeSamplingConfiguration().loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        EqualSizeSamplingConfiguration c = new EqualSizeSamplingConfiguration();
        c.loadConfigurationInModel(settings);
        m_configuration = c;
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
