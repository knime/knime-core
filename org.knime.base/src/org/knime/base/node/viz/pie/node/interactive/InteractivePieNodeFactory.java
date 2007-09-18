/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   11.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.pie.node.interactive;

import org.knime.base.node.viz.pie.node.PieNodeFactory;
import org.knime.base.node.viz.pie.node.PieNodeModel;


/**
 * Factory class of the interactive pie chart implementation.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractivePieNodeFactory extends PieNodeFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public PieNodeModel createNodeModel() {
        return new InteractivePieNodeModel();
    }
}
