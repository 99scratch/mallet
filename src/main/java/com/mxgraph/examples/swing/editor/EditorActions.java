/*
 * Copyright (c) 2001-2012, JGraph Ltd
 */
package com.mxgraph.examples.swing.editor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxGdCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.shape.mxStencilShape;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.swing.util.mxGraphActions;
import com.mxgraph.swing.view.mxCellEditor;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxResources;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import com.sensepost.mallet.util.XmlUtil;

/**
 *
 */
public class EditorActions {
	/**
	 * 
	 * @param e
	 * @return Returns the graph for the given action event.
	 */
	public static final BasicGraphEditor getEditor(ActionEvent e) {
		if (e.getSource() instanceof Component) {
			Component component = (Component) e.getSource();

			while (component != null
					&& !(component instanceof BasicGraphEditor)) {
				component = component.getParent();
			}

			return (BasicGraphEditor) component;
		}

		return null;
	}

	/**
	 *
	 */
	private static void resetEditor(BasicGraphEditor editor) {
		editor.setModified(false);
		editor.setCurrentFile(null);
		editor.getUndoManager().clear();
		editor.getGraphComponent().zoomAndCenter();
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class ToggleOutlineItem extends JCheckBoxMenuItem {
		/**
		 * 
		 */
		public ToggleOutlineItem(final BasicGraphEditor editor, String name) {
			super(name);
			setSelected(true);

			addActionListener(new ActionListener() {
				/**
				 * 
				 */
				public void actionPerformed(ActionEvent e) {
					final mxGraphOutline outline = editor.getGraphOutline();
					outline.setVisible(!outline.isVisible());
					outline.revalidate();

					SwingUtilities.invokeLater(new Runnable() {
						/*
						 * (non-Javadoc)
						 * 
						 * @see java.lang.Runnable#run()
						 */
						public void run() {
							if (outline.getParent() instanceof JSplitPane) {
								if (outline.isVisible()) {
									((JSplitPane) outline.getParent())
											.setDividerLocation(editor
													.getHeight() - 300);
									((JSplitPane) outline.getParent())
											.setDividerSize(6);
								} else {
									((JSplitPane) outline.getParent())
											.setDividerSize(0);
								}
							}
						}
					});
				}
			});
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class ExitAction extends AbstractAction {
		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			BasicGraphEditor editor = getEditor(e);

			if (editor != null) {
				if (!editor.isModified()
						|| JOptionPane.showConfirmDialog(editor,
								mxResources.get("loseChanges")) == JOptionPane.YES_OPTION) {
					mxGraph graph = editor.getGraphComponent().getGraph();

					// Check modified flag and display save dialog
					mxCell root = new mxCell();
					root.insert(new mxCell());
					graph.getModel().setRoot(root);

					editor.setModified(false);
					editor.setCurrentFile(null);
					editor.getGraphComponent().zoomAndCenter();
				} else
					return;

				editor.exit();
			}
			for (Window w : Window.getWindows()) {
				w.dispose();
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class StylesheetAction extends AbstractAction {
		/**
		 * 
		 */
		protected String stylesheet;

		/**
		 * 
		 */
		public StylesheetAction(String stylesheet) {
			this.stylesheet = stylesheet;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof mxGraphComponent) {
				mxGraphComponent graphComponent = (mxGraphComponent) e
						.getSource();
				mxGraph graph = graphComponent.getGraph();
				mxCodec codec = new mxCodec();
				Document doc = mxUtils.loadDocument(EditorActions.class
						.getResource(stylesheet).toString());

				if (doc != null) {
					codec.decode(doc.getDocumentElement(),
							graph.getStylesheet());
					graph.refresh();
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class ZoomPolicyAction extends AbstractAction {
		/**
		 * 
		 */
		protected int zoomPolicy;

		/**
		 * 
		 */
		public ZoomPolicyAction(int zoomPolicy) {
			this.zoomPolicy = zoomPolicy;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof mxGraphComponent) {
				mxGraphComponent graphComponent = (mxGraphComponent) e
						.getSource();
				graphComponent.setPageVisible(true);
				graphComponent.setZoomPolicy(zoomPolicy);
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class ScaleAction extends AbstractAction {
		/**
		 * 
		 */
		protected double scale;

		/**
		 * 
		 */
		public ScaleAction(double scale) {
			this.scale = scale;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof mxGraphComponent) {
				mxGraphComponent graphComponent = (mxGraphComponent) e
						.getSource();
				double scale = this.scale;

				if (scale == 0) {
					String value = (String) JOptionPane.showInputDialog(
							graphComponent, mxResources.get("value"),
							mxResources.get("scale") + " (%)",
							JOptionPane.PLAIN_MESSAGE, null, null, "");

					if (value != null) {
						scale = Double.parseDouble(value.replace("%", "")) / 100;
					}
				}

				if (scale > 0) {
					graphComponent.zoomTo(scale, graphComponent.isCenterZoom());
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class SaveAction extends AbstractAction {
		/**
		 * 
		 */
		protected boolean showDialog;

		/**
		 * 
		 */
		protected String lastDir = null;

		/**
		 * 
		 */
		public SaveAction(boolean showDialog) {
			this.showDialog = showDialog;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			BasicGraphEditor editor = getEditor(e);

			if (editor != null) {
				mxGraphComponent graphComponent = editor.getGraphComponent();
				mxGraph graph = graphComponent.getGraph();
				FileFilter selectedFilter = null;
				DefaultFileFilter mxeFilter = new DefaultFileFilter(".mxe",
						"mxGraph Editor " + mxResources.get("file") + " (.mxe)");
				String filename = null;
				boolean dialogShown = false;

				if (showDialog || editor.getCurrentFile() == null) {
					String wd;

					if (lastDir != null) {
						wd = lastDir;
					} else if (editor.getCurrentFile() != null) {
						wd = editor.getCurrentFile().getParent();
					} else {
						wd = System.getProperty("user.dir");
					}

					JFileChooser fc = new JFileChooser(wd);

					// Adds the default file format
					FileFilter defaultFilter = mxeFilter;
					fc.addChoosableFileFilter(defaultFilter);

					// Adds special vector graphics formats and HTML
					fc.setFileFilter(defaultFilter);
					int rc = fc.showDialog(null, mxResources.get("save"));
					dialogShown = true;

					if (rc != JFileChooser.APPROVE_OPTION) {
						return;
					} else {
						lastDir = fc.getSelectedFile().getParent();
					}

					filename = fc.getSelectedFile().getAbsolutePath();
					selectedFilter = fc.getFileFilter();

					if (selectedFilter instanceof DefaultFileFilter) {
						String ext = ((DefaultFileFilter) selectedFilter)
								.getExtension();

						if (!filename.toLowerCase().endsWith(ext)) {
							filename += ext;
						}
					}

					if (new File(filename).exists()
							&& JOptionPane.showConfirmDialog(graphComponent,
									mxResources.get("overwriteExistingFile")) != JOptionPane.YES_OPTION) {
						return;
					}
				} else {
					filename = editor.getCurrentFile().getAbsolutePath();
				}

				try {
					String ext = filename
							.substring(filename.lastIndexOf('.') + 1);

					if (ext.equalsIgnoreCase("mxe")
							|| ext.equalsIgnoreCase("xml")) {
						mxCodec codec = new mxCodec();

						FileOutputStream fos = new FileOutputStream(filename);
						StreamResult result = new StreamResult(fos);
						XmlUtil.pretty(codec.encode(graph.getModel()), result,
								2);

						editor.setModified(false);
						editor.setCurrentFile(new File(filename));
					} else {
						JOptionPane.showMessageDialog(graphComponent,
								"Unsupported file type",
								mxResources.get("error"),
								JOptionPane.ERROR_MESSAGE);
					}
				} catch (Throwable ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(graphComponent,
							ex.toString(), mxResources.get("error"),
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class PromptPropertyAction extends AbstractAction {
		/**
		 * 
		 */
		protected Object target;

		/**
		 * 
		 */
		protected String fieldname, message;

		/**
		 * 
		 */
		public PromptPropertyAction(Object target, String message) {
			this(target, message, message);
		}

		/**
		 * 
		 */
		public PromptPropertyAction(Object target, String message,
				String fieldname) {
			this.target = target;
			this.message = message;
			this.fieldname = fieldname;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof Component) {
				try {
					Method getter = target.getClass().getMethod(
							"get" + fieldname);
					Object current = getter.invoke(target);

					// TODO: Support other atomic types
					if (current instanceof Integer) {
						Method setter = target.getClass().getMethod(
								"set" + fieldname, new Class[] { int.class });

						String value = (String) JOptionPane.showInputDialog(
								(Component) e.getSource(), "Value", message,
								JOptionPane.PLAIN_MESSAGE, null, null, current);

						if (value != null) {
							setter.invoke(target, Integer.parseInt(value));
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			// Repaints the graph component
			if (e.getSource() instanceof mxGraphComponent) {
				mxGraphComponent graphComponent = (mxGraphComponent) e
						.getSource();
				graphComponent.repaint();
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class TogglePropertyItem extends JCheckBoxMenuItem {
		/**
		 * 
		 */
		public TogglePropertyItem(Object target, String name, String fieldname) {
			this(target, name, fieldname, false);
		}

		/**
		 * 
		 */
		public TogglePropertyItem(Object target, String name, String fieldname,
				boolean refresh) {
			this(target, name, fieldname, refresh, null);
		}

		/**
		 * 
		 */
		public TogglePropertyItem(final Object target, String name,
				final String fieldname, final boolean refresh,
				ActionListener listener) {
			super(name);

			// Since action listeners are processed last to first we add the
			// given
			// listener here which means it will be processed after the one
			// below
			if (listener != null) {
				addActionListener(listener);
			}

			addActionListener(new ActionListener() {
				/**
				 * 
				 */
				public void actionPerformed(ActionEvent e) {
					execute(target, fieldname, refresh);
				}
			});

			PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * java.beans.PropertyChangeListener#propertyChange(java.beans.
				 * PropertyChangeEvent)
				 */
				public void propertyChange(PropertyChangeEvent evt) {
					if (evt.getPropertyName().equalsIgnoreCase(fieldname)) {
						update(target, fieldname);
					}
				}
			};

			if (target instanceof mxGraphComponent) {
				((mxGraphComponent) target)
						.addPropertyChangeListener(propertyChangeListener);
			} else if (target instanceof mxGraph) {
				((mxGraph) target)
						.addPropertyChangeListener(propertyChangeListener);
			}

			update(target, fieldname);
		}

		/**
		 * 
		 */
		public void update(Object target, String fieldname) {
			if (target != null && fieldname != null) {
				try {
					Method getter = target.getClass().getMethod(
							"is" + fieldname);

					if (getter != null) {
						Object current = getter.invoke(target);

						if (current instanceof Boolean) {
							setSelected(((Boolean) current).booleanValue());
						}
					}
				} catch (Exception e) {
					// ignore
				}
			}
		}

		/**
		 * 
		 */
		public void execute(Object target, String fieldname, boolean refresh) {
			if (target != null && fieldname != null) {
				try {
					Method getter = target.getClass().getMethod(
							"is" + fieldname);
					Method setter = target.getClass().getMethod(
							"set" + fieldname, new Class[] { boolean.class });

					Object current = getter.invoke(target);

					if (current instanceof Boolean) {
						boolean value = !((Boolean) current).booleanValue();
						setter.invoke(target, value);
						setSelected(value);
					}

					if (refresh) {
						mxGraph graph = null;

						if (target instanceof mxGraph) {
							graph = (mxGraph) target;
						} else if (target instanceof mxGraphComponent) {
							graph = ((mxGraphComponent) target).getGraph();
						}

						graph.refresh();
					}
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class HistoryAction extends AbstractAction {
		/**
		 * 
		 */
		protected boolean undo;

		/**
		 * 
		 */
		public HistoryAction(boolean undo) {
			this.undo = undo;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			BasicGraphEditor editor = getEditor(e);

			if (editor != null) {
				if (undo) {
					editor.getUndoManager().undo();
				} else {
					editor.getUndoManager().redo();
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class NewAction extends AbstractAction {

		private static final String TEMPLATE = "/com/mxgraph/examples/swing/resources/basic_relay.mxe";

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			BasicGraphEditor editor = getEditor(e);

			if (editor != null) {
				if (!editor.isModified()
						|| JOptionPane.showConfirmDialog(editor,
								mxResources.get("loseChanges")) == JOptionPane.YES_OPTION) {
					mxGraph graph = editor.getGraphComponent().getGraph();

					// Check modified flag and display save dialog
					// load the default template graph

					try {
						InputStream template = EditorActions.class
								.getResourceAsStream(TEMPLATE);
						if (template == null)
							throw new NullPointerException("template");
						Document document = mxXmlUtils.parseXml(mxUtils
								.readInputStream(template));

						mxCodec codec = new mxCodec(document);
						codec.decode(document.getDocumentElement(),
								graph.getModel());
					} catch (Exception ex) {
						mxCell root = new mxCell();
						root.insert(new mxCell());
						graph.getModel().setRoot(root);

						ex.printStackTrace();
						JOptionPane.showMessageDialog(
								editor.getGraphComponent(), ex.toString(),
								mxResources.get("error"),
								JOptionPane.ERROR_MESSAGE);
					}

					resetEditor(editor);
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class ImportAction extends AbstractAction {
		/**
		 * 
		 */
		protected String lastDir;

		/**
		 * Loads and registers the shape as a new shape in mxGraphics2DCanvas
		 * and adds a new entry to use that shape in the specified palette
		 * 
		 * @param palette
		 *            The palette to add the shape to.
		 * @param nodeXml
		 *            The raw XML of the shape
		 * @param path
		 *            The path to the directory the shape exists in
		 * @return the string name of the shape
		 */
		public static String addStencilShape(EditorPalette palette,
				String nodeXml, String path) {

			// Some editors place a 3 byte BOM at the start of files
			// Ensure the first char is a "<"
			int lessthanIndex = nodeXml.indexOf("<");
			nodeXml = nodeXml.substring(lessthanIndex);
			mxStencilShape newShape = new mxStencilShape(nodeXml);
			String name = newShape.getName();
			ImageIcon icon = null;

			if (path != null) {
				String iconPath = path + newShape.getIconPath();
				icon = new ImageIcon(iconPath);
			}

			// Registers the shape in the canvas shape registry
			mxGraphics2DCanvas.putShape(name, newShape);

			if (palette != null && icon != null) {
				palette.addTemplate(name, icon, "shape=" + name, 80, 80, "");
			}

			return name;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			BasicGraphEditor editor = getEditor(e);

			if (editor != null) {
				String wd = (lastDir != null) ? lastDir : System
						.getProperty("user.dir");

				JFileChooser fc = new JFileChooser(wd);

				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

				// Adds file filter for Dia shape import
				fc.addChoosableFileFilter(new DefaultFileFilter(".shape",
						"Dia Shape " + mxResources.get("file") + " (.shape)"));

				int rc = fc.showDialog(null, mxResources.get("importStencil"));

				if (rc == JFileChooser.APPROVE_OPTION) {
					lastDir = fc.getSelectedFile().getParent();

					try {
						if (fc.getSelectedFile().isDirectory()) {
							EditorPalette palette = editor.insertPalette(fc
									.getSelectedFile().getName());

							for (File f : fc.getSelectedFile().listFiles(
									new FilenameFilter() {
										public boolean accept(File dir,
												String name) {
											return name.toLowerCase().endsWith(
													".shape");
										}
									})) {
								String nodeXml = mxUtils.readFile(f
										.getAbsolutePath());
								addStencilShape(palette, nodeXml, f.getParent()
										+ File.separator);
							}

							JComponent scrollPane = (JComponent) palette
									.getParent().getParent();
							editor.getLibraryPane().setSelectedComponent(
									scrollPane);

							// FIXME: Need to update the size of the palette to
							// force a layout
							// update. Re/in/validate of palette or parent does
							// not work.
							// editor.getLibraryPane().revalidate();
						} else {
							String nodeXml = mxUtils.readFile(fc
									.getSelectedFile().getAbsolutePath());
							String name = addStencilShape(null, nodeXml, null);

							JOptionPane.showMessageDialog(editor, mxResources
									.get("stencilImported",
											new String[] { name }));
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class OpenAction extends AbstractAction {
		/**
		 * 
		 */
		protected String lastDir;

		/**
		 * @throws IOException
		 * 
		 */
		protected void openGD(BasicGraphEditor editor, File file, String gdText) {
			mxGraph graph = editor.getGraphComponent().getGraph();

			// Replaces file extension with .mxe
			String filename = file.getName();
			filename = filename.substring(0, filename.length() - 4) + ".mxe";

			if (new File(filename).exists()
					&& JOptionPane.showConfirmDialog(editor,
							mxResources.get("overwriteExistingFile")) != JOptionPane.YES_OPTION) {
				return;
			}

			((mxGraphModel) graph.getModel()).clear();
			mxGdCodec.decode(gdText, graph);
			editor.getGraphComponent().zoomAndCenter();
			editor.setCurrentFile(new File(lastDir + "/" + filename));
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			BasicGraphEditor editor = getEditor(e);

			if (editor != null) {
				if (!editor.isModified()
						|| JOptionPane.showConfirmDialog(editor,
								mxResources.get("loseChanges")) == JOptionPane.YES_OPTION) {
					mxGraph graph = editor.getGraphComponent().getGraph();

					if (graph != null) {
						String wd = (lastDir != null) ? lastDir : System
								.getProperty("user.dir");

						JFileChooser fc = new JFileChooser(wd);

						// Adds file filter for supported file format
						DefaultFileFilter defaultFilter = new DefaultFileFilter(
								".mxe", "mxGraph Editor "
										+ mxResources.get("file") + " (.mxe)");
						fc.addChoosableFileFilter(defaultFilter);
						fc.setFileFilter(defaultFilter);

						int rc = fc.showDialog(null,
								mxResources.get("openFile"));

						if (rc == JFileChooser.APPROVE_OPTION) {
							lastDir = fc.getSelectedFile().getParent();

							try {
								Document document = mxXmlUtils.parseXml(mxUtils
										.readFile(fc.getSelectedFile()
												.getAbsolutePath()));

								mxCodec codec = new mxCodec(document);
								codec.decode(document.getDocumentElement(),
										graph.getModel());

								resetEditor(editor);
								editor.setCurrentFile(fc.getSelectedFile());
							} catch (IOException ex) {
								ex.printStackTrace();
								JOptionPane.showMessageDialog(
										editor.getGraphComponent(),
										ex.toString(),
										mxResources.get("error"),
										JOptionPane.ERROR_MESSAGE);
							}
						}
					}
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class ToggleAction extends AbstractAction {
		/**
		 * 
		 */
		protected String key;

		/**
		 * 
		 */
		protected boolean defaultValue;

		/**
		 * 
		 * @param key
		 */
		public ToggleAction(String key) {
			this(key, false);
		}

		/**
		 * 
		 * @param key
		 */
		public ToggleAction(String key, boolean defaultValue) {
			this.key = key;
			this.defaultValue = defaultValue;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			mxGraph graph = mxGraphActions.getGraph(e);

			if (graph != null) {
				graph.toggleCellStyles(key, defaultValue);
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class SetLabelPositionAction extends AbstractAction {
		/**
		 * 
		 */
		protected String labelPosition, alignment;

		/**
		 * 
		 * @param key
		 */
		public SetLabelPositionAction(String labelPosition, String alignment) {
			this.labelPosition = labelPosition;
			this.alignment = alignment;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			mxGraph graph = mxGraphActions.getGraph(e);

			if (graph != null && !graph.isSelectionEmpty()) {
				graph.getModel().beginUpdate();
				try {
					// Checks the orientation of the alignment to use the
					// correct constants
					if (labelPosition.equals(mxConstants.ALIGN_LEFT)
							|| labelPosition.equals(mxConstants.ALIGN_CENTER)
							|| labelPosition.equals(mxConstants.ALIGN_RIGHT)) {
						graph.setCellStyles(mxConstants.STYLE_LABEL_POSITION,
								labelPosition);
						graph.setCellStyles(mxConstants.STYLE_ALIGN, alignment);
					} else {
						graph.setCellStyles(
								mxConstants.STYLE_VERTICAL_LABEL_POSITION,
								labelPosition);
						graph.setCellStyles(mxConstants.STYLE_VERTICAL_ALIGN,
								alignment);
					}
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class SetStyleAction extends AbstractAction {
		/**
		 * 
		 */
		protected String value;

		/**
		 * 
		 * @param key
		 */
		public SetStyleAction(String value) {
			this.value = value;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			mxGraph graph = mxGraphActions.getGraph(e);

			if (graph != null && !graph.isSelectionEmpty()) {
				graph.setCellStyle(value);
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class KeyValueAction extends AbstractAction {
		/**
		 * 
		 */
		protected String key, value;

		/**
		 * 
		 * @param key
		 */
		public KeyValueAction(String key) {
			this(key, null);
		}

		/**
		 * 
		 * @param key
		 */
		public KeyValueAction(String key, String value) {
			this.key = key;
			this.value = value;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			mxGraph graph = mxGraphActions.getGraph(e);

			if (graph != null && !graph.isSelectionEmpty()) {
				graph.setCellStyles(key, value);
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class PromptValueAction extends AbstractAction {
		/**
		 * 
		 */
		protected String key, message;

		/**
		 * 
		 * @param key
		 */
		public PromptValueAction(String key, String message) {
			this.key = key;
			this.message = message;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof Component) {
				mxGraph graph = mxGraphActions.getGraph(e);

				if (graph != null && !graph.isSelectionEmpty()) {
					String value = (String) JOptionPane.showInputDialog(
							(Component) e.getSource(),
							mxResources.get("value"), message,
							JOptionPane.PLAIN_MESSAGE, null, null, "");

					if (value != null) {
						if (value.equals(mxConstants.NONE)) {
							value = null;
						}

						graph.setCellStyles(key, value);
					}
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class AlignCellsAction extends AbstractAction {
		/**
		 * 
		 */
		protected String align;

		/**
		 * 
		 * @param key
		 */
		public AlignCellsAction(String align) {
			this.align = align;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			mxGraph graph = mxGraphActions.getGraph(e);

			if (graph != null && !graph.isSelectionEmpty()) {
				graph.alignCells(align);
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class AutosizeAction extends AbstractAction {
		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			mxGraph graph = mxGraphActions.getGraph(e);

			if (graph != null && !graph.isSelectionEmpty()) {
				Object[] cells = graph.getSelectionCells();
				mxIGraphModel model = graph.getModel();

				model.beginUpdate();
				try {
					for (int i = 0; i < cells.length; i++) {
						graph.updateCellSize(cells[i]);
					}
				} finally {
					model.endUpdate();
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class ColorAction extends AbstractAction {
		/**
		 * 
		 */
		protected String name, key;

		/**
		 * 
		 * @param key
		 */
		public ColorAction(String name, String key) {
			this.name = name;
			this.key = key;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof mxGraphComponent) {
				mxGraphComponent graphComponent = (mxGraphComponent) e
						.getSource();
				mxGraph graph = graphComponent.getGraph();

				if (!graph.isSelectionEmpty()) {
					Color newColor = JColorChooser.showDialog(graphComponent,
							name, null);

					if (newColor != null) {
						graph.setCellStyles(key, mxUtils.hexString(newColor));
					}
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class BackgroundImageAction extends AbstractAction {
		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof mxGraphComponent) {
				mxGraphComponent graphComponent = (mxGraphComponent) e
						.getSource();
				String value = (String) JOptionPane.showInputDialog(
						graphComponent, mxResources.get("backgroundImage"),
						"URL", JOptionPane.PLAIN_MESSAGE, null, null,
						"http://www.callatecs.com/images/background2.JPG");

				if (value != null) {
					if (value.length() == 0) {
						graphComponent.setBackgroundImage(null);
					} else {
						Image background = mxUtils.loadImage(value);
						// Incorrect URLs will result in no image.
						// TODO provide feedback that the URL is not correct
						if (background != null) {
							graphComponent.setBackgroundImage(new ImageIcon(
									background));
						}
					}

					// Forces a repaint of the outline
					graphComponent.getGraph().repaint();
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class BackgroundAction extends AbstractAction {
		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof mxGraphComponent) {
				mxGraphComponent graphComponent = (mxGraphComponent) e
						.getSource();
				Color newColor = JColorChooser.showDialog(graphComponent,
						mxResources.get("background"), null);

				if (newColor != null) {
					graphComponent.getViewport().setOpaque(true);
					graphComponent.getViewport().setBackground(newColor);
				}

				// Forces a repaint of the outline
				graphComponent.getGraph().repaint();
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class PageBackgroundAction extends AbstractAction {
		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof mxGraphComponent) {
				mxGraphComponent graphComponent = (mxGraphComponent) e
						.getSource();
				Color newColor = JColorChooser.showDialog(graphComponent,
						mxResources.get("pageBackground"), null);

				if (newColor != null) {
					graphComponent.setPageBackgroundColor(newColor);
				}

				// Forces a repaint of the component
				graphComponent.repaint();
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class StyleAction extends AbstractAction {
		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof mxGraphComponent) {
				mxGraphComponent graphComponent = (mxGraphComponent) e
						.getSource();
				mxGraph graph = graphComponent.getGraph();
				String initial = graph.getModel().getStyle(
						graph.getSelectionCell());
				String value = (String) JOptionPane.showInputDialog(
						graphComponent, mxResources.get("style"),
						mxResources.get("style"), JOptionPane.PLAIN_MESSAGE,
						null, null, initial);

				if (value != null) {
					graph.setCellStyle(value);
				}
			}
		}
	}
}
