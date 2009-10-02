/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   29.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.mask;

import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;

/**
 * Dialog with a column selection and radio buttons to either mask date or 
 * time or milliseconds only.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class MaskTimeNodeDialog extends DefaultNodeSettingsPane {
    
    /**
     * 
     */
    public MaskTimeNodeDialog() {
        addDialogComponent(new DialogComponentColumnNameSelection(
                MaskTimeNodeModel.createColumnSelectionModel(), 
                "Select the column that should be masked", 0, 
                DateAndTimeValue.class));
        addDialogComponent(new DialogComponentButtonGroup(
                MaskTimeNodeModel.createMaskSelectionModel(), true, "Mask:", 
                MaskTimeNodeModel.MASK_DATE,
                MaskTimeNodeModel.MASK_TIME,
                MaskTimeNodeModel.MASK_MILLIS
        ));
    }

}
