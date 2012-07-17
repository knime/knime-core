package org.knime.product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.splash.BasicSplashHandler;

/**
 * This is the dynamic splash screen for KNIME that offers 3rd party vendors to
 * add their own little icons to it via the extension point
 * <code>org.knime.product.splashExtension</code>.
 *
 * @since 2.0
 * @author Thorsten Meinl, University of Konstanz
 */
public class KNIMESplashHandler extends BasicSplashHandler {
    private final ArrayList<Image> m_images = new ArrayList<Image>();

    private final ArrayList<String> m_tooltips = new ArrayList<String>();

    private static final String SPLASH_EXTENSION_ID =
            "org.knime.product.splashExtension";

    private static final String ELEMENT_ICON = "icon";

    private static final String ELEMENT_TOOLTIP = "tooltip";

    private static final String DEFAULT_TOOLTIP = "Image";

    private static final int MAX_IMAGE_WIDTH = 50;

    private static final int MAX_IMAGE_HEIGHT = 50;

    private static final int SPLASH_SCREEN_BEVEL = 8;

    private static final Rectangle PROGRESS_RECT = new Rectangle(5, 295, 445,
            15);

    private static final Rectangle MESSAGE_RECT =
            new Rectangle(7, 272, 445, 20);

    private Composite m_iconPanel;

    private Label m_installedExtensions;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final Shell splash) {
        // Store the shell
        super.init(splash);
        splash.setLayout(null);
        // Force shell to inherit the splash background
        splash.setBackgroundMode(SWT.INHERIT_DEFAULT);
        // Get all splash handler extensions
        IExtension[] extensions =
                Platform.getExtensionRegistry()
                        .getExtensionPoint(SPLASH_EXTENSION_ID).getExtensions();

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

        // sort genuine KNIME extensions to the front
        Collections.sort(configElements, new Comparator<IConfigurationElement>() {
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
        });



        // Process all splash handler extensions
        for (IConfigurationElement elem : configElements) {
            processSplashElements(elem);
        }

        // If no splash extensions were loaded abort the splash handler
        if (!hasSplashExtensions()) {
            initProgressBar();
            doEventLoop();
            return;
        }

        // Create the icon panel
        createUICompositeIconPanel();
        // Create the images
        createUIImages();

        // Configure the image panel bounds
        configureUICompositeIconPanelBounds();
        // Enter event loop and prevent the RCP application from
        // loading until all work is done
        initProgressBar();
        doEventLoop();
    }

    private void initProgressBar() {
        setProgressRect(PROGRESS_RECT);
        setMessageRect(MESSAGE_RECT);
        getContent();
    }

    private boolean hasSplashExtensions() {
        return !m_images.isEmpty();
    }

    private void createUIImages() {
        Iterator<Image> imageIterator = m_images.iterator();
        Iterator<String> tooltipIterator = m_tooltips.iterator();
        int i = 1;
        int columnCount = ((GridLayout)m_iconPanel.getLayout()).numColumns;
        // Create all the images
        // Abort if we run out of columns (left-over images will not fit within
        // the usable splash screen width)
        while (imageIterator.hasNext() && (i <= columnCount)) {
            Image image = imageIterator.next();
            String tooltip = tooltipIterator.next();
            // Create the image using a label widget
            Label label = new Label(m_iconPanel, SWT.NONE);
            label.setImage(image);
            label.setToolTipText(tooltip);
            i++;
        }
    }

    private void createUICompositeIconPanel() {
        Shell splash = getSplash();
        // Create the composite
        m_iconPanel = new Composite(splash, SWT.NONE);

        int maxWidth = 0;
        for (Image img : m_images) {
            maxWidth = Math.max(maxWidth, img.getBounds().width + 3);
        }

        final int horizontalSpacing = 10;
        // each item requires space "(maxWidth + horizontalSpacing)", except
        // for the very last image
        int maxColumnCount =
                (getUsableSplashScreenWidth() + horizontalSpacing)
                        / (maxWidth + horizontalSpacing);
        // Limit size to the maximum number of columns if the number of images
        // exceed this amount; otherwise, use the exact number of columns
        // required.
        int actualColumnCount = Math.min(m_images.size(), maxColumnCount);
        // Configure the layout
        GridLayout layout = new GridLayout(actualColumnCount, true);
        layout.horizontalSpacing = horizontalSpacing;
        layout.verticalSpacing = 0;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        m_iconPanel.setLayout(layout);

        if (m_images.size() > 0) {
            m_installedExtensions = new Label(splash, SWT.NONE);
            m_installedExtensions.setText("Installed Extensions:");

            /*
             * On Mac OS X the origin of the coordinate system is in the bottom
             * left corner. Therefore we need other y coordinates here.
             */
            int y = 195;
            if (Platform.OS_MACOSX.equals(Platform.getOS())) {
                y = 110;
            }
            m_installedExtensions.setBounds(SPLASH_SCREEN_BEVEL, y, 200, 20);
        }
    }

    private void configureUICompositeIconPanelBounds() {
        // Determine the size of the panel and position it at the bottom-right
        // of the splash screen.
        Point panelSize =
                m_iconPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

        int xWidth = panelSize.x;
        int yWidth = panelSize.y;

        /*
         * On Mac OS X the origin of the coordinate system is in the bottom left
         * corner. Therefor we need other y coordinates here.
         */
        int y = 225;
        if (Platform.OS_MACOSX.equals(Platform.getOS())) {
            y = 65;
        }
        m_iconPanel.setBounds(SPLASH_SCREEN_BEVEL, y, xWidth, yWidth);
    }

    private int getUsableSplashScreenWidth() {
        // Splash screen width minus two graphic border bevel widths
        return getSplash().getSize().x - (SPLASH_SCREEN_BEVEL * 2);
    }


    private void processSplashElements(
            final IConfigurationElement configurationElement) {
        // Attribute: icon
        processSplashElementIcon(configurationElement);
        // Attribute: tooltip
        processSplashElementTooltip(configurationElement);
    }

    private void processSplashElementTooltip(
            final IConfigurationElement configurationElement) {
        // Get attribute tooltip
        String tooltip = configurationElement.getAttribute(ELEMENT_TOOLTIP);
        // If a tooltip is not defined, give it a default
        if ((tooltip == null) || (tooltip.length() == 0)) {
            m_tooltips.add(DEFAULT_TOOLTIP);
        } else {
            m_tooltips.add(tooltip);
        }
    }

    private void processSplashElementIcon(
            final IConfigurationElement configurationElement) {
        // Get attribute icon
        String iconImageFilePath =
                configurationElement.getAttribute(ELEMENT_ICON);
        // Abort if an icon attribute was not specified
        if ((iconImageFilePath == null) || (iconImageFilePath.length() == 0)) {
            return;
        }
        // Create a corresponding image descriptor
        ImageDescriptor descriptor =
                AbstractUIPlugin.imageDescriptorFromPlugin(
                        configurationElement.getNamespaceIdentifier(),
                        iconImageFilePath);
        // Abort if no corresponding image was found
        if (descriptor == null) {
            return;
        }
        // Create the image
        Image image = descriptor.createImage();
        // Abort if image creation failed
        if (image == null) {
            return;
        }
        // Abort if the image does not have dimensions of 50x50
        if ((image.getBounds().width > MAX_IMAGE_WIDTH)
                || (image.getBounds().height > MAX_IMAGE_HEIGHT)) {
            // Dipose of the image
            image.dispose();
            return;
        }
        // Store the image and tooltip
        m_images.add(image);
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
