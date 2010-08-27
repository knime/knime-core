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
