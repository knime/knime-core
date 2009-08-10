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
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   26.07.2009 (Fabian Dill): created
 */
package org.knime.core.data.date;

import java.io.IOException;
import java.util.Calendar;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class DateCellSerializer implements DataCellSerializer<DateCell> {

    /**
     * {@inheritDoc}
     */
    @Override
    public DateCell deserialize(final DataCellDataInput input) 
        throws IOException {
        int year = input.readInt();
        int month = input.readInt();
        int day = input.readInt();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        return new DateCell(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(final DateCell cell, final DataCellDataOutput output)
            throws IOException {
        output.writeInt(cell.getYear());
        output.writeInt(cell.getMonth());
        output.writeInt(cell.getDay());

    }

}
