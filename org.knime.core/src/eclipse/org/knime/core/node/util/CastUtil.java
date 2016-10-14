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
 *   Sep 30, 2016 (hornm): created
 */
package org.knime.core.node.util;

import java.util.Optional;

import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Utility class providing functions to select and use implementations of interfaces instead of the interfaces itself.
 * The methods help to unify those cases (exception handling and messages) and also later on track those lines in the
 * code (call hierarchy).
 *
 * @author Martin Horn, KNIME.com
 */
public class CastUtil {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CastUtil.class);

    private static int COUNT = 0;

    private CastUtil() {
        //utility class
    }

    /**
     * @param theInterface
     * @return the {@link WorkflowManager} implementation
     */
    public static final WorkflowManager castWFM(final IWorkflowManager theInterface) {
        return cast(theInterface, WorkflowManager.class);
    }

    /**
     * @param theInterface
     * @return the {@link WorkflowManager} implementation
     */
    public static final Optional<WorkflowManager> castWFMOptional(final IWorkflowManager theInterface) {
        return castOptional(theInterface, WorkflowManager.class);
    }

    /**
     * Tries to cast the given interface to a particular implementation. Logger warning messages will be issued when a
     * class has been cast.
     *
     * @param theInterface the object to cast
     * @param clazz the implementation to cast to
     * @return the interface cast to the desired class or an exception, if not possible
     * @throws ClassCastException if cannot be cast, i.e. the desired implementation is not given, logger warning
     *             message will be issued
     */
    public static final <I, C extends I> C cast(final I theInterface, final Class<C> clazz) {
        if (clazz.isInstance(theInterface)) {
            LOGGER.warn("Implementation (" + clazz.getSimpleName()
                + ") used directly instead of the interface (global count: " + (COUNT++) + ").");
            return clazz.cast(theInterface);
        } else {
            throw new ClassCastException("The interface " + theInterface.getClass().getSimpleName()
                + " cannot be used here directly. Specific implementation required: " + clazz.getSimpleName());
        }
    }

    /**
     * Tries to cast the given interface to a particular implementation. If cast is not possibly an empty optional will
     * be returned. No logger warning messages will be issued.
     *
     * @param theInterface the object to cast
     * @param clazz the implementation to cast to
     * @return the interface cast to the desired class or an empty optional, if not possible
     */
    public static final <I, C extends I> Optional<C> castOptional(final I theInterface, final Class<C> clazz) {
        if (clazz.isInstance(theInterface)) {
            return Optional.of(clazz.cast(theInterface));
        } else {
            return Optional.empty();
        }
    }

}
