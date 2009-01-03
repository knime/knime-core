/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   29.01.2008 (cebron): created
 */
package org.knime.base.node.mine.cluster.assign;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.knime.base.node.mine.cluster.PMMLClusterPortObject;
import org.knime.base.node.mine.cluster.PMMLClusterPortObject.ComparisonMeasure;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * 
 * @author cebron, University of Konstanz
 */
public class ClusterAssignerNodeModel extends NodeModel {
   
   private static final int PMML_PORT = 0;
   private static final int DATA_PORT = 1;

    /**
     * 
     */
    public ClusterAssignerNodeModel() {
        super(new PortType[] {
                PMMLClusterPortObject.TYPE,
                BufferedDataTable.TYPE},
                new PortType[] {BufferedDataTable.TYPE});
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        PMMLPortObjectSpec spec = ((PMMLPortObjectSpec)inSpecs[PMML_PORT]);
        DataTableSpec dataSpec = (DataTableSpec) inSpecs[DATA_PORT];
        ColumnRearranger colre = new ColumnRearranger(dataSpec);
        colre.append(new ClusterAssignFactory(
                null, null, createNewOutSpec(dataSpec), 
                findLearnedColumnIndices(dataSpec, spec.getLearningCols())));
        DataTableSpec out = colre.createSpec();
        return new DataTableSpec[]{out};
    }
    
    private DataColumnSpec createNewOutSpec(final DataTableSpec inSpec) {
        String newColName = DataTableSpec.getUniqueColumnName(inSpec, 
                "Cluster"); 
        return new DataColumnSpecCreator(newColName, StringCell.TYPE)
            .createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        PMMLClusterPortObject model = (PMMLClusterPortObject) inData[PMML_PORT];
        ComparisonMeasure measure = model.getComparisonMeasure();
        List<Prototype> prototypes = new ArrayList<Prototype>();
        String[] labels = model.getLabels();
        double[][] protos = model.getPrototypes();
        for (int i = 0; i < protos.length; i++) {
            double[] prototype = protos[i];
            prototypes.add(new Prototype(prototype, 
                    new StringCell(labels[i])));
        }
        BufferedDataTable data = (BufferedDataTable)inData[DATA_PORT];
        ColumnRearranger colre = new ColumnRearranger(data.getSpec());
        colre.append(new ClusterAssignFactory(
                measure, prototypes, createNewOutSpec(data.getDataTableSpec()), 
                findLearnedColumnIndices(data.getSpec(), 
                        model.getSpec().getLearningCols())));
        BufferedDataTable bdt =
                exec.createColumnRearrangeTable(data, colre, exec);
        return new BufferedDataTable[]{bdt};
    }
    
    private static int[] findLearnedColumnIndices(final DataTableSpec ospec,
            final Set<DataColumnSpec> learnedCols) 
            throws InvalidSettingsException {
        int[] colIndices = new int[learnedCols.size()];
        int idx = 0;
        for (DataColumnSpec cspec : learnedCols) {
            int i = ospec.findColumnIndex(cspec.getName());
            if (i < 0) {
                throw new InvalidSettingsException("Column \""
                        + cspec.getName() + "\" not found in data input spec.");
            }
            colIndices[idx++] = i;
        }
        return colIndices;
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
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
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

    private class ClusterAssignFactory extends SingleCellFactory {
        private final ComparisonMeasure m_measure;
        private final List<Prototype> m_prototypes;
        private final int[] m_colIndices;
       
        /**
         * Constructor.
         * @param measure comparison measure
         * @param prototypes list of prototypes
         * @param newColspec the DataColumnSpec of the appended column
         * @param learnedCols columns used for training
         */
        ClusterAssignFactory(final ComparisonMeasure measure, 
                final List<Prototype> prototypes, 
                final DataColumnSpec newColspec, 
                final int[] learnedCols) {
            super(newColspec);
            m_measure = measure;
            m_prototypes = prototypes;
            m_colIndices = learnedCols;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            double mindistance = Double.MAX_VALUE;
            DataCell winnercell = DataType.getMissingCell();
            for (Prototype proto : m_prototypes) {
                double dist;
                if (m_measure.equals(ComparisonMeasure.squaredEuclidean)) {
                    dist = proto.getSquaredEuclideanDistance(row, m_colIndices);
                } else {
                    dist = proto.getDistance(row, m_colIndices);
                }
                if (dist < mindistance) {
                    mindistance = dist;
                    if (mindistance > 0) {
                        winnercell = proto.getLabel();
                    }
                }
            }
            return winnercell;
        }
        
    }
}
