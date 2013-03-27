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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   14.07.2012 (kilian): created
 */
package org.knime.base.node.preproc.rounddouble;

import java.util.ArrayList;
import java.util.List;



/**
 * Number mode for round double node.
 *
 * @author Kilian Thiel, KNIME.com AG, Zurich
 */
public enum NumberMode {

    /**
     * rounding to decimal places.
     */
    DECIMAL_PLACES("Decimal places"),

    /**
     * rounding to significant figures.
     */
    SIGNIFICANT_FIGURES("Significant figures");

    private String m_description;

    /**
     * Constructor.
     *
     * @param description The modes description.
     */
    private NumberMode(final String description) {
        m_description = description;
    }

    /**
     * Returns the number mode value for the given description. If there exists
     * no mode for description an <code>IllegalArgumentException</code> is
     * thrown.
     * @param description The description to get the number mode value for.
     * @return Number mode value for given description
     */
    public static NumberMode valueByDescription(final String description) {
        for (NumberMode mode : values()) {
            if (mode.description().equals(description)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("argument out of range: " + description);
    }

    /**
     * @return The description of the mode.
     */
    public String description() {
        return m_description;
    }

    /**
     * @return An array of all descriptions.
     */
    public static String[] getDescriptions() {
        List<String> descList = new ArrayList<String>();
        for (NumberMode mode : values()) {
            descList.add(mode.description());
        }
        return descList.toArray(new String[]{});
    }
}
