/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ----------------------------------------------------------------------------
 */
package org.knime.product.p2.actions;

import java.io.File;
import java.util.Map;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Custom p2 action that can be used by plugins in order to execute arbitrary
 * commands during their installation.
 *
 * @author Iman Karim <iman@biosolveit.de>
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.5.2
 */
public class ShellExec extends ProvisioningAction {
    private static final Bundle bundle = FrameworkUtil
            .getBundle(ShellExec.class);

    private final static ILog logger = Platform.getLog(bundle);

    @Override
    public IStatus execute(final Map<String, Object> parameters) {
        String os = null; // Operating System the Command is for. Null means
                          // all.

        if (parameters.containsKey("os")) {
            os = (String)parameters.get("os");
        }

        if (verifyOS(os)) {
            String directory = null;
            String command = null;

            if (parameters.containsKey("command")) {
                command = (String)parameters.get("command");
                logger.log(new Status(IStatus.INFO, bundle.getSymbolicName(),
                        "ShellExec command: " + command));
            }
            if (command == null) {
                logger.log(new Status(IStatus.ERROR, bundle.getSymbolicName(),
                        "Command is null!"));
                return Status.CANCEL_STATUS;
            }

            if (parameters.containsKey("directory")) {
                directory = (String)parameters.get("directory");
                logger.log(new Status(IStatus.INFO, bundle.getSymbolicName(),
                        "ShellExec directory: " + directory));
            }
            if (directory == null) {
                logger.log(new Status(IStatus.ERROR, bundle.getSymbolicName(),
                        "ShellExec directory is null!"));
                return Status.CANCEL_STATUS;
            }

            File dirFile = new File(directory);
            try {
                Process p = Runtime.getRuntime().exec(command, null, dirFile);
                int exitVal = p.waitFor();
                if (exitVal != 0) {
                    logger.log(new Status(IStatus.ERROR, bundle
                            .getSymbolicName(),
                            "ShellExec command exited non-zero exit value"));
                    return Status.CANCEL_STATUS;
                }
            } catch (Exception e) {
                logger.log(new Status(IStatus.ERROR, bundle.getSymbolicName(),
                        "Exception occured", e));
                return Status.CANCEL_STATUS;
            }
        }
        return Status.OK_STATUS;
    }

    private boolean verifyOS(final String os) {
        return (os == null) || Platform.getOS().equals(os);
    }

    @Override
    public IStatus undo(final Map<String, Object> parameters) {
        return Status.OK_STATUS;
    }
}