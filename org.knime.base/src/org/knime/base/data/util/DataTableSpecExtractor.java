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
 *   03.06.2012 (kilian): created
 */
package org.knime.base.data.util;


/**
 * This utility class provides means to extract meta information from a
 * specific data table spec and to return the extracted data as data table.
 * The extracted information consists of the column names, type, and indices,
 * the lower and upper bounds, if available (otherwise missing values), the
 * information whether there are color, size, or shape handler associated with
 * the columns and the possible values as collection cell. It can be set
 * whether the information of property handlers as well as the possible values
 * are extracted or not.
 *
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 * @since 2.6
 * @deprecated Use {@link org.knime.core.data.util.DataTableSpecExtractor} instead.
 */
@Deprecated
public class DataTableSpecExtractor extends org.knime.core.data.util.DataTableSpecExtractor {

    /**
     * @param value The option whether the property handlers will be extracted (<code>true</code>) or not (
     *            <code>false</code>).
     */
    public void setExtractPropertyHandlers(final boolean value) {
        super.setPropertyHandlerOutputFormat(value
                 ? PropertyHandlerOutputFormat.Boolean : PropertyHandlerOutputFormat.Hide);
    }

    /**
     * @param value The option whether the possible values will be extracted (<code>true</code>)
     *              or not (<code>false</code>).
     */
    public void setExtractPossibleValuesAsCollection(final boolean value) {
        super.setPossibleValueOutputFormat(value
               ? PossibleValueOutputFormat.Collection : PossibleValueOutputFormat.Hide);
    }

}
