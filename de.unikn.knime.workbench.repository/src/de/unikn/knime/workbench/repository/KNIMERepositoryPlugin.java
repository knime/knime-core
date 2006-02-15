package de.unikn.knime.workbench.repository;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.unikn.knime.workbench.core.WorkbenchErrorLogger;

/**
 * Repository Plugin.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class KNIMERepositoryPlugin extends Plugin {

    // Make sure that this *always* matches the ID in plugin.xml
    /** The plugin-id. */
    public static final String PLUGIN_ID = "de.unikn.knime.workbench."
            + "repository";

    // The shared instance.
    private static KNIMERepositoryPlugin plugin;

    // Resource bundle.
    private ResourceBundle m_resourceBundle;

    // image registry
    private ImageRegistry m_imageRegistry;

    /**
     * The constructor.
     */
    public KNIMERepositoryPlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation. We're showing a little
     * opening splashScreen wit a progress bar, while building up the
     * repository.
     * 
     * @param context The context
     * @throws Exception some startup exception
     */
    public void start(final BundleContext context) throws Exception {
        super.start(context);

//        // use shell of the active workbench
//        Shell parent = Display.getDefault().getActiveShell();
//
//        final Shell splashWin = new Shell(parent, SWT.NO_TRIM
//                | SWT.APPLICATION_MODAL);
//        splashWin.setLayout(new FillLayout());
//        Composite comp = new Composite(splashWin, SWT.NONE);
//        comp.setLayout(new GridLayout());
//        Label label = new Label(comp, SWT.NONE);
//
//        // create splash image
//        m_splashImage = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID,
//                "icons/splash.bmp").createImage();
//
//        label.setImage(m_splashImage);
//        final ProgressBar progress = new ProgressBar(comp, SWT.INDETERMINATE);
//        progress.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//        progress.setToolTipText("loading nodes....");
//
//        label.pack();
//        progress.pack();
//
//        comp.pack();
//        comp.layout(true);
//        splashWin.pack();
//        splashWin.layout(true);
//
//        // center the splash on the current monitor
//        Rectangle r = splashWin.getMonitor().getBounds();
//        Rectangle r2 = splashWin.getBounds();
//        splashWin.setBounds(r.width / 2 - r2.width / 2, r.height / 2
//                - r2.height / 2, r2.width, r2.height);
//
//        // open splash window
//        splashWin.open();

        // Do the actual work: load the repository
        try {
            RepositoryManager.INSTANCE.create();
        } catch (Exception e) {
            WorkbenchErrorLogger.error("FATAL: error initializing KNIME"
                    + " repository - check plugin.xml" + " and classpath", e);
        } finally {
//            splashWin.close();
//            splashWin.dispose();
        }

    }

    /**
     * This method is called when the plug-in is stopped.
     * 
     * @param context The context
     * @throws Exception some stopping exception
     */
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
        m_resourceBundle = null;
    }

    /**
     * Returns the shared instance.
     * 
     * @return The plugin instance (singleton)
     */
    public static KNIMERepositoryPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     * 
     * @param key The key
     * @return The string
     */
    public static String getResourceString(final String key) {
        ResourceBundle bundle = KNIMERepositoryPlugin.getDefault()
                .getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the plugin's resource bundle.
     * 
     * @return The bundle
     */
    public ResourceBundle getResourceBundle() {
        try {
            if (m_resourceBundle == null) {
                m_resourceBundle = ResourceBundle
                        .getBundle("de.unikn.knime.workbench.repository."
                                + "Resources");
            }
        } catch (MissingResourceException x) {
            m_resourceBundle = null;
        }
        return m_resourceBundle;
    }

    /**
     * Returns a (cached) image from the image registry.
     * 
     * @param descriptor The image descriptor
     * @return The image, or a default image if missing.
     */
    public Image getImage(final ImageDescriptor descriptor) {
        // create the registry if needed
        if (m_imageRegistry == null) {
            m_imageRegistry = new ImageRegistry();
        }
        // try to lookup previously cached image
        
        Image img = m_imageRegistry.get(descriptor.toString());
        
        

        // if null, create the image and store it in the registry for further
        // requests
        if (img == null) {
            img = descriptor.createImage(true);
            m_imageRegistry.put(descriptor.toString(), img);
        }

        return img;
    }

    /**
     * This only works for images located in the KNIMERepositry Plugin !
     * 
     * @param filename The filename, relative to the KNIMERepositryPlugin root
     * @return The image, default will be supplied if missing.
     */
    public Image getImage(final String filename) {
        return this.getImage(PLUGIN_ID, filename);
    }

    /**
     * Load a image from the given location from within the plugin.
     * 
     * @param pluginID The ID of the hosting plugin
     * @param filename The elative filename
     * @return The image, a default will be returned if file was missing.
     */
    public Image getImage(final String pluginID, final String filename) {
        return this.getImage(this.getImageDescriptor(pluginID, filename));

    }

    /**
     * Returns a image descriptor.
     * 
     * @param pluginID The plugin ID
     * @param filename Th relative filename
     * @return The descriptor, or null
     */
    public ImageDescriptor getImageDescriptor(final String pluginID,
            final String filename) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(pluginID, filename);
    }

}
