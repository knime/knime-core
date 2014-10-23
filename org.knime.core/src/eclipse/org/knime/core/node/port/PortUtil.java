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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.internal.SerializerMethodLoader;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject.PortObjectSerializer;
import org.knime.core.node.port.PortObjectSpec.PortObjectSpecSerializer;

/**
 * Contains framework methods that are used to persist or read
 * {@link PortObject} and {@link PortObjectSpec} objects.
 *
 * <p>Methods in this class are not meant to be used by node developers. This
 * class and its methods may change in future versions.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class PortUtil {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PortUtil.class);

    private static final Map<Class<? extends PortObjectSpec>,
    PortObjectSpecSerializer<?>> PORT_SPEC_SERIALIZER_MAP = new ConcurrentHashMap<>();

    private static final Map<Class<? extends PortObject>,
    PortObjectSerializer<?>> PORT_OBJECT_SERIALIZER_MAP = new ConcurrentHashMap<>();

    private PortUtil() {
    }

    /**
     * Get the globally used serializer for {@link PortObjectSpec} objects
     * represented by the class argument.
     *
     * @param <T> The specific PortObjectSpec class of interest.
     * @param cl Class argument.
     * @return The serializer to be used. Will throw an undeclared runtime
     * exception if retrieving the type causes problems (suggests a coding
     * problem)
     */
    @SuppressWarnings("unchecked")
    // access to CLASS_TO_SERIALIZER_MAP
    public static <T extends PortObjectSpec> PortObjectSpecSerializer<T>
    getPortObjectSpecSerializer(final Class<T> cl) {
        if (PORT_SPEC_SERIALIZER_MAP.containsKey(cl)) {
            return PortObjectSpecSerializer.class.cast(PORT_SPEC_SERIALIZER_MAP
                    .get(cl));
        }
        PortObjectSpecSerializer<T> result;
        try {
            result = SerializerMethodLoader.getSerializer(cl,
                    PortObjectSpecSerializer.class,
                            "getPortObjectSpecSerializer", true);
        } catch (NoSuchMethodException e) {
            LOGGER.coding("Errors while accessing serializer object", e);
            throw new RuntimeException(e);
        }
        PORT_SPEC_SERIALIZER_MAP.put(cl, result);
        return result;
    }

    /**
     * Get the globally used serializer for {@link PortObject} objects
     * represented by the class argument.
     *
     * @param <T> The specific PortObject class of interest.
     * @param cl Class argument.
     * @return The serializer to be used. Will throw an undeclared runtime
     * exception if retrieving the type causes problems (suggests a coding
     * problem)
     */
    @SuppressWarnings("unchecked")
    // access to CLASS_TO_SERIALIZER_MAP
    public static <T extends PortObject> PortObjectSerializer<T>
    getPortObjectSerializer(final Class<T> cl) {
        if (PORT_OBJECT_SERIALIZER_MAP.containsKey(cl)) {
            return PortObjectSerializer.class.cast(PORT_OBJECT_SERIALIZER_MAP
                    .get(cl));
        }
        PortObjectSerializer<T> result;
        try {
            result = SerializerMethodLoader.getSerializer(cl, PortObjectSerializer.class,
                            "getPortObjectSerializer", true);
        } catch (NoSuchMethodException e) {
            LOGGER.coding("Errors while accessing serializer object", e);
            throw new RuntimeException(e);
        }
        PORT_OBJECT_SERIALIZER_MAP.put(cl, result);
        return result;
    }

    public static PortObjectSpecZipOutputStream getPortObjectSpecZipOutputStream(
            final OutputStream in) throws IOException {
        PortObjectSpecZipOutputStream zipOut =
            new PortObjectSpecZipOutputStream(in);
        zipOut.putNextEntry(new ZipEntry("portSpec.file"));
        return zipOut;
    }

    public static PortObjectZipOutputStream getPortObjectZipOutputStream(
            final OutputStream in) throws IOException {
        PortObjectZipOutputStream zipOut =
            new PortObjectZipOutputStream(in);
        zipOut.putNextEntry(new ZipEntry("portObject.file"));
        return zipOut;
    }

    public static PortObjectSpecZipInputStream getPortObjectSpecZipInputStream(
            final InputStream in) throws IOException {
        PortObjectSpecZipInputStream zipIn =
            new PortObjectSpecZipInputStream(in);
        ZipEntry entry = zipIn.getNextEntry();
        if (!"portSpec.file".equals(entry.getName())) {
            throw new IOException("Expected zip entry 'portSpec.file', "
                    + "got '"  + entry.getName() + "'");
        }
        return zipIn;
    }

    public static PortObjectZipInputStream getPortObjectZipInputStream(
            final InputStream in) throws IOException {
        PortObjectZipInputStream zipIn =
            new PortObjectZipInputStream(in);
        ZipEntry entry = zipIn.getNextEntry();
        if (!"portObject.file".equals(entry.getName())) {
            throw new IOException("Expected zip entry 'portObject.file', "
                    + "got '"  + entry.getName() + "'");
        }
        return zipIn;
    }

    public static void writeObjectToFile(final PortObject po, final File file,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        writeObjectToStream(po, new FileOutputStream(file), exec);
    }

    /**
     * Write the given port object into the given output stream. The output stream does not need to be buffered and
     * is not closed after calling this method.
     *
     * @param po any port object
     * @param output any output stream, does not need to be buffered
     * @param exec execution context for reporting progress and checking for cancelation
     * @throws IOException if an I/O error occurs while serializing the port object
     * @throws CanceledExecutionException if the user canceled the operation
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void writeObjectToStream(final PortObject po,
        final OutputStream output, final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
        final boolean originalOutputIsBuffered =
                ((output instanceof BufferedOutputStream) || (output instanceof ByteArrayOutputStream));
        OutputStream os = originalOutputIsBuffered ? output : new BufferedOutputStream(output);

        final ZipOutputStream zipOut = new ZipOutputStream(os);
        zipOut.setLevel(0);

        PortObjectSpec spec = po.getSpec();
        zipOut.putNextEntry(new ZipEntry("content.xml"));
        ModelContent toc = new ModelContent("content");
        toc.addInt("version", 1);
        toc.addString("port_spec_class", spec.getClass().getName());
        toc.addString("port_object_class", po.getClass().getName());
//        if (po instanceof FileStorePortObject) {
//            final FileStorePortObject fileStorePO = (FileStorePortObject)po;
//            FileStore fileStore = FileStoreUtil.getFileStore(fileStorePO);
//            FileStoreKey fileStoreKey = fileStoreHandler.translateToLocal(fileStore, fileStorePO);
//            ModelContentWO fileStoreModelContent = toc.addModelContent("port_file_store_key");
//            fileStoreKey.save(fileStoreModelContent);
//        }
        toc.saveToXML(new NonClosableOutputStream.Zip(zipOut));

        zipOut.putNextEntry(new ZipEntry("objectSpec.file"));
        PortObjectSpecZipOutputStream specOut = getPortObjectSpecZipOutputStream(new NonClosableOutputStream.Zip(zipOut));
        PortObjectSpecSerializer specSer = getPortObjectSpecSerializer(spec.getClass());
        specSer.savePortObjectSpec(spec, specOut);
        specOut.close(); // will propagate as closeEntry

        zipOut.putNextEntry(new ZipEntry("object.file"));
        PortObjectZipOutputStream objOut = getPortObjectZipOutputStream(new NonClosableOutputStream.Zip(zipOut));
        PortObjectSerializer objSer = getPortObjectSerializer(po.getClass());
        objSer.savePortObject(po, objOut, exec);
        objOut.close(); // will propagate as closeEntry
        zipOut.finish();
        if (!originalOutputIsBuffered) {
            os.flush();
        }
    }

    public static PortObject readObjectFromFile(final File file,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        return readObjectFromStream(new FileInputStream(file), exec);
    }

    public static PortObject readObjectFromStream(final InputStream input,
            final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException {
        ZipInputStream in = new ZipInputStream(
                new BufferedInputStream(input));

        ZipEntry entry = in.getNextEntry();
        if (!"content.xml".equals(entry.getName())) {
            throw new IOException("Invalid stream, expected zip entry "
                    + "\"content.xml\", got \"" + entry.getName() + "\"");
        }
        ModelContentRO toc = ModelContent.loadFromXML(
                new NonClosableInputStream.Zip(in));
        String specClassName;
        String objClassName;
        try {
            specClassName = toc.getString("port_spec_class");
            objClassName = toc.getString("port_object_class");
        } catch (InvalidSettingsException e1) {
            throw new IOException("Can't parse content file", e1);
        }
        PortObjectSpec spec = readObjectSpec(specClassName, in);

        entry = in.getNextEntry();
        if (!"object.file".equals(entry.getName())) {
            throw new IOException("Invalid stream, expected zip entry "
                    + "\"object.file\", got \"" + entry.getName() + "\"");
        }
        Class<?> cl;
        try {
            cl = Class.forName(objClassName);
        } catch (ClassNotFoundException e) {
            throw new IOException("Can't load class \"" + specClassName
                    + "\"", e);
        }
        if (!PortObject.class.isAssignableFrom(cl)) {
            throw new IOException("Class \"" + cl.getSimpleName()
                    + "\" does not a sub-class \""
                    + PortObject.class.getSimpleName() + "\"");
        }
        PortObjectZipInputStream objIn =
            PortUtil.getPortObjectZipInputStream(
                    new NonClosableInputStream.Zip(in));
        PortObjectSerializer<?> objSer =
            PortUtil.getPortObjectSerializer(cl
                    .asSubclass(PortObject.class));
        PortObject po = objSer.loadPortObject(objIn, spec, exec);
        in.close();
        return po;
    }

    public static PortObjectSpec readObjectSpecFromFile(
            final File file) throws IOException {
        return readObjectSpecFromStream(
                new BufferedInputStream(new FileInputStream(file)));
    }

    /** Read spec from stream. Argument will be wrapped in zip input
     * stream, the spec is extracted and the stream is closed.
     * @param stream to read from.
     * @return The spec.
     * @throws IOException IO problems and unexpected content.
     * @since 2.6
     */
    public static PortObjectSpec readObjectSpecFromStream(
            final InputStream stream) throws IOException {
        ZipInputStream in = new ZipInputStream(stream);

        ZipEntry entry = in.getNextEntry();
        if (entry == null) {
            throw new IOException("Invalid file: No zip entry found");
        }
        if (!"content.xml".equals(entry.getName())) {
            throw new IOException("Invalid stream, expected zip entry "
                    + "\"content.xml\", got \"" + entry.getName() + "\"");
        }
        ModelContentRO toc = ModelContent.loadFromXML(
                new NonClosableInputStream.Zip(in));
        String specClassName;
        try {
            specClassName = toc.getString("port_spec_class");
        } catch (InvalidSettingsException e1) {
            throw new IOException("Can't parse content file", e1);
        }
        PortObjectSpec spec = readObjectSpec(specClassName, in);
        in.close();
        return spec;
    }

    private static PortObjectSpec readObjectSpec(final String specClassName,
            final ZipInputStream in)
    throws IOException {
        ZipEntry entry = in.getNextEntry();
        if (!"objectSpec.file".equals(entry.getName())) {
            throw new IOException("Invalid stream, expected zip entry "
                    + "\"objectSpec.file\", got \"" + entry.getName() + "\"");
        }
        Class<?> cl;
        try {
            cl = Class.forName(specClassName);
        } catch (ClassNotFoundException e) {
            throw new IOException("Can't load class \"" + specClassName
                    + "\"", e);
        }
        if (!PortObjectSpec.class.isAssignableFrom(cl)) {
            throw new IOException("Class \"" + cl.getSimpleName()
                    + "\" does not a sub-class \""
                    + PortObjectSpec.class.getSimpleName() + "\"");
        }
        PortObjectSpecZipInputStream specIn =
            PortUtil.getPortObjectSpecZipInputStream(
                    new NonClosableInputStream.Zip(in));
        PortObjectSpecSerializer<?> serializer =
            PortUtil.getPortObjectSpecSerializer(cl
                    .asSubclass(PortObjectSpec.class));
        PortObjectSpec spec = serializer.loadPortObjectSpec(specIn);
        specIn.close();
        return spec;
    }

}
