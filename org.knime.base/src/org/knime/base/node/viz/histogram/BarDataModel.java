/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   31.07.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;


/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
interface BarDataModel {

    /**
     * @return the caption of the x axis of this bar
     */
    public abstract String getCaption();

    /**
     * @return the aggregation value label of this bar
     */
    public abstract String getLabel();

    /**
     * The number of rows in this bar.
     * 
     * @return number of rows in this bar
     */
    public abstract int getNumberOfRows();

    /**
     * @return <code>true</code> if this bar contains no rows
     */
    public abstract boolean isEmpty();

    /**
     * Sets the aggregation column and method. Even if the column or method
     * hasn't changed provide both to this method!
     * 
     * @param aggrColIdx the index of the aggregation column
     * @param aggrMethod the aggregation method
     */
    public abstract void setAggregationColumn(final int aggrColIdx,
            final AggregationMethod aggrMethod);

    /**
     * @return the highest value of this bar for the current aggregation method
     */
    public abstract double getAggregationValue();

    /**
     * @return the <code>AggregationMethod</code> which is used in this bar
     */
    public abstract AggregationMethod getAggregationMethod();

    /**
     * @return the row key of all rows which belong to this bar
     */
    public abstract Set<DataCell> getRowKeys();

    /**
     * @param caption the new caption of this bar
     */
    public abstract void setCaption(final String caption);

    /**
     * Returns a <code>Hashtable</code> with a <code>ColorAttr</code> object
     * as key and a <code>Collection</code> of the associated 
     * <code>DataRow</code> objects.
     * @param tableSpec the table specification which contains the color data
     * @return <code>Hashtable</code> with a <code>ColorAttr</code> object
     * as key and a <code>Collection</code> of the associated 
     * <code>DataRow</code> objects
     */
    public abstract Hashtable<ColorAttr, Collection<RowKey>> 
    createColorInformation(final DataTableSpec tableSpec);
}
