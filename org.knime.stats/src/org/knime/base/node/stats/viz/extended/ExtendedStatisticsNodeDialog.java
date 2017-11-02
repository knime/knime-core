/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 */
package org.knime.base.node.stats.viz.extended;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;

/**
 * <code>NodeDialog</code> for the "ExtendedStatistics" Node. Calculates statistic moments with their distributions and
 * counts nominal values and their occurrences across all columns.
 *
 * @author Gabor Bakos
 */
class ExtendedStatisticsNodeDialog extends DefaultNodeSettingsPane {
    private final SettingsModelColumnFilter2 m_filterModel;

    /**
     * New pane for configuring the ExtendedStatistics node.
     */
    protected ExtendedStatisticsNodeDialog() {
        addDialogComponent(new DialogComponentBoolean(ExtendedStatisticsNodeModel.createMedianModel(),
            "Calculate median values (computationally expensive)"));
        createNewGroup("Nominal values");
        m_filterModel = ExtendedStatisticsNodeModel.createNominalFilterModel();
        addDialogComponent(new DialogComponentColumnFilter2(m_filterModel, 0, false));
        DialogComponentNumber numNomValueComp =
            new DialogComponentNumber(ExtendedStatisticsNodeModel.createNominalValuesModel(),
                "Max no. of most frequent and infrequent values (in view): ", 5);
        numNomValueComp.setToolTipText("Max no. of most frequent and infrequent "
            + "values per column displayed in the node view.");
        addDialogComponent(numNomValueComp);
        DialogComponentNumber numNomValueCompOutput =
            new DialogComponentNumber(ExtendedStatisticsNodeModel.createNominalValuesModelOutput(),
                "Max no. of possible values per column (in output table): ", 5);
        addDialogComponent(numNomValueCompOutput);
        addDialogComponent(new DialogComponentBoolean(ExtendedStatisticsNodeModel.createEnableHiLite(), "Enable HiLite"));
        createNewTab("Histogram");
        addDialogComponent(new DialogComponentStringSelection(ExtendedStatisticsNodeModel.createImageFormat(),
            "Histogram format: ", ExtendedStatisticsNodeModel.POSSIBLE_IMAGE_FORMATS));
        addDialogComponent(new DialogComponentNumberEdit(ExtendedStatisticsNodeModel.createHistogramWidth(), "Width: "));
        addDialogComponent(new DialogComponentNumberEdit(ExtendedStatisticsNodeModel.createHistogramHeight(),
            "Height: "));
        addDialogComponent(new DialogComponentBoolean(ExtendedStatisticsNodeModel.createShowMinMax(), "Show min/max values"));
    }
}
