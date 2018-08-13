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
 *   Sep 2, 2017 (hornm): created
 */
package org.knime.core.ui.wrapper;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.UI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;

import com.google.common.collect.MapMaker;

/**
 * Wraps another object and provides static methods to wrap and unwrap those.
 *
 * @author Martin Horn, University of Konstanz
 *
 * @param <W> the class that is wrapped
 */
public interface Wrapper<W> {

    /**
     * The global wrapper map - not for direct access - use {@link #wrapOrGet(Object, Function)} instead!
     */
    static final ConcurrentMap<Integer, Wrapper<?>> WRAPPER_MAP = new MapMaker().weakValues().makeMap();

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
     * Shortcut for <code>unwrapOptional(wrapper, NodeContainer.class)</code>.
     *
     * @param wrapper
     * @return the unwrapped node container or an empty optional
     * @since 3.6
     */
    public static Optional<NodeContainer> unwrapNCOptional(final UI wrapper) {
        return unwrapOptional(wrapper, NodeContainer.class);
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
     * Either returns a wrapper object stored in a central global wrapper map (with the identity hashcode as key - see
     * {@link System#identityHashCode(Object)}). Or wraps the given object (see {@link #wrap(Object, Class)}).
     *
     * Its primary purpose is to workaround "1:1" wrappers. Within the eclipse UI very often instances are checked for
     * object equality. Consequently, there must be exactly one wrapper class instance for a certain object to be
     * wrapped. Two wrapper instances wrapping the same object should be avoided (this happens if, e.g. a getter method
     * is called twice and each time a new wrapper instance is created around the same object returned).
     *
     * The global wrapper map caches object instances (weak references) for look up with the identity hashcode as key.
     *
     * @param object the object to be wrapped. Its identity hashcode serves as the key to look for an already existing
     *            wrapper
     * @param wrap function that does the wrapping if no already existing wrapper has been found (i.e. was cached)
     * @return a new or already existing wrapper instance that wraps an object of the given type
     */
    @SuppressWarnings("unchecked")
    public static <W> Wrapper<W> wrapOrGet(final W object, final Function<W, Wrapper<W>> wrap) {
        if (object == null) {
            return null;
        }
        Wrapper<?> wrapper = WRAPPER_MAP.computeIfAbsent(System.identityHashCode(object), i -> wrap.apply(object));
        assert wrapper.unwrap() == object;
        return (Wrapper<W>)wrapper;
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
        return unwrapOptional(wrapper, wrappedObjectClass).map(w -> w).orElseThrow(() -> new IllegalArgumentException(
            "Wrapper object ('" + wrapper.getClass().getSimpleName() + "') is actually not a wrapper. Cannot unwrap."));
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

    /**
     * Tries to wrap the passed object into its specific UI-wrapper, e.g. {@link WorkflowManager} into
     * {@link WorkflowManagerUI}. If it's already a {@link UI}-class, the very same object is just returned.
     *
     * @param obj the object to possibly wrap
     * @return the object wrapped into a UI-wrapper or itself if already UI
     * @throws IllegalArgumentException if the object is _not_ of type {@link UI}, {@link ConnectionContainer} or
     *             {@link NodeContainer}
     */
    public static UI wrap(final Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof UI) {
            return (UI)obj;
        } else if (obj instanceof ConnectionContainer) {
            return ConnectionContainerWrapper.wrap((ConnectionContainer)obj);
        } else if (obj instanceof NodeContainer) {
            return NodeContainerWrapper.wrap((NodeContainer)obj);
        } else {
            throw new IllegalArgumentException("The obj '" + obj.getClass().getSimpleName()
                + "' cannot be wrapped into a UI-object nor is it a UI object.");
        }
    }
}
