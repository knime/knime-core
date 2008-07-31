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
 *    12.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.datamodel;

import java.awt.Color;
import java.awt.geom.Arc2D;

import org.knime.base.node.viz.aggregation.AggregationValSubModel;
import org.knime.base.node.viz.histogram.datamodel.BarElementDataModel;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;


/**
 * This class implements a sub section of a pie chart.
 * @author Tobias Koetter, University of Konstanz
 */
public class PieSubSectionDataModel
    extends AggregationValSubModel<Arc2D, Arc2D> {

    private static final long serialVersionUID = -6828514317488193272L;

    /**Constructor for class PieSubSectionDataModel.
     * @param color the color of this element
     * @param supportHiliting if hiliting is supported
     */
    protected PieSubSectionDataModel(final Color color,
            final boolean supportHiliting) {
        super(color, supportHiliting);
    }

    /**Constructor for class PieSubSectionDataModel.
     * @param config the config object to use
     * @throws InvalidSettingsException if the config object is invalid
     */
    private PieSubSectionDataModel(final Config config)
    throws InvalidSettingsException {
        super(config);
    }

    /**
     * @param arc the arc of this sub section
     * @param calculator the hilite calculator
     */
    public void setSubSection(final Arc2D arc,
            final PieHiliteCalculator calculator) {
        setShape(arc, calculator);
    }

    /**
     * @param config the config object to use
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @return the loaded {@link BarElementDataModel}
     * @throws CanceledExecutionException if the operation is canceled
     * @throws InvalidSettingsException if the config object is invalid
     */
    public static PieSubSectionDataModel loadFromFile(final Config config,
            final ExecutionMonitor exec) throws CanceledExecutionException,
            InvalidSettingsException {
        exec.checkCanceled();
        return new PieSubSectionDataModel(config);
    }
}
