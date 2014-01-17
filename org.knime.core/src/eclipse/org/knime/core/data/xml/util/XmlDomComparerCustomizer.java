/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 */
package org.knime.core.data.xml.util;

import org.knime.core.node.util.filter.InputFilter;
import org.w3c.dom.Node;

/**
 * A listener that is passed to a DOMComparer and tells it which nodes to include in the check and if the the order of
 * the children of a given node is critical for the check. Clients might override
 * {@link #determineCompareStrategy(Node)} to enable compare strategy depending on an actual node or use
 * {@link XmlDomComparerCustomizer#XmlDomComparerCustomizer(ChildrenCompareStrategy)} to set a one strategy for all
 * nodes.
 *
 * @author Alexander Fillbrunn
 * @author Marcel Hanser
 * @since 2.10
 *
 */
public abstract class XmlDomComparerCustomizer extends InputFilter<Node> {

    /**
     * Determines if the order of children of an element should impact the comparison.
     *
     * @author Marcel Hanser
     */
    public enum ChildrenCompareStrategy {
        /**
         * The order is important (Default).
         */
        ORDERED,
        /**
         * The order should not impact the comparison. This ends in a performance issue as now every child of an element
         * must be checked with each child of the other element.
         */
        UNORDERED;
    }

    private final ChildrenCompareStrategy m_strategy;

    /**
     * Constructor using the {@link ChildrenCompareStrategy#ORDERED} strategy.
     */
    public XmlDomComparerCustomizer() {
        this(null);
    }

    /**
     * Constructor using the {@link ChildrenCompareStrategy#ORDERED} strategy if the given strategy is <code>null</code>
     * .
     *
     * @param strategy the compare strategy to use for every element
     */
    public XmlDomComparerCustomizer(final ChildrenCompareStrategy strategy) {
        super();
        this.m_strategy = strategy == null ? ChildrenCompareStrategy.ORDERED : strategy;
    }

    /**
     * Can be overridden by clients if a element depending {@link ChildrenCompareStrategy} is needed.
     *
     * @param element the current element.
     * @return the {@link ChildrenCompareStrategy} for this element
     */
    protected ChildrenCompareStrategy determineCompareStrategy(final Node element) {
        return m_strategy;
    }
}
