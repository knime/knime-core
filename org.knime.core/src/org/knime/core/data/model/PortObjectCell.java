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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.data.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortUtil;


/** Default implementation for a general port object.
 * @author Thomas Gabriel, KNIME.com GmbH, Zurich
 */
public class PortObjectCell extends DataCell implements PortObjectValue {

    /** Convenience access member for
     * <code>DataType.getType(PortObjectCell.class)</code>.
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(PortObjectCell.class);

    /** @return PortObjectValue.class */
    public static Class<? extends DataValue> getPreferredValueClass() {
        return PortObjectValue.class;
    }

    /** Serializer as required by parent class.
     * @return A serializer for reading/writing cells of this kind.
     */
    public static DataCellSerializer<PortObjectCell> getCellSerializer() {
        return new DataCellSerializer<PortObjectCell>() {
            /** {@inheritDoc} */
            @Override
            public PortObjectCell deserialize(final DataCellDataInput input)
                    throws IOException {
                InputStream is = (InputStream) input;
                try {
                    PortObject po = PortUtil.readObjectFromStream(is, null);
                    return new PortObjectCell(po);
                } catch (CanceledExecutionException cee) {
                    throw new IOException(cee);
                } finally {
                    // is.close();
                }
            }
            /** {@inheritDoc} */
            @Override
            public void serialize(final PortObjectCell cell,
                    final DataCellDataOutput output) throws IOException {
                OutputStream os = (OutputStream) output;
                try {
                    PortUtil.writeObjectToStream(cell.m_content, os, null);
                } catch (CanceledExecutionException cee) {
                    throw new IOException(cee);
                } finally {
                    // os.close();
                }
            }
        };
    }

    private final PortObject m_content;

    /** Constructor for creating port object cell objects.
     * @param content The port object to wrap.
     */
    public PortObjectCell(final PortObject content) {
        if (content == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_content = content;
    }

    /** {@inheritDoc} */
    @Override
    public PortObject getPortObject() {
        return m_content;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_content.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        PortObjectCell ic = (PortObjectCell) dc;
        return m_content.equals(ic.m_content);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_content.hashCode();
    }

}
