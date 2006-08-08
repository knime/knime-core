/* 
 * -------------------------------------------------------------------
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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   26.10.2005 (cebron): created
 */
package org.knime.base.node.mine.neural.rprop;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodedialog.DefaultNodeDialogPane;
import org.knime.core.node.defaultnodedialog.DialogComponentBoolean;
import org.knime.core.node.defaultnodedialog.DialogComponentColumnSelection;
import org.knime.core.node.defaultnodedialog.DialogComponentNumber;

/**
 * The RPropNodeDialog allows to configure the settings (nr. of training
 * iterations and architecture of the neural net).
 * 
 * @author Nicolas, University of Konstanz
 */
public class RPropNodeDialog extends DefaultNodeDialogPane {
    /**
     * Creates a new <code>NodeDialogPane</code> for the RProp neural net in
     * order to set the desired options.
     */
    public RPropNodeDialog() {
        super();
        this.addDialogComponent(new DialogComponentNumber(
        /* config-name: */RPropNodeModel.MAXITER_KEY,
        /* label: */"Maximum number of iterations: ",
        /* min: */1,
        /* max: */99999,
        /* default */20));
        this.addDialogComponent(new DialogComponentNumber(
        /* config-name: */RPropNodeModel.HIDDENLAYER_KEY,
        /* label: */"Number of hidden layers: ",
        /* min: */1,
        /* max: */100,
        /* default */1));
        this.addDialogComponent(new DialogComponentNumber(
        /* config-name: */RPropNodeModel.NRHNEURONS_KEY,
        /* label: */"Number of hidden neurons per layer: ",
        /* min: */1,
        /* max: */100,
        /* default */5));
        this.addDialogComponent(new DialogComponentColumnSelection(
        /* config-name: */RPropNodeModel.CLASSCOL_KEY,
        /* label: */"class column: ",
        /* columns from which port?: */RPropNodeModel.INPORT,
        /* column-type filter: */DataValue.class));
        this.addDialogComponent(new DialogComponentBoolean(
        /* config-name: */RPropNodeModel.IGNOREMV_KEY,
        /* label: */"Ignore Missing Values", false));
    }
}
