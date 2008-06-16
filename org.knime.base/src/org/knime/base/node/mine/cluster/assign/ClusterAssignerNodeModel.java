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
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author cebron, University of Konstanz
 */
public class ClusterAssignerNodeModel extends NodeModel {

    
    /*
     * ModelContent from model input port.
     */
   private ModelContentRO m_predParams;
   
   private DataTableSpec m_clusterSpec;
   
   private List<Prototype> m_prototypes;
   
   private int[] m_colIndices;
   
   private static final DataColumnSpec NEWCOLSPEC =
       new DataColumnSpecCreator("Cluster", StringCell.TYPE)
               .createSpec();

    /**
     * 
     */
    public ClusterAssignerNodeModel() {
        super(1, 1, 1, 0);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
       if (m_predParams != null) {
           extractModelInfo(m_predParams);
           Vector<Integer> colIndices = new Vector<Integer>();
           for (DataColumnSpec colspec : m_clusterSpec) {
               int index = inSpecs[0].findColumnIndex(colspec.getName());
               if (index < 0) {
                   throw new InvalidSettingsException("Column " 
                    + colspec.getName() + " not found in input DataTableSpec.");
               }
               colIndices.add(index);
           }
           m_colIndices = new int[colIndices.size()];
           for (int i = 0; i < m_colIndices.length; i++) {
               m_colIndices[i] = colIndices.get(i);
           }
           
           ColumnRearranger colre = new ColumnRearranger(inSpecs[0]);
           colre.append(new ClusterAssignFactory(NEWCOLSPEC));
           colre.createSpec();
       }
       return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        if (m_predParams == null) {
            throw new Exception("Predictor params not available.");
        }
        extractModelInfo(m_predParams);
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        Vector<Integer> colIndices = new Vector<Integer>();
        for (DataColumnSpec colspec : m_clusterSpec) {
            int index = inSpec.findColumnIndex(colspec.getName());
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
        ColumnRearranger colre = new ColumnRearranger(inSpec);
        colre.append(new ClusterAssignFactory(NEWCOLSPEC));
        BufferedDataTable bdt =
                exec.createColumnRearrangeTable(inData[0], colre, exec);
        return new BufferedDataTable[]{bdt};
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        if (index == 0) {
            m_predParams = predParams;
        }
    }
    
    private void extractModelInfo(final ModelContentRO model)
            throws InvalidSettingsException {
        ModelContentRO specRO =
                model.getModelContent(Prototype.CFG_COLUMNSUSED);
        m_clusterSpec = DataTableSpec.load(specRO);

        ModelContentRO protos = model.getModelContent(Prototype.CFG_PROTOTYPE);
        m_prototypes = new ArrayList<Prototype>();
        for (String key : protos) {
            ModelContentRO protoRO = protos.getModelContent(key);
            m_prototypes.add(Prototype.loadFrom(protoRO));
        }
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
