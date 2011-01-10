/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 *
 * History
 *    14.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.node;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedColumnHistogramDialogPane extends HistogramNodeDialogPane {

    private static final String NUMBER_OF_BINS_LABEL = "Number of bins:";

    private static final String NUMBER_OF_BINS_TOOLTIP =
        "Ignored if the selected binning column is nominal";
    private final SettingsModelIntegerBounded m_noOfBins;

    /**Constructor for class FixedColumnHistogramDialogPane.
     */
    protected FixedColumnHistogramDialogPane() {
        super();
        createNewGroup("Binning options");
        m_noOfBins = new SettingsModelIntegerBounded(
                            FixedColumnHistogramNodeModel.CFGKEY_NO_OF_BINS,
                            AbstractHistogramVizModel.DEFAULT_NO_OF_BINS, 1,
                            Integer.MAX_VALUE);
        final DialogComponent noOfBins = new DialogComponentNumber(
                m_noOfBins, NUMBER_OF_BINS_LABEL, new Integer(1));
        noOfBins.setToolTipText(NUMBER_OF_BINS_TOOLTIP);
        m_noOfBins.setEnabled(isNumericalXColumn());
        addDialogComponent(noOfBins);
        super.addXColumnChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_noOfBins.setEnabled(isNumericalXColumn());
            }
        });
    }

    /**
     * Checks if the current selected x column is numerical.
     * @return <code>true</code> if the selected x column is numerical or no
     * column is selected
     */
    protected boolean isNumericalXColumn() {
        final DataColumnSpec xColSpec = super.getSelectedXColumnSpec();
        return (xColSpec == null || xColSpec.getType().isCompatible(
                DoubleValue.class));
    }
}
