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
package phasereditor.canvas.core.codegen;

import phasereditor.canvas.core.CanvasModel;
import phasereditor.inspect.core.InspectCore;
import phasereditor.inspect.core.jsdoc.PhaserJSDoc;

/**
 * @author arian
 *
 */
public class JSGroupCodeGenerator extends JSLikeGroupCodeGenerator {

	public JSGroupCodeGenerator(CanvasModel model) {
		super(model);
	}

	@Override
	protected void generateHeader() {
		String classname = _settings.getClassName();
		String baseclass = _settings.getBaseClass();
		
		PhaserJSDoc help = InspectCore.getPhaserHelp();
		
		line("/**");
		line(" * " + classname + ".");
		line(" * @param {Phaser.Game} aGame " + help.getMethodArgHelp("Phaser.Group", "game"));
		line(" * @param {Phaser.Group} aParent " + help.getMethodArgHelp("Phaser.Group", "parent"));
		line(" * @param {string} aName " + help.getMethodArgHelp("Phaser.Group", "name"));
		line(" * @param {boolean} aAddToStage " + help.getMethodArgHelp("Phaser.Group", "addToStage"));
		line(" * @param {boolean} aEnableBody " + help.getMethodArgHelp("Phaser.Group", "enableBody"));
		line(" * @param {number} aPhysicsBodyType " + help.getMethodArgHelp("Phaser.Group", "physicsBodyType"));
		line(" */");
		openIndent("function " + classname + "(aGame, aParent, aName, aAddToStage, aEnableBody, aPhysicsBodyType) {");
		
		line(baseclass + ".call(this, aGame, aParent, aName, aAddToStage, aEnableBody, aPhysicsBodyType);");
		line();

		section(PRE_INIT_CODE_BEGIN, PRE_INIT_CODE_END, getYouCanInsertCodeHere());

		line();
		line();
	}

	@Override
	protected void generateFooter() {
		String classname = _settings.getClassName();
		String baseclass = _settings.getBaseClass();
		
		section(POST_INIT_CODE_BEGIN, POST_INIT_CODE_END, getYouCanInsertCodeHere());

		line();
		closeIndent("}");
		line();
		
		line("/** @type " + baseclass + " */");
		line("var " + classname + "_proto = Object.create(" + baseclass + ".prototype);");
		line(classname + ".prototype = " + classname + "_proto;");
		line(classname + ".prototype.constructor = " + classname + ";");
		line();

		section(END_GENERATED_CODE, getYouCanInsertCodeHere());
	}
}
