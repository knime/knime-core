/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
package org.knime.product;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.splash.BasicSplashHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * This is the dynamic splash screen for KNIME that offers 3rd party vendors to add their own little icons to it via the
 * extension point <code>org.knime.product.splashExtension</code>. The order of the splash icons can be configured by a
 * file <tt>splash.config</tt> in the installation directory. The file is expected to contains the plug-in symbolic
 * names in the desired order. Plug-ins not mentioned in the file are sorted by the standard sort order.
 *
 * @since 2.0
 * @author Thorsten Meinl, University of Konstanz
 */
public class KNIMESplashHandler extends BasicSplashHandler {
    private static class DefaultComparator implements Comparator<IConfigurationElement> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final IConfigurationElement o1, final IConfigurationElement o2) {
            String name1 = o1.getContributor().getName();
            String name2 = o2.getContributor().getName();
            if (name1.startsWith("com.knime.")) {
                if (name2.startsWith("com.knime.")) {
                    return name1.compareTo(name2);
                } else {
                    return -1;
                }
            } else if (name2.startsWith("com.knime")) {
                return 1;
            } else if (name1.startsWith("org.knime.")) {
                if (name2.startsWith("org.knime.")) {
                    return name1.compareTo(name2);
                } else {
                    return -1;
                }
            } else if (name2.startsWith("org.knime.")) {
                return 1;
            } else {
                return name1.compareTo(name2);
            }
        }
    }

    private static class ConfiguredComparator extends DefaultComparator {
        private final Map<String, Integer> m_weights = new HashMap<String, Integer>();

        ConfiguredComparator(final File configFile) throws IOException {
            BufferedReader in = new BufferedReader(new FileReader(configFile));
            String line;
            int count = 0;
            while ((line = in.readLine()) != null) {
                m_weights.put(line, count++);
            }
            in.close();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final IConfigurationElement o1, final IConfigurationElement o2) {
            String name1 = o1.getContributor().getName();
            String name2 = o2.getContributor().getName();

            Integer weight1 = m_weights.get(name1);
            Integer weight2 = m_weights.get(name2);

            if ((weight1 != null) && (weight2 != null)) {
                return weight1.compareTo(weight2);
            } else if (weight1 != null) {
                return -1;
            } else if (weight2 != null) {
                return 1;
            } else {
                return super.compare(o1, o2);
            }
        }
    }

    private static final String SPLASH_EXTENSION_ID = "org.knime.product.splashExtension";

    private static final String ELEMENT_ICON = "icon";

    private static final String ELEMENT_TOOLTIP = "tooltip";

    private static final String DEFAULT_TOOLTIP = "Image";

    private static final int SPLASH_SCREEN_BEVEL = 8;

    private static final Rectangle PROGRESS_RECT = new Rectangle(5, 295, 445, 15);

    private static final Rectangle MESSAGE_RECT = new Rectangle(7, 272, 445, 20);

    private static final RGB KNIME_GRAY = new RGB(0x7d, 0x7d, 0x7d);

    private Composite m_iconPanel;

    private Label m_installedExtensions;

    private final List<Image> m_images = new ArrayList<Image>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final Shell splash) {
        // Store the shell
        super.init(splash);
        setForeground(KNIME_GRAY);
        splash.setLayout(null);
        // Force shell to inherit the splash background
        splash.setBackgroundMode(SWT.INHERIT_DEFAULT);
        List<IConfigurationElement> splashExtensions = readSplashExtensions();

        // If no splash extensions were loaded abort the splash handler
        if (splashExtensions.size() < 1) {
            initProgressBar();
            doEventLoop();
            return;
        }

        int horizontalSpacing = 10;
        int iconSize = 50;
        int columnCount = (getUsableSplashScreenWidth() + horizontalSpacing) / (iconSize + horizontalSpacing);
        int maxIcons = columnCount;

        if (splashExtensions.size() > columnCount) {
            iconSize = 32;
            horizontalSpacing = 8;
            columnCount = (getUsableSplashScreenWidth() + horizontalSpacing) / (iconSize + horizontalSpacing);
            maxIcons = columnCount;

            if (splashExtensions.size() > columnCount) {
                iconSize = 24;
                horizontalSpacing = 6;
                columnCount = (getUsableSplashScreenWidth() + horizontalSpacing) / (iconSize + horizontalSpacing);
                maxIcons = columnCount * 2; // two rows
            }
        }

        // Create the icon panel
        createUICompositeIconPanel(columnCount, horizontalSpacing, splashExtensions);
        // Create the images
        createUIImages(splashExtensions, maxIcons, iconSize);

        // Configure the image panel bounds
        configureUICompositeIconPanelBounds();
        // Enter event loop and prevent the RCP application from
        // loading until all work is done
        initProgressBar();
        doEventLoop();
    }

    private List<IConfigurationElement> readSplashExtensions() {
        // Get all splash handler extensions
        IExtension[] extensions =
                Platform.getExtensionRegistry().getExtensionPoint(SPLASH_EXTENSION_ID).getExtensions();

        List<IConfigurationElement> configElements = new ArrayList<IConfigurationElement>();

        // add and check for duplicates
        for (IExtension ext : extensions) {
            for (IConfigurationElement elem : ext.getConfigurationElements()) {
                String id1 = elem.getAttribute("id");
                boolean exists = false;
                if (id1 != null) {
                    // check if the same id already exists
                    for (IConfigurationElement elem2 : configElements) {
                        String id2 = elem2.getAttribute("id");
                        if (id1.equals(id2)) {
                            exists = true;
                            break;
                        }
                    }
                }
                if (!exists) {
                    configElements.add(elem);
                }
            }
        }

        // sort splash icons by name or by config file
        Comparator<IConfigurationElement> comparator = new DefaultComparator();
        Location loc = Platform.getInstallLocation();
        if ((loc != null) && "file".equals(loc.getURL().getProtocol())) {
            File instDir = new File(loc.getURL().getPath());
            File splashConfig = new File(instDir, "splash.config");
            if (splashConfig.exists()) {
                try {
                    comparator = new ConfiguredComparator(splashConfig);
                } catch (IOException ex) {
                    Bundle thisBundle = FrameworkUtil.getBundle(getClass());
                    Platform.getLog(thisBundle).log(new Status(IStatus.ERROR, thisBundle.getSymbolicName(),
                                                            "Error while reading splash config file", ex));
                }
            }
        }
        Collections.sort(configElements, comparator);
        return configElements;
    }

    private void initProgressBar() {
        setProgressRect(PROGRESS_RECT);
        setMessageRect(MESSAGE_RECT);
        getContent();
    }

    private Image getSplashIcon(final IConfigurationElement splashExtension, final int iconSize) {
        String iconImageFilePath = null;
        iconImageFilePath = splashExtension.getAttribute(ELEMENT_ICON + iconSize);
        if (iconImageFilePath == null) {
            iconImageFilePath = splashExtension.getAttribute(ELEMENT_ICON);
        }

        // Abort if an icon attribute was not specified - which is weird since it is required
        if ((iconImageFilePath == null) || (iconImageFilePath.length() == 0)) {
            return null;
        }

        // Create a corresponding image descriptor
        ImageDescriptor descriptor =
                AbstractUIPlugin.imageDescriptorFromPlugin(splashExtension.getNamespaceIdentifier(), iconImageFilePath);
        // Abort if no corresponding image was found
        if (descriptor == null) {
            return null;
        }

        ImageData imageData = descriptor.getImageData();
        if (imageData == null) {
            return null;
        }
        if ((imageData.width > iconSize) || (imageData.height > iconSize)) {
            return new Image(Display.getDefault(), imageData.scaledTo(iconSize, iconSize));
        } else {
            return new Image(Display.getDefault(), imageData);
        }
    }

    private String getTooltip(final IConfigurationElement splashExtension) {
        // Get attribute tooltip
        String tooltip = splashExtension.getAttribute(ELEMENT_TOOLTIP);
        // If a tooltip is not defined, give it a default
        if ((tooltip == null) || (tooltip.length() == 0)) {
            return DEFAULT_TOOLTIP;
        } else {
            return tooltip;
        }
    }

    private void createUIImages(final List<IConfigurationElement> splashExtensions, final int maxIcons,
                                final int iconSize) {
        int count = 0;
        for (IConfigurationElement splash : splashExtensions) {
            Image image = getSplashIcon(splash, iconSize);
            if (image == null) {
                continue;
            }
            m_images.add(image);
            String tooltip = getTooltip(splash);

            // Create the image using a label widget
            Label label = new Label(m_iconPanel, SWT.NONE);
            label.setImage(image);
            label.setToolTipText(tooltip);
            if (++count >= maxIcons) {
                break;
            }
        }
    }

    private void createUICompositeIconPanel(final int columnCount, final int horizontalSpacing, final List<IConfigurationElement> splashExtensions) {
        Shell splash = getSplash();
        // Create the composite
        m_iconPanel = new Composite(splash, SWT.NONE);

        // Limit size to the maximum number of columns if the number of images
        // exceed this amount; otherwise, use the exact number of columns
        // required.
        // Configure the layout
        GridLayout layout = new GridLayout(columnCount, true);
        layout.horizontalSpacing = horizontalSpacing;
        layout.verticalSpacing = 5;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        m_iconPanel.setLayout(layout);

        if (splashExtensions.size() > 0) {
            m_installedExtensions = new Label(splash, SWT.NONE);
            m_installedExtensions.setForeground(new Color(Display.getCurrent(), KNIME_GRAY));
            m_installedExtensions.setText("Installed Extensions:");

            /*
             * On Mac OS X the origin of the coordinate system is in the bottom
             * left corner. Therefore we need other y coordinates here.
             */
            int y = 195;
            if (Platform.OS_MACOSX.equals(Platform.getOS())) {
                y = 120;
            }
            m_installedExtensions.setBounds(SPLASH_SCREEN_BEVEL, y, 200, 20);
        }
    }

    private void configureUICompositeIconPanelBounds() {
        // Determine the size of the panel and position it at the bottom-right
        // of the splash screen.
        Point panelSize = m_iconPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

        int xWidth = panelSize.x;
        int yWidth = panelSize.y;

        /*
         * On Mac OS X the origin of the coordinate system is in the bottom left
         * corner. Therefore we need other y coordinates here.
         */
        int y = 215;
        if (Platform.OS_MACOSX.equals(Platform.getOS())) {
            y = 75;
        }
        m_iconPanel.setBounds(SPLASH_SCREEN_BEVEL, y, xWidth, yWidth);
    }

    private int getUsableSplashScreenWidth() {
        // Splash screen width minus two graphic border bevel widths
        return getSplash().getSize().x - (SPLASH_SCREEN_BEVEL * 2);
    }

    private void doEventLoop() {
        Shell splash = getSplash();
        if (!splash.getDisplay().readAndDispatch()) {
            splash.getDisplay().sleep();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        super.dispose();
        // Check to see if any images were defined
        if ((m_images == null) || m_images.isEmpty()) {
            return;
        }
        // Dispose of all the images
        for (Image image : m_images) {
            image.dispose();
        }
    }
}
