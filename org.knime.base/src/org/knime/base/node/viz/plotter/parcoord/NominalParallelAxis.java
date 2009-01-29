/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   22.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.parcoord;

import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.base.util.coordinate.CoordinateMapping;
import org.knime.base.util.coordinate.NominalCoordinate;

/**
 * Represents a {@link org.knime.base.node.viz.plotter.parcoord.ParallelAxis} 
 * with nominal values and provides access to the possible values.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class NominalParallelAxis extends ParallelAxis {
    
    /**
     * 
     * @return the nominal values of this axis.
     */
    public Set<String> getPossibleValues() {
        Set<String> values = new LinkedHashSet<String>();
        CoordinateMapping[] mappings = ((NominalCoordinate)getCoordinate())
            .getReducedTickPositions(getHeight());
        for (int i = 0; i < mappings.length; i++) {
            values.add(mappings[i].getDomainValueAsString());
        }
        return values;
    }

}
