package org.knime.core.node.port;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.eclipseUtil.GlobalClassCreator;
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
    PortObjectSpecSerializer<?>> PORT_SPEC_SERIALIZER_MAP = 
        new HashMap<Class<? extends PortObjectSpec>, 
        PortObjectSpecSerializer<?>>();
    
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
            result = (PortObjectSpecSerializer<T>)
            SerializerMethodLoader.getSerializer(cl, 
                    PortObjectSpecSerializer.class,
                            "getPortObjectSpecSerializer", true);
        } catch (NoSuchMethodException e) {
            LOGGER.coding("Errors while accessing serializer object", e);
            throw new RuntimeException(e);
        }
        PORT_SPEC_SERIALIZER_MAP.put(cl, result);
        return result;
    }

    private static final Map<Class<? extends PortObject>, 
    PortObjectSerializer<?>> PORT_OBJECT_SERIALIZER_MAP =
        new HashMap<Class<? extends PortObject>, PortObjectSerializer<?>>();

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
            result = (PortObjectSerializer<T>)
            SerializerMethodLoader.getSerializer(cl, PortObjectSerializer.class,
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
        PortObjectSpec spec = po.getSpec();
        final ZipOutputStream out = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)));
        out.setLevel(0);

        out.putNextEntry(new ZipEntry("content.xml"));
        ModelContent toc = new ModelContent("content");
        toc.addInt("version", 1);
        toc.addString("port_spec_class", spec.getClass().getName());
        toc.addString("port_object_class", po.getClass().getName());
        toc.saveToXML(new NonClosableOutputStream.Zip(out));
        
        out.putNextEntry(new ZipEntry("objectSpec.file"));
        PortObjectSpecZipOutputStream specOut = 
            getPortObjectSpecZipOutputStream(
                    new NonClosableOutputStream.Zip(out));
        PortObjectSpecSerializer specSer = 
            getPortObjectSpecSerializer(spec.getClass());
        specSer.savePortObjectSpec(spec, specOut);
        specOut.close(); // will propagate as closeEntry
        
        out.putNextEntry(new ZipEntry("object.file"));
        PortObjectZipOutputStream objOut = getPortObjectZipOutputStream(
                new NonClosableOutputStream.Zip(out));
        PortObjectSerializer objSer = getPortObjectSerializer(po.getClass());
        objSer.savePortObject(po, objOut, exec);
        objOut.close(); // will propagate as closeEntry
        
        out.close();
    }

    public static PortObject readObjectFromFile(final File file, 
            final ExecutionMonitor exec) 
    throws IOException, CanceledExecutionException {
        ZipInputStream in = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(file)));
        
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
            cl = GlobalClassCreator.createClass(objClassName);
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
    
    public static PortObjectSpec readObjectSpecFromFile(final File file) 
    throws IOException {
        ZipInputStream in = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(file)));
        
        ZipEntry entry = in.getNextEntry();
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
            cl = GlobalClassCreator.createClass(specClassName);
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
