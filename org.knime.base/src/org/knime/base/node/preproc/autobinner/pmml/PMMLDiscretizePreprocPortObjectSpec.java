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
 *   15.07.2010 (hofer): created
 */
package org.knime.base.node.preproc.autobinner.pmml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocOperation;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocPortObjectSpec;
import org.xml.sax.SAXException;

/**
 *
 * @author Heiko Hofer
 */
public final class PMMLDiscretizePreprocPortObjectSpec extends PMMLPreprocPortObjectSpec {
    private PMMLPreprocDiscretize m_op;

    /**
     * @return PMMLPreprocPortObjectSpec singleton
     */
    public static PortObjectSpecSerializer<PMMLPreprocPortObjectSpec>
    getPortObjectSpecSerializer() {
        return new PortObjectSpecSerializer<PMMLPreprocPortObjectSpec>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void savePortObjectSpec(
                    final PMMLPreprocPortObjectSpec portObjectSpec,
                    final PortObjectSpecZipOutputStream out)
                    throws IOException {
                try {
                    PMMLPreprocDiscretize op =
                        ((PMMLDiscretizePreprocPortObjectSpec) portObjectSpec).getOperation();
                    out.putNextEntry(new ZipEntry(op.getClass().getName()));

                    PortObjectZipOutputStreamAndString sout
                        = new PortObjectZipOutputStreamAndString(out);
                    TransformerHandler handler =
                        createTransformerHandlerForSave(sout);
                    String writeElement = op.getTransformElement().toString();
                    handler.startElement(null, null, writeElement, null);
                    op.save(handler, null);
                    handler.endElement(null, null, writeElement);

                        handler.endDocument();

                    out.closeEntry();
                    out.close();
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public PMMLPreprocPortObjectSpec loadPortObjectSpec(
                    final PortObjectSpecZipInputStream in) throws IOException {
                PMMLPreprocOperation op = null;
                ZipEntry entry;
                while ((entry =  in.getNextEntry()) != null) {
                    String clazzName = entry.getName();
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(clazzName);
                        if (!PMMLPreprocOperation.class.isAssignableFrom(clazz)) {
                            // throw exception
                            throw new IllegalArgumentException(
                                    "Class "
                                            + clazz.getName()
                                            + " must extend PMMLPreprocOperation! "
                                            + "Loading failed!");
                        }
                        op =
                                (PMMLPreprocOperation)clazz.newInstance();
                        SAXParserFactory fac = SAXParserFactory.newInstance();
                        SAXParser parser;
                        parser = fac.newSAXParser();
                        parser.parse(new NonClosableInputStream(in),
                                op.getHandlerForLoad());

                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                    in.closeEntry();
                }

                return new PMMLDiscretizePreprocPortObjectSpec((PMMLPreprocDiscretize)op);
            }
        };
    }

    static TransformerHandler createTransformerHandlerForSave(
            final OutputStream out) throws TransformerConfigurationException,
            SAXException {
        SAXTransformerFactory fac =
                (SAXTransformerFactory)TransformerFactory.newInstance();
        TransformerHandler handler = fac.newTransformerHandler();
        Transformer t = handler.getTransformer();
//        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        handler.setResult(new StreamResult(out));
        handler.startDocument();
        return handler;
    }

    /**
     * @param op
     */
    public PMMLDiscretizePreprocPortObjectSpec(final PMMLPreprocDiscretize op) {
        m_op = op;
    }

    /**
     * @return
     */
    public PMMLPreprocDiscretize getOperation() {
        return m_op;
    }

    /**
     * Wraps an PortObjectZipOutputStream and collects all data that is written
     * to the stream in a string.
     *
     * @author Dominik Morent, KNIME.com, Zurich, Switzerland
     */
    private static class PortObjectZipOutputStreamAndString extends OutputStream {
        private StringBuilder m_buffer;
        private ZipOutputStream m_zipOut;

        /**
         * @param zipOut
         */
        public PortObjectZipOutputStreamAndString(
                final ZipOutputStream zipOut) {
            super();
            m_buffer = new StringBuilder();
            m_zipOut = zipOut;
        }

        @Override
        public void write(final int b) throws IOException {
          m_buffer.append((char) b);
          m_zipOut.write(b);
        }

        public String getString() {
          return m_buffer.toString();
        }

        /**
         * @throws IOException
         * @see java.util.zip.ZipOutputStream#close()
         */
        @Override
        public void close() throws IOException {
            m_zipOut.close();
        }

        /**
         * @throws IOException
         * @see java.util.zip.ZipOutputStream#closeEntry()
         */
        public void closeEntry() throws IOException {
            m_zipOut.closeEntry();
        }

        /**
         * @param obj
         * @return
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(final Object obj) {
            return m_zipOut.equals(obj);
        }

        /**
         * @throws IOException
         * @see java.util.zip.ZipOutputStream#finish()
         */
        public void finish() throws IOException {
            m_zipOut.finish();
        }

        /**
         * @throws IOException
         * @see java.io.FilterOutputStream#flush()
         */
        @Override
        public void flush() throws IOException {
            m_zipOut.flush();
        }

        /**
         * @return
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return m_zipOut.hashCode();
        }

        /**
         * @param e
         * @throws IOException
         * @see java.util.zip.ZipOutputStream#putNextEntry(java.util.zip.ZipEntry)
         */
        public void putNextEntry(final ZipEntry e) throws IOException {
            m_zipOut.putNextEntry(e);
        }

        /**
         * @param comment
         * @see java.util.zip.ZipOutputStream#setComment(java.lang.String)
         */
        public void setComment(final String comment) {
            m_zipOut.setComment(comment);
        }

        /**
         * @param level
         * @see java.util.zip.ZipOutputStream#setLevel(int)
         */
        public void setLevel(final int level) {
            m_zipOut.setLevel(level);
        }

        /**
         * @param method
         * @see java.util.zip.ZipOutputStream#setMethod(int)
         */
        public void setMethod(final int method) {
            m_zipOut.setMethod(method);
        }

        /**
         * @return
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return m_zipOut.toString();
        }

        /**
         * @param b
         * @param off
         * @param len
         * @throws IOException
         * @see java.util.zip.ZipOutputStream#write(byte[], int, int)
         */
        @Override
        public void write(final byte[] b, final int off, final int len)
            throws IOException {

            for (int i = off; i < off + len; i++) {
                m_buffer.append((char) b[i]);
            }
            m_zipOut.write(b, off, len);
        }

        /**
         * @param b
         * @throws IOException
         * @see java.io.FilterOutputStream#write(byte[])
         */
        @Override
        public void write(final byte[] b) throws IOException {
            for (byte c : b) {
                m_buffer.append((char)c);
            }
            m_zipOut.write(b);
        }
      }

}
