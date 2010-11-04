/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.timeseries.node.movavg;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the dialog of the moving average node.
 *
 * @author Iris Adae, University of Konstanz, Germany
 */
public class MovingAverageDialog extends DefaultNodeSettingsPane {

    /** Constructor adding four components. */
    @SuppressWarnings("unchecked")
    public MovingAverageDialog() {

        addDialogComponent(new DialogComponentStringSelection(
                createWeightModel(), "Type of Moving Average: ",
                MA_METHODS.getLabels()));

        addDialogComponent(new DialogComponentNumberEdit(
                createWindowLengthModel(),
                "Window Length", 8));

        addDialogComponent(new DialogComponentBoolean(
                createReplaceColumnModel(), "Remove original columns"));

        addDialogComponent(new DialogComponentColumnFilter(
                createColumnNamesModel(), 0, false, DoubleValue.class));
    }

    /**
     *
     * @return the settings model for the column name
     */
    static SettingsModelFilterString createColumnNamesModel() {
        return new SettingsModelFilterString("column_names");
    }

    /**
     *
     * @return the model for the window length
     */
    static SettingsModelIntegerBounded createWindowLengthModel() {
        return new SettingsModelIntegerBounded("win_length", 21, 1,
                Integer.MAX_VALUE);
    }

    /**
     *
     * @return the model for the weight (simple or exponential)
     */
    static SettingsModelString createWeightModel() {
        return new SettingsModelString("weights",
                MA_METHODS.Simple.getLabel());
    }

    /**
     *
     * @return model for the replace column checkbox
     */
    static SettingsModelBoolean createReplaceColumnModel() {
        return new SettingsModelBoolean("replace_column", false);
    }
}
