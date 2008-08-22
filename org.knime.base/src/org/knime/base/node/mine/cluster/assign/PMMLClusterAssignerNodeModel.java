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
import java.util.Vector;

import org.knime.base.node.mine.cluster.PMMLClusterPortObject;
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
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.PortObject;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.PortType;

/**
 * 
 * @author cebron, University of Konstanz
 */
public class PMMLClusterAssignerNodeModel extends GenericNodeModel {
   
   private DataTableSpec m_clusterSpec;
   
   private List<Prototype> m_prototypes;
   
   private int[] m_colIndices;
   
   private static final DataColumnSpec NEWCOLSPEC =
       new DataColumnSpecCreator("Cluster", StringCell.TYPE)
               .createSpec();

    /**
     * 
     */
    public PMMLClusterAssignerNodeModel() {
        super(new PortType[] {
                BufferedDataTable.TYPE, 
                PMMLClusterPortObject.TYPE},
                new PortType[] {BufferedDataTable.TYPE});
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        m_clusterSpec = (DataTableSpec)inSpecs[1];
        Vector<Integer> colIndices = new Vector<Integer>();
        for (DataColumnSpec colspec : m_clusterSpec) {
            int index =
                    ((DataTableSpec)inSpecs[0]).findColumnIndex(colspec
                            .getName());
            if (index < 0) {
                throw new InvalidSettingsException("Column "
                        + colspec.getName()
                        + " not found in input DataTableSpec.");
            }
            colIndices.add(index);
        }
        m_colIndices = new int[colIndices.size()];
        for (int i = 0; i < m_colIndices.length; i++) {
            m_colIndices[i] = colIndices.get(i);
        }

        ColumnRearranger colre =
                new ColumnRearranger((DataTableSpec)inSpecs[0]);
        colre.append(new ClusterAssignFactory(NEWCOLSPEC));
        DataTableSpec out = colre.createSpec();
        return new DataTableSpec[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
//        extractModelInfo(m_predParams);
        extractModelInfo((PMMLClusterPortObject)inData[1], 
                inData[1].getSpec());
        DataTableSpec inSpec = ((BufferedDataTable)inData[0])
            .getDataTableSpec();
        ColumnRearranger colre = new ColumnRearranger(inSpec);
        colre.append(new ClusterAssignFactory(NEWCOLSPEC));
        BufferedDataTable bdt =
                exec.createColumnRearrangeTable(
                        (BufferedDataTable)inData[0], colre, exec);
        return new BufferedDataTable[]{bdt};
    }
    
    

    private void extractModelInfo(final PMMLClusterPortObject model, 
            final PortObjectSpec spec)
            throws InvalidSettingsException {
        m_clusterSpec = (DataTableSpec)spec;

        m_prototypes = new ArrayList<Prototype>();
        String[] labels = model.getLabels();
        double[][] protos = model.getPrototypes();
        double[] mins = model.getMinima();
        double[] maxs = model.getMaxima();
        for (int i = 0; i < protos.length; i++) {
            double min = mins[i];
            double max = maxs[i];
            double[] prototype = protos[i];
            // fields might not be normalized
            // then the handler puts Double.NaN in the referring min and max pos
            if (!Double.isNaN(min) && !Double.isNaN(max)) {
                prototype = unormalize(protos[i], min, max);
            }
            m_prototypes.add(new Prototype(prototype, 
                    new StringCell(labels[i])));
        }
        DataColumnSpec[] inclCols = model.getUsedColumns();
        m_colIndices = new int[inclCols.length];
        for (int i = 0; i < inclCols.length; i++) {
            int idx = ((DataTableSpec)spec).findColumnIndex(
                    inclCols[i].getName());
            if (idx < 0) {
                throw new InvalidSettingsException(
                        "Column " + inclCols[i].getName() 
                        + " was not found in spec");
            } else {
                m_colIndices[i] = idx;
            }
        }
    }
    
    private double[] unormalize(final double[] normalized, 
            final double min, final double max) {
        double[] prototypes = new double[normalized.length];
        for (int i = 0; i < normalized.length; i++) {
            prototypes[i] = (normalized[i] * (max - min)) + min;
        }
        return prototypes;
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
       
        /**
         * Constructor. 
         * @param newColspec the DataColumnSpec of the appended column.
         */
        ClusterAssignFactory(final DataColumnSpec newColspec) {
            super(newColspec);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            double mindistance = Double.MAX_VALUE;
            DataCell winnercell = DataType.getMissingCell();
            for (Prototype proto : m_prototypes) {
                if (proto.getDistance(row, m_colIndices) < mindistance) {
                    mindistance = proto.getDistance(row, m_colIndices);
                    if (mindistance > 0) {
                        winnercell = proto.getLabel();
                    }
                }
            }
            return winnercell;
        }
        
    }
}
