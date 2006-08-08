/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentWO;


/**
 * Factory class for {@link BasisFunctionLearnerRow} which automatically
 * creates new basis functions of a certain type.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see BasisFunctionLearnerRow
 * 
 * TODO hierarchy level should have lower, upper bound, and possible values set
 */
public abstract class BasisFunctionFactory {
    /** the model spec. */
    private final DataTableSpec m_spec;

    /** choice of distance function. */
    private final int m_distance;

    /** <code>true</code> if hierarchical model is trained. */
    private final boolean m_hierarchy;

    private final double[] m_mins;

    private final double[] m_maxs;

    /**
     * Creates new basisfunction factory with the given spec to extract min/max
     * value for all numeric columns.
     * 
     * @param spec the training's data spec
     * @param target the class info column in the data
     * @param type the type for the model columns
     * @param distance the choice of distance function
     */
    protected BasisFunctionFactory(final DataTableSpec spec,
            final String target, final DataType type, final int distance,
            final boolean hierarchy) {
        // keep distance function
        m_distance = distance;
        // hierarchical?
        m_hierarchy = hierarchy;
        // init mins and max from domain
        int nrColumns = spec.getNumColumns() - 1; // without target
        m_mins = new double[nrColumns];
        m_maxs = new double[nrColumns];
        for (int i = 0; i < nrColumns; i++) {
            DataColumnDomain domain = spec.getColumnSpec(i).getDomain();
            double min = -Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            if (domain.hasBounds()) {
                DataCell lower = domain.getLowerBound();
                if (lower.getType().isCompatible(DoubleValue.class)) {
                    min = ((DoubleValue)lower).getDoubleValue();
                }
                DataCell upper = domain.getUpperBound();
                if (upper.getType().isCompatible(DoubleValue.class)) {
                    max = ((DoubleValue)upper).getDoubleValue();
                }
            }
            m_mins[i] = min;
            m_maxs[i] = max;
        }
        DataTableSpec modelSpec = createModelSpec(spec, target, type);
        if (hierarchy) {
            DataColumnSpec newcol = new DataColumnSpecCreator("Level",
                    IntCell.TYPE).createSpec();
            DataTableSpec levelSpec = new DataTableSpec(
                    new DataColumnSpec[]{newcol});
            m_spec = new DataTableSpec(modelSpec, levelSpec);
        } else {
            m_spec = modelSpec;
        }
    }

    public final double[] getMins() {
        return m_mins;
    }

    public final double[] getMaxs() {
        return m_maxs;
    }

    /**
     * Creates a model spec based on the data input spec by extracting all
     * {@link org.knime.core.data.def.DoubleCell} columns and the specified target column.
     * 
     * @param inSpec the input data spec
     * @param target the target classification column
     * @return a new table spec with a number of {@link org.knime.core.data.def.DoubleCell}s and
     *         the target column last
     * @param type the type for the model columns
     */
    public static final DataTableSpec createModelSpec(
            final DataTableSpec inSpec, final String target, final DataType type) {
        ArrayList<DataColumnSpec> list = new ArrayList<DataColumnSpec>();
        // find all double and integer columns
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataColumnSpec cSpec = inSpec.getColumnSpec(i);
            // if target column - is added later...
            if (target.equals(cSpec.getName())) {
                continue;
            }
            // TODO too restrictive
            if (cSpec.getType().isCompatible(DoubleValue.class)) {
                DataColumnSpecCreator newCSpec = new DataColumnSpecCreator(
                        cSpec);
                newCSpec.setType(type);
                newCSpec.setDomain(cSpec.getDomain());
                list.add(newCSpec.createSpec());
            }
        }
        // if no numeric columns available
        if (list.size() == 0) {
            return null;
        }
        // add the target column last
        int targetIndex = inSpec.findColumnIndex(target);
        list.add(inSpec.getColumnSpec(targetIndex));
        // new table spec
        DataTableSpec ret = new DataTableSpec(list
                .toArray(new DataColumnSpec[]{}));
        return ret;
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
    public final DataTableSpec getModelSpec() {
        return m_spec;
    }

    /**
     * @return <code>true</code> if hierarchical model is trained.
     */
    public final boolean isHierarchical() {
        return m_hierarchy;
    }

    /**
     * Returns a new row initialized
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

    static final String CFG_DISTANCE = "distance";

    static final String CFG_IS_HIERACHICAL = "is_hierarchical";

    static final String CFG_MODEL_SPEC = "model_spec";

    public void save(final ModelContent pp) {
        pp.addInt(CFG_DISTANCE, m_distance);
        pp.addBoolean(CFG_IS_HIERACHICAL, m_hierarchy);
        ModelContentWO modelSpec = pp.addModelContent(CFG_MODEL_SPEC);
        m_spec.save(modelSpec);
    }
}
