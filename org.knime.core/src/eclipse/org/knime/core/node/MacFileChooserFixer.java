/*
 * ------------------------------------------------------------------------
 *
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
 *   Jun 11, 2014 (jenkins): created
 */
package org.knime.core.node;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.eclipse.core.runtime.Platform;

/**
 * Event listener that works around bug #5170.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
final class MacFileChooserFixer implements AWTEventListener {
    private static boolean installed = false;

    /**
     * Installs the fixer upon the first call. Subsequent calls have no effect.
     */
    public static synchronized void installFixer() {
        if (!installed) {
            if (Platform.OS_MACOSX.equals(Platform.getOS())) {
                Toolkit.getDefaultToolkit().addAWTEventListener(new MacFileChooserFixer(),
                    AWTEvent.WINDOW_EVENT_MASK);
            }
            installed = true;
        }
    }

    private MacFileChooserFixer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void eventDispatched(final AWTEvent event) {
        if (event.getID() == WindowEvent.WINDOW_OPENED) {
            final JFileChooser fc = findFileChooser((Component)event.getSource());
            if ((fc != null) && (fc.getParent() != null)) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // if the focus is set to another window, the text file is suddenly editable
                        fc.getParent().requestFocus();
                        fc.requestFocus();
                    }
                });
            }
        }
    }

    private JFileChooser findFileChooser(final Component comp) {
        if (comp instanceof JFileChooser) {
            return (JFileChooser)comp;
        } else if (comp instanceof Container) {
            for (Component c : ((Container)comp).getComponents()) {
                JFileChooser fc = findFileChooser(c);
                if (fc != null) {
                    return fc;
                }
            }
        }
        return null;
    }
}
