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
import org.knime.base.node.viz.pie.util.GeometryUtil;


/**
 * This class implements a section of a pie chart.
 * @author Tobias Koetter, University of Konstanz
 */
public class PieSectionDataModel
    extends AggregationValModel <PieSubSectionDataModel, Arc2D, Arc2D> {

    private static final long serialVersionUID = -7650706027283786854L;

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
     * Calculates the size of all subsections of this section based on the
     * size of the given arc.
     * @param arc the arc of this section
     * @param calculator the hilite calculator which provides implementation
     * specific information
     */
    private void setSubSection(final Arc2D arc,
            final PieHiliteCalculator calculator) {
        final Collection<PieSubSectionDataModel> subSections = getElements();
        if (subSections == null || subSections.size() < 1) {
            return;
        }
        if (arc == null) {
            //reset all subsections
            for (final PieSubSectionDataModel subSection : subSections) {
                subSection.setSubSection(null, calculator);
            }
            return;
        }
        final AggregationMethod method = calculator.getAggrMethod();
        double startAngle = arc.getAngleStart();
        final double totalValue;
        if ((AggregationMethod.AVERAGE.equals(method)
                || AggregationMethod.SUM.equals(method))) {
            double value = 0;
            for (final PieSubSectionDataModel element : subSections) {
                value +=
                    Math.abs(element.getAggregationValue(method));
            }
            totalValue = value;
        } else {
            totalValue = getAggregationValue(method);
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
            final double partialExtend =
                GeometryUtil.calculatePartialExtent(arc, fraction);
            final Arc2D subArc = new Arc2D.Double(arc.getBounds(), startAngle,
                    partialExtend, Arc2D.PIE);
            subSection.setSubSection(subArc, calculator);
            startAngle += partialExtend;
        }
    }
}
