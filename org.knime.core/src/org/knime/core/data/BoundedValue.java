/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
