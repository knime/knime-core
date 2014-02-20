/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Scott Kovatch - interface to apple.awt.CHIViewEmbeddedFrame
 *******************************************************************************/
package org.knime.workbench.ui.macos;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.Library;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * This class provides a bridge between SWT and AWT, so that it
 * is possible to embed AWT components in SWT and vice versa.
 *
 * @see <a href="http://www.eclipse.org/swt/snippets/#awt">Swing/AWT snippets</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 *
 * @since 3.0
 */
public class SWT_AWT {

	/**
	 * The name of the embedded Frame class. The default class name
	 * for the platform will be used if <code>null</code>.
	 */
	public static String embeddedFrameClass;

	/**
	 * Key for looking up the embedded frame for a Composite using
	 * getData().
	 */
	static String EMBEDDED_FRAME_KEY = "org.eclipse.swt.awt.SWT_AWT.embeddedFrame";

	static {
		System.setProperty("apple.awt.usingSWT", "true");
	}

	static final String JDK16_FRAME = "apple.awt.CEmbeddedFrame";
	static final String JDK17_FRAME = "sun.lwawt.macosx.CViewEmbeddedFrame";

	static boolean loaded, swingInitialized;

	static native final long /*int*/ getAWTHandle (Canvas canvas);

	static synchronized void loadLibrary () {
		if (loaded) {
            return;
        }
		loaded = true;
		Toolkit.getDefaultToolkit();
		/*
		 * Note that the jawt library is loaded explicitly
		 * because it cannot be found by the library loader.
		 * All exceptions are caught because the library may
		 * have been loaded already.
		 */
		try {
			System.loadLibrary("jawt");
		} catch (Throwable e) {}
		Library.loadLibrary("swt-awt");
	}

	static synchronized void initializeSwing() {
		if (swingInitialized) {
            return;
        }
		swingInitialized = true;
		try {
			/* Initialize the default focus traversal policy */
			Class[] emptyClass = new Class[0];
			Object[] emptyObject = new Object[0];
			Class clazz = Class.forName("javax.swing.UIManager");
			Method method = clazz.getMethod("getDefaults", emptyClass);
			if (method != null) {
                method.invoke(clazz, emptyObject);
            }
		} catch (Throwable e) {}
	}

/**
 * Returns a <code>java.awt.Frame</code> which is the embedded frame
 * associated with the specified composite.
 *
 * @param parent the parent <code>Composite</code> of the <code>java.awt.Frame</code>
 * @return a <code>java.awt.Frame</code> the embedded frame or <code>null</code>.
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 *
 * @since 3.2
 */
public static Frame getFrame(final Composite parent) {
	if (parent == null) {
        SWT.error(SWT.ERROR_NULL_ARGUMENT);
    }
	if ((parent.getStyle() & SWT.EMBEDDED) == 0) {
        return null;
    }
	return (Frame) parent.getData(EMBEDDED_FRAME_KEY);
}

/**
 * Creates a new <code>java.awt.Frame</code>. This frame is the root for
 * the AWT components that will be embedded within the composite. In order
 * for the embedding to succeed, the composite must have been created
 * with the SWT.EMBEDDED style.
 * <p>
 * IMPORTANT: As of JDK1.5, the embedded frame does not receive mouse events.
 * When a lightweight component is added as a child of the embedded frame,
 * the cursor does not change. In order to work around both these problems, it is
 * strongly recommended that a heavyweight component such as <code>java.awt.Panel</code>
 * be added to the frame as the root of all components.
 * </p>
 *
 * @param parent the parent <code>Composite</code> of the new <code>java.awt.Frame</code>
 * @return a <code>java.awt.Frame</code> to be the parent of the embedded AWT components
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the parent Composite does not have the SWT.EMBEDDED style</li>
 * </ul>
 *
 * @since 3.0
 */
public static Frame new_Frame(final Composite parent) {
	if (parent == null) {
        SWT.error(SWT.ERROR_NULL_ARGUMENT);
    }
	if ((parent.getStyle() & SWT.EMBEDDED) == 0) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}

	final long /*int*/ handle = parent.view.id;

	Class clazz = null;
	try {
		String className = embeddedFrameClass != null ? embeddedFrameClass : JDK16_FRAME;
		if (embeddedFrameClass == null) {
			clazz = Class.forName(className, true, ClassLoader.getSystemClassLoader());
		} else {
			clazz = Class.forName(className);
		}
	} catch (ClassNotFoundException cne) {
		try {
			clazz = Class.forName(JDK17_FRAME);
		} catch (ClassNotFoundException cne1) {
			SWT.error (SWT.ERROR_NOT_IMPLEMENTED, cne1);
		}
	} catch (Throwable e) {
		SWT.error (SWT.ERROR_UNSPECIFIED , e, " [Error while starting AWT]");
	}

	initializeSwing();
	Object value = null;
	Constructor constructor = null;
	try {
		constructor = clazz.getConstructor (new Class [] {long.class});
		value = constructor.newInstance (new Object [] {new Long(handle)});
	} catch (Throwable e) {
		SWT.error(SWT.ERROR_NOT_IMPLEMENTED, e);
	}
	final Frame frame = (Frame) value;
	final boolean isJDK17 = JDK17_FRAME.equals(frame.getClass().getName());
	frame.addNotify();

	parent.setData(EMBEDDED_FRAME_KEY, frame);

	/* Forward the iconify and deiconify events */
	final Listener shellListener = new Listener () {
		public void handleEvent (final Event e) {
			switch (e.type) {
				case SWT.Deiconify:
					EventQueue.invokeLater(new Runnable () {
						public void run () {
							frame.dispatchEvent (new WindowEvent (frame, WindowEvent.WINDOW_DEICONIFIED));
						}
					});
					break;
				case SWT.Iconify:
					EventQueue.invokeLater(new Runnable () {
						public void run () {
							frame.dispatchEvent (new WindowEvent (frame, WindowEvent.WINDOW_ICONIFIED));
						}
					});
					break;
			}
		}
	};
	Shell shell = parent.getShell ();
	shell.addListener (SWT.Deiconify, shellListener);
	shell.addListener (SWT.Iconify, shellListener);

	/* When display is disposed the frame is disposed in AWT EventQueue.
	 * Force main event loop to run to let the frame finish dispose.
	 */
	final Display display = parent.getDisplay();
	display.addListener(SWT.Dispose, new Listener() {
		public void handleEvent(final Event event) {
			while (frame.isDisplayable() && !display.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			//Frame finished dispose, the listener can be removed
			if (!display.isDisposed()) {
				display.removeListener(SWT.Dispose, this);
			}
		}
	});

	/*
	 * Generate the appropriate events to activate and deactivate
	 * the embedded frame. This is needed in order to make keyboard
	 * focus work properly for lightweights.
	 */
	Listener listener = new Listener () {
		public void handleEvent (final Event e) {
			switch (e.type) {
				case SWT.Dispose:
					Shell shell = parent.getShell ();
					shell.removeListener (SWT.Deiconify, shellListener);
					shell.removeListener (SWT.Iconify, shellListener);
					if (isJDK17) {
						shell.removeListener (SWT.Activate, this);
						shell.removeListener (SWT.Deactivate, this);
					}
					parent.setVisible(false);
					EventQueue.invokeLater(new Runnable () {
						public void run () {
							try {
								frame.dispose ();
							} catch (Throwable e) {}
						}
					});
					break;
				case SWT.Activate:
					if (!parent.isFocusControl()) {
                        return;
                    }
				case SWT.FocusIn:
					EventQueue.invokeLater(new Runnable () {
						public void run () {
							if (frame.isActive()) {
                                return;
                            }
							try {
								Class clazz = frame.getClass();
								Method method = clazz.getMethod("synthesizeWindowActivation", new Class[]{boolean.class});
								if (method != null) {
                                    method.invoke(frame, new Object[]{new Boolean(true)});
                                }
							} catch (Throwable e) {e.printStackTrace();}
						}
					});
					break;
				case SWT.Deactivate:
				case SWT.FocusOut:
					EventQueue.invokeLater(new Runnable () {
						public void run () {
							if (!frame.isActive()) {
                                return;
                            }
							try {
								Class clazz = frame.getClass();
								Method method = clazz.getMethod("synthesizeWindowActivation", new Class[]{boolean.class});
								if (method != null) {
                                    method.invoke(frame, new Object[]{new Boolean(false)});
                                }
							} catch (Throwable e) {e.printStackTrace();}
						}
					});
					break;
			}
		}
	};

	parent.addListener (SWT.FocusIn, listener);
	if (isJDK17) {
		parent.addListener(SWT.FocusOut, listener);
		//To allow cross-app activation/deactivation
		shell.addListener (SWT.Activate, listener);
		shell.addListener (SWT.Deactivate, listener);
	} else {
		parent.addListener (SWT.Deactivate, listener);
	}
	parent.addListener (SWT.Dispose, listener);

	display.asyncExec(new Runnable() {
		public void run () {
			if (parent.isDisposed()) {
                return;
            }
			final Rectangle clientArea = parent.getClientArea();
			if (isJDK17) {
				try {
					Method method = frame.getClass().getMethod("validateWithBounds", new Class[] {int.class, int.class, int.class, int.class});
					if (method != null) {
                        method.invoke(frame, new Object[]{new Integer(clientArea.x), new Integer(clientArea.y), new Integer(clientArea.width), new Integer(clientArea.height)});
                    }
				} catch (Throwable e) {e.printStackTrace();}
			} else {
				EventQueue.invokeLater(new Runnable () {
					public void run () {
						frame.setSize(clientArea.width, clientArea.height);
						frame.validate();

						// Bug in Cocoa AWT? For some reason the frame isn't showing up on first draw.
						// Toggling visibility seems to be the only thing that works.
						frame.setVisible(false);
						frame.setVisible(true);
					}
				});
			}
		}
	});

	return frame;
}

/**
 * Creates a new <code>Shell</code>. This Shell is the root for
 * the SWT widgets that will be embedded within the AWT canvas.
 *
 * @param display the display for the new Shell
 * @param parent the parent <code>java.awt.Canvas</code> of the new Shell
 * @return a <code>Shell</code> to be the parent of the embedded SWT widgets
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the display is null</li>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the parent's peer is not created</li>
 * </ul>
 *
 * @since 3.0
 */
public static Shell new_Shell(final Display display, final Canvas parent) {
	if (display == null) {
        SWT.error (SWT.ERROR_NULL_ARGUMENT);
    }
	if (parent == null) {
        SWT.error (SWT.ERROR_NULL_ARGUMENT);
    }
	long /*int*/ handle = 0;

	try {
		loadLibrary ();
		handle = getAWTHandle (parent);
	} catch (Throwable e) {
		SWT.error (SWT.ERROR_NOT_IMPLEMENTED, e);
	}
	if (handle == 0) {
        SWT.error (SWT.ERROR_INVALID_ARGUMENT, null, " [peer not created]");
    }

	final Shell shell = Shell.cocoa_new (display, handle);
	final ComponentListener listener = new ComponentAdapter () {
		@Override
        public void componentResized (final ComponentEvent e) {
			display.asyncExec (new Runnable () {
				public void run () {
					if (shell.isDisposed()) {
                        return;
                    }
					Dimension dim = parent.getSize ();
					shell.setSize (dim.width, dim.height);
				}
			});
		}
	};
	parent.addComponentListener(listener);
	shell.addListener(SWT.Dispose, new Listener() {
		public void handleEvent(final Event event) {
			parent.removeComponentListener(listener);
		}
	});
	shell.setVisible (true);
	return shell;
}
}