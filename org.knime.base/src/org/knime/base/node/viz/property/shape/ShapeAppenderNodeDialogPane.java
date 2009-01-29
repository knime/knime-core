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
 *   24.05.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.shape;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Dialog to select column to apply shape to.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ShapeAppenderNodeDialogPane extends DefaultNodeSettingsPane {

    /**
     * @return settings model for column selection
     */
    static final SettingsModelString createColumnModel() {
        return new SettingsModelString("selected_column", null);
    }

    /**
     * Create new shape appender dialog.
     */
    @SuppressWarnings("unchecked")
    public ShapeAppenderNodeDialogPane() {
        addDialogComponent(new DialogComponentColumnNameSelection(
                createColumnModel(), 
                "Append shapes to ", 1, DataValue.class));
    }
}
