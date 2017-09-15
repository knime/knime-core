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
 *   Sep 2, 2017 (hornm): created
 */
package org.knime.core.ui.wrapper;

import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.UI;

/**
 * Wraps another object and provides static methods to wrap and unwrap those.
 *
 * @author Martin Horn, University of Konstanz
 *
 * @param <W> the class that is wrapped
 */
public interface Wrapper<W> {

    static final WeakHashMap<Object, Wrapper> WRAPPER_MAP = new WeakHashMap<Object, Wrapper>();

    /**
     * Unwraps the wrapped object.
     *
     * @return the wrapped object
     */
    W unwrap();

    /**
     * Shortcut for <code>unwrap(wrapper, WorkflowManager.class)</code>.
     *
     * @param wrapper
     * @return the wrapped workflow manager
     */
    public static WorkflowManager unwrapWFM(final UI wrapper) {
        return unwrap(wrapper, WorkflowManager.class);
    }

    /**
     * Shortcut for <code>unwrapOptional(wrapper, WorkflowManager.class)</code>.
     *
     * @param wrapper
     * @return the wrapped workflow manager
     */
    public static Optional<WorkflowManager> unwrapWFMOptional(final UI wrapper) {
        return unwrapOptional(wrapper, WorkflowManager.class);
    }

    /**
     * Shortcut for <code>unwrap(wrapper, NodeContainer.class)</code>.
     *
     * @param wrapper
     * @return the unwrapped node container
     */
    public static NodeContainer unwrapNC(final UI wrapper) {
        return unwrap(wrapper, NodeContainer.class);
    }

    /**
     * Shortcut for <code>unwrap(wrapper, ConnectionContainer.class)</code>.
     *
     * @param wrapper
     * @return the unwrapped node container
     */
    public static ConnectionContainer unwrapCC(final UI wrapper) {
        return unwrap(wrapper, ConnectionContainer.class);
    }

    /**
     * Either returns a wrapper object stored in a central global wrapper map for the provided key, or, wraps the given
     * object (see {@link #wrap(Object, Class)}).
     *
     * It is NOT checked, whether the cached wrapper instance really wraps the very same object that is passed, too! The
     * caller must assure this himself by passing the right key.
     *
     * Its primary purpose is to workaround "1:1" wrappers. Within the eclipse UI very often instances are checked for
     * object equality. Consequently, there must be exactly one wrapper class instance for a certain object to be
     * wrapped. Two wrapper instances wrapping the same object should be avoided (this happens if, e.g. a getter method
     * is called twice and each time a new wrapper instance is created around the same object returned).
     *
     * The global wrapper map caches object instances (weak references) for look up with the dedicated key.
     *
     * @param object the object to be wrapped that also at the same time serves as the key to look for already existing
     *            wrapper
     * @param wrap function that does the wrapping if necessary
     * @return a new or already existing wrapper instance that wraps an object of the given type
     */
    @SuppressWarnings("unchecked")
    public static <W> Wrapper<W> wrapOrGet(final W object, final Function<W, Wrapper<W>> wrap) {
        if (object == null) {
            return null;
        }
        return WRAPPER_MAP.computeIfAbsent(object, o -> wrap.apply((W)o));
    }

    /**
     * Checks if the passed object is a wrapper (i.e. implements {@link Wrapper}) and returns the wrapped object.
     * Otherwise an {@link IllegalArgumentException} is thrown.
     *
     * @param wrapper the supposed wrapper
     * @param wrappedObjectClass the class of the object to be unwrapped
     * @throws IllegalArgumentException if the wrapper object is not a wrapper
     * @return the unwrapped object
     */
    public static <W> W unwrap(final UI wrapper, final Class<W> wrappedObjectClass) {
        return unwrapOptional(wrapper, wrappedObjectClass).map(w -> w).orElseThrow(
            () -> new IllegalArgumentException("Wrapper object ('" + wrapper.getClass().getSimpleName() + "') is actually not a wrapper. Cannot unwrap."));
    }

    /**
     * Checks if the passed object is a wrapper (i.e. implements {@link Wrapper}) and returns the wrapped object.
     * Otherwise an empty optional is returned.
     *
     * @param wrapper the supposed wrapper
     * @param wrappedObjectClass the class of the object to be unwrapped
     * @return the unwrapped object or an empty optional if it coudn't be unwrapped
     * @throws IllegalArgumentException if the unwrapped object is not compatible with the given wrapped object class
     */
    public static <W> Optional<W> unwrapOptional(final UI wrapper, final Class<W> wrappedObjectClass) {
        if (wrapper instanceof Wrapper) {
            Wrapper cast = (Wrapper)wrapper;
            Object unwrap = cast.unwrap();
            if (!wrappedObjectClass.isAssignableFrom(unwrap.getClass())) {
                throw new IllegalArgumentException("The object " + wrapper
                    + " cannot be unwrapped since it's not of type " + wrappedObjectClass.getSimpleName());
            }
            return Optional.of((W) unwrap);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Shortcut for unwrapOptional(wrapper, NodeContainer.class).
     *
     * @param wrapper the supposed wrapper
     * @return the unwrapped node container or an empty optional if it couldn't be unwrapped
     */
    public static Optional<NodeContainer> unwrapOptionalNC(final UI wrapper) {
        return unwrapOptional(wrapper, NodeContainer.class);
    }

    /**
     * Checks whether a object wraps a certain object of a given class.
     *
     * @param wrapper the supposed wrapper
     * @param wrappedObjectClass the class of the object to check for
     * @return <code>true</code> if the object wraps an object with the given class, <code>false</code> if it doesn't
     *         wrap it OR the object is not of type {@link Wrapper}
     */
    public static <W> boolean wraps(final Object wrapper, final Class<W> wrappedObjectClass) {
        if (wrapper instanceof Wrapper) {
            Wrapper<W> cast = (Wrapper<W>)wrapper;
            return wrappedObjectClass.isAssignableFrom(cast.unwrap().getClass());
        } else {
            return false;
        }
    }

}
