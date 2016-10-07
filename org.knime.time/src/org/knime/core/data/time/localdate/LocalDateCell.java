/*
 * ------------------------------------------------------------------------
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
 *   13.09.2009 (Fabian Dill): created
 */
package org.knime.core.data.time.localdate;

import java.time.LocalDate;
import java.util.Objects;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;

/**
 * Cell implementation to represent local date in KNIME such as 2016-10-05.
 * It's the default implementation of {@link LocalDateValue}.
 *
 * @since 3.3
 * @see LocalDateCellFactory
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
@SuppressWarnings("serial")
public final class LocalDateCell extends DataCell implements LocalDateValue, BoundedValue, StringValue {

    /** {@link DataType} of this cell. */
    static final DataType TYPE = DataType.getType(LocalDateCell.class);

    private final LocalDate m_localDate;

    /** Package scope constructor, called from factory.
     * @param localDate Non-null argument.
     */
    LocalDateCell(final LocalDate localDate) {
        m_localDate = Objects.requireNonNull(localDate);
    }

    @Override
    public LocalDate getLocalDate() {
        return m_localDate;
    }

    @Override
    public String getStringValue() {
        return m_localDate.toString();
    }

    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((LocalDateCell)dc).m_localDate.equals(m_localDate);
    }

    @Override
    public int hashCode() {
        return m_localDate.hashCode();
    }

    @Override
    public String toString() {
        return getStringValue();
    }

}
