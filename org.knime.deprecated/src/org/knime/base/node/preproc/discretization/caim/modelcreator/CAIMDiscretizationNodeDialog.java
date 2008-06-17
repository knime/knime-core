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
 *   25.10.2006 (sieb): created
 */
package org.knime.base.node.preproc.discretization.caim.modelcreator;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodedialog.DefaultNodeDialogPane;
import org.knime.core.node.defaultnodedialog.DialogComponentBoolean;
import org.knime.core.node.defaultnodedialog.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodedialog.DialogComponentColumnSelection;

/**
 * Dialog for the CAIM discretization algorithm. The dialog offers the selection
 * of those numeric columns that are intended to be discretized.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class CAIMDiscretizationNodeDialog extends DefaultNodeDialogPane {

    /**
     * Constructor: create NodeDialog with one column filter panel.
     */
    public CAIMDiscretizationNodeDialog() {

        super();

        // create the default components
        // the class column selector
        DialogComponentColumnSelection classColumn =
                new DialogComponentColumnSelection(
                        CAIMDiscretizationNodeModel.CLASS_COLUMN_KEY,
                        "Class column:",
                        CAIMDiscretizationNodeModel.DATA_INPORT,
                        StringValue.class);
        this.addDialogComponent(classColumn);

        // the column filter panel
        DialogComponentColumnFilter columnFilter =
                new DialogComponentColumnFilter(
                       CAIMDiscretizationNodeModel.INCLUDED_COLUMNS_KEY,
                       "Columns for discretization:", false, DoubleValue.class);
        this.addDialogComponent(columnFilter);

        // whether to sort in memory
        DialogComponentBoolean inMemory =
                new DialogComponentBoolean(
                        CAIMDiscretizationNodeModel.SORT_IN_MEMORY_KEY,
                        "Sort in memory:", true);

        this.addDialogComponent(inMemory);
    }
}
