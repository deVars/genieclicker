package scripts;
// Don't know how to run this in executor
import com.adobe.genie.executor.Genie;
import com.adobe.genie.executor.GenieLocatorInfo;
import com.adobe.genie.executor.LogConfig;
import com.adobe.genie.executor.components.ExecuteActions;
import com.adobe.genie.executor.components.GenieComponent;
import com.adobe.genie.executor.components.GenieDisplayObject;
import com.adobe.genie.executor.components.GenieMovieClip;
import com.adobe.genie.executor.enums.GenieLogEnums;
import com.adobe.genie.executor.events.BaseEvent;
import com.adobe.genie.executor.exceptions.ConnectionFailedException;
import com.adobe.genie.executor.exceptions.StepFailedException;
import com.adobe.genie.executor.exceptions.StepTimedOutException;
import com.adobe.genie.genieCom.SWFApp;
import com.adobe.genie.genieUIRobot.UIFunctions;
import com.adobe.genie.utils.Utils;

import java.util.*;

public class ClickerMain {
	// Static Data and UI qualifiers
	final String UI_QUALIFIER = "FP^mainPanel:::SE^container:::CH^HeroPanelDisplay::PX^4::PTR^0::IX^1::ITR^0";
	final String HERO_PANEL_ID = "FP^HeroPanelDisplay:::SE^Sprite:::CH^HeroEntryDisplay::PX^0::PTR^0::IX^5::ITR^0";
	final String PROGRESSION_BUTTON_REDSTRIKE_ID = "PROGRESSIONBUTTONREDSTRIKEID";
	final String PROGRESSION_BUTTON_ID = "PROGRESSIONBUTTONID";
	final String BUY_AVAILABLE_UPGRADES_ID = "BUYAVAILABLEUPGRADES";
	final String HERO_ENTRY_QUALIFIER = "swc.mainui::HeroEntryDisplay";
	final String CANDY_QUALIFIER = "swc.Effects::Goodies";
	final String HERO_LEVEL_PREFIX = "heroLevel";
	final String HERO_LEVEL_BUTTON_PREFIX = "heroLevelButton";
	final String BUY_AVAILABLE_UPGRADES = "Buy Available Upgrades";
	final String TERRA = "Terra";
	final String PHTHALO = "Phthalo ";
	final String AMENHOTEP = "Amenhotep";
	final String dataJSONString = "[" +
			"{\"name\": \"Cid, the Helpful Adventurer\", \"maxUpgrades\":\"7\"}," +
			"]";
	final long TURN_TIMER_MAX = 420; //360 (30 minute) + 120 (10 minute) of runs
	final long SLEEP_5S = 5000;
	final long SLEEP_5SS = 500;

	private enum GameState {
		GAME_STATE_START, GAME_STATE_BOOSTING_GILDED, GAME_STATE_PREP_ASCEND,
		GAME_STATE_ASCEND
	}

	// private UIKeyBoard uiKeyBoard = new UIKeyBoard();
	private Map<String, String> idPool = new HashMap<String, String>();

	private static GenieComponent topGC;
	UIFunctions uiFunctions = new UIFunctions(false);

	public void main() {
		LogConfig logConfig = new LogConfig();
		GameState gameState = GameState.GAME_STATE_START;
		Boolean loopForever = true;
		int turnTimer = 0;

		logConfig.setNoLogging(false); //for now
		while (loopForever) {
			gameState = clickerLoop(logConfig, gameState, turnTimer);
			turnTimer = postLoopActions(gameState, turnTimer);
		}

	}

	private GameState clickerLoop(LogConfig logConfig, GameState gameState, int turnTimer) {
		Genie genie = null;
		try {
			genie = Genie.init(logConfig);
			if (genie != null) {
				final SWFApp app = genie.connectToApp("two");
				_t("connected to app");
				_t("starting a state check");
				if (gameState != GameState.GAME_STATE_PREP_ASCEND &&
						gameState != GameState.GAME_STATE_ASCEND) {
					gameState = checkGameState(app);
					if (turnTimer > TURN_TIMER_MAX) {
						gameState = GameState.GAME_STATE_PREP_ASCEND;
					}
				}
				gameState = dvClicker(app, turnTimer);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (genie != null) {
				genie.stop();
			}
		}

		return gameState;
	}

	private int postLoopActions(GameState gameState, int turnTimer) {
		try {
			switch (gameState) {
				case GAME_STATE_START:
					Thread.sleep(2000);
					break;
				case GAME_STATE_BOOSTING_GILDED:
					Thread.sleep(120000);
					break;
				case GAME_STATE_ASCEND:
					turnTimer = 0;
					break;
				default:
					Thread.sleep(30000);
					break;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return turnTimer++;
	}

	private GameState dvClicker(SWFApp app, int turnTimer)
			throws ConnectionFailedException, StepTimedOutException, InterruptedException, StepFailedException {
		topGC = new GenieComponent("", app);
		GameState gameState = GameState.GAME_STATE_START;

		switch(gameState) {
			case GAME_STATE_START:
				_t("state is game start");
				doGameStart(app);
				break;
			case GAME_STATE_BOOSTING_GILDED:
				_t("state is game main run");
				doGameRun(app);
				enableProgression(app);
				break;
			case GAME_STATE_PREP_ASCEND:
				_t("state is prep ascend");
				doPrepAscend(app);
				gameState = GameState.GAME_STATE_ASCEND;
				break;
			case GAME_STATE_ASCEND:
				_t("state is ascension");
				doAscend(app);
				_t("resetting state to game start");
				idPool = new HashMap<String, String>();
				gameState = GameState.GAME_STATE_START;
				System.gc();
				break;
		}

		System.gc();
		return gameState;
	}

	private void doGameStart(SWFApp app)
			throws StepTimedOutException, StepFailedException, InterruptedException{
		GenieComponent terra = getHero(app, TERRA);
		if (terra != null) {
			if (getHeroLevel(app, terra) > 100) {
				buyAvailableUpgrades(app);
			}
		}

		levelUpHeroes(app);
	}

	private void levelUpHeroes(SWFApp app)
			throws StepTimedOutException, StepFailedException, InterruptedException{
		_t("leveling up heroes");
		for (GenieComponent heroEntry: getHeroEntries(app)) {
			long heroLevel = getHeroLevel(app, heroEntry);

			if (heroLevel < 4100) {
				GenieComponent heroLvlButton = getLevelButton(app, heroEntry);
				levelUpx100(new GenieDisplayObject(heroLvlButton.getGenieID(), app));
			}
		}
	}

	private void doGameRun(SWFApp app)
			throws StepTimedOutException, StepFailedException, InterruptedException{
		GenieComponent phthalo = getHero(app, PHTHALO);
		if (phthalo == null) {
			return;
		}

		GenieComponent phthaloLvButton = getLevelButton(app, phthalo);

		for (int i = 0; i < 3; i++) {
			levelUpx100(new GenieDisplayObject(phthaloLvButton.getGenieID(), app));
		}
		buyAvailableUpgrades(app);
		tryClickingOnCandy(app);
	}

	private void doPrepAscend(SWFApp app)
			throws StepTimedOutException, StepFailedException, InterruptedException{
		while (!isThereCandy(app)) {
			levelUpHeroes(app);
		}

	}

	private void doAscend(SWFApp app)
			throws StepTimedOutException, StepFailedException, InterruptedException{
		GenieComponent amenhotep = getHero(app, AMENHOTEP);
		if (amenhotep == null) {
			return;
		}

		GenieComponent upgradeCt;
		GenieLocatorInfo glInfo = new GenieLocatorInfo();

		while (getHeroLevel(app, amenhotep) < 150) {
			GenieComponent amenhotepLvButton = getLevelButton(app, amenhotep);
			levelUpx100(new GenieDisplayObject(amenhotepLvButton.getGenieID(), app));
		}

		glInfo.propertyValueTable.put("name", "upgradeHolder");
		upgradeCt = amenhotep.getChildren(glInfo, false, false)[0];
		upgradeCt.getChildAt(3).click(); //click on Ascend
		upgradeCt.waitFor(1);	// wait for confirmation pop-up

		glInfo = new GenieLocatorInfo();
		glInfo.propertyValueTable.put("name", "okButton");
		topGC.getChildren(glInfo, true, false)[0].click();
	}

	private GameState checkGameState(SWFApp app)
			throws StepTimedOutException, StepFailedException {
		GenieComponent terra = getHero(app, TERRA);
		if (terra == null) {
			tryClickingOnCandy(app);
			return GameState.GAME_STATE_START;
		}

		if (getHeroLevel(app, terra) < 200) {
			return GameState.GAME_STATE_START;
		}

		GenieComponent phthalo = getHero(app, PHTHALO);
		if (phthalo == null) {
			return GameState.GAME_STATE_START;
		}

		long phthaloHeroLevel = getHeroLevel(app, phthalo);
		if (phthaloHeroLevel < 2100) {
			return GameState.GAME_STATE_BOOSTING_GILDED;
		}

		if (phthaloHeroLevel > 2100) {
			return GameState.GAME_STATE_PREP_ASCEND;
		}

		return GameState.GAME_STATE_START;
	}

    /*private GenieMovieClip getUI(SWFApp app) {
    	return (new GenieMovieClip(UI_QUALIFIER,app));
    }*/

	private void buyAvailableUpgrades(SWFApp app)
			throws StepTimedOutException, StepFailedException {
		_t("buying available upgrades");

		if (idPool.containsKey(BUY_AVAILABLE_UPGRADES_ID)) {
			_t("ID is present, using that instead");
			GenieComponent buyAvailableUpgradesButton = new GenieComponent(idPool.get(BUY_AVAILABLE_UPGRADES_ID), app, false);
			if (buyAvailableUpgradesButton.isPresent()) {
				_t("found it");
				buyAvailableUpgradesButton.click();
				return;
			} else {
				idPool.remove(BUY_AVAILABLE_UPGRADES_ID);
			}
		}

		GenieLocatorInfo glInfo = new GenieLocatorInfo();

		_t("couldn't find it, resorting to searching");
		glInfo.text = BUY_AVAILABLE_UPGRADES;
		GenieComponent buyButton = topGC.getChildren(glInfo, true, false)[0];
		if (buyButton.isPresent()) {
			_t("storing id for future use");
			idPool.put(BUY_AVAILABLE_UPGRADES_ID, buyButton.getGenieID());
			buyButton.click();
		}
	}

	private java.util.List<GenieDisplayObject> getHeroEntries(SWFApp app)
			throws StepTimedOutException, StepFailedException {
		GenieMovieClip heroPanel = new GenieMovieClip(HERO_PANEL_ID, app);
		GenieLocatorInfo glInfo = new GenieLocatorInfo();

		glInfo.qualifiedClassName = HERO_ENTRY_QUALIFIER;

		return Arrays.asList(heroPanel.getChildren(glInfo, false, false));
	}

	private GenieComponent getHero(SWFApp app, String heroName)
			throws StepTimedOutException, StepFailedException {
		if (idPool.containsKey(heroName)) {
			GenieComponent hero = new GenieComponent(idPool.get(heroName), app, false);
			if (hero.isPresent()) {
				_t("using id from pool for getting hero");
				return hero;
			} else {
				idPool.remove(heroName);
			}
		}

		GenieMovieClip heroPanel = new GenieMovieClip(HERO_PANEL_ID, app);
		GenieLocatorInfo glInfo = new GenieLocatorInfo();
		java.util.List<GenieDisplayObject> heroList;

		glInfo.text = heroName;
		_t("getting hero " + heroName);
		heroList = Arrays.asList(heroPanel.getChildren(glInfo, true, false));
		if (heroList.isEmpty()) {
			return null;
		}
		_t("storing id of hero for future use");
		idPool.put(heroName, heroList.get(0).getParent().getGenieID());
		return heroList.get(0).getParent();
	}

	private long getHeroLevel(SWFApp app, GenieComponent hero)
			throws StepTimedOutException, StepFailedException {
		GenieComponent firstChild = null;
		String lvlString;

		if(idPool.containsKey(HERO_LEVEL_PREFIX + hero.getGenieID())) {
			String id = idPool.get(HERO_LEVEL_PREFIX + hero.getGenieID());
			GenieComponent gC = new GenieComponent(id, app, false);
			if (!gC.isPresent()) {
				idPool.remove(HERO_LEVEL_PREFIX + hero);
			} else {
				firstChild = gC;
			}
		} else {
			GenieLocatorInfo glInfo = new GenieLocatorInfo();

			glInfo.propertyValueTable.put("name", "level");
			firstChild = (hero.getChildren(glInfo, true, false))[0];
			idPool.put(HERO_LEVEL_PREFIX + hero, firstChild.getGenieID());
		}

		if (firstChild == null) {
			throw new StepFailedException();
		}

		lvlString = firstChild.getValueOf("text").replace("Lvl ","");
		_t("hero level is " + lvlString);

		return (long) Double.parseDouble(lvlString);
	}

	private void levelUpx100(GenieDisplayObject button)
			throws StepTimedOutException, StepFailedException, InterruptedException{
		// System.out.println(System.getProperty("os.name").toLowerCase());
		if(System.getProperty("os.name").toLowerCase().startsWith("mac")) {
			// metakeyClick(button, "VK_META");
			metakeyClick(button);
		} else {
			// metakeyClick(button, "VK_CONTROL");
			metakeyClick(button);
		}
	}

	/*private void levelUpx25(GenieDisplayObject button)
			throws StepTimedOutException, StepFailedException, InterruptedException {
		// metakeyClick(button, "VK_Z");
	}*/

	private void enableProgression(SWFApp app)
			throws StepFailedException, StepTimedOutException {
		_t("checking if progression is disabled");
		if (idPool.containsKey(PROGRESSION_BUTTON_REDSTRIKE_ID) && idPool.containsKey(PROGRESSION_BUTTON_ID)) {
			GenieComponent progButton = new GenieComponent(PROGRESSION_BUTTON_REDSTRIKE_ID, app, false);
			if (progButton.isPresent()) {
				progButton.click();
			}
		} else {
			GenieLocatorInfo glInfo = new GenieLocatorInfo();
			glInfo.propertyValueTable.put("name", "progressionButton");
			java.util.List<GenieComponent> childList = Arrays.asList(topGC.getChildren(glInfo, true, false));
			if (!childList.isEmpty()) {
				GenieComponent progressButton = childList.get(0);
				idPool.put(PROGRESSION_BUTTON_ID, progressButton.getGenieID());
				GenieLocatorInfo glInfo2 = new GenieLocatorInfo();
				glInfo2.propertyValueTable.put("name", "redStrike");
				java.util.List<GenieComponent> redStrikeChildren = Arrays.asList(progressButton.getChildren(glInfo2, true, false));
				if (!redStrikeChildren.isEmpty()) {
					_t("enabling progression");
					idPool.put(PROGRESSION_BUTTON_REDSTRIKE_ID, redStrikeChildren.get(0).getGenieID());
					redStrikeChildren.get(0).click();
				}
			}
		}
	}

	private boolean isThereCandy(SWFApp app)
			throws StepTimedOutException, StepFailedException {
		GenieLocatorInfo glInfo = new GenieLocatorInfo();
		glInfo.qualifiedClassName = CANDY_QUALIFIER;

		java.util.List<GenieComponent> gdoArray =
				Arrays.asList(topGC.getChildren(glInfo, true, false));
		_t("is there candy?");
		if (gdoArray.isEmpty()) {
			_t("i guess not");
		} else {
			_t("yes, there is");
		}
		return gdoArray.size() > 0;
	}

	private void tryClickingOnCandy(SWFApp app)
			throws StepTimedOutException, StepFailedException {
		GenieLocatorInfo glInfo = new GenieLocatorInfo();
		glInfo.qualifiedClassName = CANDY_QUALIFIER;
		_t("trying to click on candy");
		java.util.List<GenieComponent> gdoArray = Arrays.asList(topGC.getChildren(glInfo, true, false));
		if(!gdoArray.isEmpty()) {
			for (GenieComponent gC: gdoArray) {
				gC.click();
			}
		} else {
			_t("no candies found");
		}
	}

	private void metakeyClick(GenieDisplayObject button)
			throws StepTimedOutException, StepFailedException, InterruptedException{
		// uiKeyBoard.pressKey(metakey);
		ArrayList<String> argsList = Utils.getArrayListFromStringParams("CONTROL",
				"1", "3000");
		dvXA dvxa = new dvXA(button, BaseEvent.CLICK_EVENT,
				GenieLogEnums.GenieStepType.STEP_NATIVE_TYPE,
				button.getClass().getSimpleName(),
				new ArrayList<String>());
		dvXA dvxa2 = new dvXA(button, BaseEvent.PERFORM_KEYACTION,
				GenieLogEnums.GenieStepType.STEP_NATIVE_TYPE,
				button.getClass().getSimpleName(), argsList);
		dvxa2.performAction();
		//Thread.sleep(2000);
		dvxa.performAction();
		/*UIGenieID uiGenieID;
		button.getLocalCoordinates();*/
		/*Robot r = new Robot();
		Point p = uiGenieID.getCurrentScreenCoordinates(button.getGenieID());
		while(p.y < 200 || p.y > 750) {
			_t("Hero's y coord is: " + p.y);
			int delta = 0;
			if (p.y > 500) {
				delta = (p.y - 500) / 200;
			} else if (p.y < 200) {
				delta = (p.y - 500) / 200;
			}
			r.mouseWheel(delta);
			p = uiGenieID.getCurrentScreenCoordinates(button.getGenieID());
		}
		uiGenieID.doubleClick(button.getGenieID(), 50, 30, 1);*/
		//button.performKeyAction("CONTROL", 1, 5000);

		// Point coord = button.getLocalCoordinates();
		// uiLocal.click(coord.x + 50, coord.y + 50, 1);
		// uiKeyBoard.releaseKey(metakey);
	}

	private GenieComponent getLevelButton(SWFApp app, GenieComponent parent)
			throws StepTimedOutException, StepFailedException {
		if (idPool.containsKey(HERO_LEVEL_BUTTON_PREFIX + parent.getGenieID())) {
			_t("using id in pool for level button");
			return new GenieDisplayObject(idPool.get(HERO_LEVEL_BUTTON_PREFIX + parent.getGenieID()), app);
		}

		GenieLocatorInfo glInfo = new GenieLocatorInfo();

		glInfo.propertyValueTable.put("name", "levelButton");
		java.util.List<GenieComponent> children = Arrays.asList(parent.getChildren(glInfo, true, false));
		if (children.isEmpty()) {
			return null;
		}

		GenieComponent levelButton = children.get(0);
		if (levelButton.isPresent()) {
			_t("storing level button id " + HERO_LEVEL_PREFIX + parent.getGenieID() + " to idPool");
			idPool.put(HERO_LEVEL_BUTTON_PREFIX + parent.getGenieID(), levelButton.getGenieID());
			return levelButton;
		}

		return null;
	}

	private class dvXA extends ExecuteActions {
		GenieComponent gc;
		ArrayList<String> preload = null;
		String eventName;

		public dvXA(GenieComponent genieComponent, String evName,
					GenieLogEnums.GenieStepType stType, String clName,
					ArrayList<String> args) {
			super(genieComponent, evName, stType, clName, args);
			gc = genieComponent;
			eventName = evName;
		}

		protected String prepareArgs(ArrayList<String> args) {
			BaseEvent be = new BaseEvent();
			int argsLen = args.size();
			for (int i=0; i < argsLen; i++)
			{
				be.arguments.add(args.get(i));
			}

			if(preload != null && preload.size() > 0)
			{
				argsLen = preload.size();
				for (int i=0; i < argsLen; i++)
				{
					be.preloadArguments.add(preload.get(i));
				}
			}

			return createPreloadData(gc.getGenieID(), eventName, be);
		}

		private String createPreloadData(String genieID, String eventName, BaseEvent event) {
			event.arguments = Utils.getXMLCompatibleStr(event.arguments);
			StringBuffer buf = new StringBuffer();
			String data = "";
			if(event.arguments.size() > 0) {
				buf.append("<XML>");
			} else {
				buf.append("<String>");
			}

			buf.append("");

			buf.append("<GenieID>" + genieID + "</GenieID>");
			buf.append("<Event>" + eventName + "</Event>");
			// buf.append("<Ctrl>" + event.ctrlKey + "</Ctrl>");
			buf.append("<Ctrl>" + true + "</Ctrl>");
			buf.append("<Alt>" + event.altKey + "</Alt>");
			buf.append("<Shift>" + event.shiftKey + "</Shift>");
			buf.append("<MouseX>" + event.mouseX + "</MouseX>");
			buf.append("<MouseY>" + event.mouseY + "</MouseY>");

			buf.append("<Arguments>");

			for(int i=0; i < event.arguments.size(); ++i) {
				buf.append("<Argument>" + event.arguments.get(i) + "</Argument>");
			}
			buf.append("</Arguments>");

			if(event.preloadArguments != null && event.preloadArguments.size() > 0)	{
				buf.append("<PreloadArguments>");
				for (int i=0; i<event.preloadArguments.size(); ++i) {
					buf.append("<Argument>" + event.preloadArguments.get(i) + "</Argument>");
				}
				buf.append("</PreloadArguments>");
			}

			if(event.arguments.size() > 0) {
				buf.append("</XML>");
			} else {
				buf.append("</String>");
			}

			data=buf.toString();
			_t(data);
			return data;
		}
	}

	private void _t(String text) {
		System.out.println();
		System.out.println("====================================================");
		System.out.println(text);
		System.out.println("====================================================");
		System.out.println();
	}
}
