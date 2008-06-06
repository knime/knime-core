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
 */
package org.knime.base.node.viz.parcoord;

import org.knime.core.node.defaultnodedialog.DefaultNodeDialogPane;
import org.knime.core.node.defaultnodedialog.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodedialog.DialogComponentNumber;

/**
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public class ParallelCoordinatesNodeDialog extends DefaultNodeDialogPane {

    /**
     * Creates a new dialog for Parallel Coordinates.
     */
    ParallelCoordinatesNodeDialog() {
        super();
        this.addDialogComponent(new DialogComponentNumber(
                /*ConfigName=*/ ParallelCoordinatesNodeModel.MAXNUMROWS,
                /*Label=*/ "maximum number of rows displayed",
                /*min=*/ 1,
                /*max=*/ 1000000,
                /*default=*/ 1000
                ));
        this.addDialogComponent(new DialogComponentColumnFilter(
                /*ConfigName=*/ ParallelCoordinatesNodeModel.HIDDENCOLUMNS,
                /*Label=*/ "Columns to be visible in Parallel Coordinate View"
                ));
    }

}
