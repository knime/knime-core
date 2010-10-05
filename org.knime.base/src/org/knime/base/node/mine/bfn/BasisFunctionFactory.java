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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
     * @param spec the training's data spec
     * @param targetColumns the class info column in the data
     * @param type the type for the model columns
     * @param distance the choice of distance function
     */
    protected BasisFunctionFactory(final DataTableSpec spec, 
            final String[] targetColumns, final DataType type, 
            final int distance) {
        // keep distance function
        m_distance = distance;
        // init mins and maxs from domain
        String[] dataColumns = findDataColumns(spec, 
                Arrays.asList(targetColumns));
        m_mins = new MutableDouble[dataColumns.length];
        m_maxs = new MutableDouble[dataColumns.length];
        for (int i = 0; i < dataColumns.length; i++) {
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
     * Find all numeric columns which are not target columns.
     * @param spec the input spec
     * @param targetCols column(s) set as target
     * @return array of data column names
     */
    public static final String[] findDataColumns(final DataTableSpec spec,
            final List<String> targetCols) {
        // if no data columns are found, use all numeric columns
        List<String> dataCols = new ArrayList<String>();
        for (DataColumnSpec cspec : spec) {
            if (!targetCols.contains(cspec.getName())
                    && cspec.getType().isCompatible(DoubleValue.class)) {
                dataCols.add(cspec.getName());
            }
        }
        return dataCols.toArray(new String[0]);
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
