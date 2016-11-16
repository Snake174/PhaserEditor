// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.canvas.ui.editors;

import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.beans.PropertyChangeEvent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import javafx.geometry.Point2D;
import phasereditor.canvas.core.AssetTable;
import phasereditor.canvas.core.CanvasEditorModel;
import phasereditor.canvas.core.JSCodeGenerator;
import phasereditor.canvas.core.SceneSettings;
import phasereditor.canvas.core.WorldModel;
import phasereditor.canvas.ui.editors.behaviors.ZoomBehavior;
import phasereditor.canvas.ui.editors.grid.PGrid;
import phasereditor.canvas.ui.editors.operations.ChangeSettingsOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.palette.PaletteComp;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.PatternFilter2;

/**
 * @author arian
 *
 */
public class CanvasEditor extends EditorPart
		implements  IPersistableEditor, IEditorSharedImages {

	private static final String PALETTE_CONTEXT_ID = "phasereditor.canvas.ui.palettecontext";
	public final static String ID = "phasereditor.canvas.ui.editors.canvas";
	public final static String NODES_CONTEXT_ID = "phasereditor.canvas.ui.nodescontext";
	protected static final String SCENE_CONTEXT_ID = "phasereditor.canvas.ui.scenecontext";
	protected static final String EDITOR_CONTEXT_ID = "phasereditor.canvas.ui.any";

	public static final IUndoContext UNDO_CONTEXT = new IUndoContext() {

		@Override
		public boolean matches(IUndoContext context) {
			return context == this;
		}

		@Override
		public String getLabel() {
			return "CANVAS_CONTEXT";
		}
	};

	private ObjectCanvas _canvas;
	private CanvasEditorModel _model;
	private PGrid _grid;
	private SashForm _leftSashForm;
	private FilteredTree _outlineTree;

	private SashForm _mainSashForm;
	private PaletteComp _paletteComp;
	protected IContextActivation _paletteContext;

	public CanvasEditor() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == CanvasEditor.class) {
			return this;
		}
		return super.getAdapter(adapter);
	}

	@SuppressWarnings("unused")
	public void modelDirtyChanged(PropertyChangeEvent event) {
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

		boolean hasErrors = _model.getWorld().hasErrors();

		if (hasErrors) {
			if (!MessageDialog.openQuestion(getSite().getShell(), "Canvas",
					"The scene has errors, do you want to save?")) {
				return;
			}
		}

		try {
			IFileEditorInput input = (IFileEditorInput) getEditorInput();
			JSONObject data = new JSONObject();
			_model.getWorld().setAssetTable(new AssetTable(_model.getWorld()));
			_model.write(data);
			input.getFile().setContents(new ByteArrayInputStream(data.toString(2).getBytes()), true, false, monitor);

			if (getCanvas().getSettingsModel().isGenerateOnSave()) {
				generateCode();
			}

			_model.getWorld().setDirty(false);
			firePropertyChange(PROP_DIRTY);
		} catch (JSONException | CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		IFileEditorInput fileInput = (IFileEditorInput) input;
		IFile file = fileInput.getFile();
		try (InputStream contents = file.getContents();) {
			JSONObject data = new JSONObject(new JSONTokener(contents));
			_model = new CanvasEditorModel(file);
			try {
				_model.read(data);
			} catch (Exception e) {
				e.printStackTrace();
				_model = new CanvasEditorModel(file);
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						Shell shell = Display.getDefault().getActiveShell();
						MessageDialog.openError(shell, "Error", "The scene data cannot ve loaded.\n" + e.getMessage());
					}
				});
			}
			_model.getWorld().setFile(file);
			_model.getWorld().addPropertyChangeListener(WorldModel.PROP_STRUCTURE, arg -> {
				firePropertyChange(PROP_DIRTY);
			});
			swtRun(this::updateTitle);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doSaveAs() {
		// nothing
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return _model.getWorld().isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@SuppressWarnings("unused")
	@Override
	public void createPartControl(Composite parent1) {
		GridLayout gl_parent1 = new GridLayout(1, false);
		gl_parent1.marginWidth = 2;
		gl_parent1.marginHeight = 2;
		parent1.setLayout(gl_parent1);

		_mainSashForm = new SashForm(parent1, SWT.NONE);
		_mainSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_leftSashForm = new SashForm(_mainSashForm, SWT.VERTICAL);

		_outlineTree = new FilteredTree(_leftSashForm, SWT.BORDER | SWT.MULTI, new PatternFilter2(), true);
		Tree tree = _outlineTree.getViewer().getTree();
		_outlineTree.getViewer().setLabelProvider(new OutlineLabelProvider());
		_outlineTree.getViewer().setContentProvider(new OutlineContentProvider());

		_grid = new PGrid(_leftSashForm, SWT.NONE);
		_leftSashForm.setWeights(new int[] { 1, 1 });

		_centerComposite = new Composite(_mainSashForm, SWT.NONE);
		GridLayout gl_composite = new GridLayout(2, false);
		gl_composite.marginHeight = 0;
		gl_composite.marginWidth = 0;
		gl_composite.horizontalSpacing = 0;
		gl_composite.verticalSpacing = 2;
		_centerComposite.setLayout(gl_composite);
		_canvas = new ObjectCanvas(_centerComposite, SWT.BORDER);
		_canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		_paletteComp = new PaletteComp(_centerComposite, SWT.NONE);
		GridData gd_paletteComp = new GridData(SWT.RIGHT, SWT.FILL, false, true, 1, 1);
		gd_paletteComp.widthHint = 80;
		_paletteComp.setLayoutData(gd_paletteComp);

		_mainSashForm.setWeights(new int[] { 1, 4 });

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		// name

		// ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

		initPalette();

		initCanvas();

		initMenus();

		initOutline();

		restoreState();

		initContexts();
	}

	private void initPalette() {
		_paletteComp.setProject(getEditorInputFile().getProject());
	}

	private void initContexts() {
		getContextService().activateContext(EDITOR_CONTEXT_ID);

		_canvas.addFocusListener(new FocusListener() {

			private IContextActivation _sceneContext;

			@Override
			public void focusLost(FocusEvent e) {
				getContextService().deactivateContext(_sceneContext);
			}

			@Override
			public void focusGained(FocusEvent e) {
				_sceneContext = getContextService().activateContext(SCENE_CONTEXT_ID);
			}
		});

		FocusListener nodesContextHandler = new FocusListener() {

			private IContextActivation _nodesContext;

			@Override
			public void focusLost(FocusEvent e) {
				getContextService().deactivateContext(_nodesContext);
			}

			@Override
			public void focusGained(FocusEvent e) {
				_nodesContext = getContextService().activateContext(NODES_CONTEXT_ID);
			}
		};
		_canvas.addFocusListener(nodesContextHandler);
		_outlineTree.getViewer().getControl().addFocusListener(nodesContextHandler);

		// palette

		_paletteComp.getViewer().getTable().addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				getContextService().deactivateContext(_paletteContext);
			}

			@Override
			public void focusGained(FocusEvent e) {
				_paletteContext = getContextService().activateContext(PALETTE_CONTEXT_ID);
			}
		});
	}

	private void initMenus() {
		createMenuManager();
	}

	private void initCanvas() {
		_canvas.init(this, _model, _grid, _outlineTree.getViewer(), _paletteComp);
		getEditorSite().setSelectionProvider(_canvas.getSelectionBehavior());
	}

	private void restoreState() {
		if (_state != null) {
			_canvas.getZoomBehavior().setScale(_state.zoomScale);
			_canvas.getZoomBehavior().setTranslate(_state.translate);
			_mainSashForm.setWeights(_state.sashWights);

			if (_state.paletteData != null) {
				_paletteComp.updateFromJSON(new JSONObject(_state.paletteData));
//				_showPaletteAction.setChecked(_paletteComp.isPaletteVisible());
			}

			if (!_state.showSidePane) {
//				_showSidePaneAction.run();
			}
//			_showSidePaneAction.setChecked(_state.showSidePane);
		}
	}

	private void initOutline() {
		TreeViewer viewer = _outlineTree.getViewer();

		viewer.setInput(_canvas);

		viewer.expandAll();

		int operations = DND.DROP_DEFAULT | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		viewer.addDragSupport(operations, transfers, new DragSourceListener() {

			private ISelection _data;

			@Override
			public void dragStart(DragSourceEvent event) {
				_data = viewer.getSelection();
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				event.data = _data;
				LocalSelectionTransfer.getTransfer().setSelection(_data);
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				// finished
			}
		});

		viewer.addDropSupport(operations, transfers, new OutlineDropAdapter(this));

	}

	public IContextService getContextService() {
		IContextService service = getSite().getService(IContextService.class);
		return service;
	}

	private void createMenuManager() {
		MenuManager manager = createContextMenu();
		getEditorSite().registerContextMenu(manager, _canvas.getSelectionBehavior(), false);

		_canvas.setMenu(manager.createContextMenu(_canvas));
		_outlineTree.getViewer().getControl().setMenu(manager.createContextMenu(_outlineTree));
	}

	public IFile getFileToGenerate() {
		WorldModel model = getCanvas().getWorldModel();
		String fname = model.getClassName() + ".js";
		return getEditorInputFile().getParent().getFile(new Path(fname));
	}

	public void generateCode() {

		if (_model.getWorld().hasErrors()) {
			MessageDialog.openWarning(getSite().getShell(), "Canvas",
					"The scene has errors, the JavaScript code generation is aborted.");
			return;
		}

		JSCodeGenerator generator = new JSCodeGenerator();
		try {
			WorldModel model = getCanvas().getWorldModel();
			IFile file = getFileToGenerate();
			String replace = null;

			if (file.exists()) {
				byte[] bytes = Files.readAllBytes(file.getLocation().makeAbsolute().toFile().toPath());
				replace = new String(bytes);
			}

			String content = generator.generate(model, replace);

			ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes());
			if (file.exists()) {
				file.setContents(stream, IResource.NONE, null);
			} else {
				file.create(stream, false, null);
			}
			file.refreshLocal(1, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static MenuManager createContextMenu() {
		MenuManager menuManager = new MenuManager();
		menuManager.add(new GroupMarker("object"));
		return menuManager;
	}

	@Override
	public void setFocus() {
		_canvas.setFocus();
	}

	public PaletteComp getPalette() {
		return _paletteComp;
	}

	public ObjectCanvas getCanvas() {
		return _canvas;
	}

	public TreeViewer getOutline() {
		return _outlineTree.getViewer();
	}

	public PGrid getPropertyGrid() {
		return _grid;
	}

	@Override
	public void dispose() {
		if (_canvas != null) {
			_canvas.getUpdateBehavior().dispose();
		}
		super.dispose();
	}

	public IFile getEditorInputFile() {
		return ((IFileEditorInput) getEditorInput()).getFile();
	}

	public CanvasEditorModel getModel() {
		return _model;
	}

	protected void updateTitle() {
		setPartName(getEditorInputFile().getName());
		firePropertyChange(PROP_TITLE);
		}

	@Override
	public void saveState(IMemento memento) {
		ZoomBehavior zoom = _canvas.getZoomBehavior();
		memento.putFloat("canvas.zoom.scale", (float) zoom.getScale());
		memento.putFloat("canvas.translate.x", (float) zoom.getTranslate().getX());
		memento.putFloat("canvas.translate.y", (float) zoom.getTranslate().getY());
		memento.putString("canvas.palette.data", _paletteComp.toJSON().toString());
		int[] weights = _mainSashForm.getWeights();
		memento.putString("canvas.sash.weights", Arrays.toString(weights));
		memento.putBoolean("canvas.sidepane.show", _mainSashForm.getMaximizedControl() == null);
	}

	static class State {
		double zoomScale = 0;
		Point2D translate = new Point2D(0, 0);
		int[] sashWights = new int[] { 1, 5 };
		String paletteData;
		boolean showSidePane;
	}

	private State _state;
	private Composite _centerComposite;

	@Override
	public void restoreState(IMemento memento) {
		try {
			_state = new State();

			{
				Float scale = memento.getFloat("canvas.zoom.scale");
				_state.zoomScale = scale == null ? 0 : scale.doubleValue();
			}

			{
				Float x = memento.getFloat("canvas.translate.x");
				Float y = memento.getFloat("canvas.translate.y");
				_state.translate = new Point2D(x == null ? 50 : x.doubleValue(), y == null ? 50 : y.doubleValue());
			}

			{
				String value = memento.getString("canvas.sash.weights");
				if (value != null) {
					value = value.replace("[", "").replace("]", "");
					String[] array = value.split(",");
					_state.sashWights = new int[array.length];
					for (int i = 0; i < array.length; i++) {
						_state.sashWights[i] = Integer.parseInt(array[i].trim());
					}
				}
			}

			{
				_state.paletteData = memento.getString("canvas.palette.data");
			}

			{
				Boolean b = memento.getBoolean("canvas.sidepane.show");
				_state.showSidePane = b != null && b.booleanValue();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void openDialogSettings() {
		CanvasSettingsDialog dlg = new CanvasSettingsDialog(getSite().getShell());
		JSONObject data = new JSONObject();
		_model.getSettings().write(data);
		SceneSettings settings = new SceneSettings(data);
		dlg.setModel(settings);
		if (dlg.open() == Window.OK) {
			settings.write(data);
			getCanvas().getUpdateBehavior()
					.executeOperations(new CompositeOperation(new ChangeSettingsOperation(data)));
		}
	}

	public void handleFileRename(IFile newFile) {
		super.setInput(new FileEditorInput(newFile));
		_model.getWorld().setFile(newFile);
		updateTitle();
	}

	public void toggleSidePanel() {
		if (_mainSashForm.getMaximizedControl() == _centerComposite) {
			_mainSashForm.setMaximizedControl(null);
		} else {
			_mainSashForm.setMaximizedControl(_centerComposite);
		}
	}

	public void togglePalette() {
		boolean visible = getPalette().isPaletteVisible();
		getPalette().setPaletteVisble(!visible);
	}
}
