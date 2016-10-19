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
 * ---------------------------------------------------------------------
 *
 * History
 *   07.01.2008 (ohl): created
 */
package org.knime.core.node.util;

import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.eclipse.swt.widgets.Display;
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;

/**
 * Provides helper methods mostly useful when implementing NodeViews.<br>
 * Especially the methods {@link #runOrInvokeLaterInEDT(Runnable)}, {@link #invokeAndWaitInEDT(Runnable)}, and
 * {@link #invokeLaterInEDT(Runnable)} are quite useful because they take care of
 * retaining the {@link NodeContext} if the calling thread.
 *
 * @author Peter Ohl, University of Konstanz
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public final class ViewUtils {
    private static final boolean disableSWTEventLoopWorkaround = Boolean
        .getBoolean("knime.swt.disableEventLoopWorkaround");

    private static final boolean DISPLAY_AVAILABLE;

    private static final AtomicBoolean LAF_SET = new AtomicBoolean();

    private static final Runnable EMPTY_RUNNABLE = new Runnable() {
        @Override
        public void run() {
        }
    };


    static {
        boolean available;
        if (Boolean.getBoolean("java.awt.headless")) {
            available = false;
        } else {
            try {
                // This call is to initialize the SWT class. The class initialization will throw an error in case
                // no X libraries are available under Linux. In this case we shouldn't try to access the SWT display.
                // See also bug #4465.
                Display.getAppName();
                available = true;
            } catch (UnsatisfiedLinkError err) {
                NodeLogger.getLogger(ViewUtils.class).info("Could not intitialize SWT display, probably because X11 or GTK "
                        + " libraries are missing. Assuming we are running headless.", err);
                available = false;
            }
        }
        DISPLAY_AVAILABLE = available;
    }

    private ViewUtils() {

    }

    /**
     * Executes the specified runnable in the Swing Event Dispatch Thread. If
     * the caller is already running in the EDT, it just executes the
     * <code>run</code> method of the runnable, otherwise it passes the runnable
     * to the EDT and waits until its <code>run</code> method returns.
     *
     * @param runMe the <code>run</code> method of this will be executed.
     * @throws InvocationTargetRuntimeException if the executed code throws an
     *             exception (the cause of it is set to the exception thrown by
     *             the executed code then), or if the execution was interrupted
     *             in the EDT.
     * @see SwingUtilities#invokeAndWait(Runnable)
     */
    public static void invokeAndWaitInEDT(final Runnable runMe)
            throws InvocationTargetRuntimeException {

        // if already in event dispatch thread, run immediately
        if (SwingUtilities.isEventDispatchThread()) {
            runMe.run();
        } else {
            try {
                if (!disableSWTEventLoopWorkaround && DISPLAY_AVAILABLE) {
                    final Runnable nodeContextRunnable = getNodeContextWrapper(runMe);
                    final Display display = Display.getCurrent();
                    if (display == null) {
                        // not the main thread, safe to use invokeAndWait directly
                        SwingUtilities.invokeAndWait(nodeContextRunnable);
                    } else {
                        // we are in the main thread which is the GUI thread under MacOSX
                        // we must not block here (i.e. use invokeAndWait) but keep the event loop
                        // running
                        final AtomicBoolean swingRunnableFinished = new AtomicBoolean();

                        Runnable lockedRunnable = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    nodeContextRunnable.run();
                                } finally {
                                    swingRunnableFinished.set(true);
                                    // this call is to wake up the main thread from its "sleep" below
                                    display.asyncExec(EMPTY_RUNNABLE);
                                }
                            }
                        };
                        SwingUtilities.invokeLater(lockedRunnable);
                        while (!swingRunnableFinished.get()) {
                            if (!display.readAndDispatch()) {
                                display.sleep();
                            }
                        }
                    }
                } else {
                    // otherwise queue into event dispatch thread
                    SwingUtilities.invokeAndWait(getNodeContextWrapper(runMe));
                }
            } catch (InvocationTargetException ite) {
                Throwable c = ite.getCause();
                if (c == null) {
                    c = ite;
                }
                throw new InvocationTargetRuntimeException(
                        "Exception during execution in Event Dispatch Thread",
                        c);
            } catch (InterruptedException ie) {
                Throwable c = ie.getCause();
                if (c == null) {
                    c = ie;
                }
                throw new InvocationTargetRuntimeException(
                        Thread.currentThread() + " was interrupted", c);
            }
        }

    }

    /**
     * Executes the specified runnable some time in the Swing Event Dispatch
     * Thread. If the caller is already running in the EDT, it immediately
     * executes the <code>run</code> method and does not return until it
     * finishes. Otherwise it queues the argument for execution in the EDT and
     * returns (not waiting for the <code>run</code> method to finish).
     *
     * @param runMe the <code>run</code> method of this will be executed.
     * @see SwingUtilities#invokeLater(Runnable)
     */
    public static void runOrInvokeLaterInEDT(final Runnable runMe) {

        if (SwingUtilities.isEventDispatchThread()) {
            runMe.run();
        } else {
            SwingUtilities.invokeLater(getNodeContextWrapper(runMe));
        }
    }


    /**
     * Executes the specified runnable some time in the Swing Event Dispatch
     * Thread.
     *
     * @param runMe the <code>run</code> method of this will be executed.
     * @see SwingUtilities#invokeLater(Runnable)
     * @since 2.8
     */
    public static void invokeLaterInEDT(final Runnable runMe) {
        SwingUtilities.invokeLater(getNodeContextWrapper(runMe));
    }

    /**
     * Constructs a new FlowLayout panel and adds the argument component(s) to
     * it. The layout has a centered alignment and a default 5-unit
     * horizontal and vertical gap.
     *
     * @param components The components to add
     * @return The panel /w flow layout and the components added to it.
     */
    public static JPanel getInFlowLayout(final JComponent... components) {
        return getInFlowLayout(FlowLayout.CENTER, components);
    }

    /**
     * Constructs a new FlowLayout panel and adds the argument component(s) to
     * it. The layout has the specified alignment and a default 5-unit
     * horizontal and vertical gap. The value of the alignment argument must be
     * one of FlowLayout.LEFT, FlowLayout.RIGHT, FlowLayout.CENTER,
     * FlowLayout.LEADING, or FlowLayout.TRAILING.
     *
     * @param alignment The flow panel alignment
     * @param components The components to add
     * @return The panel /w flow layout and the components added to it.
     */
    public static JPanel getInFlowLayout(final int alignment,
            final JComponent... components) {
        return getInFlowLayout(alignment, 5, 5, components);
    }

    /**
     * Constructs a new FlowLayout panel and adds the argument component(s) to
     * it. The layout has the specified alignment and horizontal and vertical
     * gap. The value of the alignment argument must be
     * one of FlowLayout.LEFT, FlowLayout.RIGHT, FlowLayout.CENTER,
     * FlowLayout.LEADING, or FlowLayout.TRAILING.
     *
     * @param      alignment The flow panel alignment
     * @param      hgap    the horizontal gap between components
     *                     and between the components and the
     *                     borders of the <code>Container</code>
     * @param      vgap    the vertical gap between components
     *                     and between the components and the
     *                     borders of the <code>Container</code>
     * @param components The components to add
     * @return The panel /w flow layout and the components added to it.
     */
    public static JPanel getInFlowLayout(final int alignment, final int hgap,
            final int vgap, final JComponent... components) {
        JPanel panel = new JPanel(new FlowLayout(alignment, hgap, vgap));
        for (JComponent c : components) {
            panel.add(c);
        }
        return panel;
    }

    /** Convenience method to load an icon from package relative path. The icon
     * is supposed to be located relative to the package associated with the
     * argument class under the path <code>path</code>. This method will
     * not throw an exception when the loading fails but instead return a
     * <code>null</code> icon.
     * @param className The class object, from which to retrieve the
     * {@link Class#getPackage() package}, e.g. <code>FooValue.class</code>.
     * @param path The icon path relative to package associated with the
     * class argument.
     * @return the icon loaded from that path or <code>null</code> if it loading fails
     */
    public static Icon loadIcon(
            final Class<?> className, final String path) {
        try {
            ClassLoader loader = className.getClassLoader();
            String packagePath =
                className.getPackage().getName().replace('.', '/');
            String correctedPath = path;
            if (!path.startsWith("/")) {
                correctedPath = "/" + path;
            }

            correctedPath = packagePath + correctedPath;
            if (correctedPath.contains("..")) {
                // replace relative paths such as "/abc/../bla.png"
                // with "/bla.png" because otherwise resources in Jar-files
                // won't be found
                Pattern p = Pattern.compile("/[^/\\.]+/\\.\\./");
                Matcher m = p.matcher(correctedPath);
                while (m.find()) {
                    correctedPath = m.replaceFirst("/");
                    m = p.matcher(correctedPath);
                }
            }

            return new ImageIcon(loader.getResource(correctedPath));
        } catch (Exception e) {
            NodeLogger.getLogger(DataValue.class).debug(
                    "Unable to load icon at path " + path, e);
            return null;
        }
    }

    /**
     * Centers a window relative to a parent window.
     *
     * @param window the window that will be centered
     * @param parentBounds bounds of the parent relative to which the window should be centered
     * @since 2.12
     */
    public static void centerLocation(final Window window, final Rectangle parentBounds) {
        if (parentBounds != null) {
            // Middle point of rectangle
            Point middle = new Point(parentBounds.width / 2, parentBounds.height / 2);
            // Left upper point for window
            Point newLocation = new Point(middle.x - (window.getWidth() / 2) + parentBounds.x,
                                          middle.y - (window.getHeight() / 2) + parentBounds.y);
            window.setLocation(newLocation);
        } else {
        	window.setLocationRelativeTo(null);
        }
    }

    private static Runnable getNodeContextWrapper(final Runnable orig) {
        if (NodeContext.getContext() == null) {
            return orig;
        } else {
            final NodeContext ctx = NodeContext.getContext();
            return new Runnable() {
                @Override
                public void run() {
                    NodeContext.pushContext(ctx);
                    try {
                        orig.run();
                    } finally {
                        NodeContext.removeLastContext();
                    }

                }
            };
        }
    }

    /**
     * Return a zoom factor depending on the display's dpi resolution. Currently 3 values are returned: 0-143 dpi: 100%,
     * 144-191: 150%, 192-higher: 200%. Call in SWT Display Thread!
     *
     * @return a value representing the display zoom factor. 100 equals 100%, 150 is 150%, etc.
     * @since 3.0
     */
    public static int getDisplayZoom() {
        if (!DISPLAY_AVAILABLE) {
            return 100;
        }
        /* Eclipse knows (currently) 3 zoom levels depending on the display's dpi.
         * They use 100% up until 143 dpi, 150% for 144-191dpi and 200% for more than 191 dpi.
         * also see
         * https://wiki.eclipse.org/Bug_421383_-_Graphics_Scaling_issues_on_high_DPI_displays
         * org.eclipse.swt.graphics.DPIUtil implements some highDPI API - but is package private :(
         */
        Display defDisplay = Display.getDefault();
        int x_dpi = defDisplay.getDPI().x;
        if (x_dpi < 144) {
            return 100;
        } else if (x_dpi < 192) {
            return 150;
        } else {
            return 200;
        }
    }

    /** filenames of image of original size should (optionally) end on this. */
    private static final String IMAGE_100_EXTENSION = "@1x";

    /** filenames of image of 150% size should end on this. */
    private static final String IMAGE_150_EXTENSION = "@1.5x";

    /** filenames of image of 200% size should end on this. */
    private static final String IMAGE_200_EXTENSION = "@2x";

    /**
     * @param path full path to the icon file without size extension
     * @return the path with the extension for the 100% image (@1x) appended (before the file extension)
     * @since 3.0
     */
    public static String getImageFile100(final String path) {
        return getBaseName(path) + IMAGE_100_EXTENSION + getExtension(path);
    }

    /**
     * @param path100 full path to the icon file with or without the 100% extension
     * @return the path with the extension for the 150% image (@1.5x) appended (before the file extension)
     * @since 3.0
     */
    public static String getImageFile150(final String path100) {
        return getImageFileNameWithoutSize(path100) + IMAGE_150_EXTENSION + getExtension(path100);
    }

    /**
     * @param path100 full path to the icon file with or without the 100% extension
     * @return the path with the extension for the 200% image (@2x) appended (before the file extension)
     * @since 3.0
     */
    public static String getImageFile200(final String path100) {
        return getImageFileNameWithoutSize(path100) + IMAGE_200_EXTENSION + getExtension(path100);
    }

    /**
     * @return filename without zoom/size extension and without size extension
     */
    private static String getImageFileNameWithoutSize(final String fileName) {
        String ext = getExtension(fileName);
        int cutoff = ext.length();
        int length = fileName.length();
        if (fileName.endsWith(IMAGE_100_EXTENSION + ext)) {
            cutoff += IMAGE_100_EXTENSION.length();
        } else if (fileName.endsWith(IMAGE_150_EXTENSION + ext)) {
            cutoff += IMAGE_150_EXTENSION.length();
        } else if (fileName.endsWith(IMAGE_200_EXTENSION + ext)) {
            cutoff += IMAGE_200_EXTENSION.length();
        }
        if (length > cutoff) {
            return fileName.substring(0, length - cutoff);
        } else {
            return "";
        }
    }

    /**
     * @param fileName to strip extension off
     * @return the full file path without extension.
     */
    private static String getBaseName(final String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 1) {
            return fileName;
        }
        return fileName.substring(0, dot);
    }

    /**
     * @param fileName to get extension from
     * @return the extension of the file name provided. Including the dot. "/tmp/foo.png" will return ".png"
     */
    protected static String getExtension(final String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 1) {
            return "";
        }
        return fileName.substring(dot);
    }

    /**
     * Sets the look&feel for all Swing components. This method can be called multiple times but only the first
     * invocation will change the look&feel.
     *
     * @since 3.3
     */
    public static void setLookAndFeel() {
        if (!LAF_SET.getAndSet(true)) {
            SwingUtilities.invokeLater(() -> {
                try {
                    String sysLaF = UIManager.getSystemLookAndFeelClassName();
                    // The GTK L&F has apparently some serious problems. Weka dialogs
                    // cannot be opened (NPE) and in 1.6.0 there were problems with
                    // "Xlib: sequence lost" ... resulting in KNIME going down.
                    if (sysLaF.equals("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")) {
                        sysLaF = UIManager.getCrossPlatformLookAndFeelClassName();
                    }
                    UIManager.setLookAndFeel(sysLaF);
                } catch (Exception e) {
                    NodeLogger.getLogger(ViewUtils.class).error("Unable to set Look&Feel: " + e.getMessage(), e);
                    // use the default look and feel then.
                }
            });
        }
    }
}
