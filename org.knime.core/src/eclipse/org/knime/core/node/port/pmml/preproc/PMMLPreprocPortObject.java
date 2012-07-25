/*
 *
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
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 15, 2010 (morent): created
 */
package org.knime.core.node.port.pmml.preproc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;
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
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.AbstractPortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.xml.sax.SAXException;

/**
 * The PMML preprocessing operations can now be included directly in the
 * {@link PMMLPortObject} instead of handling them in a special port type.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
@Deprecated
public class PMMLPreprocPortObject extends AbstractPortObject {
    /** Convenience accessor for the port type. */
    public static final PortType TYPE =
            new PortType(PMMLPreprocPortObject.class);

    protected static final String LOCAL_TRANS = "LocalTransformations";
    /** Constant for CDATA. */
    protected static final String CDATA = "CDATA";

    private final List<PMMLPreprocOperation> m_operations
            = new LinkedList<PMMLPreprocOperation>();

    private PMMLPreprocPortObjectSpec m_spec;

    /**
     *
     */
    public PMMLPreprocPortObject() {
        // necessary for loading
    }

    public PMMLPreprocPortObject(final PMMLPreprocOperation ... operations) {
        List<String> columnNames = new ArrayList<String>();
        for (PMMLPreprocOperation op : operations) {
            m_operations.add(op);
            columnNames.addAll(op.getColumnNames());
        }
        m_spec = new PMMLPreprocPortObjectSpec(columnNames);
    }

    public PMMLPreprocPortObject(final PMMLPreprocPortObject ... port) {
        List<String> columnNames = new ArrayList<String>();
        for (PMMLPreprocPortObject p : port) {
            for (PMMLPreprocOperation op : p.getOperations()) {
                m_operations.add(op);
                columnNames.addAll(op.getColumnNames());
            }
        }
        m_spec = new PMMLPreprocPortObjectSpec(columnNames);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final PortObjectZipInputStream in,
            final PortObjectSpec spec, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
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
                PMMLPreprocOperation op =
                        (PMMLPreprocOperation)clazz.newInstance();
                SAXParserFactory fac = SAXParserFactory.newInstance();
                SAXParser parser;
                parser = fac.newSAXParser();
                parser.parse(new NonClosableInputStream(in),
                        op.getHandlerForLoad());
                m_operations.add(op);
            } catch (Exception e) {
                throw new IOException(e);
            }
            in.closeEntry();
        }
        m_spec = (PMMLPreprocPortObjectSpec)spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final PortObjectZipOutputStream out,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (m_operations.size() == 0) {
            return;
        }
        try {
            int size = m_operations.size();
            final double subProgress = 1.0 / size;
            int n = 0;
            for (PMMLPreprocOperation op : m_operations) {
                out.putNextEntry(new ZipEntry(op.getClass().getName()));

                PortObjectZipOutputStreamAndString sout
                    = new PortObjectZipOutputStreamAndString(out);
                TransformerHandler handler =
                    createTransformerHandlerForSave(sout);
                String writeElement = op.getTransformElement().toString();
                handler.startElement("", "", writeElement, null);
                op.save(handler,
                        exec.createSubProgress(subProgress));
                handler.endElement("", "", writeElement);
                handler.endDocument();

                out.closeEntry();
                exec.setProgress(subProgress * ++n);
                exec.checkCanceled();
            }
            out.close();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public String toString()  {
//        if (m_operations.size() == 0) {
//            return "";
//        }
//        try {
//            int size = m_operations.size();
//            final double subProgress = 1.0 / size;
//            int n = 0;
//
//            for (PMMLPreprocOperation op : m_operations) {
//                out.putNextEntry(new ZipEntry(op.getClass().getName()));
//
//                PortObjectZipOutputStreamAndString sout
//                    = new PortObjectZipOutputStreamAndString(out);
//                TransformerHandler handler =
//                    createTransformerHandlerForSave(sout);
//                op.save(handler,
//                        exec.createSubProgress(subProgress));
//                handler.endElement(null, null, LOCAL_TRANS);
//                handler.endDocument();
//                System.out.println(sout.getString());
//                out.closeEntry();
//                exec.setProgress(subProgress * ++n);
//                exec.checkCanceled();
//            }
//            out.close();
//        } catch (Exception e) {
//            throw new IOException(e);
//        }
//    }

    static TransformerHandler createTransformerHandlerForSave(
            final OutputStream out) throws TransformerConfigurationException,
            SAXException {
        SAXTransformerFactory fac =
                (SAXTransformerFactory)TransformerFactory.newInstance();
        TransformerHandler handler = fac.newTransformerHandler();
        Transformer t = handler.getTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        handler.setResult(new StreamResult(out));
        handler.startDocument();
        return handler;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPreprocPortObjectSpec getSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        StringBuffer sb = new StringBuffer();
        for (PMMLPreprocOperation op : m_operations) {
            sb.append(op.getSummary());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        return new JComponent[] {new PMMLPreprocPortObjectView(this)};
    }



    /**
     * Wraps an PortObjectZipOutputStream and collects all data that is written
     * to the stream in a string.
     *
     * @author Dominik Morent, KNIME.com, Zurich, Switzerland
     */
    private static class PortObjectZipOutputStreamAndString extends OutputStream {
        private final StringBuilder m_buffer;
        private final PortObjectZipOutputStream m_zipOut;

        /**
         * @param zipOut
         */
        public PortObjectZipOutputStreamAndString(
                final PortObjectZipOutputStream zipOut) {
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
            if (!(obj instanceof PortObjectZipOutputStreamAndString)) {
                return false;
            }
            return m_zipOut.equals(
                    ((PortObjectZipOutputStreamAndString)obj).m_zipOut);
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



    /**
     * @return the operations
     */
    public List<PMMLPreprocOperation> getOperations() {
        return Collections.unmodifiableList(m_operations);
    }



}
