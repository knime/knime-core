/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn;

import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentWO;
import org.knime.core.util.MutableDouble;


/**
 * Factory class for {@link BasisFunctionLearnerRow} which automatically
 * creates new basis functions of a certain type.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see BasisFunctionLearnerRow
 */
public abstract class BasisFunctionFactory {
    
    /** Key for the distance function. */
    static final String CFG_DISTANCE = "distance";
    
    /** Key for the model spec. */
    static final String CFG_MODEL_SPEC = "model_spec";
    
    /** Name of the basisfunction class column. */
    public static final DataColumnSpec CLASS_COLUMN = 
        new DataColumnSpecCreator("Class", StringCell.TYPE).createSpec();
    
    private static final DataColumnSpec WEIGHT_COLUMN = 
        new DataColumnSpecCreator("Weight", IntCell.TYPE).createSpec();
    
    private static final DataColumnSpec SPREAD_COLUMN = 
        new DataColumnSpecCreator("Spread", DoubleCell.TYPE).createSpec();
    
    private static final DataColumnSpec FEATURES_COLUMN = 
        new DataColumnSpecCreator("Features", IntCell.TYPE).createSpec();
    
    private static final DataColumnSpec VARIANCE_COLUMN = 
        new DataColumnSpecCreator("Variance", DoubleCell.TYPE).createSpec();
    
    /** the model spec. */
    private final DataTableSpec m_spec;

    /** choice of distance function. */
    private final int m_distance;

    /** Domain minimum for each dimension, might be adjusted during learning. */
    private final MutableDouble[] m_mins;

    /** Domain maximum for each dimension, might be adjusted during learning. */
    private final MutableDouble[] m_maxs;

    /**
     * Creates new basisfunction factory with the given spec to extract min/max
     * value for all numeric columns.
     * 
     * @param spec the training's data spec
     * @param dataColumns used for training
     * @param targetColumns the class info column in the data
     * @param type the type for the model columns
     * @param distance the choice of distance function
     */
    protected BasisFunctionFactory(final DataTableSpec spec,
            final String[] dataColumns, final String[] targetColumns, 
            final DataType type, final int distance) {
        // keep distance function
        m_distance = distance;
        // init mins and maxs from domain
        int nrColumns = dataColumns.length;
        m_mins = new MutableDouble[nrColumns];
        m_maxs = new MutableDouble[nrColumns];
        for (int i = 0; i < nrColumns; i++) {
            DataColumnDomain domain = 
                spec.getColumnSpec(dataColumns[i]).getDomain();
            m_mins[i] = new MutableDouble(Double.NaN);
            m_maxs[i] = new MutableDouble(Double.NaN);
            if (domain.hasBounds()) {
                DataCell lower = domain.getLowerBound();
                if (lower.getType().isCompatible(DoubleValue.class)) {
                    m_mins[i].setValue(
                            ((DoubleValue)lower).getDoubleValue());
                }
                DataCell upper = domain.getUpperBound();
                if (upper.getType().isCompatible(DoubleValue.class)) {
                    m_maxs[i].setValue(
                            ((DoubleValue)upper).getDoubleValue());
                }
            }
        }
        m_spec = createModelSpec(spec, dataColumns, targetColumns, type);
    }

    /**
     * 
     * @return the lower bounds.
     */
    public final MutableDouble[] getMinimums() {
        return m_mins;
    }

    /**
     * 
     * @return the upper bounds.
     */
    public final MutableDouble[] getMaximums() {
        return m_maxs;
    }

    /**
     * Creates a model spec based on the data input spec by extracting all
     * {@link org.knime.core.data.def.DoubleCell} columns and the specified 
     * target column.
     * 
     * @param inSpec the input data spec
     * @param dataColumns the data columns used for training
     * @param targetColumns the target classification columns
     * @return a new table spec with a number of 
     * {@link org.knime.core.data.def.DoubleCell}s and
     *         the target column last
     * @param type the type for the model columns
     */
    public static final DataTableSpec createModelSpec(
            final DataTableSpec inSpec, final String[] dataColumns, 
            final String[] targetColumns, final DataType type) {
        // list of final columns in table spec
        ArrayList<DataColumnSpec> list = new ArrayList<DataColumnSpec>();
        // find all double and integer columns
        for (String dataColumn : dataColumns) {
            DataColumnSpec cSpec = inSpec.getColumnSpec(dataColumn);
            assert (cSpec.getType().isCompatible(DoubleValue.class));
            DataColumnSpecCreator newCSpec = new DataColumnSpecCreator(
                    cSpec);
            newCSpec.setType(type);
            newCSpec.setDomain(cSpec.getDomain());
            list.add(newCSpec.createSpec());
        }
        // if no numeric columns available
        if (list.size() == 0) {
            return new DataTableSpec();
        }
        // add the target columns
        DataColumnSpec target0 = inSpec.getColumnSpec(targetColumns[0]);
        if (targetColumns.length > 1
                && target0.getType().isCompatible(DoubleValue.class)) {
            StringCell[] targetStrings = new StringCell[targetColumns.length];
            for (int i = 0; i < targetColumns.length; i++) {
                targetStrings[i] = new StringCell(targetColumns[i]);
            }
            DataColumnSpecCreator cSpec = new DataColumnSpecCreator(
                    CLASS_COLUMN);
            cSpec.setDomain(new DataColumnDomainCreator(
                    targetStrings).createDomain());
            list.add(cSpec.createSpec());
        } else {
            assert (targetColumns.length == 1);
            DataColumnSpecCreator cSpec = new DataColumnSpecCreator(target0);
            cSpec.setType(StringCell.TYPE);
            list.add(target0);
        }
        // add additional rule info
        list.add(WEIGHT_COLUMN);
        list.add(SPREAD_COLUMN);
        list.add(FEATURES_COLUMN);
        list.add(VARIANCE_COLUMN);
        // new table spec
        return new DataTableSpec(list.toArray(new DataColumnSpec[]{}));
    }

    /**
     * Returns the choice of distance function.
     * 
     * @return distance function
     */
    public final int getDistance() {
        return m_distance;
    }

    /**
     * @return the model's spec with class info column
     */
    public DataTableSpec getModelSpec() {
        return m_spec;
    }

    /**
     * Returns a new row initialised
     * by a {@link DataRow} as its initial center vector and a class
     * label.
     * 
     * @param key this row's key
     * @param classInfo data cell contains class info
     * @param row the initial center vector
     * @return a new row of a certain type
     */
    public abstract BasisFunctionLearnerRow commit(final RowKey key,
            final DataCell classInfo, final DataRow row);
    
    /**
     * Saves to model content.
     * @param pp the model content this is saved to.
     */
    public void save(final ModelContent pp) {
        pp.addInt(CFG_DISTANCE, m_distance);
        ModelContentWO modelSpec = pp.addModelContent(CFG_MODEL_SPEC);
        m_spec.save(modelSpec);
    }
}
