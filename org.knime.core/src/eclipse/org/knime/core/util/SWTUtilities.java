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
package org.knime.core.util;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

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
     * If we have a "SWT Utility Methods" class lurking somewhere, this might be better suited to go there; it is
     * presently in this class only because the source problem arose due attempting to open dialogs in a KNIME
     * application which was in the background.
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
     * If we have a "SWT Utility Methods" class lurking somewhere, this might be better suited to go there; it is
     * presently in this class only because the source problem arose due attempting to open dialogs in a KNIME
     * application which was in the background.
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
}
