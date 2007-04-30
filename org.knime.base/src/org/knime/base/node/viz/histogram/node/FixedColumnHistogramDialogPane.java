/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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

import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;


/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedColumnHistogramDialogPane extends HistogramNodeDialogPane {

    private static final String NUMBER_OF_BINS_LABEL = "Number of bins:";

    private static final String NUMBER_OF_BINS_TOOLTIP = 
        "Ignored if the selected x column is nominal";
    
    private final DialogComponentNumber m_noOfBins;
    
    /**Constructor for class FixedColumnHistogramDialogPane.
     */
    protected FixedColumnHistogramDialogPane() {
        super();
        createNewGroup("Binning options");
        m_noOfBins = new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                        FixedColumnHistogramNodeModel.CFGKEY_NO_OF_BINS,
                        AbstractHistogramVizModel.DEFAULT_NO_OF_BINS, 1, 
                        Integer.MAX_VALUE),
                        NUMBER_OF_BINS_LABEL, 1);
        m_noOfBins.setToolTipText(NUMBER_OF_BINS_TOOLTIP);
        addDialogComponent(m_noOfBins);
    }
}
