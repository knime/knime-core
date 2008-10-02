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
 *   10.06.2008 (ohl): adapted to new default node settings classes
 */
package org.knime.base.node.preproc.discretization.caim2.modelcreator;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;

/**
 * Dialog for the CAIM discretization algorithm. The dialog offers the selection
 * of those numeric columns that are intended to be discretized.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class CAIMDiscretizationNodeDialog extends DefaultNodeSettingsPane {

    /**
     * Constructor: create NodeDialog with one column filter panel.
     */
    @SuppressWarnings("unchecked")
    public CAIMDiscretizationNodeDialog() {

        // create the default components the class column selector
        DialogComponentColumnNameSelection classColumn =
                new DialogComponentColumnNameSelection(
                        CAIMDiscretizationNodeModel.createClassColModel(),
                        "Class column:",
                        CAIMDiscretizationNodeModel.DATA_INPORT,
                        StringValue.class);
        this.addDialogComponent(classColumn);

        // the column filter panel
        DialogComponentColumnFilter columnFilter =
                new DialogComponentColumnFilter(CAIMDiscretizationNodeModel
                        .createIncludeColsModel(),
                        CAIMDiscretizationNodeModel.DATA_INPORT,
                        DoubleValue.class);
        this.addDialogComponent(columnFilter);

        // whether to sort in memory
        DialogComponentBoolean inMemory =
                new DialogComponentBoolean(
                        CAIMDiscretizationNodeModel.createSortInMemModel(),
                        "Sort in memory:");

        this.addDialogComponent(inMemory);
    }
}
