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
 *   14.09.2009 (Fabian Dill): created
 */
package org.knime.core.data.time.localtime;

import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoField;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;

/**
 * Serializes a {@link LocalTimeCell} using a single integer (nanoOfDay).
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 3.3
 * @noreference This class is not intended to be referenced by clients.
 */
public final class LocalTimeCellSerializer implements DataCellSerializer<LocalTimeCell> {

    @Override
    public LocalTimeCell deserialize(final DataCellDataInput input) throws IOException {
        final long nanoOfDay = input.readLong();
        final LocalTime localTime = LocalTime.ofNanoOfDay(nanoOfDay);
        return new LocalTimeCell(localTime);
    }

    @Override
    public void serialize(final LocalTimeCell cell, final DataCellDataOutput output) throws IOException {
        final LocalTime localTime = cell.getLocalTime();
        final long nanoOfDay = localTime.getLong(ChronoField.NANO_OF_DAY);
        output.writeLong(nanoOfDay);
    }

}
