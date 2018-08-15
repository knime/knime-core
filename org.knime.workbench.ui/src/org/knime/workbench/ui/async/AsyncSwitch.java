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
 *   Jul 10, 2018 (hornm): created
 */
package org.knime.workbench.ui.async;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.node.workflow.async.AsyncNodeContainerUI;
import org.knime.core.ui.node.workflow.async.AsyncWorkflowAnnotationUI;
import org.knime.core.ui.node.workflow.async.AsyncWorkflowManagerUI;
import org.knime.core.ui.node.workflow.async.CompletableFutureEx;
import org.knime.core.util.SWTUtilities;

/**
 * Helper methods to switch between the synchronous and asynchronous implementations of an interface (such as
 * {@link WorkflowManagerUI} and {@link AsyncWorkflowManagerUI}).
 *
 * In case of asynchronous methods, the provided helper methods will block till the result is available (i.e. the future
 * is completed). While waiting a 'busy cursor' is shown and later a progress monitor window (see
 * {@link IProgressService#busyCursorWhile(org.eclipse.jface.operation.IRunnableWithProgress)}) displayed. If the
 * async-method call fails (e.g. due to a timeout), the error will be logged and displayed in an error-dialog.
 *
 * Synchronous methods (i.e. methods that don't return a {@link Future}) are treated normally, i.e. just called and
 * their result returned.
 *
 * Usage example:
 *
 * <pre>
 *  copy = wfmAsyncSwitch(
 *      wfm -> wfm.cut(content.build()),
 *      wfm -> wfm.cutAsync(content.build()),
 *      hostWFM, "Deleting content ...");
 * </pre>
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class AsyncSwitch {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AsyncSwitch.class);

    private AsyncSwitch() {
        //utility class
    }

    /**
     * Convenient method to either call the synchronous methods on a {@link WorkflowManagerUI} implementation or the
     * asynchronous methods if the more specific {@link AsyncWorkflowManagerUI} is implemented.
     *
     * Method will block in any case.
     *
     * @param syncWfm the callback called if the provided workflow manager is a synchronous implementation
     * @param asyncWfm the callback called the the provided workflow manager is a asynchronous implementation (i.e.
     *            derived from {@link AsyncWorkflowManagerUI}).
     * @param wfm the actual implementation that is checked and the calls are delegated to
     * @param waitingMessage a message to be shown while waiting for a asynchronous method call to finish
     * @return the actual result of the method call (possibly after waiting in the async case)
     */
    public static <T> T wfmAsyncSwitch(final Function<WorkflowManagerUI, T> syncWfm,
        final Function<AsyncWorkflowManagerUI, CompletableFuture<? extends T>> asyncWfm, final WorkflowManagerUI wfm,
        final String waitingMessage) {
        if (wfm instanceof AsyncWorkflowManagerUI) {
            return waitForTermination(asyncWfm.apply((AsyncWorkflowManagerUI)wfm), waitingMessage);
        } else {
            return syncWfm.apply(wfm);
        }
    }

    /**
     * Almost the same as {@link #wfmAsyncSwitch(Function, Function, WorkflowManagerUI, String)} but for
     * {@link NodeContainerUI}/{@link AsyncNodeContainerUI}.
     *
     *
     * @param syncNc
     * @param asyncNc
     * @param nc
     * @param waitingMessage
     * @return the actual result, possibly after some waiting in the async case
     */
    public static <T> T ncAsyncSwitch(final Function<NodeContainerUI, T> syncNc,
        final Function<AsyncNodeContainerUI, CompletableFuture<T>> asyncNc, final NodeContainerUI nc,
        final String waitingMessage) {
        if (nc instanceof AsyncNodeContainerUI) {
            return waitForTermination(asyncNc.apply((AsyncNodeContainerUI)nc), waitingMessage);
        } else {
            return syncNc.apply(nc);
        }
    }

    /**
     * Almost the same as {@link #wfmAsyncSwitch(Function, Function, WorkflowManagerUI, String)} but additionally
     * re-throws a specified exception caught from {@link CompletableFutureEx#getOrThrow()}.
     *
     * @param syncWfm
     * @param asyncWfm
     * @param wfm
     * @param waitingMessage
     * @return the actual result, possibly after some waiting in the async case
     * @throws E the expected exception
     */
    public static <T, E extends Exception> T wfmAsyncSwitchRethrow(
        final RethrowFunction<WorkflowManagerUI, T, E> syncWfm,
        final Function<AsyncWorkflowManagerUI, CompletableFutureEx<? extends T, E>> asyncWfm,
        final WorkflowManagerUI wfm, final String waitingMessage) throws E {
        if (wfm instanceof AsyncWorkflowManagerUI) {
            final AtomicReference<T> ref = new AtomicReference<T>();
            final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
            CompletableFutureEx<? extends T, E> future = asyncWfm.apply((AsyncWorkflowManagerUI)wfm);
            try {
                PlatformUI.getWorkbench().getProgressService().busyCursorWhile((monitor) -> {
                    monitor.beginTask(waitingMessage, 100);
                    try {
                        ref.set(future.getOrThrow());
                    } catch (ExecutionException e) {
                        exception.set(e.getCause());
                    } catch (Exception ex) {
                        exception.set(ex);
                    }
                });
            } catch (InterruptedException | InvocationTargetException ex) {
                exception.set(ex);
            }
            if (exception.get() != null) {
                Throwable t = exception.get();
                if (future.getExceptionClass().isAssignableFrom(t.getClass())) {
                    throw (E)t;
                } else {
                    openDialogAndLog(t, waitingMessage);
                }
            }
            return ref.get();
        } else {
            return syncWfm.apply(wfm);
        }
    }

    /**
     * Almost the same as {@link #wfmAsyncSwitchRethrow(Function, Function, WorkflowManagerUI, String)} but for
     * {@link NodeContainerUI}/{@link AsyncNodeContainerUI}.
     *
     *
     * @param syncNc
     * @param asyncNc
     * @param nc
     * @param waitingMessage
     * @return the actual result, possibly after some waiting in the async case
     * @throws E the expected exception
     */
    public static <T, E extends Exception> T ncAsyncSwitchRethrow(final RethrowFunction<NodeContainerUI, T, E> syncNc,
        final Function<AsyncNodeContainerUI, CompletableFutureEx<T, E>> asyncNc, final NodeContainerUI nc,
        final String waitingMessage) throws E {
        if (nc instanceof AsyncNodeContainerUI) {
            final AtomicReference<T> ref = new AtomicReference<T>();
            final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
            CompletableFutureEx<T, E> future = asyncNc.apply((AsyncNodeContainerUI)nc);
            try {
                PlatformUI.getWorkbench().getProgressService().busyCursorWhile((monitor) -> {
                    monitor.beginTask(waitingMessage, 100);
                    try {
                        ref.set(future.getOrThrow());
                    } catch (ExecutionException ex) {
                        exception.set(ex.getCause());
                    } catch (Exception ex) {
                        exception.set(ex);
                    }
                });
            } catch (InterruptedException | InvocationTargetException ex) {
                exception.set(ex);
            }
            if (exception.get() != null) {
                Throwable t = exception.get();
                if (future.getExceptionClass().isAssignableFrom(t.getClass())) {
                    throw (E)t;
                } else {
                    openDialogAndLog(t, waitingMessage);
                }
            }
            return ref.get();
        } else {
            return syncNc.apply(nc);
        }
    }

    /**
     * Almost the same as {@link #wfmAsyncSwitch(Function, Function, WorkflowManagerUI, String)} but for
     * {@link WorkflowAnnotation}/{@link AsyncWorkflowAnnotationUI}.
     *
     * @param syncWa
     * @param asyncWa
     * @param wa
     * @param waitingMessage
     * @return the actual result, possibly after some waiting in the async case
     */
    public static <T> T waAsyncSwitch(final Function<WorkflowAnnotation, T> syncWa,
        final Function<AsyncWorkflowAnnotationUI, CompletableFuture<T>> asyncWa, final WorkflowAnnotation wa,
        final String waitingMessage) {
        if (wa instanceof AsyncWorkflowAnnotationUI) {
            return waitForTermination(asyncWa.apply((AsyncWorkflowAnnotationUI)wa), waitingMessage);
        } else {
            return syncWa.apply(wa);
        }
    }

    /**
     * Waits for the provided future to complete while showing a busy cursor and later a 'waiting'-dialog.
     *
     * @param future future to wait to be completed
     * @param waitingMessage the message to be displayed in the waiting dialog
     * @return the future's result when done
     */
    public static <T> T waitForTermination(final CompletableFuture<T> future, final String waitingMessage) {
        final AtomicReference<T> ref = new AtomicReference<T>();
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        try {
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile((monitor) -> {
                monitor.beginTask(waitingMessage, 100);
                try {
                    ref.set(future.get());
                } catch (ExecutionException e) {
                    exception.set(e.getCause());
                }
            });
        } catch (InterruptedException | InvocationTargetException ex) {
            exception.set(ex);
        }
        if (exception.get() != null) {
            openDialogAndLog(exception.get(), waitingMessage);
        }
        return ref.get();
    }

    /**
     * A function that possibly throws a pre-defined exception on {@link #apply(Object)}.
     */
    @FunctionalInterface
    public static interface RethrowFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    private static void openDialogAndLog(final Throwable e, final String waitingMessage) {
        String message = "An unexpected problem occurred while '" + waitingMessage + "': " + e.getMessage();
        final Display display = Display.getDefault();
        display.syncExec(() -> {
            final Shell shell = SWTUtilities.getActiveShell(display);
            MessageBox mb = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
            mb.setText("Unexpected Problem");
            mb.setMessage(message + "\nSee log for details.");
            mb.open();
            LOGGER.error(message, e);
        });
    }
}
