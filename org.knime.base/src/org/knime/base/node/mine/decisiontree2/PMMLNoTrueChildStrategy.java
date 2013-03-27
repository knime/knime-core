/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 11, 2009 (morent): created
 */
package org.knime.base.node.mine.decisiontree2;

import java.util.EnumSet;
import java.util.HashMap;

/**
 * Represents the no true child strategies as defined in PMML
 * (<a>http://www.dmg.org/v4-0/TreeModel.html#NoTrueChildStrategies</a>).
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public enum PMMLNoTrueChildStrategy {
    /** returnNullPrediction no-true child strategy. */
    RETURN_NULL_PREDICTION("returnNullPrediction"),
    /** returnLastPrediction no-true child strategy. */
    RETURN_LAST_PREDICTION("returnLastPrediction");

    private final String m_represent;
    private static final HashMap<String, PMMLNoTrueChildStrategy> LOOKUP
            = new HashMap<String, PMMLNoTrueChildStrategy>();

    private PMMLNoTrueChildStrategy(final String represent) {
        m_represent = represent;
    }

    static {
        for (PMMLNoTrueChildStrategy strategy
                : EnumSet.allOf(PMMLNoTrueChildStrategy.class)) {
            LOOKUP.put(strategy.toString(), strategy);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_represent;
    }

    /**
     * Returns the corresponding no true child strategy to the representation.
     *
     * @param represent the representation to retrieve the strategy for
     * @return the strategy
     * @see java.util.HashMap#get(java.lang.Object)
     */
    public static PMMLNoTrueChildStrategy get(final String represent) {
        if (represent == null) {
            return getDefault();
        } else {
            return LOOKUP.get(represent);
        }
    }

    /**
     * Returns the default no true child strategy.
     *
     * @return the default strategy
     */
    public static PMMLNoTrueChildStrategy getDefault() {
        return RETURN_NULL_PREDICTION;
    }
}
