/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2011
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
  *   Mar 29, 2011 (morent): created
  */

package org.knime.core.data.xml;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.port.pmml.PMMLModelType;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author morent
 *
 */
public class PMMLCell extends DataCell implements PMMLValue, StringValue {
    /**
     * Type for this cell implementation.
     * Convenience access member for {@link PMMLCellFactory#TYPE}.
     */
    public static final DataType TYPE = DataType.getType(PMMLCell.class);

    private final static PMMLSerializer SERIALIZER = new PMMLSerializer();
    private static class PMMLSerializer
            implements DataCellSerializer<PMMLCell> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final PMMLCell cell,
                final DataCellDataOutput output) throws IOException {
            try {
                output.writeUTF(cell.getStringValue());
            } catch (IOException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException("Could not serialize XML", ex);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public PMMLCell deserialize(final DataCellDataInput input)
                throws IOException {
            String s = input.readUTF();
            try {
                return new PMMLCell(new PMMLCellContent(s));
            } catch (ParserConfigurationException e) {
                throw new IOException(e.getMessage(), e);
            } catch (SAXException e) {
                throw new IOException(e.getMessage(), e);
            } catch (XMLStreamException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the serializer for XML cells.
     *
     * @return a serializer
     */
    public static DataCellSerializer<PMMLCell> getCellSerializer() {
        return SERIALIZER;
    }

    /**
     * Returns the preferred value class for XML cells which is
     * {@link PMMLValue}.
     *
     * @return the preferred value class
     */
    public static Class<? extends DataValue> getPreferredValueClass() {
        return PMMLValue.class;
    }

    private final PMMLCellContent m_content;

    /**
     * Create a new instance.
     * @param content the content of this cell
     */
    PMMLCell(final PMMLCellContent content) {
        m_content = content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document getDocument() {
        return m_content.getDocument();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_content.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        PMMLCell that = (PMMLCell)dc;
        return this.m_content.equals(that.m_content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_content.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringValue() {
        return m_content.getStringValue();
    }

    /**
     * @return the content
     */
    protected XMLCellContent getContent() {
        return m_content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPMMLVersion() {
       return m_content.getPMMLVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PMMLModelType> getModelTypes() {
        return m_content.getModelTypes();
    }

}
