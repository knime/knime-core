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
 *   26.10.2005 (cebron): created
 */
package org.knime.base.node.mine.neural.rprop;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The RPropNodeDialog allows to configure the settings (nr. of training
 * iterations and architecture of the neural net).
 * 
 * @author Nicolas, University of Konstanz
 */
public class RPropNodeDialog extends DefaultNodeSettingsPane {
    /**
     * Creates a new <code>NodeDialogPane</code> for the RProp neural net in
     * order to set the desired options.
     */
    @SuppressWarnings("unchecked")
    public RPropNodeDialog() {
        super();
        this.addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
        /* config-name: */RPropNodeModel.MAXITER_KEY,
        /* default */20,
        /* min: */1,
        /* max: */RPropNodeModel.MAXNRITERATIONS), 
        /* label: */"Maximum number of iterations: ",
        /* step */1));
        this.addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
        /* config-name: */RPropNodeModel.HIDDENLAYER_KEY,
        /* default */1,
        /* min: */1,
        /* max: */100),
        /* label: */"Number of hidden layers: ",
        /* step */ 1));
        this.addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
        /* config-name: */RPropNodeModel.NRHNEURONS_KEY,
        /* default */5,
        /* min: */1,
        /* max: */100), 
        /* label: */"Number of hidden neurons per layer: ",
        /* step */ 1));
        this.addDialogComponent(new DialogComponentColumnNameSelection(
                new SettingsModelString(
        /* config-name: */RPropNodeModel.CLASSCOL_KEY, ""),
        /* label: */"class column: ",
        /* columns from which port?: */RPropNodeModel.INPORT,
        /* column-type filter: */DataValue.class));
        
        this.addDialogComponent(new DialogComponentBoolean(
                new SettingsModelBoolean(
        /* config-name: */RPropNodeModel.IGNOREMV_KEY,
        /* default */ false),
        /* label: */"Ignore Missing Values"));
    }
}
