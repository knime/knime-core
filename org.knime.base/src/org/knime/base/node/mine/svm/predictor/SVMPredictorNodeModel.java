/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   01.10.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.predictor;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.knime.base.node.mine.svm.PMMLSVMPortObject;
import org.knime.base.node.mine.svm.Svm;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * NodeModel of the SVM Predictor Node.
 * @author cebron, University of Konstanz
 */
public class SVMPredictorNodeModel extends NodeModel {

    /*
     * The extracted Support Vector Machines.
     */
    private Svm[] m_svms;

    /*
     * Column indices to use.
     */
    private int[] m_colindices;

    /**
     * Constructor, one model and data input, one (classified) data output.
     */
    public SVMPredictorNodeModel() {
        super(new PortType[] {PMMLSVMPortObject.TYPE, BufferedDataTable.TYPE },
                new PortType[] {
                BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
            DataTableSpec testSpec = (DataTableSpec) inSpecs[1];
            PMMLPortObjectSpec trainingSpec = (PMMLPortObjectSpec) inSpecs[0];
            // try to find all columns (except the class column)
            Vector<Integer> colindices = new Vector<Integer>();
            for (DataColumnSpec colspec : trainingSpec.getLearningCols()) {
                if (colspec.getType().isCompatible(DoubleValue.class)) {
                    int colindex = testSpec.findColumnIndex(colspec.getName());
                    if (colindex < 0) {
                        throw new InvalidSettingsException("Column " + "\'"
                                + colspec.getName() + "\' not found"
                                + " in test data");
                    }
                    colindices.add(colindex);
                }
            }
            m_colindices = new int[colindices.size()];
            for (int i = 0; i < m_colindices.length; i++) {
                m_colindices[i] = colindices.get(i);
            }
            SVMPredictor svmpredict = new SVMPredictor(m_svms, m_colindices);
            ColumnRearranger colre = new ColumnRearranger(testSpec);
            colre.append(svmpredict);
            return new DataTableSpec[]{colre.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        m_svms = ((PMMLSVMPortObject)inData[0]).getSvms();
        DataTableSpec testSpec =
                ((BufferedDataTable)inData[1]).getDataTableSpec();
        DataTableSpec trainingSpec =
                ((PMMLPortObject)inData[0]).getSpec().getDataTableSpec();
        // try to find all columns (except the class column)
        Vector<Integer> colindices = new Vector<Integer>();
        for (DataColumnSpec colspec : trainingSpec) {
            if (colspec.getType().isCompatible(DoubleValue.class)) {
                int colindex = testSpec.findColumnIndex(colspec.getName());
                if (colindex < 0) {
                    throw new InvalidSettingsException("Column " + "\'"
                            + colspec.getName() + "\' not found"
                            + " in test data");
                }
                colindices.add(colindex);
            }
        }
        m_colindices = new int[colindices.size()];
        for (int i = 0; i < m_colindices.length; i++) {
            m_colindices[i] = colindices.get(i);
        }
        SVMPredictor svmpredict = new SVMPredictor(m_svms, m_colindices);
        BufferedDataTable testData = (BufferedDataTable) inData[1];
        ColumnRearranger colre =
                new ColumnRearranger(testData.getDataTableSpec());
        colre.append(svmpredict);
        BufferedDataTable result =
                exec.createColumnRearrangeTable(testData, colre, exec);
        return new BufferedDataTable[]{result};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        //
    }

}
