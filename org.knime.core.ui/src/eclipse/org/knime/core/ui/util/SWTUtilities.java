/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Aug 10, 2018 (loki): created
 */
package org.knime.core.ui.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;

/**
 * I did as best a search as i could to find a pre-existing class of this ilk (something that provides SWT related
 * helper methods and could find none.) Also, i named this <code>SWTUtilities</code> instead of <code>SWTUtil</code>,
 * the latter which better obey the majority of the utility class names in this package, because there is already an
 * internal Eclipse class of that name.  It seems plausible that there is this sort of collection of methods
 * elsewhere; if there isn't, consider moving the graphics-related static utility methods in
 * <code>org.knime.workbench.editor2.editparts.AnnotationEditPart</code> into this class as well.
 *
 * @author loki der quaeler
 * @since 3.7
 */
public class SWTUtilities {
    /**
     * As discovered in AP-10122, when KNIME receives an action due to mouse event passthrough (a situation possible on
     * macOS where mouse & scroll events can pass to a desktop-visible window even though the owning application is not
     * the application in the foreground), Display.getCurrent().getActiveShell() returns null.
     *
     * This method will attempt to return the applicable Shell instance, by hook or by crook.
     *
     * @param display an instance of Display which contains the active shell; if this is null, this method will return
     *            null
     * @return a Shell instance which is the active shell for the application
     */
    public static Shell getActiveShell(final Display display) {
        if (display == null) {
            return null;
        }

        final Shell shell = display.getActiveShell();

        if (shell == null) {
            Shell likelyActiveShell = null;

            for (Shell s : display.getShells()) {
                if (s.getText().contains("KNIME")) {
                    return s;
                }

                if (s.getShells().length == 0) {
                    likelyActiveShell = s;
                }
            }

            return likelyActiveShell;
        }

        return shell;
    }

    /**
     * As discovered in AP-10122, when KNIME receives an action due to mouse event passthrough (a situation possible on
     * macOS where mouse & scroll events can pass to a desktop-visible window even though the owning application is not
     * the application in the foreground), Display.getCurrent().getActiveShell() returns null.
     *
     * This method will attempt to return the applicable Shell instance, by hook or by crook.
     *
     * @return a Shell instance which is the active shell for the application
     */
    public static Shell getActiveShell() {
        return getActiveShell(Display.getCurrent());
    }

    /**
     * Oh com'on SWT - seriously: why isn't this a method in {@link Composite}?
     *
     * @param parent the Composite instance which will have all of its children removed.
     * @since 4.0
     */
    public static void removeAllChildren(final Composite parent) {
        for (final Control child : parent.getChildren()) {
            child.dispose();
        }
    }

    /**
     * I'm not sure why the SWT authors thought implementing 'setVisible(false)' should mean we space should continue to
     * be laid out but just not painted, like the worlds most obvious invisibility cloak... but they did - bless'em.
     *
     * @param widget the widget to make visible or hidden; the widget's layout data must be {@link GridData} or else
     *            this method will do nothing more than invoke {@link Control#setVisible(boolean)}
     * @param visible true to make visible, false to make hidden
     * @since 4.0
     */
    public static void spaceReclaimingSetVisible(final Control widget, final boolean visible) {
        final Object o = widget.getLayoutData();

        widget.setVisible(visible);
        if (o instanceof GridData) {
            ((GridData)o).exclude = !visible;
        }
    }

    /**
     * This loads a font via our main workbench {@link Display}. The SWT method only takes a file path so we need write
     * this font to a temporary file first (and we take an {@link InputStream} as we will be getting this asset usually
     * out of bundle.) The SWT authors swear that once loaded into a given Display, it can be fetched from that Display
     * instance, but this is not what testing bears out; we can still create an instance of {@link Font} referencing the
     * correct name. We should cache this loading in the future.
     *
     * @param is a stream for a TTF; this will be closed by this method
     * @param size the desired point size of the font
     * @param style e.g {@link SWT#BOLD}
     * @return an instance of {@link Font} if the load is successful - null if not; remember to dispose() when
     *         appropriate
     * @since 4.0
     */
    public static Font loadFontFromInputStream(final InputStream is, final int size, final int style) {
        if (is == null) {
            return null;
        }

        final Path tempDirectory = KNIMEConstants.getKNIMETempPath();
        final String tempFontFileName = "font_" + System.currentTimeMillis()
                                                + Integer.toHexString((int)(Math.random() * 10000.0)) + ".ttf";
        final Path fontFile = tempDirectory.resolve(tempFontFileName);
        String fontFileAbsolutePath = null;

        try {
            try (final ReadableByteChannel inputChannel = Channels.newChannel(is)) {
                try (final FileChannel fontFileChannel = FileChannel.open(fontFile, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    fontFileChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);

                    fontFileAbsolutePath = fontFile.toString();
                }
            }

            fontFile.toFile().deleteOnExit();
        } catch (final IOException e) {
            NodeLogger.getLogger(SWTUtilities.class).error("Error caught writing temporary font.", e);
        }

        if (fontFileAbsolutePath == null) {
            return null;
        }

        final Display d = PlatformUI.getWorkbench().getDisplay();
        d.loadFont(fontFileAbsolutePath);

        try {
            final String fontName = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontFile.toFile()).deriveFont(12).getFontName();

            return new Font(d, fontName, size, style);
        } catch (final Exception e) {
            NodeLogger.getLogger(SWTUtilities.class).error("Error caught writing temporary font.", e);
        }

        return null;
    }
}
