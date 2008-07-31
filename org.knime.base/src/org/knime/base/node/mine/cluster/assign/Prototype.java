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
 * -------------------------------------------------------------------
 * 
 * History
 *   13.01.2006 (cebron): created
 */
package org.knime.base.node.mine.cluster.assign;

import org.knime.base.node.mine.bfn.Distance;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * This class represents a prototype.
 * 
 * @author cebron, University of Konstanz
 */
public class Prototype {
    
    /**
     * Name of the config-object, where the {@link Prototype}s should be 
     * stored.
     */
    public static final String CFG_PROTOTYPE = "Prototypes";
    
    /**
     * Key to store the columns used for clustering in the PredParams.
     */
    public static final String CFG_COLUMNSUSED = "colsused";
    
    /*
     * Key to save/load the label of the prototype to/from ModelContent.
     */
    private static final String MODELCFG_LABEL = "Label";
    
    /*
     * Key to save/load the values of the prototype to/from ModelContent.
     */
    private static final String MODELCFG_VALUES = "Values";
    
    
    /*
     * Actual values of the Prototype.
     */
    private final double[] m_values;

    /*
     * Label of this cluster prototype
     */
    private final DataCell m_label;

    /**
     * Prototype is initialized with double values.
     * 
     * @param values for initialization.
     */
    public Prototype(final double[] values) {
       this(values, DataType.getMissingCell());
    }

    /**
     * Prototype is initialized with double values and label.
     * 
     * @param values of the cluster prototype.
     * @param classlabel of the cluster prototype.
     */
    public Prototype(final double[] values, final DataCell classlabel) {
        m_values = values;
        m_label = classlabel;
    }

    /**
     * 
     * @return the actual label of the cluster prototype.
     */
    public DataCell getLabel() {
        return m_label;
    }

    /**
     * 
     * @return actual values of this prototype.
     */
    public double[] getValues() {
        return m_values;
    }

    /**
     * Computes the distance between this prototype and a given {@link DataRow}.
     * Ignores all DataCells that are not compatible to DoubleValue.
     * 
     * @param row to compare.
     * @param indices to use.
     * @return distance value.
     */
    public double getDistance(final DataRow row, final int[] indices) {
        //TODO: Allow arbitrary distance objects as parameter
        double[] values = new double[indices.length];
        for (int i = 0; i < values.length; i++) {
            DataCell cell = row.getCell(indices[i]);
            if (cell.isMissing()) {
                return -1;
            }
            if (cell.getType().isCompatible(DoubleValue.class)) {
                values[i] = ((DoubleValue) cell).getDoubleValue();
            }
        }
        Distance dist = Distance.getInstance();
        return dist.compute(m_values, values);
    }
    
    /**
     * Computes the distance between this prototype and a given {@link DataRow}.
     * Ignores all DataCells that are not compatible to DoubleValue.
     * 
     * @param row to compare.
     * @return distance value.
     */
    public double getDistance(final DataRow row) {
        int[] indices = new int[row.getNumCells()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        return getDistance(row, indices);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(m_label);
        sb.append(": ");
        for (int i = 0; i < m_values.length; i++) {
            sb.append(m_values[i] + ",");
        }
        return sb.toString();
    }
    
    /**
     * Saves the {@link Prototype} to a {@link ModelContentWO} object.
     * @param model the ModelContent to save to.
     */
    public void save(final ModelContentWO model) {
        model.addDataCell(MODELCFG_LABEL, m_label);
        model.addDoubleArray(MODELCFG_VALUES, m_values);
    }
    
    /**
     * @param model ModelContent containing information of the {@link Prototype}
     * @return new {@link Prototype}
     * @throws InvalidSettingsException if the settings can not be retrieved.
     */
    public static Prototype loadFrom(final ModelContentRO model)
            throws InvalidSettingsException {
        DataCell label = model.getDataCell(MODELCFG_LABEL);
        double[] values = model.getDoubleArray(MODELCFG_VALUES);
        return new Prototype(values, label);
    }
    
}
