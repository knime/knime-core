/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Nov 27, 2008 (wiswedel): created
 */
package org.knime.testing.data.blob;

import java.io.IOException;
import java.util.Random;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.node.NodeLogger;

/**
 * Blob cell that keeps a string identifier and whose size is artifically
 * increased by several 100k (random bytes)
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LargeBlobCell extends BlobDataCell {
    
    public static final DataType TYPE = DataType.getType(LargeBlobCell.class);
    
    public static final int SIZE_OF_CELL = 1024 * 1024;

    /** Don't compress this cell. */
    public static final boolean USE_COMPRESSION = false;
    
    private final String m_identifier;
    
    public static final DataCellSerializer<LargeBlobCell> getCellSerializer() {
        return new DataCellSerializer<LargeBlobCell>() {
            private final Random m_random;
            
            {
                long time = System.currentTimeMillis();
                NodeLogger.getLogger(LargeBlobCell.class).debug(
                        "Using seed " + time);
                m_random = new Random(time);
            }
            
            /** {@inheritDoc} */
            @Override
            public LargeBlobCell deserialize(final DataCellDataInput input)
                    throws IOException {
                for (int i = 0; i < SIZE_OF_CELL / 2; i++) {
                    input.readByte();
                }
                String identifier = input.readUTF();
                for (int i = 0; i < SIZE_OF_CELL / 2; i++) {
                    input.readByte();
                }
                return new LargeBlobCell(identifier);
            }
            
            /** {@inheritDoc} */
            @Override
            public void serialize(LargeBlobCell cell, DataCellDataOutput output)
                    throws IOException {
                byte[] ar = new byte[SIZE_OF_CELL / 2];
                m_random.nextBytes(ar);
                output.write(ar);
                output.writeUTF(cell.m_identifier);
                m_random.nextBytes(ar);
                output.write(ar);
            }
        };
    }
    
    /**
     * 
     */
    public LargeBlobCell(final String identifier) {
        if (identifier == null) {
            throw new NullPointerException();
        }
        m_identifier = identifier;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((LargeBlobCell)dc).m_identifier.equals(m_identifier);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_identifier.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_identifier;
    }

}
