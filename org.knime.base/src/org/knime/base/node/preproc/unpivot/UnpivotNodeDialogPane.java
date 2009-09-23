/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.base.node.preproc.unpivot;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;

/**
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class UnpivotNodeDialogPane extends DefaultNodeSettingsPane {

    /**
     * Create new unpivoting node dialog.
     */
    public UnpivotNodeDialogPane() {
        createNewGroup(" Value columns ");
        addDialogComponent(new DialogComponentColumnFilter(
                createColumnFilterValueColumns(), 0, false));
        createNewGroup(" Retained columns ");
        addDialogComponent(new DialogComponentColumnFilter(
                createColumnFilterOrderColumns(), 0, false));
        createNewGroup(" Options ");
        addDialogComponent(new DialogComponentBoolean(
                createHiLiteModel(), "Enable hiliting"));

    }

    /**
     * Return settings model for retained output columns.
     * @return settings model for retained columns
     */
    static SettingsModelFilterString createColumnFilterOrderColumns() {
        return new SettingsModelFilterString("order_columns");
    }

    /**
     * Return settings model for value columns.
     * @return settings model for retained columns
     */
    static SettingsModelFilterString createColumnFilterValueColumns() {
        return new SettingsModelFilterString("value_columns");
    }

    /**
     * Create model to enable/disable hiliting.
     * @return settings model for hiliting
     */
    static SettingsModelBoolean createHiLiteModel() {
        return new SettingsModelBoolean("enable-hiliting", false);
    }

}
