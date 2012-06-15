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
 *   07.02.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.type.data;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.knime.base.node.jsnippet.expression.TypeException;
import org.knime.core.data.DataCell;
import org.knime.core.data.date.DateAndTimeCell;
import org.xml.sax.SAXException;

/**
 * Converter to create a XMLCell from a java object.
 *
 * @author Heiko Hofer
 */
public class JavaToDateAndTimeCell extends JavaToDataCell {
    /**
     * Create a new instance.
     */
    public JavaToDateAndTimeCell() {
        super(Date.class, Calendar.class);
    }

    /**
     * {@inheritDoc}
     * @throws IOException if an io error occurs while reading the XML string
     * @throws SAXException if an error occurs while parsing
     * @throws ParserConfigurationException if the parser cannot be instantiated
     * @throws XMLStreamException if error occurs while parsing
     */
    @Override
    public DataCell createDataCell(final Object value)
        throws TypeException, IOException, ParserConfigurationException,
            SAXException, XMLStreamException {
        if (canProcess(value)) {
            if (value instanceof Calendar) {
                Calendar calendar = (Calendar)value;
                return new DateAndTimeCell(calendar.getTimeInMillis(),
                        calendarHasDate(calendar),
                        calendarHasTime(calendar),
                        calendarHasMillis(calendar));
            } else { // value instanceof Date
                Date date = (Date)value;
                return new DateAndTimeCell(date.getTime(),
                        true, true, true);
            }
        } else {
            throw new TypeException("The data cell of type "
                    + "\"Date and Time\""
                    + " cannot be created from an java object of type "
                    + value.getClass().getSimpleName());
        }
    }

    /** Return true when given calendar has time in the sense of hasDate from
     * {@link DateAndTimeCell}.
     */
    private boolean calendarHasDate(final Calendar calendar) {
        // see Calendar for field documentation
        return calendarIsSet(calendar, 0, 8);
    }

    /** Return true when given calendar has date in the sense of hasTime from
     * {@link DateAndTimeCell}.
     */
    private boolean calendarHasTime(final Calendar calendar) {
        // see Calendar for field documentation
        return calendarIsSet(calendar, 9, 13);
    }

    /** Return true when given calendar has milliseconds in the sense of
     * hasMillis from {@link DateAndTimeCell}.
     */
    private boolean calendarHasMillis(final Calendar calendar) {
        // see Calendar for field documentation
        return calendarIsSet(calendar, 14, 14);
    }

    /** Test if the fields from min to max are set on the calendar. */
    private boolean calendarIsSet(final Calendar calendar,
            final int min, final int max) {
        for (int i = min; i <= max; i++) {
            if (calendar.isSet(i)) {
                return true;
            }
        }
        return false;
    }
}
