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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.testing.data.filestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.io.IOExceptionWithCause;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.ViewUtils;
import org.knime.testing.data.filestore.LargeFileStorePortObject.LargeFileElement;

/** A port object holding multiple {@link LargeFile} objects.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
 public final class LargeFileStorePortObject extends FileStorePortObject implements Iterable<LargeFileElement> {

    public static final PortType TYPE = new PortType(LargeFileStorePortObject.class);

    public static final class Serializer extends PortObjectSerializer<LargeFileStorePortObject> {
        @Override
        public LargeFileStorePortObject loadPortObject(final PortObjectZipInputStream in, final PortObjectSpec spec, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            ZipEntry nextEntry = in.getNextEntry();
            if (!nextEntry.getName().equals("content.xml")) {
                throw new IOException("expected content.xml, got " + nextEntry.getName());
            }
            ModelContentRO m = ModelContent.loadFromXML(in);
            in.close();
            try {
                List<LargeFileElement> list = new ArrayList<>();
                ModelContentRO elements = m.getModelContent("elements");
                for (String s : elements.keySet()) {
                    ModelContentRO element = elements.getModelContent(s);
                    list.add(LargeFileElement.load(element));
                }
                return new LargeFileStorePortObject(list, null);
            } catch (InvalidSettingsException e) {
                throw new IOExceptionWithCause(e);
            }
        }
        @Override
        public void savePortObject(final LargeFileStorePortObject portObject, final PortObjectZipOutputStream out, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            ModelContent m = new ModelContent("data");
            ModelContentWO elements = m.addModelContent("elements");
            int index = 0;
            for (LargeFileElement f : portObject) {
                ModelContentWO element = elements.addModelContent(Integer.toString(index++));
                f.save(element);
            }
            out.putNextEntry(new ZipEntry("content.xml"));
            m.saveToXML(out);
        }

    }

    private final List<LargeFileElement> m_elements;

    /** Deserialization constructor. */
    private LargeFileStorePortObject(final List<LargeFileElement> largeFileElementRestored, final Object ignored) {
        super(); // deserialization constructor
        m_elements = largeFileElementRestored;
    }

    private LargeFileStorePortObject(final List<LargeFileElement> largeFileElements) {
        super(extractFileStores(largeFileElements));
        m_elements = largeFileElements;
    }

    @Override
    protected void flushToFileStore() throws IOException {
        super.flushToFileStore();
        for (LargeFileElement f : m_elements) {
            f.getLargeFile().flushToFileStore();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<LargeFileElement> iterator() {
        return m_elements.iterator();
    }

    @Override
    protected void postConstruct() throws IOException {
        super.postConstruct();
        int count = super.getFileStoreCount();
        if (count != m_elements.size()) {
            throw new IOException(String.format("unexpected count, expected %d, actual %d", m_elements.size(), count));
        }
        for (int i = 0; i < count; i++) {
            m_elements.get(i).setLargeFile(LargeFile.restore(getFileStore(i)));
        }
    }

    @Override
    public String getSummary() {
        return m_elements.size() + " element(s)";
    }

    @Override
    public PortObjectSpec getSpec() {
        return LargeFileStorePortObjectSpec.INSTANCE;
    }

    @Override
    public JComponent[] getViews() {
        StringBuilder str = new StringBuilder("<html><body>\n");
        str.append("<table border=\"1\">");
        str.append("<tr><th>Key</th><th>Seed (expected)</th><th>Seed (actual)</th><th>correct?</th></tr>\n");
        for (LargeFileElement e : this) {
            str.append("<tr><td>").append(e.getKey()).append("</td>\n");
            str.append("<td>").append(e.getSeed()).append("</td>\n");
            String seedActual;
            boolean ok = false;
            try {
                final long read = e.getLargeFile().read();
                ok = (read == e.getSeed());
                seedActual = Long.toString(read);
            } catch (IOException ex) {
                final String message = "Error accessing file store: " + ex.getMessage();
                NodeLogger.getLogger(getClass()).error(message, ex);
                seedActual = message;
            }
            str.append("<td>").append(seedActual).append("</td>\n");
            str.append("<td>").append(ok).append("</td></tr>\n");
        }
        str.append("</table>\n");
        str.append("</body></html>");
        final JLabel jLabel = new JLabel(str.toString());
        JPanel inFlowLayout = ViewUtils.getInFlowLayout(jLabel);
        inFlowLayout.setName("Large File Store Port Object");
        return new JComponent[] {inFlowLayout};
    }

    private static List<FileStore> extractFileStores(final List<LargeFileElement> largeFileElements) {
        ArrayList<FileStore> result = new ArrayList<>();
        for (LargeFileElement f : largeFileElements) {
            result.add(f.getLargeFile().getFileStore());
        }
        return result;
    }

    public static final class PortObjectCreator {
        private final List<LargeFileElement> m_elements = new ArrayList<>();

        public void add(final String key, final long seed, final LargeFile largeFile) {
            m_elements.add(new LargeFileElement(key, seed, largeFile));
        }
        public LargeFileStorePortObject create() {
            return new LargeFileStorePortObject(Collections.unmodifiableList(new ArrayList<>(m_elements)));
        }
    }

    public static final class LargeFileElement {
        private final String m_key;
        private final long m_seed;
        private LargeFile m_largeFile;

        private LargeFileElement(final String key, final long seed, final LargeFile largeFile) {
            m_key = key;
            m_seed = seed;
            m_largeFile = largeFile;
        }

        /** @return the key */
        public String getKey() {
            return m_key;
        }
        /** @return the largeFile */
        public LargeFile getLargeFile() {
            return m_largeFile;
        }
        /** @return the seed */
        public long getSeed() {
            return m_seed;
        }
        /** @param largeFile the largeFile to set */
        void setLargeFile(final LargeFile largeFile) {
            m_largeFile = largeFile;
        }
        void save(final ModelContentWO model) {
            model.addString("key", m_key);
            model.addLong("seed", m_seed);
        }
        static LargeFileElement load(final ModelContentRO model) throws InvalidSettingsException {
            String key = model.getString("key");
            long seed = model.getLong("seed");
            return new LargeFileElement(key, seed, null);
        }

    }

}
