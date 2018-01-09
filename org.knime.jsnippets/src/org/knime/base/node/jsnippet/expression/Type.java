/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 *
 * History
 *   12.12.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.expression;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

/**
 * Types for the java snippet.
 *
 * @author Heiko Hofer
 */
public final class Type {
    /** the string type. */
    public static final String tString = "";

    /** the integer type. */
    public static final Integer tInt = 0;

    /** the double type. */
    public static final Double tDouble = 0.0;

    /** the long type. */
    public static final Long tLong = 0l;

    /** the boolean type. */
    public static final Boolean tBoolean = false;

    /** the java.util.Date type. */
    public static final Date tDate = new Date(0);

    /** the java.util.Calendar type. */
    public static final Calendar tCalendar = new GregorianCalendar();

    /** the DOM type. */
    public static Document tXML = null;
    static {
        try {
            tXML = DocumentBuilderFactory.newInstance().
                         newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            // should never be reached
        }
    }

    private Type() {
        // utility classes do not need to be instantiated.
    }

    /**
     * Get the identifier associated with the given type.
     * @param type the type
     * @return the identifier associated with the given type
     */
    @SuppressWarnings("rawtypes")
    public static String getIdentifierFor(final Class type) {
        if (type.equals(String.class)) {
            return "tString";
        } else if (type.equals(Integer.class)) {
            return "tInt";
        } else if (type.equals(Double.class)) {
            return "tDouble";
        } else if (type.equals(Long.class)) {
            return "tBoolean";
        } else if (type.equals(Boolean.class)) {
            return "tString";
        } else if (type.equals(Date.class)) {
            return "tDate";
        } else if (type.equals(Calendar.class)) {
            return "tCalendar";
        } else if (type.equals(Document.class)) {
            return "tXML";
        }
        throw new TypeException("Unknown type: " + type.getName());
    }

    /**
     * Get the class of the given value. The value is supposed to be a member
     * value of this class.
     * @param t the variable
     * @return the class
     */
    @SuppressWarnings("rawtypes")
    public static Class getMembersClass(final Object t) {
        if (t == tCalendar) {
            return Calendar.class;
        } else if (t == tXML) {
            return Document.class;
        } else {
            return t.getClass();
        }
    }
}
