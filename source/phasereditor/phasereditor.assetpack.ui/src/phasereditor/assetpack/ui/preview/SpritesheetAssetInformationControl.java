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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel.FrameModel;
import phasereditor.ui.info.BaseInformationControl;

public class SpritesheetAssetInformationControl extends BaseInformationControl {

	public SpritesheetAssetInformationControl(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void updateContent(Control control, Object model) {
		QuickSpritesheetAssetPreviewComp comp = (QuickSpritesheetAssetPreviewComp) control;
		SpritesheetAssetModel asset;
		if (model instanceof SpritesheetAssetModel) {
			asset = (SpritesheetAssetModel) model;
			comp.setModel(asset);
		} else {
			FrameModel frame = (SpritesheetAssetModel.FrameModel) model;
			asset = frame.getAsset();
			comp.setModel(asset);
			comp.getCanvas().setSpritesheet(asset);
			comp.getCanvas().setFrame(frame.getIndex());
			comp.getCanvas().setSingleFrame(true);
		}
	}

	@Override
	protected Control createContent2(Composite parentComp) {
		QuickSpritesheetAssetPreviewComp comp = new QuickSpritesheetAssetPreviewComp(parentComp, SWT.NONE);
		return comp;
	}
}
