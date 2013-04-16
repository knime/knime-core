/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;

/**
 * Provides helper methods mostly useful when implementing NodeViews.
 *
 * @author ohl, University of Konstanz
 */
public final class ViewUtils {

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
                // otherwise queue into event dispatch thread
                SwingUtilities.invokeAndWait(runMe);
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
            SwingUtilities.invokeLater(runMe);
        }
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
     * @return the icon loaded from that path or null if it loading fails
     */
    public static Icon loadIcon(
            final Class<?> className, final String path) {
        ImageIcon icon;
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

            icon = new ImageIcon(loader.getResource(correctedPath));
        } catch (Exception e) {
            NodeLogger.getLogger(DataValue.class).debug(
                    "Unable to load icon at path " + path, e);
            icon = null;
        }
        return icon;
    }


}
