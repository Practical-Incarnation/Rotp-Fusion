package rotp.model.game;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import rotp.ui.util.IParam;
import rotp.ui.util.ParamFlagColor;
import rotp.ui.util.ParamInteger;
import rotp.ui.util.ParamList;
import rotp.ui.util.ParamList.IndexableMap;
import rotp.ui.util.ParamSubUI;
import rotp.ui.util.ParamTitle;

public interface IFlagOptions extends IBaseOptsTools {
	String AUTO_FLAG_GUI_ID		= "AUTO_FLAG";
	String FLAG_COLOR_NONE		= "FLAG_COLOR_NONE";
    String FLAG_COLOR_WHITE		= "FLAG_COLOR_WHITE";
    String FLAG_COLOR_RED		= "FLAG_COLOR_RED";
    String FLAG_COLOR_BLUE		= "FLAG_COLOR_BLUE";
    String FLAG_COLOR_GREEN		= "FLAG_COLOR_GREEN";
    String FLAG_COLOR_YELLOW	= "FLAG_COLOR_YELLOW";
    String FLAG_COLOR_AQUA		= "FLAG_COLOR_AQUA";
    String FLAG_COLOR_ORANGE	= "FLAG_COLOR_ORANGE";
    String FLAG_COLOR_LTBLUE	= "FLAG_COLOR_LTBLUE";
    String FLAG_COLOR_PURPLE	= "FLAG_COLOR_PURPLE";
    String FLAG_COLOR_PINK		= "FLAG_COLOR_PINK";

    String AUTO_FLAG_NOT	= "SETTINGS_MOD_AUTO_FLAG_NO_AUTOMATION";
	String AUTO_FLAG_TYPE	= "SETTINGS_MOD_AUTO_FLAG_TYPE";
	String AUTO_FLAG_ENV	= "SETTINGS_MOD_AUTO_FLAG_ENVIRONMENT";
	String AUTO_FLAG_ASSET	= "SETTINGS_MOD_AUTO_FLAG_RESOURCES";
	String AUTO_FLAG_RUINS	= "SETTINGS_MOD_AUTO_FLAG_RUINS";
	String AUTO_FLAG_TECH	= "SETTINGS_MOD_AUTO_FLAG_TECH";
	String AUTO_FLAG_CLEAR	= "SETTINGS_MOD_AUTO_FLAG_CLEAR";
    
	IndexableMap flagAssignationMap = flagAssignationMap();
	static IndexableMap flagAssignationMap() {
		IndexableMap map = new IndexableMap();
		List<String> flagAssignationList = Arrays.asList (
	    		AUTO_FLAG_NOT,
	    		AUTO_FLAG_TECH,
	    		AUTO_FLAG_RUINS,
	    		AUTO_FLAG_ASSET,
	    		AUTO_FLAG_ENV,
	    		AUTO_FLAG_TYPE,
	    		AUTO_FLAG_CLEAR
				);
		for (String element : flagAssignationList)
			map.put(element, element); // Temporary; needs to be further initialized
		return map;
	}

	// ========================================================================
	// BR: AUTO-FLAG PARAMETERS SUB UI
//	ParamInteger loopFlag			= new ParamInteger(MOD_UI, "LOOP_FLAG", 0, 0, 4, 1, 1, 1) {
//		{
//			loop(true);
//			specialValue(0, MOD_UI + "LOOP_NONE");
//			specialValue(1,	MOD_UI + "LOOP_TOP_LEFT");
//			specialValue(2,	MOD_UI + "LOOP_TOP_RIGHT");
//			specialValue(3,	MOD_UI + "LOOP_BOTTOM_RIGHT");
//			specialValue(4,	MOD_UI + "LOOP_BOTTOM_LEFT");
//		}
//	};
//	default int selectedLoopFlag()	{ return loopFlag.get(); } 

	ParamList autoFlagAssignation1	= new ParamList(MOD_UI, "AUTO_FLAG_ASSIGN_1",
			AUTO_FLAG_NOT, flagAssignationMap) {
		{ showFullGuide(true); }
	};
	default String selectedAutoFlagAssignation1() { return autoFlagAssignation1.get(); }

	ParamList autoFlagAssignation2	= new ParamList(MOD_UI, "AUTO_FLAG_ASSIGN_2",
			AUTO_FLAG_NOT, flagAssignationMap) {
		{ showFullGuide(true); }
	};
	default String selectedAutoFlagAssignation2() { return autoFlagAssignation2.get(); }

	ParamList autoFlagAssignation3	= new ParamList(MOD_UI, "AUTO_FLAG_ASSIGN_3",
			AUTO_FLAG_NOT, flagAssignationMap) {
		{ showFullGuide(true); }
	};
	default String selectedAutoFlagAssignation3() { return autoFlagAssignation3.get(); }

	ParamList autoFlagAssignation4	= new ParamList(MOD_UI, "AUTO_FLAG_ASSIGN_4",
			AUTO_FLAG_NOT, flagAssignationMap) {
		{ showFullGuide(true); }
	};
	default String selectedAutoFlagAssignation4() { return autoFlagAssignation4.get(); }

	ParamFlagColor flagTerranColor 		= new ParamFlagColor("AUTO_FLAG_TERRAN",		FLAG_COLOR_GREEN);
	ParamFlagColor flagJungleColor 		= new ParamFlagColor("AUTO_FLAG_JUNGLE",		FLAG_COLOR_GREEN);
	ParamFlagColor flagOceanColor 		= new ParamFlagColor("AUTO_FLAG_OCEAN",			FLAG_COLOR_AQUA);
	ParamFlagColor flagAridColor 		= new ParamFlagColor("AUTO_FLAG_ARID",			FLAG_COLOR_YELLOW);
	ParamFlagColor flagSteppeColor 		= new ParamFlagColor("AUTO_FLAG_STEPPE",		FLAG_COLOR_YELLOW);
	ParamFlagColor flagDesertColor 		= new ParamFlagColor("AUTO_FLAG_DESERT",		FLAG_COLOR_YELLOW);
	ParamFlagColor flagMinimalColor 	= new ParamFlagColor("AUTO_FLAG_MINIMAL",		FLAG_COLOR_YELLOW);
	ParamFlagColor flagBarrenColor 		= new ParamFlagColor("AUTO_FLAG_BARREN",		FLAG_COLOR_ORANGE);
	ParamFlagColor flagTundraColor 		= new ParamFlagColor("AUTO_FLAG_TUNDRA",		FLAG_COLOR_WHITE);
	ParamFlagColor flagDeadColor 		= new ParamFlagColor("AUTO_FLAG_DEAD",			FLAG_COLOR_WHITE);
	ParamFlagColor flagInfernoColor 	= new ParamFlagColor("AUTO_FLAG_INFERNO",		FLAG_COLOR_RED);
	ParamFlagColor flagToxicColor 		= new ParamFlagColor("AUTO_FLAG_TOXIC",			FLAG_COLOR_RED);
	ParamFlagColor flagRadiatedColor 	= new ParamFlagColor("AUTO_FLAG_RADIATED",		FLAG_COLOR_PURPLE);
	ParamFlagColor flagAsteroidColor 	= new ParamFlagColor("AUTO_FLAG_ASTEROID",		FLAG_COLOR_PINK);

	ParamFlagColor flagEnvGaiaColor 	= new ParamFlagColor("AUTO_FLAG_ENV_GAIA",		FLAG_COLOR_GREEN);
	ParamFlagColor flagEnvFertileColor 	= new ParamFlagColor("AUTO_FLAG_ENV_FERTILE",	FLAG_COLOR_AQUA);
	ParamFlagColor flagEnvNormalColor 	= new ParamFlagColor("AUTO_FLAG_ENV_NORMAL",	FLAG_COLOR_NONE);
	ParamFlagColor flagEnvHostileColor 	= new ParamFlagColor("AUTO_FLAG_ENV_HOSTILE",	FLAG_COLOR_ORANGE);
	ParamFlagColor flagEnvNoneColor 	= new ParamFlagColor("AUTO_FLAG_ENV_NONE",		FLAG_COLOR_NONE);

	ParamFlagColor flagUltraPoorColor 	= new ParamFlagColor("AUTO_FLAG_ULTRA_POOR",	FLAG_COLOR_YELLOW);
	ParamFlagColor flagPoorColor 		= new ParamFlagColor("AUTO_FLAG_POOR",			FLAG_COLOR_ORANGE);
	ParamFlagColor flagAssetNormalColor = new ParamFlagColor("AUTO_FLAG_NORMAL",		FLAG_COLOR_NONE);
	ParamFlagColor flagRichColor 		= new ParamFlagColor("AUTO_FLAG_RICH",			FLAG_COLOR_PINK);
	ParamFlagColor flagUltraRichColor 	= new ParamFlagColor("AUTO_FLAG_ULTRA_RICH",	FLAG_COLOR_RED);
	ParamFlagColor flagAntaranColor 	= new ParamFlagColor("AUTO_FLAG_RUINS_ANTARAN",	FLAG_COLOR_NONE);
	ParamFlagColor flagOrionColor 		= new ParamFlagColor("AUTO_FLAG_RUINS_ORION",	FLAG_COLOR_NONE);
	ParamFlagColor flagNoneColor 		= new ParamFlagColor("AUTO_FLAG_NONE",			FLAG_COLOR_NONE);

	ParamFlagColor flagTechGaiaColor 	= new ParamFlagColor("AUTO_FLAG_TECH_GAIA",		FLAG_COLOR_GREEN);
	ParamFlagColor flagTechFertileColor = new ParamFlagColor("AUTO_FLAG_TECH_FERTILE",	FLAG_COLOR_GREEN);
	ParamFlagColor flagTechGoodColor 	= new ParamFlagColor("AUTO_FLAG_TECH_GOOD",		FLAG_COLOR_BLUE);
	ParamFlagColor flagTechStandardColor= new ParamFlagColor("AUTO_FLAG_TECH_STANDARD",	FLAG_COLOR_YELLOW);
	ParamFlagColor flagTechBarrenColor 	= new ParamFlagColor("AUTO_FLAG_TECH_BARREN",	FLAG_COLOR_ORANGE);
	ParamFlagColor flagTechDeadColor 	= new ParamFlagColor("AUTO_FLAG_TECH_DEAD",		FLAG_COLOR_WHITE);
	ParamFlagColor flagTechToxicColor 	= new ParamFlagColor("AUTO_FLAG_TECH_TOXIC",	FLAG_COLOR_RED);
	ParamFlagColor flagTechRadiatedColor= new ParamFlagColor("AUTO_FLAG_TECH_RADIATED",	FLAG_COLOR_PURPLE);
	ParamFlagColor flagTechNoneColor 	= new ParamFlagColor("AUTO_FLAG_TECH_NONE",		FLAG_COLOR_PINK);
	
	ParamFlagColor flagRuinsOrionColor 	= new ParamFlagColor("AUTO_FLAG_VESTIGES_ORION",FLAG_COLOR_BLUE);
	ParamFlagColor flagRuinsAntaranColor= new ParamFlagColor("AUTO_FLAG_VESTIGES_ANTARAN", FLAG_COLOR_LTBLUE);
	ParamFlagColor flagRuinsNoneColor 	= new ParamFlagColor("AUTO_FLAG_VESTIGES_NONE",	FLAG_COLOR_NONE);

	ParamInteger flagColorCount = new ParamInteger(MOD_UI, "FLAG_COLOR_COUNT", 1, 1, 4);
	default int	 selectedFlagColorCount() { return flagColorCount.get(); }

	// ==================== GUI List Declarations ====================
	//
	ParamSubUI autoFlagOptionsUI = autoFlagOptionsUI();

	static LinkedList<LinkedList<IParam>> autoFlagOptionsMap() {
		LinkedList<LinkedList<IParam>> map = new LinkedList<>();
		map.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("AUTO_FLAG_ID_SELECTION"),
				autoFlagAssignation1, autoFlagAssignation2,
				autoFlagAssignation3, autoFlagAssignation4,

//				headerSpacer,
//				loopFlag,
				
				headerSpacer,
				new ParamTitle("AUTO_FLAG_COLONY_TECH"),
				flagTechGaiaColor, flagTechFertileColor, flagTechGoodColor,
				flagTechStandardColor, flagTechBarrenColor, flagTechDeadColor,
				flagTechToxicColor, flagTechRadiatedColor, flagTechNoneColor
				)));
		map.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("AUTO_FLAG_VESTIGES"),
				flagRuinsOrionColor, flagRuinsAntaranColor, flagRuinsNoneColor,

				headerSpacer,
				new ParamTitle("AUTO_FLAG_RESOURCES"),
				flagOrionColor, flagAntaranColor,
				flagUltraRichColor, flagRichColor, flagAssetNormalColor,
				flagPoorColor, flagUltraPoorColor, flagNoneColor,
				
				headerSpacer,
				new ParamTitle("AUTO_FLAG_ENVIRONMENT"),
				flagEnvGaiaColor, flagEnvFertileColor,
				flagEnvNormalColor,	flagEnvHostileColor, flagEnvNoneColor
				)));
		map.add(new LinkedList<>(Arrays.asList(
				new ParamTitle("AUTO_FLAG_TYPE"),
				flagTerranColor, flagJungleColor, flagOceanColor,
				flagAridColor, flagSteppeColor, flagDesertColor, flagMinimalColor,
				flagBarrenColor, flagTundraColor, flagDeadColor,
				flagInfernoColor, flagToxicColor, flagRadiatedColor,
				flagAsteroidColor
				)));
		return map;
	}
	static ParamSubUI autoFlagOptionsUI() {
		return new ParamSubUI( MOD_UI, "AUTO_FLAG_UI", autoFlagOptionsMap(),
				"AUTO_FLAG_TITLE", AUTO_FLAG_GUI_ID);
	}
	static LinkedList<IParam> autoFlagOptions() {
		return IBaseOptsTools.getSingleList(autoFlagOptionsMap());
	}
}
