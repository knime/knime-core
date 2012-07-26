package com.swtdesigner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Display;

/**
 * Utility class for managing OS resources associated with SWT controls such as
 * colors, fonts, images, etc.
 *
 * !!! IMPORTANT !!! Application code must explicitly invoke the
 * <code>dispose()</code> method to release the operating system resources
 * managed by cached objects when those objects and OS resources are no longer
 * needed (e.g. on application shutdown)
 *
 * Note: if you need to use this class in an environment without JFace, you will
 * need to create a stub for the org.eclipse.jface.resource.ImageDescriptor
 * class. The following should work fine:
 *
 * package org.eclipse.jface.resource; import java.net.URL; public abstract
 * class ImageDescriptor { protected ImageDescriptor() { } public static
 * ImageDescriptor createFromFile(Class location, String filename) { return
 * null; } public static ImageDescriptor createFromURL(URL url) { return null; }
 * public Image createImage() { return null; } }
 *
 * This class may be freely distributed as part of any application or plugin.
 * <p>
 * Copyright (c) 2003 - 2005, Instantiations, Inc. <br>
 * All Rights Reserved
 *
 * @version $Revision$
 * @author scheglov_ke
 * @author Dan Rubel
 */
public final class ResourceManager {

    private ResourceManager() {
        // utility class
    }

    /**
     * Dispose of cached objects and their underlying OS resources. This should
     * only be called when the cached objects are no longer needed (e.g. on
     * application shutdown)
     */
    public static void dispose() {
        disposeColors();
        disposeFonts();
        disposeImages();
        disposeCursors();
    }

    // ////////////////////////////
    // Color support
    // ////////////////////////////

    /**
     * Maps RGB values to colors.
     */
    private static HashMap colorMap = new HashMap();

    /**
     * Returns the system color matching the specific ID.
     *
     * @param systemColorID int The ID value for the color
     * @return Color The system color matching the specific ID
     */
    public static Color getColor(final int systemColorID) {
        Display display = Display.getCurrent();
        return display.getSystemColor(systemColorID);
    }

    /**
     * Returns a color given its red, green and blue component values.
     *
     * @param r int The red component of the color
     * @param g int The green component of the color
     * @param b int The blue component of the color
     * @return Color The color matching the given red, green and blue componet
     *         values
     */
    public static Color getColor(final int r, final int g, final int b) {
        return getColor(new RGB(r, g, b));
    }

    /**
     * Returns a color given its RGB value.
     *
     * @param rgb RGB The RGB value of the color
     * @return Color The color matching the RGB value
     */
    public static Color getColor(final RGB rgb) {
        Color color = (Color) colorMap.get(rgb);
        if (color == null) {
            Display display = Display.getCurrent();
            color = new Color(display, rgb);
            colorMap.put(rgb, color);
        }
        return color;
    }

    /**
     * Dispose of all the cached colors.
     */
    public static void disposeColors() {
        for (Iterator iter = colorMap.values().iterator(); iter.hasNext();) {
            ((Color) iter.next()).dispose();
        }
        colorMap.clear();
    }

    // ////////////////////////////
    // Image support
    // ////////////////////////////

    /**
     * Maps image names to images.
     */
    private static HashMap classImageMap = new HashMap();

    /**
     * Maps image descriptors to images.
     */
    private static HashMap descriptorImgMap = new HashMap();

    /**
     * Maps images to image decorators.
     */
    private static HashMap imgToDecoratorMap = new HashMap();

    /**
     * Returns an image encoded by the specified input stream.
     *
     * @param is InputStream The input stream encoding the image data
     * @return Image The image encoded by the specified input stream
     */
    private static Image getImage(final InputStream is) {
        Display display = Display.getCurrent();
        ImageData data = new ImageData(is);
        if (data.transparentPixel > 0) {
            return new Image(display, data, data.getTransparencyMask());
        }
        return new Image(display, data);
    }

    /**
     * Returns an image stored in the file at the specified path.
     *
     * @param path String The path to the image file
     * @return Image The image stored in the file at the specified path
     */
    public static Image getImage(final String path) {
        return getImage("default", path);
    }

    /**
     * Returns an image stored in the file at the specified path.
     *
     * @param section The section to which belongs specified image
     * @param path String The path to the image file
     * @return Image The image stored in the file at the specified path
     */
    public static Image getImage(final String section, final String path) {
        String key = section + "|" + ResourceManager.class.getName() + "|"
                + path;
        Image image = (Image) classImageMap.get(key);
        if (image == null) {
            try {
                FileInputStream fis = new FileInputStream(path);
                image = getImage(fis);
                classImageMap.put(key, image);
                fis.close();
            } catch (Exception e) {
                return null;
            }
        }
        return image;
    }

    /**
     * Returns an image stored in the file at the specified path relative to the
     * specified class.
     *
     * @param clazz Class The class relative to which to find the image
     * @param path String The path to the image file
     * @return Image The image stored in the file at the specified path
     */
    public static Image getImage(final Class clazz, final String path) {
        String key = clazz.getName() + "|" + path;
        Image image = (Image) classImageMap.get(key);
        if (image == null) {
            if (path.length() > 0 && path.charAt(0) == '/') {
                String newPath = path.substring(1, path.length());
                image = getImage(new BufferedInputStream(clazz.getClassLoader()
                        .getResourceAsStream(newPath)));
            } else {
                image = getImage(clazz.getResourceAsStream(path));
            }
            classImageMap.put(key, image);
        }
        return image;
    }

    /**
     * Returns an image descriptor stored in the file at the specified path
     * relative to the specified class.
     *
     * @param clazz Class The class relative to which to find the image
     *            descriptor
     * @param path String The path to the image file
     * @return ImageDescriptor The image descriptor stored in the file at the
     *         specified path
     */
    public static ImageDescriptor getImageDescriptor(final Class clazz,
            final String path) {
        return ImageDescriptor.createFromFile(clazz, path);
    }

    /**
     * Returns an image descriptor stored in the file at the specified path.
     *
     * @param path String The path to the image file
     * @return ImageDescriptor The image descriptor stored in the file at the
     *         specified path
     */
    public static ImageDescriptor getImageDescriptor(final String path) {
        try {
            return ImageDescriptor.createFromURL(
                    new File(path).toURI().toURL());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Returns an image based on the specified image descriptor.
     *
     * @param descriptor ImageDescriptor The image descriptor for the image
     * @return Image The image based on the specified image descriptor
     */
    public static Image getImage(final ImageDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }
        Image image = (Image) descriptorImgMap.get(descriptor);
        if (image == null) {
            image = descriptor.createImage();
            descriptorImgMap.put(descriptor, image);
        }
        return image;
    }

    /**
     * Style constant for placing decorator image in top left corner of base
     * image.
     */
    public static final int TOP_LEFT = 1;

    /**
     * Style constant for placing decorator image in top right corner of base
     * image.
     */
    public static final int TOP_RIGHT = 2;

    /**
     * Style constant for placing decorator image in bottom left corner of base
     * image.
     */
    public static final int BOTTOM_LEFT = 3;

    /**
     * Style constant for placing decorator image in bottom right corner of base
     * image.
     */
    public static final int BOTTOM_RIGHT = 4;

    /**
     * Returns an image composed of a base image decorated by another image.
     *
     * @param baseImage Image The base image that should be decorated
     * @param decorator Image The image to decorate the base image
     * @return Image The resulting decorated image
     */
    public static Image decorateImage(final Image baseImage,
            final Image decorator) {
        return decorateImage(baseImage, decorator, BOTTOM_RIGHT);
    }

    /**
     * Returns an image composed of a base image decorated by another image.
     *
     * @param baseImage Image The base image that should be decorated
     * @param decorator Image The image to decorate the base image
     * @param corner The corner to place decorator image
     * @return Image The resulting decorated image
     */
    public static Image decorateImage(final Image baseImage,
            final Image decorator, final int corner) {
        HashMap decoratedMap = (HashMap) imgToDecoratorMap.get(baseImage);
        if (decoratedMap == null) {
            decoratedMap = new HashMap();
            imgToDecoratorMap.put(baseImage, decoratedMap);
        }
        Image result = (Image) decoratedMap.get(decorator);
        if (result == null) {
            Rectangle bid = baseImage.getBounds();
            Rectangle did = decorator.getBounds();
            result = new Image(Display.getCurrent(), bid.width, bid.height);
            GC gc = new GC(result);
            gc.drawImage(baseImage, 0, 0);
            //
            if (corner == TOP_LEFT) {
                gc.drawImage(decorator, 0, 0);
            } else if (corner == TOP_RIGHT) {
                gc.drawImage(decorator, bid.width - did.width - 1, 0);
            } else if (corner == BOTTOM_LEFT) {
                gc.drawImage(decorator, 0, bid.height - did.height - 1);
            } else if (corner == BOTTOM_RIGHT) {
                gc.drawImage(decorator, bid.width - did.width - 1, bid.height
                        - did.height - 1);
            }
            //
            gc.dispose();
            decoratedMap.put(decorator, result);
        }
        return result;
    }

    /**
     * Dispose all of the cached images.
     */
    public static void disposeImages() {
        for (Iterator i = classImageMap.values().iterator(); i.hasNext();) {
            ((Image) i.next()).dispose();
        }
        classImageMap.clear();
        //
        for (Iterator i = descriptorImgMap.values().iterator(); i.hasNext();) {
            ((Image) i.next()).dispose();
        }
        descriptorImgMap.clear();
        //
        for (Iterator i = imgToDecoratorMap.values().iterator(); i.hasNext();) {
            HashMap decoratedMap = (HashMap) i.next();
            for (Iterator j = decoratedMap.values().iterator(); j.hasNext();) {
                Image image = (Image) j.next();
                image.dispose();
            }
        }
    }

    /**
     * Dispose cached images in specified section.
     *
     * @param section the section do dispose
     */
    public static void disposeImages(final String section) {
        for (Iterator i = classImageMap.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            if (!key.startsWith(section + "|")) {
                continue;
            }
            Image image = (Image) classImageMap.get(key);
            image.dispose();
            i.remove();
        }
    }

    // ////////////////////////////
    // Plugin images support
    // ////////////////////////////

    /**
     * Maps URL to images.
     */
    private static HashMap urlImageMap = new HashMap();

    /**
     * Retuns an image based on a plugin and file path.
     *
     * @param plugin Object The plugin containing the image
     * @param name String The path to th eimage within the plugin
     * @return Image The image stored in the file at the specified path
     */
    public static Image getPluginImage(final Object plugin, final String name) {
        try {
            try {
                URL url = getPluginImageURL(plugin, name);
                if (urlImageMap.containsKey(url)) {
                    return (Image) urlImageMap.get(url);
                }
                InputStream is = url.openStream();
                Image image;
                try {
                    image = getImage(is);
                    urlImageMap.put(url, image);
                } finally {
                    is.close();
                }
                return image;
            } catch (Throwable e) {
                // nothing
            }
        } catch (Throwable e) {
            // nothing
        }
        return null;
    }

    /**
     * Retuns an image descriptor based on a plugin and file path.
     *
     * @param plugin Object The plugin containing the image
     * @param name String The path to th eimage within the plugin
     * @return ImageDescriptor The image descriptor stored in the file at the
     *         specified path
     */
    public static ImageDescriptor getPluginImageDescriptor(final Object plugin,
            final String name) {
        try {
            try {
                URL url = getPluginImageURL(plugin, name);
                return ImageDescriptor.createFromURL(url);
            } catch (Throwable e) {
                // nothing
            }
        } catch (Throwable e) {
            // nothing
        }
        return null;
    }

    /**
     * Retuns an URL based on a plugin and file path.
     *
     * @param plugin Object The plugin containing the file path
     * @param name String The file path
     * @return URL The URL representing the file at the specified path
     * @throws Exception
     */
    private static URL getPluginImageURL(final Object plugin, final String name)
            throws Exception {
        Class pluginClass = Class.forName("org.eclipse.core.runtime.Plugin");
        Method getDescriptorMethod = pluginClass.getMethod("getDescriptor",
                new Class[] {});
        Class pluginDescriptorClass = Class
                .forName("org.eclipse.core.runtime.IPluginDescriptor");
        Method getInstallURLMethod = pluginDescriptorClass.getMethod(
                "getInstallURL", new Class[] {});
        //
        Object pluginDescriptor = getDescriptorMethod.invoke(plugin,
                new Object[] {});
        URL installURL = (URL) getInstallURLMethod.invoke(pluginDescriptor,
                new Object[] {});
        URL url = new URL(installURL, name);
        return url;
    }

    // ////////////////////////////
    // Font support
    // ////////////////////////////

    /**
     * Maps font names to fonts.
     */
    private static HashMap fontMap = new HashMap();

    /**
     * Maps fonts to their bold versions.
     */
    private static HashMap fontToBoldFontMap = new HashMap();

    /**
     * Returns a font based on its name, height and style.
     *
     * @param name String The name of the font
     * @param height int The height of the font
     * @param style int The style of the font
     * @return Font The font matching the name, height and style
     */
    public static Font getFont(final String name, final int height,
            final int style) {
        String fullName = name + "|" + height + "|" + style;
        Font font = (Font) fontMap.get(fullName);
        if (font == null) {
            font = new Font(Display.getCurrent(), name, height, style);
            fontMap.put(fullName, font);
        }
        return font;
    }

    /**
     * Return a bold version of the give font.
     *
     * @param baseFont Font The font for whoch a bold version is desired
     * @return Font The bold version of the give font
     */
    public static Font getBoldFont(final Font baseFont) {
        Font font = (Font) fontToBoldFontMap.get(baseFont);
        if (font == null) {
            FontData[] fontDatas = baseFont.getFontData();
            FontData data = fontDatas[0];
            font = new Font(Display.getCurrent(), data.getName(), data
                    .getHeight(), SWT.BOLD);
            fontToBoldFontMap.put(baseFont, font);
        }
        return font;
    }

    /**
     * Dispose all of the cached fonts.
     */
    public static void disposeFonts() {
        for (Iterator iter = fontMap.values().iterator(); iter.hasNext();) {
            ((Font) iter.next()).dispose();
        }
        fontMap.clear();
    }

    // ////////////////////////////
    // CoolBar support
    // ////////////////////////////

    /**
     * Fix the layout of the specified CoolBar.
     *
     * @param bar CoolBar The CoolBar that shgoud be fixed
     */
    public static void fixCoolBarSize(final CoolBar bar) {
        CoolItem[] items = bar.getItems();
        // ensure that each item has control (at least empty one)
        for (int i = 0; i < items.length; i++) {
            CoolItem item = items[i];
            if (item.getControl() == null) {
                item.setControl(new Canvas(bar, SWT.NONE) {
                    @Override
                    public Point computeSize(final int wHint, final int hHint,
                            final boolean changed) {
                        return new Point(20, 20);
                    }
                });
            }
        }
        // compute size for each item
        for (int i = 0; i < items.length; i++) {
            CoolItem item = items[i];
            Control control = item.getControl();
            control.pack();
            Point size = control.getSize();
            item.setSize(item.computeSize(size.x, size.y));
        }
    }

    // ////////////////////////////
    // Cursor support
    // ////////////////////////////

    /**
     * Maps IDs to cursors.
     */
    private static HashMap idToCursorMap = new HashMap();

    /**
     * Returns the system cursor matching the specific ID.
     *
     * @param id int The ID value for the cursor
     * @return Cursor The system cursor matching the specific ID
     */
    public static Cursor getCursor(final int id) {
        Integer key = Integer.valueOf(id);
        Cursor cursor = (Cursor) idToCursorMap.get(key);
        if (cursor == null) {
            cursor = new Cursor(Display.getDefault(), id);
            idToCursorMap.put(key, cursor);
        }
        return cursor;
    }

    /**
     * Dispose all of the cached cursors.
     */
    public static void disposeCursors() {
        for (Iterator i = idToCursorMap.values().iterator(); i.hasNext();) {
            ((Cursor) i.next()).dispose();
        }
        idToCursorMap.clear();
    }
}
