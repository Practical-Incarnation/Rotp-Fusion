/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.ai.interfaces;

import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.StarSystem;

public interface General {
    void nextTurn();
    boolean inWarMode();
    public List<StarSystem> rushShipSystems();
    public List<StarSystem> rushDefenseSystems();
    float invasionPriority(StarSystem v);
    
    // specific to Xilmi AI
    default Empire bestVictim()               { return null; }
    default Empire bestAlly(boolean includeCurrentAllies) { return null; }
    default Empire biggestThreat() { return null; }
    default float defenseRatio()              { return 0.5f; }
    default float totalEmpirePopulationCapacity(Empire emp) { return 0; }
    default int additionalColonizersToBuild(boolean returnPotentialUncolonizedInstead) { return 0; }
    default boolean allowedToBomb(StarSystem sys) { return true; }
    default boolean strongEnoughToAttack() { return true; }
    default boolean isExpander() { return false; }
    default boolean isInvader() { return false; }
    default boolean isRusher() { return false; }
    default boolean isSpy() { return false; }
    default boolean isTrader() { return false; }
    default float timeToKill(Empire attacker, Empire defender) { return Float.MIN_VALUE; }
    default float warROI() { return Float.MAX_VALUE; }
    default int minTransportSize() { return 5; }
    default boolean needScoutRepellers(boolean potential) { return false; }
    default boolean sensePotentialAttack() { return false; }
    default Location colonyCenter(Empire emp) { return new Location(0, 0); }
    default Location fleetCenter(Empire emp) { return new Location(0, 0); }
    default float absolution() { return 0f; }
    default float smartPowerLevel() { return 0f; }
    default float highestProdScore() { return 1f; }
    default float gameProgress() { return 0f; }
    default float predictEmpireChanceToDeclareWarIfIDeclaredWarOn(Empire subject, Empire object, boolean chanceToDeclareWarOnMeInstead) { return 0f; }
    default float troopsNecessaryToTakePlanet(EmpireView ev, StarSystem sys) { return 0f; }
}
