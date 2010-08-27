/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
     * @param indices to use.
     * @return distance value.
     */
    public double getSquaredEuclideanDistance(final DataRow row, 
            final int[] indices) {
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
        return dist.computeSquaredEuclidean(m_values, values);
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
