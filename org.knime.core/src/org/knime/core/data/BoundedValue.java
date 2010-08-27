/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   Oct 11, 2007 (wiswedel): created
 */
package org.knime.core.data;

/**
 * A {@link DataCell} should implement this interface to indicate that its
 * objects can be reasonably ordered. &quot;Reasonably&quot; refers here to:
 * <ul>
 * <li>There is more than just a trivial ordering (for instance lexicographical
 * ordering) defined for this cell type. Particularly, the preferred value
 * class of a DataCell (see <a href="DataCell.html#preferredvalueclass">here</a>
 * for details) should expose a non-trivial comparator in its 
 * {@link DataValue.UtilityFactory}.</li>
 * <li>Determining the bounds of a table column whose {@link DataType} is
 * compatible with this interface is desirable (see below).</li>
 * </ul>
 * 
 * <p>
 * This interface should be used, for instance for numeric data cells such as
 * {@link org.knime.core.data.def.DoubleCell} or
 * {@link org.knime.core.data.def.IntCell}, it should also be used for ordinal
 * columns (so far there is no ordinal column type in KNIME but if there were it
 * would implement this interface). On the other hand, types such as
 * {@link org.knime.core.data.def.StringCell} or
 * {@link org.knime.core.data.def.ComplexNumberCell} do not implement this
 * interface as they do not define a meaningful comparator.
 * 
 * <p>
 * Although not strictly enforced, a {@link DataColumnSpec} will have non-null
 * lower and upper bounds in its {@link DataColumnDomain} if the column's
 * {@link DataType} is compatible to this interface. It will typically not have
 * bounds for other types.
 * 
 * <p>
 * This interface does not specify any methods. It is only used to indicate the
 * above outlined property.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface BoundedValue extends DataValue {

}
