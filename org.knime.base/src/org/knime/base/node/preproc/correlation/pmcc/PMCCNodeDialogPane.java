/* 
 * -------------------------------------------------------------------
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
 *   Feb 17, 2007 (wiswedel): created
 */
package org.knime.base.node.preproc.correlation.pmcc;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * Dialog for correlation node. Shows only a column filter.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class PMCCNodeDialogPane extends DefaultNodeSettingsPane {

    /** Inits dialog, adds only a column filter. */
    @SuppressWarnings("unchecked")
    public PMCCNodeDialogPane() {
        SettingsModelFilterString fS = PMCCNodeModel.createNewSettingsObject();
        DialogComponentColumnFilter cF = new DialogComponentColumnFilter(
                fS, 0, DoubleValue.class, NominalValue.class);
        addDialogComponent(cF);
        
        SettingsModelIntegerBounded sI = 
            PMCCNodeModel.createNewPossValueCounterModel();
        DialogComponentNumberEdit cI = 
            new DialogComponentNumberEdit(sI, "Possible Values Count"); 
        addDialogComponent(cI);
    }
}
