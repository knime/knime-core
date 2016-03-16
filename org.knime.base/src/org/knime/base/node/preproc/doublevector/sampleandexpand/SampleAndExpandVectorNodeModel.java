/*
 * ------------------------------------------------------------------------
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
 * Created on 2014.03.20. by gabor
 */
package org.knime.base.node.preproc.doublevector.sampleandexpand;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.data.vector.stringvector.StringVectorValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;

/**
 * This is the model implementation for a node which samples and expands a double vector
 * to individual double columns.
 *
 * @author M. Berthold
 * @since 3.2
 */
public class SampleAndExpandVectorNodeModel extends NodeModel {

    private int[] m_samplingScheme = null;
    private enum VType { String, Double };
    private VType m_vectorType = null;

    /* static factory methods for the SettingsModels used here and in the NodeDialog. */
    /**
     * @return the settings model used to store the source column name.
     */
    static SettingsModelString createColSelectSettingsModel() {
        return new SettingsModelString("SelectedColumn", null);
    }
    private final SettingsModelString m_selColumn = createColSelectSettingsModel();

    static SettingsModelInteger createNrColumnsSettingsModel() {
        return new SettingsModelInteger("NrSampledColumns", 100);
    }
    private final SettingsModelInteger m_nrSampledCols = createNrColumnsSettingsModel();

    static SettingsModelInteger createRandomSeedSettingsModel() {
        return new SettingsModelInteger("Random Seed", 43);
    }
    private final SettingsModelInteger m_randomSeed = createRandomSeedSettingsModel();

    /**
     * Initialize model. One Data Inport, two Data Outports.
     */
    protected SampleAndExpandVectorNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.NONDISTRIBUTED, OutputPortRole.NONDISTRIBUTED};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // user settings must be set and valid
        assert m_selColumn.getStringValue() != null;
        // selected column should exist in input table
        if (!inSpecs[0].containsName(m_selColumn.getStringValue())) {
            throw new InvalidSettingsException("Selected column '"
                    + m_selColumn.getStringValue() + "' does not exist in input table!");
        }
        // and it should be of type double vector
        if (inSpecs[0].getColumnSpec(m_selColumn.getStringValue()).getType().isCompatible(DoubleVectorValue.class)) {
            m_vectorType = VType.Double;
        } else if (inSpecs[0].getColumnSpec(m_selColumn.getStringValue()).getType().isCompatible(DoubleVectorValue.class)) {
            m_vectorType = VType.String;
        } else {
            throw new InvalidSettingsException("Selected column '"
                    + m_selColumn.getStringValue() + "' does not contain double or string vectors!");
        }
        // checks passed - let's start doing the real stuff
        return new DataTableSpec[]{createColumnRearranger(inSpecs[0], m_vectorType).createSpec(), createdSecondSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
        BufferedDataTable outTable = exec.createColumnRearrangeTable(inData[0],
            createColumnRearranger(inData[0].getDataTableSpec(), m_vectorType), exec);
//        RowOutput rowOutput1 = new BufferedDataTableRowOutput(outTable);
//        RowInput rowInput = new DataTableRowInput(inData[0]);
//        this.execute(rowInput, rowOutput1, rowOutput2, inData[0].size(), exec);
        // create second output
        List<String> colNames = inData[0].getDataTableSpec().getColumnSpec(m_selColumn.getStringValue()).getElementNames();
        BufferedDataContainer sampledColsTable = exec.createDataContainer(createdSecondSpec());
        for (int i = 0; i < m_samplingScheme.length; i++) {
            int ix = m_samplingScheme[i];
            String colName = colNames.get(ix);
            sampledColsTable.addRowToTable(
                new DefaultRow(RowKey.createRowKey((long)i), new IntCell(ix), new StringCell(colName)));
        }
        sampledColsTable.close();
        return new BufferedDataTable[]{outTable, sampledColsTable.getTable()};
    }

    /**
     * Creates the ColumnRearranger for the re-arranger table. Also used to compute the output table spec.
     *
     * @param inTableSpec the spec of the source table
     * @return the column rearranger
     */
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inTableSpec, final VType vtype)
            throws InvalidSettingsException {
        int sourceColumnIndex = inTableSpec.findColumnIndex(m_selColumn.getStringValue());
        DataColumnSpec sourceSpec = inTableSpec.getColumnSpec(sourceColumnIndex);
        if (m_samplingScheme == null) {
            m_samplingScheme = generateSamplingSchema(sourceSpec.getElementNames().size(),
                m_nrSampledCols.getIntValue());
        }
        ColumnRearranger c = new ColumnRearranger(inTableSpec);
        DataColumnSpec[] newSpecs;
        newSpecs = IntStream.range(0, m_nrSampledCols.getIntValue()).mapToObj(
            i -> new DataColumnSpecCreator(sourceSpec.getElementNames().get(m_samplingScheme[i]),
                                           m_vectorType.equals(VType.Double) ? DoubleCell.TYPE : StringCell.TYPE
                                               ).createSpec()).toArray(DataColumnSpec[]::new);
        c.append(new AbstractCellFactory(true, newSpecs) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                DataCell dc = row.getCell(sourceColumnIndex);
                if ((m_vectorType.equals(VType.Double) && dc instanceof DoubleVectorValue)
                        || (m_vectorType.equals(VType.String) && dc instanceof StringVectorValue)) {
                    DataCell[] res = new DataCell[m_nrSampledCols.getIntValue()];
                    for (int i = 0; i < m_nrSampledCols.getIntValue(); i++) {
                        res[i] = m_vectorType.equals(VType.Double) ?
                              new DoubleCell(((DoubleVectorValue)dc).getValue(m_samplingScheme[i]))
                            : new StringCell(((StringVectorValue)dc).getValue(m_samplingScheme[i]));
                    }
                    return res;
                }
                return new MissingCell[100];
            }
        });
        return c;
    }

    private DataTableSpec createdSecondSpec() {
        DataTableSpecCreator dtsc = new DataTableSpecCreator();
        dtsc.setName("Used Columns");
        dtsc.addColumns(new DataColumnSpecCreator("Index", IntCell.TYPE).createSpec());
        dtsc.addColumns(new DataColumnSpecCreator("Column Name", StringCell.TYPE).createSpec());
        return dtsc.createSpec();
    }

    private int[] generateSamplingSchema(final int nrCols, final int nrSamples) {
        RandomDataGenerator r = new RandomDataGenerator();
        r.reSeed(m_randomSeed.getIntValue());
        int[] sampledIndices = r.nextPermutation(nrCols, nrSamples);
        Arrays.sort(sampledIndices);
        return sampledIndices;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // delete previous random samping scheme so that a new one is selected upon configure.
        m_samplingScheme = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_selColumn.saveSettingsTo(settings);
        m_nrSampledCols.saveSettingsTo(settings);
        m_randomSeed.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selColumn.validateSettings(settings);
        m_nrSampledCols.validateSettings(settings);
        m_randomSeed.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selColumn.loadSettingsFrom(settings);
        m_nrSampledCols.loadSettingsFrom(settings);
        m_randomSeed.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do.
    }
}
