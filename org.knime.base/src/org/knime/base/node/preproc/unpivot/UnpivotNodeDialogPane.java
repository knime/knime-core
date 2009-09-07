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
	 * 
	 */
	public UnpivotNodeDialogPane() {
		createNewGroup(" Order columns ");
		addDialogComponent(new DialogComponentColumnFilter(
				createColumnFilter_OrderColumns(), 0));
		createNewGroup(" Value columns ");
		addDialogComponent(new DialogComponentColumnFilter(
				createColumnFilter_ValueColumns(), 0));
		createNewGroup(" Options ");
		addDialogComponent(new DialogComponentBoolean(
				createHiLiteModel(), "Enable hiliting"));

	}
	
	static SettingsModelFilterString createColumnFilter_OrderColumns() {
		return new SettingsModelFilterString("order_columns");
	}
	
	static SettingsModelFilterString createColumnFilter_ValueColumns() {
		return new SettingsModelFilterString("value_columns");
	}
	
	static SettingsModelBoolean createHiLiteModel() {
		return new SettingsModelBoolean("enable-hiliting", false);
	}

}
