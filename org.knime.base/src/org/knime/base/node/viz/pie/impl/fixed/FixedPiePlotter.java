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
 *    23.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.impl.fixed;

import org.knime.base.node.viz.pie.datamodel.fixed.FixedPieVizModel;
import org.knime.base.node.viz.pie.impl.PiePlotter;
import org.knime.core.node.property.hilite.HiLiteHandler;


/**
 * The fixed column implementation of the pie plotter.
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedPiePlotter
    extends PiePlotter<FixedPieProperties, FixedPieVizModel> {

    private static final long serialVersionUID = 7346765619645092687L;

    /**Constructor for class FixedPiePlotter.
     * @param properties the properties panel
     * @param handler the hilite handler
     */
    public FixedPiePlotter(final FixedPieProperties properties,
            final HiLiteHandler handler) {
        super(properties, handler);
    }

}
