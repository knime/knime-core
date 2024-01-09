/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.port;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.data.filestore.internal.NotInWorkflowWriteFileStoreHandler;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.Node;
import org.knime.core.node.port.PortObject.PortObjectSerializer;
import org.knime.core.node.port.PortObjectSpec.PortObjectSpecSerializer;
import org.knime.core.util.FileUtil;

/**
 * Contains framework methods that are used to persist or read {@link PortObject} and {@link PortObjectSpec} objects.
 *
 * <p>
 * Methods in this class are not meant to be used by node developers. This class and its methods may change in future
 * versions.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class PortUtil {
    private PortUtil() {
    }

    /**
     * Get the globally used serializer for {@link PortObjectSpec} objects represented by the class argument.
     *
     * @param <T> The specific PortObjectSpec class of interest.
     * @param c Class argument.
     * @return The serializer to be used. Will throw an undeclared runtime exception if retrieving the type causes
     *         problems (suggests a coding problem)
     */
    @SuppressWarnings("unchecked")
    // access to CLASS_TO_SERIALIZER_MAP
    public static <T extends PortObjectSpec> PortObjectSpecSerializer<T> getPortObjectSpecSerializer(final Class<T> c) {
        return (PortObjectSpecSerializer<T>)PortTypeRegistry.getInstance().getSpecSerializer(c)
            .orElseThrow(() -> new RuntimeException(
                String.format("No serializer for spec class \"%s\" available - grep log files for details",
                    c.getClass().getName())));
    }

    /**
     * Get the globally used serializer for {@link PortObject} objects represented by the class argument.
     *
     * @param <T> The specific PortObject class of interest.
     * @param cl Class argument.
     * @return The serializer to be used. Will throw an undeclared runtime exception if retrieving the type causes
     *         problems (suggests a coding problem)
     */
    @SuppressWarnings("unchecked")
    // access to CLASS_TO_SERIALIZER_MAP
    public static <T extends PortObject> PortObjectSerializer<T> getPortObjectSerializer(final Class<T> cl) {
        return (PortObjectSerializer<T>)PortTypeRegistry.getInstance().getObjectSerializer(cl)
            .orElseThrow(() -> new RuntimeException(
                String.format("No serializer for object class \"%s\" available - grep log files for details",
                    cl.getClass().getName())));
    }

    public static PortObjectSpecZipOutputStream getPortObjectSpecZipOutputStream(final OutputStream in)
        throws IOException {
        PortObjectSpecZipOutputStream zipOut = new PortObjectSpecZipOutputStream(in);
        zipOut.putNextEntry(new ZipEntry("portSpec.file"));
        return zipOut;
    }

    public static PortObjectZipOutputStream getPortObjectZipOutputStream(final OutputStream in) throws IOException {
        PortObjectZipOutputStream zipOut = new PortObjectZipOutputStream(in);
        zipOut.putNextEntry(new ZipEntry("portObject.file"));
        return zipOut;
    }

    public static PortObjectSpecZipInputStream getPortObjectSpecZipInputStream(final InputStream in)
        throws IOException {
        PortObjectSpecZipInputStream zipIn = new PortObjectSpecZipInputStream(in);
        ZipEntry entry = zipIn.getNextEntry();
        if (!"portSpec.file".equals(entry.getName())) {
            throw new IOException("Expected zip entry 'portSpec.file', got '" + entry.getName() + "'");
        }
        return zipIn;
    }

    public static PortObjectZipInputStream getPortObjectZipInputStream(final InputStream in) throws IOException {
        PortObjectZipInputStream zipIn = new PortObjectZipInputStream(in);
        ZipEntry entry = zipIn.getNextEntry();
        if (!"portObject.file".equals(entry.getName())) {
            throw new IOException("Expected zip entry 'portObject.file', got '" + entry.getName() + "'");
        }
        return zipIn;
    }

    public static void writeObjectToFile(final PortObject po, final File file, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        try (FileOutputStream f = new FileOutputStream(file)) {
            writeObjectToStream(po, f, exec);
        }
    }

    /**
     * Write the given port object into the given output stream. The output stream does not need to be buffered and is
     * not closed after calling this method.
     *
     * @param po any port object
     * @param output any output stream, does not need to be buffered
     * @param exec execution context for reporting progress and checking for cancelation
     * @throws IOException if an I/O error occurs while serializing the port object
     * @throws CanceledExecutionException if the user canceled the operation
     */
    public static void writeObjectToStream(final PortObject po, final OutputStream output, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        final boolean originalOutputIsBuffered =
            ((output instanceof BufferedOutputStream) || (output instanceof ByteArrayOutputStream));
        OutputStream os = originalOutputIsBuffered ? output : new BufferedOutputStream(output);

        final ZipOutputStream zipOut = new ZipOutputStream(os);

        PortObjectSpec spec = po.getSpec();
        zipOut.putNextEntry(new ZipEntry("content.xml"));
        ModelContent toc = new ModelContent("content");
        toc.addInt("version", 1);
        toc.addString("port_spec_class", spec.getClass().getName());
        toc.addString("port_object_class", po.getClass().getName());
        NotInWorkflowWriteFileStoreHandler fileStoreHandler = null;
        if (po instanceof FileStorePortObject) {
            fileStoreHandler = NotInWorkflowWriteFileStoreHandler.create();
            ModelContentWO fileStoreModelContent = toc.addModelContent("filestores");
            fileStoreModelContent.addString("handlerUUID", fileStoreHandler.getStoreUUID().toString());

            final FileStorePortObject fileStorePO = (FileStorePortObject)po;
            FileStoreUtil.invokeFlush(fileStorePO);
            List<FileStore> fileStores = FileStoreUtil.getFileStores(fileStorePO);
            ModelContentWO fileStoreKeysModel = fileStoreModelContent.addModelContent("port_file_store_keys");
            for (int i = 0; i < fileStores.size(); i++) {
                FileStoreKey key = fileStoreHandler.translateToLocal(fileStores.get(i), fileStorePO);
                key.save(fileStoreKeysModel.addModelContent("filestore_key_" + i));
            }
        }
        toc.saveToXML(new NonClosableOutputStream.Zip(zipOut));

        zipOut.putNextEntry(new ZipEntry("objectSpec.file"));
        try (PortObjectSpecZipOutputStream specOut =
                    getPortObjectSpecZipOutputStream(new NonClosableOutputStream.Zip(zipOut))) {
            PortObjectSpecSerializer specSer = PortTypeRegistry.getInstance().getSpecSerializer(spec.getClass()).get();
            specSer.savePortObjectSpec(spec, specOut);
        } // 'close' will propagate as closeEntry

        zipOut.putNextEntry(new ZipEntry("object.file"));
        try (PortObjectZipOutputStream objOut = getPortObjectZipOutputStream(new NonClosableOutputStream.Zip(zipOut))) {
            PortObjectSerializer objSer = PortTypeRegistry.getInstance().getObjectSerializer(po.getClass()).get();
            objSer.savePortObject(po, objOut, exec);
        } // 'close' will propagate as closeEntry

        if (fileStoreHandler != null) {
            if (fileStoreHandler.hasCopiedFileStores()) {
                zipOut.putNextEntry(new ZipEntry("filestores/"));
                zipOut.closeEntry();
                File baseDir = fileStoreHandler.getBaseDir();
                FileUtil.zipDir(zipOut, Arrays.asList(baseDir.listFiles()), "filestores/",
                    FileUtil.ZIP_INCLUDEALL_FILTER, exec.createSubProgress(0.5));
            }
            fileStoreHandler.clearAndDispose();
        }

        zipOut.finish();
        if (!originalOutputIsBuffered) {
            os.flush();
        }
    }

    /**
     * Reads a port object from a file, previously saved via
     * {@link #writeObjectToFile(PortObject, File, ExecutionMonitor)}.
     *
     * @param file To read from, not null.
     * @param exec to report progress.
     * @return The port object - in case of a {@link FileStorePortObject} the port object is associated with a
     * {@link NotInWorkflowWriteFileStoreHandler}.
     * @throws IOException If that fails
     * @throws CanceledExecutionException If canceled
     */
    public static PortObject readObjectFromFile(final File file, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        try (FileInputStream input = new FileInputStream(file)) {
            return readObjectFromStream(input, exec);
        }
    }

    /**
     * Reads a port object from a stream, previously saved via
     * {@link #writeObjectToStream(PortObject, OutputStream, ExecutionMonitor)}.
     *
     * @param input To read from, not null.
     * @param exec to report progress.
     * @return The port object - in case of a {@link FileStorePortObject} the port object is associated with a
     * {@link NotInWorkflowWriteFileStoreHandler}.
     * @throws IOException If that fails
     * @throws CanceledExecutionException If canceled
     */
    public static PortObject readObjectFromStream(final InputStream input, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        return readObjectFromStreamInternal(input, exec).portObject();
    }


    private record PortObjectReadResult(PortObject portObject, NotInWorkflowWriteFileStoreHandler fileStoreHandler) {}

    private static PortObjectReadResult readObjectFromStreamInternal(final InputStream input,
        final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
        try (ZipInputStream in = new ZipInputStream(new BufferedInputStream(input))) {
            ZipEntry entry = in.getNextEntry();

            // Check that the given input stream really specifies a port object
            if(entry == null) {
                throw new IOException("File does not specify a valid port object model");
            }

            if (!"content.xml".equals(entry.getName())) {
                throw new IOException(
                    "Invalid stream, expected zip entry \"content.xml\", got \"" + entry.getName() + "\"");
            }
            ModelContentRO toc = ModelContent.loadFromXML(new NonClosableInputStream.Zip(in));
            String specClassName = toc.getString("port_spec_class");
            String objClassName = toc.getString("port_object_class");
            // reads "objectSpec.file"
            PortObjectSpec spec = readObjectSpec(specClassName, in);

            entry = in.getNextEntry();
            if (!"object.file".equals(entry.getName())) {
                throw new IOException(
                    "Invalid stream, expected zip entry \"object.file\", got \"" + entry.getName() + "\"");
            }
            Class<? extends PortObject> cl = PortTypeRegistry.getInstance().getObjectClass(objClassName)
                .orElseThrow(() -> new IOException("Can't load class \"" + specClassName + "\""));

            PortObject portObject;
            try (PortObjectZipInputStream objIn = getPortObjectZipInputStream(new NonClosableInputStream.Zip(in))) {
                PortObjectSerializer<?> objSer =
                        PortTypeRegistry.getInstance().getObjectSerializer(cl.asSubclass(PortObject.class)).get();
                portObject= objSer.loadPortObject(objIn, spec, exec);
            }

            final NotInWorkflowWriteFileStoreHandler notInWorkflowFSHandler;
            if (portObject instanceof FileStorePortObject) {
                ModelContentRO fileStoreModelContent = toc.getModelContent("filestores");
                UUID iFileStoreHandlerUUID = UUID.fromString(fileStoreModelContent.getString("handlerUUID"));
                ModelContentRO fileStoreKeysModel = fileStoreModelContent.getModelContent("port_file_store_keys");
                List<FileStoreKey> fileStoreKeys = new ArrayList<>();
                for (String key : fileStoreKeysModel.keySet()) {
                    fileStoreKeys.add(FileStoreKey.load(fileStoreKeysModel.getModelContent(key)));
                }
                notInWorkflowFSHandler = new NotInWorkflowWriteFileStoreHandler(iFileStoreHandlerUUID);

                entry = in.getNextEntry();
                if (entry != null && "filestores/".equals(entry.getName())) {
                    File fileStoreDir = FileUtil.createTempDir("knime_fs_" + cl.getSimpleName() + "-");
                    FileUtil.unzip(in, fileStoreDir, 1);
                    notInWorkflowFSHandler.setBaseDir(fileStoreDir);
                }
                FileStoreUtil.retrieveFileStoreHandlerFrom((FileStorePortObject)portObject, fileStoreKeys,
                    notInWorkflowFSHandler.getDataRepository());
            } else {
                notInWorkflowFSHandler = null;
            }
            return new PortObjectReadResult(portObject, notInWorkflowFSHandler);
        } catch (final InvalidSettingsException ex) {
            throw new IOException("Unable to parse content.xml in port object file", ex);
        }
    }

    /**
     * Reads a {@link PortObject}, associating it with the {@link ExecutionContext} in case it's a
     * {@link FileStorePortObject}.
     *
     * @param file to read from
     * @param context The node's context
     * @return the new object
     * @throws IOException ...
     * @throws CanceledExecutionException ...
     * @since 5.3
     */
    // added in 5.3, backported to 5.2.1
    public static PortObject readObjectFromFileViaContext(final File file, final ExecutionContext context)
            throws IOException, CanceledExecutionException {
        try (FileInputStream input = new FileInputStream(file)) {
            return readObjectFromStreamViaContext(input, context);
        }
    }

    /**
     * Reads a {@link PortObject}, associating it with the {@link ExecutionContext} in case it's a
     * {@link FileStorePortObject}.
     *
     * @param input to read from
     * @param context The node's context
     * @return the new object
     * @throws IOException ...
     * @throws CanceledExecutionException ...
     * @since 5.3
     */
    // added in 5.3, backported to 5.2.1
    public static PortObject readObjectFromStreamViaContext(final InputStream input, final ExecutionContext context)
            throws IOException, CanceledExecutionException {
        PortObjectReadResult result = readObjectFromStreamInternal(input, context);
        final NotInWorkflowWriteFileStoreHandler notInWorkflowFileStoreHandler = result.fileStoreHandler();
        PortObject portObject = result.portObject();
        if (notInWorkflowFileStoreHandler != null) {
            portObject = Node.copyPortObject(portObject, context);
            notInWorkflowFileStoreHandler.clearAndDispose();
        }
        return portObject;
    }

    public static PortObjectSpec readObjectSpecFromFile(final File file) throws IOException {
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            return readObjectSpecFromStream(stream);
        }
    }

    /**
     * Read spec from stream. Argument will be wrapped in zip input stream, the spec is extracted and the stream is
     * closed.
     *
     * @param stream to read from.
     * @return The spec.
     * @throws IOException IO problems and unexpected content.
     * @since 2.6
     */
    public static PortObjectSpec readObjectSpecFromStream(final InputStream stream) throws IOException {
        try (ZipInputStream in = new ZipInputStream(stream)) {
            ZipEntry entry = in.getNextEntry();
            if (entry == null) {
                throw new IOException("Invalid file: No zip entry found");
            }
            if (!"content.xml".equals(entry.getName())) {
                throw new IOException(
                    "Invalid stream, expected zip entry \"content.xml\", got \"" + entry.getName() + "\"");
            }
            ModelContentRO toc = ModelContent.loadFromXML(new NonClosableInputStream.Zip(in));
            String specClassName;
            try {
                specClassName = toc.getString("port_spec_class");
            } catch (InvalidSettingsException e1) {
                throw new IOException("Can't parse content file", e1);
            }
            return readObjectSpec(specClassName, in);
        }
    }

    private static PortObjectSpec readObjectSpec(final String specClassName, final ZipInputStream in)
        throws IOException {
        ZipEntry entry = in.getNextEntry();
        if (!"objectSpec.file".equals(entry.getName())) {
            throw new IOException(
                "Invalid stream, expected zip entry \"objectSpec.file\", got \"" + entry.getName() + "\"");
        }
        Class<? extends PortObjectSpec> cl = PortTypeRegistry.getInstance().getSpecClass(specClassName)
            .orElseThrow(() -> new IOException("Can't load class \"" + specClassName + "\""));
        try (PortObjectSpecZipInputStream specIn =
            PortUtil.getPortObjectSpecZipInputStream(new NonClosableInputStream.Zip(in))) {
            PortObjectSpecSerializer<?> serializer =
                PortTypeRegistry.getInstance().getSpecSerializer(cl.asSubclass(PortObjectSpec.class)).get();
            return serializer.loadPortObjectSpec(specIn);
        }
    }

}
