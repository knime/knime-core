/*
 * -------------------------------------------------------------------
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
 *    13.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.aggregation;

import java.awt.Color;
import java.awt.Shape;

/**
 * This interface provides methods which are common to the aggregation model
 * and sub model.
 *
 * @author Tobias Koetter, University of Konstanz
 * @param <S> the basic shape
 * @param <H> the optional hilite shape
 */
public interface AggregationModel<S, H extends Shape> {

    /**
     * @return the optional name of this element (could be <code>null</code>)
     */
    public String getName();

    /**
     * @return the color to use for this element
     */
    public Color getColor();

    /**
     * @param method the {@link AggregationMethod} to use
     * @return the aggregation value of this element
     */
    public double getAggregationValue(final AggregationMethod method);

    /**
     * @return the sum of all aggregation values
     */
    public double getAggregationSum();

    /**
     * @return the shape of this element
     */
    public S getShape();

    /**
     * @return <code>true</code> if the sub elements should be drawn
     */
    public boolean isPresentable();

    /**
     * @return <code>true</code> if this element is selected
     */
    public boolean isSelected();

    /**
     * @return <code>true</code> if this model contains no rows
     */
    public boolean isEmpty();
    /**
     * @return <code>true</code> if hiliting is supported
     */
    public boolean supportsHiliting();

    /**
     * @return <code>true</code> if at least one row of this element is hilited
     */
    public boolean isHilited();

    /**
     * Call the {@link #supportsHiliting()} method to check if hiliting
     * is supported.
     * @return the hilite shape of this element
     */
    public H getHiliteShape();

    /**
     * Call the {@link #supportsHiliting()} method to check if hiliting
     * is supported.
     * @return the number of hilited rows in this element
     */
    public int getHiliteRowCount();

    /**
     * @return the number of rows of this element
     */
    public int getRowCount();


    /**
     * @return the number of real values (without missing values)
     */
    public int getValueCount();
}
