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
 *   04.10.2006 (uwe): created
 */
package org.knime.base.nodes.mining.pca;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 * Node dialog for PCA Learner.
 * 
 * @author Uwe Nagel, University of Konstanz
 * 
 */
public class PCAComputeNodeDialog extends DefaultNodeSettingsPane {
    /**
     * Constructor: create NodeDialog with one combo box.
     */
    public PCAComputeNodeDialog() {

        super();

        // create a combo box that reads the normalization type for the pca
        // DialogComponentStringSelection emissColumnSelector = new
        // DialogComponentStringSelection(new SettingsModelStringArray()
        // PCANodeModel.NORMALIZATION_TYPE_KEY, "Normalization type:",
        // PCANodeModel.NORMALIZATION_TYPE_LIST);
        // addDialogComponent(new DialogComponentNumber(new
        // SettingsModelInteger(
        // PCANodeModel.RESULT_DIMENSIONS, 2), "dimensions to reduce to",
        // 1));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
                PCANodeModel.FAIL_MISSING, false),
                "Fail if missing values are encountered (skipped per default)"));
        addDialogComponent(new DialogComponentColumnFilter(
                new SettingsModelFilterString(PCANodeModel.INPUT_COLUMNS),
                PCAComputeNodeModel.DATA_INPORT, DoubleValue.class));
    }
}
