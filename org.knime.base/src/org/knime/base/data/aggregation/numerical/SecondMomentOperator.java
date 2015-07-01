/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   May 26, 2015 (Lara): created
 */
package org.knime.base.data.aggregation.numerical;

import org.apache.commons.math3.stat.descriptive.moment.SecondMoment;
import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DoubleValue;

/**
 * Computes the second moment
 *
 * @author Lara Gorini
 * @since 2.12
 */
public class SecondMomentOperator extends StorelessUnivariantStatisticOperator {

    /**
     * Constructor for class SecondCentralMomentOperator.
     *
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public SecondMomentOperator(final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings) {
        super(new OperatorData("Second moment", false, false, DoubleValue.class, false), globalSettings,
            AggregationOperator.setInclMissingFlag(opColSettings, false), new SecondMoment());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        return new SecondMomentOperator(globalSettings, opColSettings);
    }

    @Override
    public String getDescription() {
        return "Calculates the second moment value per group.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDetailedDescription() {
        return "Imagine five values x0, ... ,x4 and for example let be <font style='text-decoration: overline'>x0x1</font>"
            + "the mean of values x0 and x1. Then the method computes: x0 + (x1 - x0 )^2 * 1/2 + (x2-<font style='text-decoration: overline'>x0x1</font>)^2 * 2/3"
            + "(x3-<font style='text-decoration: overline'>x0x1x2</font>)^2 * 3/4 + (x4-<font style='text-decoration: overline'>x0x1x2x3</font>)^2 * 4/5.";
    }

}
