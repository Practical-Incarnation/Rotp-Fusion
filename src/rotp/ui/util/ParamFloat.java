/*
 * Copyright 2015-2020 Ray Fowler
 *
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rotp.ui.util;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.text.DecimalFormat;

import rotp.model.game.IGameOptions;
import rotp.ui.game.BaseModPanel;

public class ParamFloat extends AbstractParam<Float> {
	
	private String guiFormat = "%";
	private String cfgFormat = "0.0##";
	private boolean loop = false;
	
	// ========== Constructors ==========
	//
	/**
	 * @param gui  The label header
	 * @param name The name
	 * @param defaultValue The default value
	 */
	public ParamFloat(String gui, String name, Float defaultValue) {
		super(gui, name, defaultValue, null, null, 1.0f, 1.0f, 1.0f, 1.0f);
	}
	/**
	 * @param gui  The label header
	 * @param name The name
	 * @param defaultValue The default value
	 * @param minValue The minimum value (null = none)
	 * @param maxValue The maximum value (null = none)
	 */
	public ParamFloat(String gui, String name, Float defaultValue
			, Float minValue, Float maxValue) {
		super(gui, name, defaultValue, minValue, maxValue, 1.0f, 1.0f, 1.0f, 1.0f);
	}
	/**
	 * @param gui  The label header
	 * @param name The name
	 * @param defaultValue The default value
	 * @param minValue The minimum value (null = none)
	 * @param maxValue The maximum value (null = none)
	 * @param baseInc  The base increment
	 * @param shiftInc The increment when Shift is hold
	 * @param ctrlInc  The increment when Ctrl is hold
	 */
	public ParamFloat(String gui, String name, Float defaultValue
			, Float minValue, Float maxValue
			, Float baseInc, Float shiftInc, Float ctrlInc) {
		super(gui, name, defaultValue, minValue, maxValue,
				baseInc, shiftInc, ctrlInc, shiftInc*ctrlInc/baseInc);
	}
	/**
	 * @param gui  The label header
	 * @param name The name
	 * @param defaultValue The default value
	 * @param minValue The minimum value (null = none)
	 * @param maxValue The maximum value (null = none)
	 * @param baseInc  The base increment
	 * @param shiftInc The increment when Shift is hold
	 * @param ctrlInc  The increment when Ctrl is hold
	 * @param cfgFormat String decimal formating for Remnant.cfg: default value = "%"
	 * @param guiFormat String decimal formating for GUI display: default value = "0.0##"
	 */
	public ParamFloat(String gui, String name, Float defaultValue
			, Float minValue, Float maxValue
			, Float baseInc, Float shiftInc, Float ctrlInc
			, String cfgFormat, String guiFormat) {
		super(gui, name, defaultValue, minValue, maxValue,
				baseInc, shiftInc, ctrlInc, shiftInc*ctrlInc/baseInc);
		this.cfgFormat = cfgFormat;
		this.guiFormat = guiFormat;
	}
	// ========== Overriders ==========
	//
	@Override public String guideDefaultValue()	{ return getString(defaultValue()); }
	@Override public String[] getModifiers()	{
		if (baseInc().equals(shiftInc()))
			return null;
		return new String[] {getString(baseInc()),
							getString(shiftInc()),
							getString(ctrlInc()),
							getString(shiftCtrlInc())};
	}
	@Override public String getCfgValue(Float value) {
		if (isCfgPercent()) {
			return String.format("%d", Math.round(value * 100f));
		}
		if (isCfgPerThousand()) {
			return new DecimalFormat("0.0")
						.format(Math.round(value  * 1000f) / 10f);
		}
		return new DecimalFormat(cfgFormat).format(value);
	}
	@Override public String guideValue() {
		if (isGuiPercent()) {
			return String.format("%d", Math.round(get() * 100f));
		}
		if (isGuiPerThousand()) {
			return new DecimalFormat("0.0")
						.format(Math.round(get() * 1000f) / 10f);
		}
		return new DecimalFormat(guiFormat).format(get());
	}
	@Override public void setFromCfgValue(String newValue) {
		if (isCfgPercent()) {
			Integer val = stringToInteger(newValue.replace("%", ""));
			if (val == null) 
				setFromCfg(stringToFloat(newValue));
			else
				setFromCfg(val/100f);
		} else
			setFromCfg(stringToFloat(newValue));
	}	
	@Override public void next() { next(baseInc()); }
	@Override public void prev() { next(-baseInc()); }
	@Override public void toggle(MouseEvent e, BaseModPanel frame)		{ next(getInc(e) * getDir(e)); }
	@Override public void toggle(MouseWheelEvent e) { next(getInc(e) * getDir(e)); }
	@Override protected Float getOptionValue(IGameOptions options) {
		return options.dynOpts().getFloat(getLangLabel(), creationValue());
	}
	@Override protected void setOptionValue(IGameOptions options, Float value) {
		options.dynOpts().setFloat(getLangLabel(), value);
	}
	// ========== Other Methods ==========
	//
	public void next(MouseEvent e) { next(Math.abs(getInc(e))); }
	public void prev(MouseEvent e) { next(-Math.abs(getInc(e))); }
	private String getString(float value) {
		if (isGuiPercent()) {
			return String.format("%d", Math.round(value * 100f)) + "%";
		}
		if (isGuiPerThousand()) {
			return new DecimalFormat("0.0")
						.format(Math.round(value * 1000f) / 10f) + "%";
		}
		return new DecimalFormat(guiFormat).format(value);
	}
	private void next(float i) {
		if (i == 0) {
			setFromDefault(false, true);
			return;
		}
		Float value = get() + i;
		if (maxValue() != null && value > maxValue()) {
			if (loop && minValue() != null) {
				set(minValue());
				return;
			} else {
				set(maxValue());
				return;
			}
		} else if (minValue() != null && value < minValue()) {
			if (loop && maxValue() != null) {
				set(maxValue());
				return;
			} else {
				set(minValue());
				return;
			}
		}
		set(value);
	}
	private boolean isGuiPercent() { return guiFormat.equals("%"); }
	private boolean isCfgPercent() { return cfgFormat.equals("%"); }
	private boolean isGuiPerThousand() { return guiFormat.equals("‰"); }
	private boolean isCfgPerThousand() { return cfgFormat.equals("‰"); }
}
