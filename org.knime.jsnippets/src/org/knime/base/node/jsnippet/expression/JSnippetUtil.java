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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   15.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.expression;

import java.sql.Date;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.data.xml.XMLValue;
import org.w3c.dom.Document;

/**
 *
 * @author Heiko Hofer
 */
public class JSnippetUtil {
	/**
	 * @param type
	 * @return
	 */
	public static String createJavaTypeForDataType(final DataType type) {
		if (type.equals(XMLCell.TYPE)) {
			return "Document";
		} else if (type.equals(DateAndTimeCell.TYPE)) {
			return "Date";
		} else if (type.equals(LongCell.TYPE)) {
			return "Long";
		} else if (type.equals(IntCell.TYPE)) {
			return "Integer";
		} else if (type.equals(DoubleCell.TYPE)) {
			return "Double";
		} else if (type.equals(StringCell.TYPE)) {
			return "String";
		} else {
			throw new IllegalStateException("No supported data type: "
					+ type);
		}
	}

	public static <T> T getJavaValue(final DataCell cell, final T t)
		throws TypeException {
		Class c = t.getClass();
		Class<? extends DataValue> type =
			JSnippetUtil.getDataValueClassForJavaClass(c);
		if (cell.getType().isCompatible(type)) {
			if (c.equals(Document.class)) {
				return (T)((XMLValue)cell).getDocument();
			} else if (c.equals(Date.class)) {
				return (T)((DateAndTimeValue)cell).getUTCCalendarClone().
				getTime();
			} else if (c.equals(Long.class)) {
				return (T)new Long(((LongValue)cell).getLongValue());
			} else if (c.equals(Integer.class)) {
				return (T)new Integer(((IntValue)cell).getIntValue());
			} else if (c.equals(Double.class)) {
				return (T)new Double(((DoubleValue)cell).getDoubleValue());
			} else if (c.equals(Boolean.class)) {
				return (T)new Boolean(((BooleanValue)cell).getBooleanValue());
			} else if (c.equals(String.class)) {
				return (T)((StringValue)cell).getStringValue();
			} else {
				// not reachable
				throw new IllegalStateException(
						"Reached not reachable code.");
			}
		} else {
			throw new TypeException("Incompatible type.");
		}
	}

	public static Class<? extends DataValue> getDataValueClassForJavaClass(
			final Class c) {
		if (c.equals(Document.class)) {
			return XMLValue.class;
		} else if (c.equals(Date.class)) {
			return DateAndTimeValue.class;
		} else if (c.equals(Long.class)) {
			return LongValue.class;
		} else if (c.equals(Integer.class)) {
			return IntValue.class;
		} else if (c.equals(Double.class)) {
			return DoubleValue.class;
		} else if (c.equals(Boolean.class)) {
			return BooleanValue.class;
		} else if (c.equals(String.class)) {
			return StringValue.class;
		} else {
			throw new IllegalStateException("Unknown data type for class "
					+ c.getName());
		}
	}

	public static DataCell getDataCellForJavaValue(final DataType type, final Object value) {
		if (type.equals(XMLCell.TYPE)) {
			if (value instanceof Document) {
				return XMLCellFactory.create((Document)value);
			} else if (value instanceof String) {
				try {
					return XMLCellFactory.create((String)value);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			} else {
				throw new IllegalStateException("Incompatible types");
			}
		} else if (type.equals(DateAndTimeCell.TYPE)) {
//			if (value instanceof Document) {
//				return new DateAndTimeCell
//				return XMLCellFactory.create((Document)value);
//			} else if (value instanceof String) {
//				return XMLCellFactory.create((String)value);
//			} else {
			// TODO Ask Bernd how to create DateAndTimeCells from Date
				throw new IllegalStateException("Incompatible types");
//			}
		} else if (type.equals(LongCell.TYPE)) {
			if (value instanceof Long) {
				return new LongCell((Long)value);
			} else {
				throw new IllegalStateException("Incompatible types");
			}
		} else if (type.equals(IntCell.TYPE)) {
			if (value instanceof Integer) {
				return new IntCell((Integer)value);
			} else {
				throw new IllegalStateException("Incompatible types");
			}
		} else if (type.equals(DoubleCell.TYPE)) {
			if (value instanceof Double) {
				return new DoubleCell((Double)value);
			} else {
				throw new IllegalStateException("Incompatible types");
			}
		} else if (type.equals(BooleanCell.TYPE)) {
			if (value instanceof Boolean) {
				if ((Boolean)value) {
					return BooleanCell.TRUE;
				} else {
					return BooleanCell.FALSE;
				}
			} else {
				throw new IllegalStateException("Incompatible types");
			}
		} else if (type.equals(StringCell.TYPE)) {
			if (value instanceof String) {
				return new StringCell((String)value);
			} else {
				throw new IllegalStateException("Incompatible types");
			}
		} else {
			throw new IllegalStateException("Unknown data type " + type);
		}
	}

}
