/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   Jan 31, 2018 (ortmann): created
 */
package org.knime.base.node.stats.outlier.applier;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.base.algorithms.outlier.OutlierPortObject;
import org.knime.base.algorithms.outlier.OutlierReviser;
import org.knime.base.algorithms.outlier.listeners.Warning;
import org.knime.base.algorithms.outlier.listeners.WarningListener;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * Model to identify outliers based on interquartile ranges.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class OutlierApplyNodeModel extends NodeModel implements WarningListener {

    /** Init the outlier detector node model with one input and output. */
    OutlierApplyNodeModel() {
        super(new PortType[]{OutlierPortObject.TYPE, BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final OutlierPortObject outlierPort = (OutlierPortObject)inData[0];
        final BufferedDataTable in = (BufferedDataTable)inData[1];

        OutlierReviser outlierReviser = outlierPort.getOutRevBuilder().build();
        outlierReviser.addListener(this);

        outlierReviser.treatOutliers(exec, in, outlierPort.getOutlierModel(in.getDataTableSpec()));
        return new PortObject[]{outlierReviser.getOutTable(), outlierReviser.getSummaryTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec outlierPortSpec = (DataTableSpec)inSpecs[0];
        final DataTableSpec inTableSpec = (DataTableSpec)inSpecs[1];

        // ensure that the in data table contains the group columns that were used to learn the outlier reviser
        final String[] groupColNames = OutlierPortObject.getGroupColNames(outlierPortSpec);
        if (!Arrays.stream(groupColNames)//
            .allMatch(inTableSpec::containsName)) {
            throw new InvalidSettingsException(Arrays.stream(groupColNames).collect(Collectors.joining(", ",
                "Outlier detector used group(s) (", ") which does not, or only partially, exist in the table")));
        }

        // check if the data type for the groups differs between those the model was trained on and the input table
        final String[] groupSpecNames = OutlierPortObject.getGroupSpecNames(outlierPortSpec);
        final String[] wrongDataType = IntStream.range(0, groupColNames.length)//
            .filter(i -> outlierPortSpec.getColumnSpec(groupSpecNames[i]).getType() != inTableSpec
                .getColumnSpec(groupColNames[i]).getType())//
            .mapToObj(i -> groupColNames[i])//
            .toArray(String[]::new);
        if (wrongDataType.length != 0) {
            throw new InvalidSettingsException(Arrays.stream(wrongDataType)//
                .collect(Collectors.joining(", ", "The data type for column(s) (",
                    ") differs between the input table and that the model was created from")));
        }

        // get the outlier column names stored in the port spec
        final String[] outlierColNames = OutlierPortObject.getOutlierColNames(outlierPortSpec);

        // check for outlier columns that are missing the input table
        final List<String> nonExistOrCompatibleOutliers = Arrays.stream(outlierColNames)
            .filter(s -> (!inTableSpec.containsName(s)
                || !inTableSpec.getColumnSpec(s).getType().isCompatible(DoubleValue.class)))//
            .collect(Collectors.toList());
        // if all of them  are missing throw an exception
        if (outlierColNames.length == nonExistOrCompatibleOutliers.size()) {
            throw new InvalidSettingsException(Arrays.stream(outlierColNames)//
                .collect(Collectors.joining(", ", "The model was created for columns (",
                    ") which does not exist in the table or is not compatible")));
        }
        if (nonExistOrCompatibleOutliers.size() > 0) {
            setWarningMessage(nonExistOrCompatibleOutliers.stream()//
                .collect(Collectors.joining(", ", "Column(s) (",
                    ") as specified by the outlier detector is not present or compatible.")));
        }
        return new PortObjectSpec[]{OutlierReviser.getOutTableSpec(inTableSpec),
            OutlierReviser.getSummaryTableSpec(inTableSpec, groupColNames)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
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
    public void warning(final Warning warning) {
        // TODO Auto-generated method stub

    }

}
