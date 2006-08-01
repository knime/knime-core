package org.knime.ext.sun;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class SunPlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.knime.ext.sun";

	// The shared instance
	private static SunPlugin plugin;
	
	/**
	 * The constructor
	 */
	public SunPlugin() {
		plugin = this;
	}

	/**
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	@Override
    public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
    public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static SunPlugin getDefault() {
		return plugin;
	}

}
