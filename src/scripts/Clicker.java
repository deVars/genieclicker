package scripts;

import com.adobe.genie.executor.GenieLocatorInfo;
import com.adobe.genie.executor.GenieScript;
import com.adobe.genie.executor.components.ExecuteActions;
import com.adobe.genie.executor.components.GenieComponent;
import com.adobe.genie.executor.components.GenieDisplayObject;
import com.adobe.genie.executor.components.GenieMovieClip;
import com.adobe.genie.executor.enums.GenieLogEnums;
import com.adobe.genie.executor.events.BaseEvent;
import com.adobe.genie.executor.exceptions.StepFailedException;
import com.adobe.genie.executor.exceptions.StepTimedOutException;

import com.adobe.genie.genieCom.SWFApp;
import com.adobe.genie.genieUIRobot.UIFunctions;
import com.adobe.genie.utils.Utils;

import java.awt.AWTException;
import java.util.*;


public class Clicker extends GenieScript {
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
	final long START_WAIT = 2;
	final long RUN_WAIT = 180;
	final long ASCEND_WAIT = 30;

	private enum GameState {
		GAME_STATE_START, GAME_STATE_BOOSTING_GILDED, GAME_STATE_PREP_ASCEND,
		GAME_STATE_ASCEND
	}

	// private UIKeyBoard uiKeyBoard = new UIKeyBoard();
	private Map<String, String> idPool = new HashMap<String, String>();

	private static GenieComponent topGC;
	UIFunctions uiFunctions = new UIFunctions(false);

	public Clicker() throws Exception {
		super();
	}

    @Override
    public void start() throws Exception {
		EXIT_ON_FAILURE = false;
		CAPTURE_SCREENSHOT_ON_FAILURE = false;

		final SWFApp app = connectToApp("two");
		_t("connecting to app");

		topGC = new GenieComponent("", app);
    	GameState gameState = GameState.GAME_STATE_START;
    	_t("test started");

    	boolean forever = true;
    	long turnTimer = 0; //

    	while (forever) {
    		forever = false;

			_t("starting a state check");
    		if (gameState != GameState.GAME_STATE_PREP_ASCEND &&
    				gameState != GameState.GAME_STATE_ASCEND) {
				gameState = checkGameState(app);
				if (turnTimer > TURN_TIMER_MAX) {
					gameState = GameState.GAME_STATE_PREP_ASCEND;
				}
			}

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
			if (forever) {
				if (gameState == GameState.GAME_STATE_START) {
					this.wait(START_WAIT);
				} else if (gameState == GameState.GAME_STATE_BOOSTING_GILDED) {
					this.wait(RUN_WAIT);
				} else {
					this.wait(ASCEND_WAIT);
				}
			}

			turnTimer++;
		}

    }

    private void doGameStart(SWFApp app)
			throws StepTimedOutException, StepFailedException, InterruptedException, AWTException {
    	GenieComponent terra = getHero(app, TERRA);
		if (terra != null) {
			if (getHeroLevel(app, terra) > 100) {
				buyAvailableUpgrades(app);
			}
		}

		levelUpHeroes(app);
    }

    private void levelUpHeroes(SWFApp app)
			throws StepTimedOutException, StepFailedException, InterruptedException, AWTException {
		_t("leveling up heroes");
		for (GenieComponent heroEntry: getHeroEntries(app)) {
			long heroLevel = getHeroLevel(app, heroEntry);

			if (heroLevel < 4100) {
				GenieComponent heroLvlButton = getLevelButton(app, heroEntry);
				levelUpx100(new GenieDisplayObject(heroLvlButton.getGenieID(), app));
				levelUpx100(new GenieDisplayObject(heroLvlButton.getGenieID(), app));
				levelUpx100(new GenieDisplayObject(heroLvlButton.getGenieID(), app));
			}
		}
    }

    private void doGameRun(SWFApp app)
			throws StepTimedOutException, StepFailedException, InterruptedException, AWTException {
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
			throws StepTimedOutException, StepFailedException, InterruptedException, AWTException {
		disableProgression(app);
    	while (!isThereCandy(app)) {
    		levelUpHeroes(app);
		}

		doAscend(app);
    }

    private void doAscend(SWFApp app)
			throws StepTimedOutException, StepFailedException, InterruptedException, AWTException {
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

    private List<GenieDisplayObject> getHeroEntries(SWFApp app)
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
		List<GenieDisplayObject> heroList;

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
			throws StepTimedOutException, StepFailedException, InterruptedException, AWTException {
		// System.out.println(System.getProperty("os.name").toLowerCase());
		if(System.getProperty("os.name").toLowerCase().startsWith("mac")) {
			// metakeyClick(button, "VK_META");
			metakeyClick(button);
		} else {
			// metakeyClick(button, "VK_CONTROL");
			metakeyClick(button);
		}
    }

	private void disableProgression(SWFApp app)
			throws StepFailedException, StepTimedOutException {
		if (idPool.containsKey(PROGRESSION_BUTTON_ID)) {
			GenieComponent progBtn =
				new GenieComponent(idPool.get(PROGRESSION_BUTTON_ID), app, false);
			GenieComponent progRedBtn = null;

			if (idPool.containsKey(PROGRESSION_BUTTON_REDSTRIKE_ID)) {
				progRedBtn =
					new GenieComponent(idPool.get(PROGRESSION_BUTTON_REDSTRIKE_ID),
						app, false);
			}

			if (progBtn.isPresent()) {
				if (progRedBtn == null) {
					progBtn.click();
				} else if (!progRedBtn.isPresent()) {
					progBtn.click();
				}
			}
		}
	}

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
			List<GenieComponent> childList = Arrays.asList(topGC.getChildren(glInfo, true, false));
			if (!childList.isEmpty()) {
				GenieComponent progressButton = childList.get(0);
				idPool.put(PROGRESSION_BUTTON_ID, progressButton.getGenieID());
				GenieLocatorInfo glInfo2 = new GenieLocatorInfo();
				glInfo2.propertyValueTable.put("name", "redStrike");
				List<GenieComponent> redStrikeChildren = Arrays.asList(progressButton.getChildren(glInfo2, true, false));
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

		List<GenieComponent> gdoArray =
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
		List<GenieComponent> gdoArray = Arrays.asList(topGC.getChildren(glInfo, true, false));
		if(!gdoArray.isEmpty()) {
			for (GenieComponent gC: gdoArray) {
				gC.click();
			}
		} else {
			_t("no candies found");
		}
	}

    private void metakeyClick(GenieDisplayObject button)
			throws StepTimedOutException, StepFailedException, InterruptedException, AWTException {
		// uiKeyBoard.pressKey(metakey);
        String osName = System.getProperty("os.name").toLowerCase();
        String delayString;

        if(osName.startsWith("mac") || osName.startsWith("windows")) {
            delayString = "3000";
        } else {
            delayString = "3500";
        }

		ArrayList<String> argsList =
                Utils.getArrayListFromStringParams("CONTROL", "1", delayString);
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
		List<GenieComponent> children = Arrays.asList(parent.getChildren(glInfo, true, false));
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
			//data = data + "";

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

	/*private <T extends Collection> T stringToJson (String json) {
		return new JSONDeserializer<T>().deserialize(json);
	}*/
}
