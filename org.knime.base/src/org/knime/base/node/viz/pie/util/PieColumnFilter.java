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
 *    12.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.util;

import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.util.ColumnFilter;


/**
 * A {@link ColumnFilter} implementation that filters all columns with no
 * valid domain and more than 250 values.
 * @author Tobias Koetter, University of Konstanz
 */
public final class PieColumnFilter implements ColumnFilter {

    private static PieColumnFilter instance;

    /**
     * Maximum number of supported sections. This boundary is also mentioned
     * in the node description!!!
     */
    public static final int MAX_NO_OF_SECTIONS = 360;

    private PieColumnFilter() {
        //avoid object creation
    }

    /**
     * @return the only instance of this singleton
     */
    public static PieColumnFilter getInstance() {
        if (instance == null) {
            instance = new PieColumnFilter();
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    public String allFilteredMsg() {
        return "No column matches filter criteria. Criteria domain "
        + "shouldn't extend " + MAX_NO_OF_SECTIONS
        + " values.";
    }

    /**
     * {@inheritDoc}
     */
    public boolean includeColumn(final DataColumnSpec colSpec) {
        if (colSpec == null) {
            throw new NullPointerException(
                    "Column specification must not be null");
        }
        return validDomain(colSpec);
    }

    /**
     * @param colSpec the {@link DataColumnSpec} to validate
     * @return <code>true</code> if the column specification is valid
     */
    public static final boolean validDomain(final DataColumnSpec colSpec) {
        if (colSpec == null) {
            throw new NullPointerException("colSpec must not be null");
        }
        final DataColumnDomain domain = colSpec.getDomain();
        if (domain == null || domain.getValues() == null) {
            return true;
        }
        if (colSpec.getType().isCompatible(NominalValue.class)) {
            if (domain.getValues().size() < 1
                    || domain.getValues().size() > MAX_NO_OF_SECTIONS) {
                return false;
            }
            return true;
        }
//        else if (colSpec.getType().isCompatible(IntValue.class)) {
//              if (domain.getLowerBound() == null
//                      || domain.getUpperBound() == null) {
//                  return false;
//              }
//           final int lower = ((IntValue)domain.getLowerBound()).getIntValue();
//           final int upper = ((IntValue)domain.getUpperBound()).getIntValue();
//              return (upper - lower < MAX_NO_OF_SECTIONS);
//          }
        return true;
    }
}
