/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
