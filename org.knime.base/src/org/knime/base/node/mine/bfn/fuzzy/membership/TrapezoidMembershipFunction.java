/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
package org.knime.base.node.mine.bfn.fuzzy.membership;

import org.knime.core.data.DoubleValue;
import org.knime.core.util.MutableDouble;

/**
 * Trapezoid membership function with four values for support and core left and
 * right values whereby the support region can be defined infinity. The anchor
 * need to be a value within the core-region. If the anchor's value is changed,
 * the core- and support-region is adjusted if necessary. If the core-region
 * changes, the support-region is - if necessary - adjusted. But not the other
 * way around in both cases.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class TrapezoidMembershipFunction extends MembershipFunction {

    /**
     * Creates a new trapezoid membership function.
     * @param anchor The initial value.
     * @param min Minimum value.
     * @param max Maximum value.
     */
    public TrapezoidMembershipFunction(final DoubleValue anchor,
            final MutableDouble min, final MutableDouble max) {
        super(anchor, min, max);
    }
}
