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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy.membership;

import org.knime.core.data.DoubleValue;

/**
 * Triangle membership function with three values core/anchor and support-left
 * and -right whereby the support region can be defined infinity at the
 * begining. If the anchor's value is changed, the support-region is adjusted if
 * necessay, but not the other way around.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class TriangleMembershipFunction extends MembershipFunction {
    private TriangleMembershipFunction(final double min, final double max) {
        super(min, max);
    }

    /**
     * Creates a new triagle membership function with its given anchor and two
     * values used to assign the min and max border.
     * 
     * @param anchor the initial center point of this fuzzy function
     * @param min the minimum left border
     * @param max the maximum right border
     */
    public TriangleMembershipFunction(final DoubleValue anchor,
            final double min, final double max) {
        super(anchor, min, max);
    }

    /**
     * Creates a new triangle membership function with its given anchor and the
     * -{@link Double#MAX_VALUE} resp. {@link Double#MAX_VALUE} to
     * assign the min and max border.
     * 
     * @param anchor the initial center point of this fuzzy function
     */
    public TriangleMembershipFunction(final DoubleValue anchor) {
        this(anchor, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    /**
     * Creates a new membership function with -{@link Double#MAX_VALUE} and
     * {@link Double#MAX_VALUE} as global min vs. max; the anchor is undefined
     * and therefore this membership missing.
     */
    public TriangleMembershipFunction() {
        this(-Double.MAX_VALUE, Double.MAX_VALUE);
    }

    /**
     * We don't have a real core for triangle membership functions.
     * 
     * @param value not used
     */
    @Override
    public void setCoreLeft(final double value) {

    }

    /**
     * We don't have a real core for triangle membership functions.
     */
    @Override
    public void resetCore() {

    }

    /**
     * We don't have a real core for triangle membership functions.
     * 
     * @param value not used
     */
    @Override
    public void setCoreRight(final double value) {

    }
}
