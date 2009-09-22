/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   14.09.2009 (Fabian Dill): created
 */
package org.knime.core.data.date;

import java.io.IOException;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;

/**
 * Serializes a {@link TimestampCell} by writing the long representing the UTC 
 * time and the booleans whether date, time, or milliseconds are available.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class TimestampCellSerializer implements
        DataCellSerializer<TimestampCell> {

    /**
     * {@inheritDoc}
     */
    @Override
    public TimestampCell deserialize(final DataCellDataInput input)
            throws IOException {
        long utcTime = input.readLong();
        boolean hasDate = input.readBoolean();
        boolean hasTime = input.readBoolean();
        boolean hasMillis = input.readBoolean();
        TimestampCell cell = new TimestampCell(utcTime, hasDate, hasTime, 
                hasMillis);
        return cell;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(final TimestampCell cell, 
            final DataCellDataOutput output)
            throws IOException {
        output.writeLong(cell.getUTCTimeInMillis());
        output.writeBoolean(cell.hasDate());
        output.writeBoolean(cell.hasTime());
        output.writeBoolean(cell.hasMillis());
    }

}
