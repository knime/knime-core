/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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
 * Created on Jan 2, 2014 by wiswedel
 */
package org.knime.core.node.util;

import org.knime.core.node.InvalidSettingsException;

/**
 * Static check functions used during settings loading etc.
 *
 * @author Marcel Hanser, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public final class CheckUtils {

    /** No instantiation. */
    private CheckUtils() {
    }

    /**
     * Checks the given object to be not <code>null</code>. If the check fails the exception is thrown containing the
     * message resulting from {@link String#format(String, Object...)} with the given template and arguments. <br/>
     * <b>Note: the string is only formatted if the exception is actually thrown.</b>
     *
     * @param toCheck the object which may not be <code>null</code>
     * @param template the string template which determines the exception message
     * @param templateArgs the arguments for the string formating
     * @return the argument itself
     * @throws IllegalArgumentException if argument is <code>null</code>
     * @throws NullPointerException if template is <code>null</code>
     * @param <T> the type of the object
     */
    public static final <T> T
        checkArgumentNotNull(final T toCheck, final String template, final Object... templateArgs)
            throws IllegalArgumentException {
        return checkArgumentNotNull(toCheck, stringFormatSupplier(template, templateArgs));
    }

    /**
     * Checks the given predicate to be <code>true</code>.
     *
     * @param predicate is checked to be <code>true</code>
     * @param message the exception message
     * @throws IllegalArgumentException if predicate is <code>false</code>
     */
    public static final void checkArgument(final boolean predicate, final String message)
        throws IllegalArgumentException {
        checkArgument(predicate, stringSupplier(message));
    }

    /**
     * Checks the given predicate to be <code>true</code>. If the check fails the exception is thrown containing the
     * message resulting from {@link String#format(String, Object...)} with the given template and arguments. <br/>
     * <b>Note: the string is only formatted if the exception is actually thrown.</b>
     *
     * @param predicate is checked to be <code>true</code>
     * @param template the string template which determines the exception message
     * @param templateArgs the arguments for the string formating
     * @throws IllegalArgumentException if predicate is <code>false</code>
     * @throws NullPointerException if the template is <code>null</code>
     */
    public static final void
        checkArgument(final boolean predicate, final String template, final Object... templateArgs)
            throws IllegalArgumentException {
        checkArgument(predicate, stringFormatSupplier(template, templateArgs));
    }

    /**
     * Checks the given object to be not <code>null</code>.
     *
     * @param toCheck the object which may not be <code>null</code>
     * @return the argument itself
     * @throws NullPointerException if toCheck is <code>null</code>
     * @param <T> the type of the object
     */
    public static final <T> T checkNotNull(final T toCheck) throws NullPointerException {
        return checkNotNull(toCheck, stringSupplier(""));
    }

    /**
     * Checks the given object to be not <code>null</code>.
     *
     * @param toCheck the object which may not be <code>null</code>
     * @return the argument itself
     * @param message the exception message
     * @throws NullPointerException if toCheck is <code>null</code>
     * @param <T> the type of the object
     */
    public static final <T> T checkNotNull(final T toCheck, final String message) throws NullPointerException {
        return checkNotNull(toCheck, stringSupplier(message));
    }

    /**
     * Checks the given object to be not <code>null</code>. If the check fails the exception is thrown containing the
     * message resulting from {@link String#format(String, Object...)} with the given template and arguments. <br/>
     * <b>Note: the string is only formatted if the exception is actually thrown.</b>
     *
     * @param toCheck the object which may not be <code>null</code>
     * @param template the string template which determines the exception message
     * @param templateArgs the arguments for the string formating
     * @return the argument itself
     * @throws NullPointerException if the argument or template is <code>null</code>
     * @param <T> the type of the object
     */
    public static final <T> T checkNotNull(final T toCheck, final String template, final Object... templateArgs)
        throws NullPointerException {
        return checkNotNull(toCheck, stringFormatSupplier(template, templateArgs));
    }

    /**
     * Checks the given object to be not <code>null</code>. If the check fails the exception is thrown containing the
     * message resulting from {@link String#format(String, Object...)} with the given template and arguments. <br/>
     * <b>Note: the string is only formatted if the exception is actually thrown.</b>
     *
     * @param predicate is checked to be <code>true</code>
     * @param template the string template which determines the exception message
     * @param templateArgs the arguments for the string formating
     * @throws InvalidSettingsException if predicate is <code>false</code>
     * @throws NullPointerException if the template is <code>null</code>
     */
    public static final void checkSetting(final boolean predicate, final String template, final Object... templateArgs)
        throws InvalidSettingsException {
        checkSetting(predicate, stringFormatSupplier(template, templateArgs));
    }

    /**
     * Checks the given object to be not <code>null</code>. If the check fails the exception is thrown containing the
     * message resulting from {@link String#format(String, Object...)} with the given template and arguments. <br/>
     * <b>Note: the string is only formatted if the exception is actually thrown.</b>
     *
     * @param toCheck the object which may not be <code>null</code>
     * @param template the string template which determines the exception message
     * @param templateArgs the arguments for the string formating
     * @return the argument itself
     * @throws InvalidSettingsException if the object is <code>null</code>
     * @throws NullPointerException if the template is <code>null</code>
     * @param <T> the type of the object
     */
    public static final <T> T checkSettingNotNull(final T toCheck, final String template, final Object... templateArgs)
        throws InvalidSettingsException {
        return checkSettingNotNull(toCheck, stringFormatSupplier(template, templateArgs));
    }

    private static final <T> T checkArgumentNotNull(final T argument, final Supplier<String> supplier)
        throws IllegalArgumentException {
        checkArgument(argument != null, supplier);
        return argument;
    }

    private static final <T> T checkSettingNotNull(final T argument, final Supplier<String> supplier)
        throws InvalidSettingsException {
        checkSetting(argument != null, supplier);
        return argument;
    }

    private static final void checkArgument(final boolean predicate, final Supplier<String> supplier)
        throws IllegalArgumentException {
        if (!predicate) {
            throw new IllegalArgumentException(supplier.get());
        }
    }

    private static final void checkSetting(final boolean predicate, final Supplier<String> supplier)
        throws InvalidSettingsException {
        if (!predicate) {
            throw new InvalidSettingsException(supplier.get());
        }
    }

    private static final <T> T checkNotNull(final T argument, final Supplier<String> supplier)
        throws NullPointerException {
        if (argument == null) {
            throw new NullPointerException(supplier.get());
        }
        return argument;
    }

    private static Supplier<String> stringFormatSupplier(final String template, final Object... arguments) {
        // implicite null check
        final String curr = template.toString();

        return new Supplier<String>() {

            @Override
            public String get() {
                return String.format(curr, arguments);
            }
        };
    }

    private static Supplier<String> stringSupplier(final String template) {
        return new Supplier<String>() {

            @Override
            public String get() {
                return template;
            }
        };
    }

    private static interface Supplier<T> {
        T get();
    }
}
