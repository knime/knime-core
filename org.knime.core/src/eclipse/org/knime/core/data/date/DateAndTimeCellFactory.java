/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   21.08.2015 (thor): created
 */
package org.knime.core.data.date;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Pattern;

import org.knime.core.data.ConfigurableDataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory.FromComplexString;
import org.knime.core.data.DataCellFactory.FromSimpleString;
import org.knime.core.data.DataType;

/**
 * Factory for creating {@link DateAndTimeCell}s from various input types.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 3.0
 */
public final class DateAndTimeCellFactory implements FromSimpleString, FromComplexString, ConfigurableDataCellFactory {
    /**
     * The data type for the cells created by this factory.
     */
    public static final DataType TYPE = DateAndTimeCell.TYPE;

    private static final Collection<String> PREDEFINED_PARAMETERS =
        Collections.unmodifiableCollection(Arrays.asList("yyyy-MM-dd;HH:mm:ss.S", "dd.MM.yyyy;HH:mm:ss.S",
            "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy/dd/MM", "dd.MM.yyyy", "yyyy-MM-dd", "HH:mm:ss"));

    private static final Pattern MS_PATTERN = Pattern.compile("\\.[0-9]{1,3}$");

    private static final Pattern DATE_PATTERN = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}");

    private static final Pattern TIME_PATTERN = Pattern.compile("[0-9]{2}:[0-9]{2}:[0-9]{2}");

    private DateFormat m_format = DateFormat.getDateInstance();

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell createCell(final String s) {
        return create(s, m_format);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getDataType() {
        return TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final String parameters) {
        if ((parameters != null) && !parameters.isEmpty()) {
            m_format = new SimpleDateFormat(parameters);
        } else {
            m_format = DateFormat.getDateInstance();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParameterDescription() {
        return "Specify the concrete date format using the syntax from Java's SimpleDateFormat.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getPredefinedParameters() {
        return PREDEFINED_PARAMETERS;
    }

    /**
     * Creates a new DateAndTimeCell from the given string. All strings created by DateAndTimeCell.toString() or
     * DateAndTimeCell.getStringValue() are accepted.
     * The string must consist of only a date, only a time or both, separated with the capital letter "T".
     * The milliseconds part of the time is optional.
     * The following formats are allowed:
     * Date: yyyy-MM-dd
     * Time: HH:mm:ss
     * Time with milliseconds: HH:mm:ss.S
     * Date and time: yyyy-MM-dd'T'HH:mm:ss
     * Date and time with milliseconds: yyyy-MM-dd'T'HH:mm:ss.S
     * @param s the string to parse into date
     * @return the cell containing the parsed date
     * @throws IllegalArgumentException when the string cannot be parsed
     **/
    public static DataCell create(final String s) {
        boolean hasMillis = MS_PATTERN.matcher(s).find();
        boolean hasDate = DATE_PATTERN.matcher(s).find();
        boolean hasTime = TIME_PATTERN.matcher(s).find();
        if (!(hasMillis || hasDate || hasTime) || (!hasTime && hasMillis)) {
            throw new IllegalArgumentException("The given string does not conform to the required format.");
        }

        DateFormat format = DateAndTimeCell.getFormat(hasDate, hasTime, hasMillis);
        synchronized(format) {
            return create(s, format);
        }
    }

    /**
     * Creates a new data cell from the given string. The passed date format for this locale is used to parse the
     * date string.
     *
     * @param s any parseable date string
     * @param dateFormat a date format
     * @return a new data cell
     * @throws IllegalArgumentException if the input cannot be parsed to a date
     */
    public static DataCell create(final String s, final DateFormat dateFormat)  {
        Date date;
        try {
            date = dateFormat.parse(s);
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }

        boolean hasDay = (dateFormat instanceof SimpleDateFormat) ?
            ((SimpleDateFormat) dateFormat).toPattern().matches(".*[yYMLwWDdFEu].*") : true;
        boolean hasTime = (dateFormat instanceof SimpleDateFormat) ?
            ((SimpleDateFormat) dateFormat).toPattern().matches(".*[HhkKms].*") : true;
        boolean hasMilliseconds = (dateFormat instanceof SimpleDateFormat) ?
            ((SimpleDateFormat) dateFormat).toPattern().matches(".*[S].*") : true;

        return new DateAndTimeCell(date.getTime(), hasDay, hasTime, hasMilliseconds);
    }
}
