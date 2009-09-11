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
 */
package org.knime.base.node.mine.pca;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * Dialog for pca inversion.
 * 
 * @author uwe, University of Konstanz
 */
public class PCAReverseNodeDialog extends DefaultNodeSettingsPane {
    /**
     * create dialog.
     */
    public PCAReverseNodeDialog() {
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
                PCANodeModel.FAIL_MISSING, false),
                "Fail if missing values are encountered "
                        + "(skipped per default)"));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
                PCAReverseNodeModel.REMOVE_PCACOLS, false),
                "Replace PCA columns"));
        addDialogComponent(new DialogComponentColumnFilter(
                new SettingsModelFilterString(PCAReverseNodeModel.PCA_COLUMNS),
                PCAReverseNodeModel.DATA_INPORT, DoubleValue.class));
    }
}
