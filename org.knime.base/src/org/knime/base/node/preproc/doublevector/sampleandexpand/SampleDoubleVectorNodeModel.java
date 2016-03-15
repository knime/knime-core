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


import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;

/**
 * This is the model implementation for a node which samples and expands a double vector
 * to individual double columns.
 *
 * @author M. Berthold
 * @since 3.2
 */
public class SampleDoubleVectorNodeModel extends SimpleStreamableFunctionNodeModel {

    /* static factory methods for the SettingsModels used here and in the NodeDialog. */
    /**
     * @return the settings model used to store the source column name.
     */
    static SettingsModelString createColSelectSettingsModel() {
        return new SettingsModelString("SelectedColumn", null);
    }
    private final SettingsModelString m_selColumn = createColSelectSettingsModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // selected column should exist in input table
        if (!inSpecs[0].containsName(m_selColumn.getStringValue())) {
            throw new InvalidSettingsException("Selected column '"
                    + m_selColumn.getStringValue() + "' does not exist in input table!");
        }
        // and it should be of type double vector
        if (!inSpecs[0].getColumnSpec(m_selColumn.getStringValue()).getType().isCompatible(DoubleVectorValue.class)) {
            throw new InvalidSettingsException("Selected column '"
                    + m_selColumn.getStringValue() + "' does not contain double vectors!");
        }
        // Create re-arranger, retrieve spec, and return.
        DataTableSpec outSpec = createColumnRearranger(inSpecs[0]).createSpec();
        return new DataTableSpec[]{outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        BufferedDataTable outTable = exec.createColumnRearrangeTable(inData[0],
                        createColumnRearranger(inData[0].getDataTableSpec()),
                        exec);
        return new BufferedDataTable[]{outTable};
    }

    /**
     * Creates the ColumnRearranger for the re-arranger table. Also used to compute the output table spec.
     *
     * @param inTableSpec the spec of the source table
     * @return the column rearranger
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inTableSpec)
            throws InvalidSettingsException {
        // user settings must be set and valid
        assert m_selColumn.getStringValue() != null;
        ColumnRearranger c = new ColumnRearranger(inTableSpec);
        DataColumnSpec[] newSpecs;
        newSpecs = IntStream.range(0,100).mapToObj(
            i -> new DataColumnSpecCreator("val"+i, DoubleCell.TYPE).createSpec()).toArray(DataColumnSpec[]::new);
        c.append(new AbstractCellFactory(newSpecs) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                return new DoubleCell[100];
            }
        });
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_selColumn.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selColumn.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selColumn.loadSettingsFrom(settings);
    }

}
