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
 *    12.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.datamodel;

import java.awt.Color;
import java.awt.geom.Arc2D;
import java.util.Collection;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.aggregation.AggregationValModel;
import org.knime.base.node.viz.pie.datamodel.PieVizModel.PieHiliteCalculator;
import org.knime.base.node.viz.pie.util.GeometryUtil;


/**
 * This class implements a section of a pie chart.
 * @author Tobias Koetter, University of Konstanz
 */
public class PieSectionDataModel
extends AggregationValModel <PieSubSectionDataModel, Arc2D, Arc2D> {

    /**Constructor for class PieSectionDataModel.
     * @param name the name of this section
     * @param color the color oft his section
     * @param supportHiliting if hiliting should be supported
     */
    protected PieSectionDataModel(final String name, final Color color,
            final boolean supportHiliting) {
        super(name, color, supportHiliting);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PieSubSectionDataModel createElement(final Color color) {
        return new PieSubSectionDataModel(color, supportsHiliting());
    }

    /**
     * @param arc the arc of this section
     * @param calculator the hilite calculator
     */
    public void setPieSection(final Arc2D arc,
            final PieHiliteCalculator calculator) {
        setShape(arc, calculator);
        setSubSection(arc, calculator);
    }

    /**
     * @param arc
     * @param calculator
     */
    private void setSubSection(final Arc2D arc,
            final PieHiliteCalculator calculator) {
        final AggregationMethod method = calculator.getAggrMethod();
        final Collection<PieSubSectionDataModel> subSections = getElements();
        double startAngle = arc.getAngleStart();
        //we have to be care full with the value range in stacked layout
        //because of the mixture of positive and negatives
        double totalValue = getAggregationValue(method);
        if ((AggregationMethod.AVERAGE.equals(method)
                || AggregationMethod.SUM.equals(method))) {
            totalValue = 0;
            for (final PieSubSectionDataModel element : subSections) {
                totalValue +=
                    Math.abs(element.getAggregationValue(method));
            }
        }
        for (final PieSubSectionDataModel subSection : subSections) {
            final double value =
                Math.abs(subSection.getAggregationValue(method));
            double fraction;
            if (totalValue == 0) {
                fraction = 0;
            } else {
                fraction = value / totalValue;
            }
            final double partialAngle =
                GeometryUtil.calculatePartialExtent(arc, fraction);
            final Arc2D subArc = new Arc2D.Double(arc.getBounds(), startAngle,
                    partialAngle, Arc2D.PIE);
            subSection.setSubSection(subArc, calculator);
            startAngle += partialAngle;
        }
    }
}
