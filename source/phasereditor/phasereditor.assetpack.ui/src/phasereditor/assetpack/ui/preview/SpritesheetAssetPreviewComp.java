// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.assetpack.ui.preview;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.ui.widgets.SpritesheetPreviewCanvas;
import phasereditor.ui.Animation;

public class SpritesheetAssetPreviewComp extends Composite {
	private DataBindingContext m_bindingContext;

	SpritesheetPreviewCanvas _canvas;
	private Button _gridButton;

	public static class FpsValidator implements IValidator {

		@Override
		public IStatus validate(Object value) {
			Integer fps = (Integer) value;
			if (fps.intValue() < 1) {
				return ValidationStatus.error("Wrong FPS value");
			} else if (fps.intValue() > 120) {
				return ValidationStatus.error("Is not too fast?");
			}
			return Status.OK_STATUS;
		}

	}

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public SpritesheetAssetPreviewComp(Composite parent, int style) {
		super(parent, style);

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (_animation != null) {
					_animation.stop();
				}
			}
		});
		setLayout(new GridLayout(1, false));

		_canvas = new SpritesheetPreviewCanvas(this, SWT.NONE);
		_canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(3, false);
		gl_composite.marginWidth = 0;
		gl_composite.marginHeight = 0;
		composite.setLayout(gl_composite);

		_gridButton = new Button(composite, SWT.NONE);
		_gridButton.setText("Play");
		GridData gd_allButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_allButton.widthHint = 80;
		_gridButton.setLayoutData(gd_allButton);
		_gridButton.setToolTipText("If pressedm it show the whole image and the frames grid.");

		_label = new Label(composite, SWT.NONE);
		_label.setText("fps");
		_label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

		_comboViewer = new ComboViewer(composite, SWT.READ_ONLY);
		Combo combo = _comboViewer.getCombo();
		GridData gd_combo = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_combo.widthHint = 50;
		combo.setLayoutData(gd_combo);
		_comboViewer.setLabelProvider(new LabelProvider());
		_comboViewer.setContentProvider(new ArrayContentProvider());
		_gridButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				gridButtonPressed();
			}
		});

		afterCreateWidgets();

		m_bindingContext = initDataBindings();

		decorateControls();

	}

	private void afterCreateWidgets() {
		Object[] fpsList = new Object[6 + 2];
		for (int i = 0; i < 6; i++) {
			fpsList[i + 2] = Integer.valueOf((i + 1) * 10);
		}
		fpsList[0] = Integer.valueOf(1);
		fpsList[1] = Integer.valueOf(5);

		_comboViewer.setInput(fpsList);
	}

	private Label _label;

	protected Animation _animation;

	private SpritesheetAssetModel _model;

	private void animate() {
		_animation = new Animation(30) {

			@Override
			public void action() {
				try {
					int n = _canvas.getFrameCount();
					if (n > 0) {
						int f = _canvas.getFrame();
						f = (f + 1) % n;
						_canvas.setFrame(f);
						_canvas.redraw();
					}
				} catch (SWTException e) {
					// invalid access.
					stop();
				}
			}
		};
		_animation.start();
	}

	protected void gridButtonPressed() {
		boolean single = _canvas.isSingleFrame();
		single = !single;
		_gridButton.setText(single ? "Stop" : "Play");
		_animation.pause(!single);
		_canvas.setSingleFrame(single);
		_canvas.redraw();
	}

	private void decorateControls() {
		IObservableList bindings = m_bindingContext.getBindings();
		for (int i = 0; i < bindings.size(); i++) {
			Binding b = (Binding) bindings.get(i);
			ControlDecorationSupport.create(b, SWT.TOP | SWT.LEFT);
		}
	}

	public void setModel(SpritesheetAssetModel model) {
		_model = model;

		_canvas.setSpritesheet(model);
		IFile file = model.getUrlFile();
		_canvas.setImageFile(file);
		_canvas.setFrame(0);
		_canvas.setSingleFrame(false);
		_gridButton.setText("Play");
		animate();
		_animation.pause(true);
		setFps(5);
	}

	public SpritesheetAssetModel getModel() {
		return _model;
	}

	public void stopAnimation() {
		if (_animation != null) {
			_animation.pause(true);
		}
	}

	private SpritesheetAssetPreviewComp _self = this;
	private int _fps = 30;

	public int getFps() {
		return _fps;
	}

	public void setFps(int fps) {
		_fps = fps;
		firePropertyChange("fps");
		if (_animation != null) {
			_animation.setFps(fps);
		}
	}

	private transient final PropertyChangeSupport support = new PropertyChangeSupport(this);
	private ComboViewer _comboViewer;

	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		support.removePropertyChangeListener(l);
	}

	public void addPropertyChangeListener(String property, PropertyChangeListener l) {
		support.addPropertyChangeListener(property, l);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener l) {
		support.removePropertyChangeListener(property, l);
	}

	public void firePropertyChange(String property) {
		support.firePropertyChange(property, true, false);
	}

	protected DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue observeSingleSelection_comboViewer = ViewerProperties.singleSelection().observe(_comboViewer);
		IObservableValue fps_selfObserveValue = BeanProperties.value("fps").observe(_self);
		bindingContext.bindValue(observeSingleSelection_comboViewer, fps_selfObserveValue, null, null);
		//
		return bindingContext;
	}
}
