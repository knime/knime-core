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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   Feb 23, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.predict;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import org.knime.base.node.mine.regression.PMMLRegressionContentHandler;
import org.knime.base.node.mine.regression.PMMLRegressionContentHandler.NumericPredictor;
import org.knime.base.node.mine.regression.PMMLRegressionContentHandler.RegressionTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLModelType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.w3c.dom.Node;

/**
 * Node model for the linear regression predictor.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class RegressionPredictorNodeModel extends NodeModel {
	

	   /** The node logger for this class. */
	   private static final NodeLogger LOGGER =
	           NodeLogger.getLogger(RegressionPredictorNodeModel.class);

    /** Initialization with 1 data input, 1 model input and 1 data output. */
    public RegressionPredictorNodeModel() {
        super(new PortType[]{PMMLPortObject.TYPE,
                BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
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
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
    	PMMLPortObject regModel = (PMMLPortObject)inData[0];
        
           List<Node> models = regModel.getPMMLValue().getModels(
                   PMMLModelType.RegressionModel);
           if (models.isEmpty()) {
               String msg = "No Regression Model found.";
               LOGGER.error(msg);
               throw new RuntimeException(msg);
           }
           PMMLRegressionContentHandler handler 
           					= new PMMLRegressionContentHandler(
           							(PMMLPortObjectSpec)inData[0].getSpec());
           handler.parse(models.get(0));        
        BufferedDataTable data = (BufferedDataTable)inData[1];
        DataTableSpec spec = data.getDataTableSpec();
        ColumnRearranger c =
                createRearranger(spec, regModel.getSpec(), handler);
        BufferedDataTable out = exec.createColumnRearrangeTable(data, c, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        PMMLPortObjectSpec regModelSpec = (PMMLPortObjectSpec)inSpecs[0];
        DataTableSpec dataSpec = (DataTableSpec)inSpecs[1];
        if (dataSpec == null || regModelSpec == null) {
            throw new InvalidSettingsException(
                    "No input specification available");
        }
        ColumnRearranger rearranger =
                createRearranger(dataSpec, regModelSpec, null);
        DataTableSpec outSpec = rearranger.createSpec();
        return new DataTableSpec[]{outSpec};
    }

    private ColumnRearranger createRearranger(final DataTableSpec inSpec,
            final PMMLPortObjectSpec regModelSpec,
            final PMMLRegressionContentHandler regModel)
            throws InvalidSettingsException {
        if (regModelSpec == null) {
            throw new InvalidSettingsException("No input");
        }

        // exclude last (response column)
        String targetCol = "Response";
        for (String s : regModelSpec.getTargetFields()) {
            targetCol = s;
            break;
        }

        final List<String> learnFields;
        if (regModel != null) {
            RegressionTable regTable = regModel.getRegressionTable();
            learnFields = new ArrayList<String>();
            for (NumericPredictor p : regTable.getVariables()) {
                learnFields.add(p.getName());
            }
        } else {
            learnFields =
                    new ArrayList<String>(regModelSpec.getLearningFields());
        }

        final int[] colIndices = new int[learnFields.size()];
        int k = 0;
        for (String learnCol : learnFields) {
            int index = inSpec.findColumnIndex(learnCol);
            if (index < 0) {
                throw new InvalidSettingsException("Missing column for "
                        + "regressor variable : \"" + learnCol + "\"");
            }
            DataColumnSpec regressor = inSpec.getColumnSpec(index);
            String name = regressor.getName();
            DataColumnSpec col = inSpec.getColumnSpec(index);
            if (!col.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Incompatible type of "
                        + "column \"" + name + "\": " + col.getType());
            }

            colIndices[k++] = index;
        }
        // try to use some smart naming scheme for the append column
        String oldName = targetCol;
        if (inSpec.containsName(oldName)
                && !oldName.toLowerCase().endsWith("(prediction)")) {
            oldName = oldName + " (prediction)";
        }
        String newColName = DataTableSpec.getUniqueColumnName(inSpec, oldName);
        DataColumnSpec newCol =
                new DataColumnSpecCreator(newColName, DoubleCell.TYPE)
                        .createSpec();

        SingleCellFactory fac = new SingleCellFactory(newCol) {
            @Override
            public DataCell getCell(final DataRow row) {
                RegressionTable t = regModel.getRegressionTable();
                int j = 0;
                double result = t.getIntercept();
                for (NumericPredictor p : t.getVariables()) {
                    DataCell c = row.getCell(colIndices[j++]);
                    if (c.isMissing()) {
                        return DataType.getMissingCell();
                    }
                    double v = ((DoubleValue)c).getDoubleValue();
                    if (p.getExponent() != 1) {
                        v = Math.pow(v, p.getExponent());
                    }
                    result += p.getCoefficient() * v;
                }
                return new DoubleCell(result);
            }
        };
        ColumnRearranger c = new ColumnRearranger(inSpec);
        c.append(fac);
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
}
