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
package rotp.model.ai.cruel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import rotp.model.ai.FleetPlan;
import rotp.model.ai.interfaces.General;
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.tech.Tech;
import rotp.model.tech.TechBombWeapon;
import rotp.util.Base;

public class AIGeneral implements Base, General {
    private final Empire empire;
    private float civProd = 0;
    private final HashMap<StarSystem, List<Ship>> targetedSystems;
    private final List<StarSystem> rushDefenseSystems;
    private final List<StarSystem> rushShipSystems;
    private float civTech = 0;
    //better buffer values in private-members instead of recalculating every time
    private Empire bestVictim = null;
    private boolean searchedVictimThisTurn = false;
    private Location uncolonizedCenter;
    private boolean updatedUncolonizedCenterThisTurn = false;
    private float defenseRatio = -1;
    private float totalArmedFleetCost = -1;
    private int additionalColonizersToBuild = -1;
    private float totalEmpirePopulationCapacity = -1;
    private float warROI = -1;
    private float visibleEnemyFighterCost = -1;
    private float visibleEnemyTransportCost = -1;
    private float myFighterCost = -1;
    private float smartPower = -1;
    private float highestProdScore = -1;

    public AIGeneral (Empire c) {
        empire = c;
        targetedSystems = new HashMap<>();
        rushDefenseSystems = new ArrayList<>();
        rushShipSystems = new ArrayList<>();
    }
    private HashMap<StarSystem, List<Ship>> targetedSystems() { return targetedSystems; }
    @Override
    public List<StarSystem> rushDefenseSystems() { return rushDefenseSystems; }
    @Override
    public List<StarSystem> rushShipSystems() { return rushShipSystems; }
    @Override
    public String toString()   { return concat("General: ", empire.raceName()); }
    @Override
    public boolean inWarMode()  { return empire.numEnemies() > 0; }
    @Override
    public void nextTurn() {
        civProd = empire.totalPlanetaryProduction();
        civTech = empire.tech().avgTechLevel();
        resetTargetedSystems();
        rushDefenseSystems.clear();
        rushShipSystems.clear();
        bestVictim = null;
        searchedVictimThisTurn = false;
        updatedUncolonizedCenterThisTurn = false;
        defenseRatio = -1;
        additionalColonizersToBuild = -1;
        totalArmedFleetCost = -1;
        totalEmpirePopulationCapacity = -1;
        warROI = -1;
        visibleEnemyFighterCost = -1;
        visibleEnemyTransportCost = -1;
        myFighterCost = -1;
        smartPower = -1;
        highestProdScore = -1;
        
        //empire.tech().learnAll();
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" "+empire.leader().name()+" personality: "+empire.leader().personality()+" objective: "+empire.leader().objective());
        Galaxy gal = galaxy();
        for (int id=0;id<empire.sv.count();id++) 
            reviseFleetPlan(gal.system(id));
        additionalColonizersToBuild = additionalColonizersToBuild(false);
        if(empire.atWar() || sensePotentialAttack())
        {
            int[] counts = galaxy().ships.shipDesignCounts(empire.id);
            ShipDesignLab lab = empire.shipLab();
            float fighterCost = 0.0f;
            float colonizerCost = 0.0f;
            for (int i=0;i<counts.length;i++) 
            {
                if(lab.design(i).hasColonySpecial())
                {
                    colonizerCost += lab.design(i).cost() * counts[i];
                    //System.out.println(galaxy().currentTurn()+" "+empire.name()+" "+lab.design(i).name()+" cost: "+lab.design(i).cost()+" count: "+counts[i]);
                    continue;
                }
                fighterCost += lab.design(i).cost() * counts[i] * empire.shipDesignerAI().fightingAdapted(lab.design(i));
            }
            colonizerCost += additionalColonizersToBuild * empire.shipDesignerAI().BestDesignToColonize().cost();
            //System.out.println(galaxy().currentTurn()+" "+empire.name()+" fightercost: "+fighterCost+" col-cost: "+colonizerCost+" col-need before: "+additionalColonizersToBuild+" at war: "+empire.atWar()+" sense potential attack: "+sensePotentialAttack());
            while(colonizerCost > fighterCost && additionalColonizersToBuild > 0)
            {
                additionalColonizersToBuild--;
                colonizerCost -= empire.shipDesignerAI().BestDesignToColonize().cost();
            }
            if(empire.diplomatAI().militaryRank(empire, false) > empire.diplomatAI().popCapRank(empire, false))
                additionalColonizersToBuild = 0;
            //System.out.println(galaxy().currentTurn()+" "+empire.name()+" col-need after: "+additionalColonizersToBuild);
        }
        ShipDesign design = empire.shipDesignerAI().BestDesignToColonize();
        Location uncolonizedCenter = uncolonizedCenter(empire);
        while (additionalColonizersToBuild > 0)
        {
            float highestScore = 0;
            Colony bestCol = null;
            for (int id=0; id<empire.sv.count();id++) {
                if(empire.sv.empire(id) != empire)
                    continue;
                StarSystem sys = galaxy().system(id);
                Colony col = sys.colony();
                if(col.currentProductionCapacity() <= 0.5f && col.production() < design.cost() && col.shipyard().desiredShips() > 0)
                    continue;
                float turnsToBuild = max(1f, col.shipyard().turnsToBuild(design));
                float turnsToMove = max(1f, (float)Math.ceil(max(1, sys.distanceTo(uncolonizedCenter)) / design.warpSpeed()));
                float score = 1f / (turnsToBuild + turnsToMove);
                //System.out.println(empire.name()+" "+col.name()+" score: "+score+" turnsToBuild: "+turnsToBuild+" turnsToMove: "+turnsToMove+" uncolonizedCenter: x: "+uncolonizedCenter.x()+" y: "+uncolonizedCenter.y());
                if(col.shipyard().building())
                    continue;
                if(score > highestScore)
                {
                    bestCol = col;
                    highestScore = score;
                }
            }
            if(bestCol == null)
                break;
            if(!bestCol.shipyard().building() || bestCol.shipyard().design().cost() >= design.cost())
            {
                bestCol.shipyard().design(design);
                bestCol.shipyard().addQueuedBC(design.cost());
                float colonyProduction = (bestCol.totalIncome() - bestCol.minimumCleanupCost()) * bestCol.planet().productionAdj();
                int desiredCount = min(additionalColonizersToBuild, (int)Math.floor((float)colonyProduction / (float)design.cost()));
                desiredCount = max(1, desiredCount);
                bestCol.shipyard().addDesiredShips(desiredCount);
                //System.out.println(galaxy().currentTurn()+" "+empire.name()+" should order "+desiredCount+" colonizers at "+bestCol.name());
                additionalColonizersToBuild-=desiredCount;
            }
        }
    }
    // modnar: adjustments to invasion valuation
    // Desire value to invade planet, factor in both planet size and factories
    // Higher desire value for Rich, Ultra-Rich, Artifacts
    // Lower desire value for Poor, Ultra-Poor
    public float takePlanetValue(StarSystem sys) {
        int sysId = sys.id;
        if (!empire.sv.inShipRange(sysId))  return 0.0f;
        if (!empire.sv.isScouted(sysId))    return 0.0f;
        if (!empire.sv.isColonized(sysId))  return 0.0f;
        
        float size = empire.sv.currentSize(sysId); // planet size
        float fact = empire.sv.factories(sysId); // factory count
        
        // increase planet value depending on size and factories (val is normalized below)
        // (4*pow(SIZE, 0.7) + min(20, sqrt(FACTORIES)))
        // min(20) is ballpark max invasion tech chances (400 factories)
        // 
        // Normal,   size-100,    0 factories:  val = 100
        // Normal,   size-100,  100 factories:  val = 110
        // Normal,   size-100,  200 factories:  val = 115
        // Normal,   size-100,  300 factories:  val = 118
        // Normal,   size-100,  400 factories:  val = 120
        // Normal,   size-140,  560 factories:  val = 147
        // Normal,   size-220, 1540 factories:  val = 194
        // Normal,   size-70,   140 factories:  val =  90
        // Normal,   size-70,   210 factories:  val =  93
        // Poor,     size-100,    0 factories:  val =  75
        // Poor,     size-100,  200 factories:  val =  86
        // Rich,     size-50,    50 factories:  val = 138
        // Artifact, size-80,   240 factories:  val = 203
        float val = (float) (4.0f*Math.pow(size, 0.7f) + Math.min(20.0f, Math.sqrt(fact)));

        // Higher desire value for Rich, Ultra-Rich, Artifacts
        // Lower desire value for Poor, Ultra-Poor
        // modnar: increase values for poor/ultra-poor
        if (empire.sv.isUltraPoor(sysId))
            val *= 0.6;
        else if (empire.sv.isPoor(sysId))
            val *= 0.75;
        else if (empire.sv.isResourceNormal(sysId))
            val *= 1;
        else if (empire.sv.isRich(sysId))
            val *= 2;
        else if (empire.sv.isUltraRich(sysId))
            val *= 3;

        //float for artifacts, triple for super-artifacts
        if (empire.sv.isArtifact(sysId))
            val *= 2;
        else if (empire.sv.isOrionArtifact(sysId))
            val *= 3;
        
        // modnar: killer instinct
        // higher value for the last few planets of an empire
        int remainingSystems = galaxy().empire(empire.sv.empId(sysId)).numColonies();
        if (remainingSystems <=4) {
            val *= ((remainingSystems + 7)/(remainingSystems + 1));
        }
        
        // modnar: normalized to normal size-100 planet with 200 factories (115)
        return val/115;
    }
    @Override
    public float invasionPriority(StarSystem sys) {
        int sysId = sys.id;
        if (!empire.sv.inShipRange(sysId))  return 0.0f;
        if (!empire.sv.isScouted(sysId))    return 0.0f;
        if (!empire.sv.isColonized(sysId))  return 0.0f;
        if (!empire.canColonize(sys.planet().type()))  return 0.0f;
        
        // modnar: increase invasion priority with planet size and factory count
        float pr = empire.sv.currentSize(sysId) + empire.sv.factories(sysId)/20.0f;
        
        // modnar: killer instinct
        // higher priority to take out the last few planets of an empire
        int remainingSystems = galaxy().empire(empire.sv.empId(sysId)).numColonies();
        if (remainingSystems <=3) {
            pr *= ((remainingSystems + 7)/(remainingSystems + 1));
        }
        
        if (empire.sv.isPoor(sysId))
            pr *= 2;
        else if (empire.sv.isResourceNormal(sysId))
            pr *= 3;
        else if (empire.sv.isRich(sysId))
            pr *= 4;
        else if (empire.sv.isUltraRich(sysId))
            pr *= 5;

        //float for artifacts, triple for super-artifacts
        if (empire.sv.isArtifact(sysId))
            pr *= 2;
        else if (empire.sv.isOrionArtifact(sysId))
            pr *= 3;
        pr /= Math.sqrt(max(1,empire.sv.distance(sysId)));
        pr /= Math.sqrt(max(1,empire.sv.bases(sysId)));
        return pr/10;
    }
    public void reviseFleetPlan(StarSystem sys) {
        int sysId = sys.id;
        
        // if out of ship range, ignore
        if (!empire.sv.inShipRange(sysId))
            return;

        if(needScoutRepellers(false) && (sys.empire() == empire || !empire.sv.isColonized(sysId)) && !sys.hasMonster())
        {
            //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" making repel-plan for "+sys.name());
            if(empire.shipDesignerAI().BestDesignToRepell() != null)
            {
                FleetPlan fp = empire.sv.fleetPlan(sys.id);
                fp.priority = 1100;
                if(empire.sv.isBorderSystem(sysId))
                    fp.priority += 50;
                //System.out.print("\n"+galaxy().currentTurn()+" "+sys.name()+" wants: "+empire.shipDesignerAI().BestDesignToRepell().name());
                fp.addShips(empire.shipDesignerAI().BestDesignToRepell(), 1);
            }
        }
        
        // for uncolonized systems
        if (!empire.sv.isColonized(sysId)) {
            return;
        }

        // for our systems
        if (empire == empire.sv.empire(sysId)) {
            float value = invasionPriority(sys);
            if (sys.colony().inRebellion())
                orderRebellionFleet(sys);
            return;
        }

        EmpireView ev = empire.viewForEmpire(empire.sv.empId(sysId));
        
        // for empires we are at war with.. we always invade or bomb
        if (ev.embassy().isEnemy()) {
            if (willingToInvade(ev, sys))
                orderInvasionFleet(ev, sys);
            return;
        }
    }
    public float invasionCost(EmpireView v, StarSystem sys)
    {
        float needed = troopsNecessaryToTakePlanet(v, sys);
        float combatTransport = empire.combatTransportPct();
        if(sys.empire().tech().subspaceInterdiction())
            combatTransport /= 2;
        float additional = expectedEnemyTransportKillPower(sys) * (1 - empire.fleetCommanderAI().bridgeHeadConfidence(sys));
        if(combatTransport > 0)
            needed = max(needed + additional, needed / combatTransport);
        else
            needed += additional;
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+": Considering invasion of "+sys.name()+" bridgeHeadConfidence: "+empire.fleetCommanderAI().bridgeHeadConfidence(sys)+" expected shot down: "+additional * (1 - empire.fleetCommanderAI().bridgeHeadConfidence(sys)));
        float invasionCost = needed * empire.tech().populationCost() / empire.race().growthRateMod();
        return invasionCost;
    }
    public float invasionGain(EmpireView v, StarSystem sys)
    {
        float facSavings = empire.sv.factories(sys.id) * (empire.tech().baseFactoryCost() - 2) + sys.planet().alienFactories(empire.id) * empire.tech().baseFactoryCost();
        float invasionGain = facSavings;
        List<Tech> possibleTechs = v.empire().tech().techsUnknownTo(empire);
        float avgTechCost = 0;
        int techCount = 0;
        for(Tech possi:possibleTechs)
        {
            avgTechCost += possi.researchCost();
            techCount++;
        }
        if(techCount > 0)
            avgTechCost /= techCount;
        float techCaptureCountEstimate = min(6, techCount, 0.02f * empire.sv.factories(sys.id));
        float techCaputureGain = techCaptureCountEstimate * avgTechCost;
        invasionGain += techCaputureGain;
        return invasionGain;
    }
    public boolean willingToInvade(EmpireView v, StarSystem sys) {
        if(!empire.warEnemies().contains(sys.empire()) && !empire.generalAI().strongEnoughToAttack())
            return false;
        if (!empire.canSendTransportsTo(sys))
            return false;
        //we gain factories, save us from building a colonizer and killing enemy-population also has value to us of half of what they pay for it
        float invasionGain = invasionGain(v, sys) + empire.shipDesignerAI().BestDesignToColonize().cost();
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+": Considering invasion of "+sys.name()+" cost: "+invasionCost(v, sys)+" gain: "+invasionGain+" cs: "+empire.shipLab().colonyDesign().cost()+" bridgeHeadConfidence: "+empire.fleetCommanderAI().bridgeHeadConfidence(sys));
        return invasionCost(v, sys) <= invasionGain;
    }
    public void orderRebellionFleet(StarSystem sys) {
        launchRebellionTroops(sys);
    }
    public void orderInvasionFleet(EmpireView v, StarSystem sys) {
        launchGroundTroops(v, sys, 1);
    }
    
    public void launchGroundTroops(EmpireView v, StarSystem target, float mult) {
        //float troops0 = troopsNecessaryToBypassBases(target);
        float needed = mult*troopsNecessaryToTakePlanet(v, target);
        float combatTransport = empire.combatTransportPct();
        if(target.empire().tech().subspaceInterdiction())
            combatTransport /= 2;
        float additional = expectedEnemyTransportKillPower(target) * (1 - empire.fleetCommanderAI().bridgeHeadConfidence(target));
        if(combatTransport > 0)
            needed = max(needed + additional, needed / combatTransport);
        else
            needed += additional;
        int alreadySent = empire.transportsInTransit(target);
        float troopsDesired = needed + empire.sv.currentSize(target.id) * 0.25f - alreadySent;
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" invading "+target.name()+" troops desired: "+troopsDesired);
        if (troopsDesired < 1)
            return;

        List<StarSystem> allSystems = empire.allColonizedSystems();
        List<StarSystem> launchPoints = new ArrayList<>();
        StarSystem.TARGET_SYSTEM = target;
        Collections.sort(allSystems,StarSystem.DISTANCE_TO_TARGET_SYSTEM);

        float troopsAvailable = 0;
        float maxTravelTime = 0;

        for (StarSystem sys : allSystems) {
            if (troopsAvailable < troopsDesired) {
                float travelTime = sys.colony().transport().travelTime(target);
                // modnar: only consider systems within 8 travel turns at the start of the game
                // decrease with faster warp (faster transport speed)
                // down to 3 travel turns with warp-9
                // warp (topSpeed): 1, 2, 3, 4, 5, 6, 7, 8, 9
                // transport speed: 1, 1, 2, 3, 4, 5, 6, 7, 8
                // allowableTurns:  8, 8, 8, 6 ,5, 4, 4, 3, 3
                // max distance:    8, 8,16,18,20,20,24,21,24
                float topSpeed = empire.tech().topSpeed();
                float allowableTurns = (float) (1 + Math.min(7, Math.floor(22 / topSpeed)));
                if ((travelTime <= allowableTurns) && sys.colony().canTransport()) {
                    launchPoints.add(sys);
                    maxTravelTime = max(maxTravelTime, travelTime);
                    // modnar: keep planets at least 60% full
                    // to prevent complete draining of planets
                    // TODO: modify with leader personality and source planet fertility
                    //troopsAvailable += sys.colony().maxTransportsAllowed();
                    troopsAvailable += Math.max(0.0f, sys.colony().population() - 0.6f*sys.colony().planet().currentSize());
                }
            }
        }

        //not enough troops to take planet! switch to defense
        if (troopsAvailable < needed)
            return;

        for (StarSystem sys: launchPoints)
            maxTravelTime = max(maxTravelTime, sys.colony().transport().travelTime(target));

        // send transports from launch points
        for (StarSystem sys : launchPoints) {
            // modnar: keep planets at least 60% full
            // to prevent complete draining of planets
            // TODO: modify with leader personality and source planet fertility
            // int troops = sys.colony().maxTransportsAllowed();
            int troops = (int) Math.floor(Math.max(0.0f, sys.colony().population() - 0.6f*sys.colony().planet().currentSize()));
            sys.colony().scheduleTransportsToSystem(target, troops, maxTravelTime);
        }
    }
    public void launchRebellionTroops(StarSystem target) {
        float troops1 =  target.colony().rebels()*2;
        int alreadySent = empire.transportsInTransit(target);
        float troopsDesired = troops1 - alreadySent;

        if (troopsDesired < 1)
            return;

        List<StarSystem> allSystems = empire.allColonizedSystems();
        List<StarSystem> launchPoints = new ArrayList<>();
        StarSystem.TARGET_SYSTEM = target;
        Collections.sort(allSystems,StarSystem.DISTANCE_TO_TARGET_SYSTEM);

        float troopsAvailable = 0;

        for (StarSystem sys : allSystems) {
            if (troopsAvailable < troopsDesired) {
                if (sys.colony().canTransport()) {
                    launchPoints.add(sys);
                    troopsAvailable += sys.colony().maxTransportsAllowed();
                }
            }
        }

        // send transports from launch points
        for (StarSystem sys : launchPoints) {
            int troops = sys.colony().maxTransportsAllowed();
            sys.colony().scheduleTransportsToSystem(target, troops);
        }
    }
    public float troopsNecessaryToBypassBases(StarSystem sys) {
        return empire.sv.bases(sys.id) * troopToEnemyBaseRatio(sys);
    }
    public float troopToEnemyBaseRatio(StarSystem sys) {
        int id = sys.id;
        EmpireView ev = empire.viewForEmpire(empire.sv.empire(id));
        return ev.spies().tech().weapon().techLevel() / empire.tech().construction().techLevel();
    }
    @Override
    public float troopsNecessaryToTakePlanet(EmpireView ev, StarSystem sys) {
        int id = sys.id;
        
        // modnar: (?) this old estimate gives completely wrong results for ground combat
        //return empire.sv.population(id) * (50 + ev.spies().tech().troopCombatAdj(true)) / (50 + empire.tech().troopCombatAdj(false));
        
        // modnar: correct ground combat ratio estimates for troopsNecessary
        if (ev.spies().tech().troopCombatAdj(true) >= empire.tech().troopCombatAdj(false)) {
            float defAdv = ev.spies().tech().troopCombatAdj(true) - empire.tech().troopCombatAdj(false);
            // killRatio = attackerCasualties / defenderCasualties
            float killRatio = (float) ((Math.pow(100,2) - Math.pow(100-defAdv,2)/2) / (Math.pow(100-defAdv,2)/2));
            return empire.sv.population(id) * killRatio;
        }
        else {
            float atkAdv = empire.tech().troopCombatAdj(false) - ev.spies().tech().troopCombatAdj(true);
            // killRatio = attackerCasualties / defenderCasualties
            float killRatio = (float) ((Math.pow(100-atkAdv,2)/2) / (Math.pow(100,2) - Math.pow(100-atkAdv,2)/2));
            return empire.sv.population(id) * killRatio;
        }
    }
    public int transportGauntletRounds(float speed) {
        switch((int)speed) {
            case 0: case 1: case 2: case 3: case 4:
                return 4;
            case 5: case 6:
                return 3;
            case 7: case 8:
                return 2;
            case 9: default:
                return 1;
        }
    }
    public float expectedEnemyTransportKillPower(StarSystem sys) {
        float damage = 0;
        for(ShipFleet fl : sys.empire().allFleets()) {
            damage += fl.firepower(0);
        }
        for(ShipFleet otherFleet : sys.orbitingFleets()) {
            if(otherFleet.empire() == empire || otherFleet.empire().allies().contains(empire) || otherFleet.empire() == sys.empire())
                continue;
            damage += otherFleet.firepower(0);
        }
        for(ShipFleet otherFleet : sys.incomingFleets()) {
            if(otherFleet.empire() == empire || otherFleet.empire().allies().contains(empire) || otherFleet.empire() == sys.empire())
                continue;
            damage += otherFleet.firepower(0);
        }
        if(sys.colony() != null)
            damage += sys.colony().defense().firepower(0);
        damage *= transportGauntletRounds(max(1, empire.tech().topEngineWarpTech().baseWarp() - 1));
        damage /= empire.tech().topArmorTech().transportHP;
        return damage;
    }
    public void orderBombardmentFleet(EmpireView v, StarSystem sys, float fleetSize) {
        
        int sysId = sys.id;
        EmpireView ev = empire.viewForEmpire(empire.sv.empId(sysId));
        float targetTech = ev.spies().tech().avgTechLevel(); // modnar: target tech level
        
        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        // set fleet orders for bombardment...
        float bcMultiplier = 1 + (empire.sv.hostilityLevel(sys.id)/2);
        
        // modnar: test fleet sizes, include enemyFleetSize, factoring in relative tech levels
        float bombBcNeeded = max(baseBCPresent*1.5f*(targetTech+10.0f)/(civTech+10.0f),bcMultiplier*civProd);
        float fightBcNeeded = 2*fleetSize*(targetTech+10.0f)/(civTech+10.0f);
        float destroyerBcNeeded = (bombBcNeeded + fightBcNeeded) * 0.2f;
        
        // ail: bombers and fighters according to what is needed
        int destroyersNeeded = (int) Math.ceil(destroyerBcNeeded/empire.shipLab().destroyerDesign().cost());
        int bombersNeeded = (int) Math.ceil(bombBcNeeded/empire.shipLab().bomberDesign().cost());
        int fightersNeeded = (int) Math.ceil(fightBcNeeded/empire.shipLab().fighterDesign().cost());

        ShipDesignLab lab = empire.shipLab();
        // modnar: should use min speed here (?)
        float speed = min(lab.destroyerDesign().warpSpeed(), lab.bomberDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
        fp.addShips(empire.shipLab().bomberDesign(), bombersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
        fp.stagingPointId = empire.optimalStagingPoint(sys, speed);
        if (v.embassy().finalWar()) 
            fp.priority = FleetPlan.BOMB_FINAL_WAR+ invasionPriority(sys)/100;
        else
            fp.priority = FleetPlan.BOMB_ENEMY+ invasionPriority(sys)/100;
    }
    public void orderBombEncroachmentFleet(EmpireView v, StarSystem sys, float fleetSize) {
        // set fleet orders for bombardment...
        int sysId = sys.id;
        EmpireView ev = empire.viewForEmpire(empire.sv.empId(sysId));
        float targetTech = ev.spies().tech().avgTechLevel(); // modnar: target tech level
        
        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcMultiplier = 1 + (empire.sv.hostilityLevel(sys.id)/2);
        
        // modnar: test fleet sizes, include enemyFleetSize, factoring in relative tech levels
        float bombBcNeeded = max(baseBCPresent*1.5f*(targetTech+10.0f)/(civTech+10.0f),bcMultiplier*civProd);
        float fightBcNeeded = 2*fleetSize*(targetTech+10.0f)/(civTech+10.0f);
        float destroyerBcNeeded = (bombBcNeeded + fightBcNeeded) * 0.2f;
        
        // ail: bombers and fighters according to what is needed
        int destroyersNeeded = (int) Math.ceil(destroyerBcNeeded/empire.shipLab().destroyerDesign().cost());
        int bombersNeeded = (int) Math.ceil(bombBcNeeded/empire.shipLab().bomberDesign().cost());
        int fightersNeeded = (int) Math.ceil(fightBcNeeded/empire.shipLab().fighterDesign().cost());

        ShipDesignLab lab = empire.shipLab();
        // modnar: should use min speed here (?)
        float speed = min(lab.destroyerDesign().warpSpeed(), lab.bomberDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
        fp.addShips(empire.shipLab().bomberDesign(), bombersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
        fp.stagingPointId = empire.optimalStagingPoint(sys, speed);
        fp.priority = FleetPlan.BOMB_ENCROACHMENT;
    }
    
    public void considerSneakAttackFleet(EmpireView v, StarSystem sys, float fleetSize) {
        // pacifist/honorable never sneak attack
        if (empire.leader().isPacifist()
        || empire.leader().isHonorable())
            return;
        
        // modnar: no sneak attack when number of enemies > 2
        // same as regular war declaration check, significant factor in extra wars
        if (empire.numEnemies() > 2)
            return;

        float baseChance = 0.3f - (empire.numEnemies()*0.3f);
        if (empire.leader().isAggressive())
            baseChance += 0.6f;
        else if (empire.leader().isDiplomat())
            baseChance -= 0.2f;
        else if (empire.leader().isRuthless())
            baseChance += 0.3f;

        // lower sneak attack chance on planet we can't capture
        if (!empire.canColonize(sys.planet().type()))
                baseChance -= 0.3f;
        
        // modnar: factor in own empire average tech level
        // suppress sneak attack war in early game when average tech level is below 10
        float myTechLvl = empire.tech().avgTechLevel(); // minimum average tech level is 1.0
        float techMod = 1.0f;
        if (myTechLvl < 10.0f) {
            techMod = myTechLvl / 20.0f + 0.5f; // linear change with tech level (range from 0.55 to 1.0)
        }
        baseChance *= techMod;
        
        // modnar: change sneak attack chance by number of our wars vs. number of their wars
        // try not to get into too many wars, and pile on if target is in many wars
        float enemyMod = (float) (0.2f * (v.empire().numEnemies() - empire.numEnemies()));
        baseChance += enemyMod;

        float value = (empire.sv.factories(sys.id) * 10);
        float cost = fleetSize + (empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost());
        float bonus = -0.5f + (value / (value+cost));

        if ((baseChance+bonus) > 0.5)  {
            orderBombardmentFleet(v, sys, fleetSize);
            empire.sv.fleetPlan(sys.id).priority = FleetPlan.BOMB_UNDEFENDED;
        }
    }
    private void setRepelFleetPlan(StarSystem sys, float fleetSize) {
        float baseBCPresent = empire.sv.bases(sys.id)*empire.tech().newMissileBaseCost();
        float bcNeeded = max(empire.shipLab().fighterDesign().cost(), fleetSize*3); // modnar: reduce repel fleet
        bcNeeded -= baseBCPresent;
        if (bcNeeded <= 0)
            return;
        
        rushDefenseSystems.add(sys);

        // use up to half of BC for Destroyers... rest for fighters
        int destroyersNeeded = (int) Math.ceil((bcNeeded/2)/empire.shipLab().destroyerDesign().cost());
        bcNeeded = max(0, bcNeeded-(destroyersNeeded * empire.shipLab().destroyerDesign().cost()));
        int fightersNeeded = (int) Math.ceil(bcNeeded/empire.shipLab().fighterDesign().cost());

        ShipDesignLab lab = empire.shipLab();
        // modnar: should use min speed here (?)
        float speed = min(lab.destroyerDesign().warpSpeed(), lab.fighterDesign().warpSpeed());
        FleetPlan fp = empire.sv.fleetPlan(sys.id);
        fp.priority = FleetPlan.REPEL + invasionPriority(sys)/100;
        fp.stagingPointId = empire.optimalStagingPoint(sys, speed);
        fp.addShips(empire.shipLab().destroyerDesign(), destroyersNeeded);
        fp.addShips(empire.shipLab().fighterDesign(), fightersNeeded);
    }
    private void resetTargetedSystems() {
        Set<StarSystem> systems = targetedSystems().keySet(); // re-inits
        for (StarSystem s: systems)
            targetedSystems.get(s).clear();

        Galaxy gal = galaxy();
        for (Ship ship: empire.visibleShips()){
            if (ship.inTransit() && empire.aggressiveWith(ship.empId())) {
                if (empire.knowETA(ship)) {
                    StarSystem dest = gal.system(ship.destSysId());
                    if (!targetedSystems.containsKey(dest))
                        targetedSystems.put(dest, new ArrayList<>());
                    targetedSystems.get(dest).add(ship);
                }
            }
        }
    }
    @Override
    public float timeToKill(Empire attacker, Empire defender)
    {
        float avgFleetDistance = 0;
        float fleetDistanceCounts = 0;
        float avgProductionDistance = 0;
        float productionDistanceCounts = 0;
        for(StarSystem theirs: defender.allColonizedSystems())
        {
            for(ShipFleet fleet: attacker.allFleets())
            {
                float speed = fleet.slowestStackSpeed();
                if(theirs.inNebula())
                    speed = 1;
                avgFleetDistance += max(fleet.distanceTo(theirs) / speed, 1) * fleet.bcValue();
                //fleet.travelTimeTo(theirs, fleet.slowestStackSpeed()) * fleet.bcValue();
                fleetDistanceCounts += fleet.bcValue();
            }
            for(StarSystem mine: attacker.allColonizedSystems())
            {
                float speed = attacker.tech().topSpeed();
                if(theirs.inNebula())
                    speed = 1;
                float colonyContributionValue = mine.colony().totalIncome() * mine.planet().productionAdj();
                //System.out.println(attacker.name()+" "+mine.name()+" can make "+newGrownPopulation+" per turn. so far: "+popDistanceCounts);
                float dist = mine.distanceTo(theirs);
                avgProductionDistance += max(dist / speed, 1) * colonyContributionValue;
                productionDistanceCounts += colonyContributionValue;
            }
        }
        if(fleetDistanceCounts > 0)
            avgFleetDistance /= fleetDistanceCounts;
        if(productionDistanceCounts > 0)
            avgProductionDistance /= productionDistanceCounts;
        avgFleetDistance *= 2;
        avgProductionDistance *= 2;
        float averageDamagerPerBc = 0;
        TechBombWeapon bomb = attacker.tech().topBombWeaponTech();
        averageDamagerPerBc = (max(0, bomb.damageLow() - defender.tech().maxPlanetaryShieldLevel()) + max(0, bomb.damageHigh() - defender.tech().maxPlanetaryShieldLevel())) / 2;
        averageDamagerPerBc /= bomb.cost * bomb.costMiniaturization(attacker) * 4;
        
        float killTime = Float.MAX_VALUE;
        if(avgFleetDistance == 0)
            avgFleetDistance = Float.MAX_VALUE;
        if(avgProductionDistance == 0)
            avgProductionDistance = Float.MAX_VALUE;
        float ProductionTurnsForKillInOneTurn = Float.MAX_VALUE;
        if(averageDamagerPerBc > 0)
        {
            killTime = defender.totalPlanetaryPopulation() * 200 / (attacker.totalFleetCost() * averageDamagerPerBc) + avgFleetDistance;
            ProductionTurnsForKillInOneTurn = defender.totalPlanetaryPopulation() * 200 / (attacker.totalPlanetaryProduction() * averageDamagerPerBc) + avgProductionDistance;
        }
        //System.out.println(attacker.name()+" vs. "+defender.name()+" popKillTime: "+PopKillTime+" totalPopGrowthPerTurnPotential: "+totalPopGrowthPerTurnPotential+" avgPopDistance: "+avgPopDistance);
        float totalKillTime = 1 / (1 / killTime + 1 / ProductionTurnsForKillInOneTurn);
        //System.out.println(attacker.name()+" vs. "+defender.name()+" totalKillTime: "+totalKillTime+" ship-killtime: "+killTime+" prod-killtime: "+ProductionTurnsForKillInOneTurn);
        return totalKillTime;
    }
    @Override
    public float warROI() {
        if(warROI > -1)
            return warROI;
        warROI = Float.MAX_VALUE;
        float totalTime = 0;
        for(Empire enemy : empire.enemies())
        {
            totalTime += 1 / timeToKill(enemy, empire);
        }
        warROI = 3 / totalTime;
        return warROI;
    }
    @Override
    public Empire bestVictim() {
        if(searchedVictimThisTurn)
        {
            return bestVictim;
        }
        searchedVictimThisTurn = true;
        float highestScore = 0;
        Empire archEnemy = null;
        if(empire.contactedEmpires().isEmpty())
        {
            bestVictim = archEnemy;
            return bestVictim;
        }
        for(Empire emp : empire.contactedEmpires())
        {
            //we don't want to fight smaller empires that might vote for us when we're up for the vote
            if(empire.allies().contains(emp))
                continue;
            if(!empire.inShipRange(emp.id))
                continue;
            float currentScore = totalEmpirePopulationCapacity(emp) / (fleetCenter(empire).distanceTo(colonyCenter(emp)) + colonyCenter(empire).distanceTo(colonyCenter(emp)));
            float tradeMod = 1;
            if(empire.viewForEmpire(emp).trade() != null && empire.totalPlanetaryIncome() > 0)
                tradeMod += empire.viewForEmpire(emp).trade().profit() / empire.totalPlanetaryIncome();
            else
                tradeMod = 0.9f;
            float enemyMultiplyer = 1.0f;
            for(Empire theirFoe : emp.warEnemies()) {
                enemyMultiplyer += theirFoe.powerLevel(theirFoe) / (emp.powerLevel(emp) + theirFoe.powerLevel(theirFoe));
            }
            currentScore *= enemyMultiplyer;
            currentScore /= tradeMod;
            float powerRatio = empire.powerLevel(empire) / emp.powerLevel(emp);
            float milPowerRatio = 0;
            if(emp.militaryPowerLevel() > 0)
                milPowerRatio = smartPowerLevel() / emp.militaryPowerLevel();
            powerRatio = max(powerRatio, milPowerRatio);
            currentScore *= powerRatio;
            float spyAnnoyanceMod = 100f;
            for(DiplomaticIncident inc : empire.viewForEmpire(emp).embassy().allIncidents())
            {
                if(inc.isSpying())
                    spyAnnoyanceMod -= inc.severity;
            }
            currentScore *= spyAnnoyanceMod;
            //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" vs "+emp.name()+" dist: "+fleetCenter(empire).distanceTo(colonyCenter(emp))+" rev-dist: "+fleetCenter(emp).distanceTo(colonyCenter(empire))+" milrank: "+empire.diplomatAI().militaryRank(emp, true)+" poprank: "+empire.diplomatAI().popCapRank(emp, true)+" tradeMod: "+tradeMod+" spy-mod: "+spyAnnoyanceMod+" score: "+currentScore);
            if(currentScore > highestScore)
            {
                highestScore = currentScore;
                archEnemy = emp;
            }
        }
        /*if(archEnemy != null)
            System.out.println(galaxy().currentTurn()+" "+empire.name()+" => "+archEnemy.name()+" score: "+highestScore);*/
        bestVictim = archEnemy;
        return bestVictim;
    }
    @Override
    public float predictEmpireChanceToDeclareWarIfIDeclaredWarOn(Empire subject, Empire object, boolean chanceToDeclareWarOnMeInstead) {
        float subjectPower = subject.powerLevel(subject);
        float subjectMilitaryPower = subject.militaryPowerLevel();
        float subjectCurrentEnemyPower = 0;
        float subjectCurrentEnemyMilitaryPower = 0;
        for(Empire currentEnemyOfSubject : subject.warEnemies()) {
            subjectCurrentEnemyPower += currentEnemyOfSubject.powerLevel(currentEnemyOfSubject);
            subjectCurrentEnemyMilitaryPower += currentEnemyOfSubject.militaryPowerLevel();
            if(currentEnemyOfSubject == object || currentEnemyOfSubject == empire) //they wouldn't backstab one of them because they already are at war
                return 0;
        }
        if(subjectCurrentEnemyPower > subjectPower || subjectCurrentEnemyMilitaryPower > subjectMilitaryPower) //they are at war and losing so they won't won't be able to pick another target
            return 0;
        float victimScoreSumForSubject = 0;
        float victimScoreObjectForSubject = 0;
        float victimScoreMeForSubject = 0;
        for(Empire contactOfSubject : subject.contactedEmpires()) {
            if(!subject.inShipRange(contactOfSubject.id))
                continue;
            float currentScore = totalEmpirePopulationCapacity(contactOfSubject) / (fleetCenter(subject).distanceTo(colonyCenter(contactOfSubject)) + colonyCenter(subject).distanceTo(colonyCenter(contactOfSubject)));
            float powerRatio = subject.powerLevel(subject) / contactOfSubject.powerLevel(contactOfSubject);
            float milPowerRatio = 0;
            if(contactOfSubject.militaryPowerLevel() > 0)
                milPowerRatio = smartPowerLevel() / contactOfSubject.militaryPowerLevel();
            powerRatio = max(powerRatio, milPowerRatio);
            currentScore *= powerRatio;
            float enemyMultiplyer = 1.0f;
            for(Empire theirFoe : contactOfSubject.warEnemies()) {
                enemyMultiplyer += theirFoe.powerLevel(theirFoe) / (contactOfSubject.powerLevel(contactOfSubject) + theirFoe.powerLevel(theirFoe));
            }
            if(contactOfSubject == object && !contactOfSubject.warEnemies().contains(empire))
                enemyMultiplyer += empire.powerLevel(empire) / (contactOfSubject.powerLevel(contactOfSubject) + empire.powerLevel(empire));
            if(contactOfSubject == empire && !empire.warEnemies().contains(object))
                enemyMultiplyer += object.powerLevel(object) / (contactOfSubject.powerLevel(contactOfSubject) + object.powerLevel(object));
            currentScore *= enemyMultiplyer;
            if(contactOfSubject == empire)
                victimScoreMeForSubject = currentScore;
            if(contactOfSubject == object)
                victimScoreObjectForSubject = currentScore;
            victimScoreSumForSubject += currentScore;
        }
        if(victimScoreSumForSubject <= 0)
            return 0;
        if(chanceToDeclareWarOnMeInstead)
            return victimScoreMeForSubject / victimScoreSumForSubject;
        return victimScoreObjectForSubject / victimScoreSumForSubject;
    }
    @Override
    public float totalEmpirePopulationCapacity(Empire emp)
    {
        if(totalEmpirePopulationCapacity >= 0 && emp == empire && emp.isAIControlled())
            return totalEmpirePopulationCapacity;
        float capacity = 0;
        for (int id=0;id<emp.sv.count();id++) 
        {
            StarSystem current = galaxy().system(id);
            if(current.colony() == null)
                continue;
            if(current.empId() != emp.id)
                continue;
            capacity += current.planet().currentSize();
        }
        if(empire == emp)
            totalEmpirePopulationCapacity = capacity;
        return capacity;
    }
    public float visibleEnemyFighterCost()
    {
        if(visibleEnemyFighterCost >= 0)
            return visibleEnemyFighterCost;
        float cost = 0;
        for(ShipFleet fl:empire.enemyFleets())
        {
            if(empire.enemies().contains(fl.empire()))
            {
                //System.out.print("\n"+empire.name()+" see fleet of "+fl.empire().name()+" with Fgtr-value: "+empire.fleetCommanderAI().bcValue(fl, false, true, false, false));
                cost += empire.fleetCommanderAI().bcValue(fl, false, true, false, false);
            }
        }
        visibleEnemyFighterCost = cost;
        return visibleEnemyFighterCost;
    }
    public float visibleEnemyTransportCost()
    {
        if(visibleEnemyTransportCost >= 0)
            return visibleEnemyTransportCost;
        float cost = 0;
        for(Ship sh : empire.visibleShips())
        {
            if(empire.aggressiveWith(sh.empId()))
                if(!sh.nullDest() && galaxy().system(sh.destSysId()).empire() == empire)
                    if(sh.isTransport())
                    {
                        Transport tr = (Transport)sh;
                        cost += tr.size() * tr.empire().tech().populationCost();
                    }
        }
        return cost;
    }
    public float myFighterCost()
    {
        if(myFighterCost >= 0)
            return myFighterCost;
        float fighterCost = 0.0f;
        for (ShipDesign design:empire.shipLab().designs()) 
        {
            if(design.hasColonySpecial())
                continue;
            fighterCost += design.cost() * galaxy().ships.shipDesignCount(empire.id, design.id()) * empire.shipDesignerAI().fightingAdapted(design);
        }
        myFighterCost = fighterCost;
        return myFighterCost;
    }
    @Override
    public float defenseRatio()
    {
        if(defenseRatio >= 0)
        {
            return defenseRatio;
        }
        float dr = 0.0f;
        float totalMissileBaseCost = 0.0f;
        float totalShipCost = 0.0f;
        float highestPower = 0.0f;
        float enemyPop = 0.0f;
        float biggestPop = 0.0f;
        float totalKillingPower = 0.0f;
        StarSystem dummySys = null;
        float dummyScore = 0.0f;
        boolean empireInRange = false;
        float enemyOneTurnProd = 0.0f;
        for(Empire enemy : empire.contactedEmpires())
        {
            if(!empireInRange && empire.inShipRange(enemy.id))
                empireInRange = true;
            totalMissileBaseCost += enemy.missileBaseCostPerBC();
            totalShipCost += enemy.shipMaintCostPerBC();
            if(enemy.militaryPowerLevel() > highestPower)
                highestPower = enemy.militaryPowerLevel();
            float thisOnesPop = 0.0f;
            if(empire.enemies().contains(enemy))
                enemyOneTurnProd += enemy.totalPlanetaryProduction();
            for(StarSystem sys : enemy.allColonizedSystems())
            {
                if(sys.colony() != null)
                {
                    if(empire.sv.inShipRange(sys.id))
                    {
                        if(empire.enemies().contains(enemy))
                            enemyPop += enemy.totalPlanetaryPopulation();
                        thisOnesPop += enemy.totalPlanetaryPopulation();
                    }
                        
                    float score = (1 + sys.colony().defense().shieldLevel()) * sys.population();
                    if(score > dummyScore)
                    {
                        dummyScore = score;
                        dummySys = sys;
                    }
                }
            }
            if(thisOnesPop > biggestPop)
                biggestPop = thisOnesPop;
        }
        if(dummySys != null)
        {
            for(ShipFleet fl : empire.allFleets())
            {
                totalKillingPower += fl.expectedBombardDamage(dummySys) / 200.0;
            }
        }
        float overKill = 0.0f;
        enemyPop = max(enemyPop, biggestPop);
        if(enemyPop > 0)
            overKill = totalKillingPower / enemyPop;
        if(highestPower + smartPowerLevel() > 0)
            dr = 0.25f + 0.75f * (highestPower / (highestPower + smartPowerLevel()));
        //System.out.print("\n"+empire.name()+" enemyPop: "+enemyPop+" totalKillingPower: "+totalKillingPower+" overKill: "+overKill+" dr-pre adjust: "+dr);
        if(overKill > 1)
            dr = 1 - ((1 - dr) / overKill);
        //System.out.print("\n"+empire.name()+" dr-post adjust: "+dr);
        if(totalMissileBaseCost+totalShipCost > 0)
        {
            dr = min(dr, totalShipCost / (totalMissileBaseCost+totalShipCost));
        }
        if(myFighterCost() < visibleEnemyFighterCost() + enemyOneTurnProd || !empireInRange)
            dr = 1.0f;
        dr = max(0.5f, dr);
        //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" dr: "+dr+" myFighterCost: "+myFighterCost()+" visibleEnemyFighterCost: "+visibleEnemyFighterCost());
        defenseRatio = dr;
        return defenseRatio;
    }
    public boolean amSieging(StarSystem sys)
    {
        for(ShipFleet fl : sys.orbitingFleets())
        {
            if(fl.empire() != empire)
                continue;
            if(fl.expectedBombardDamage() > 0 && allowedToBomb(sys))
                return true;
        }
        return false;
    }
    @Override
    public int additionalColonizersToBuild(boolean returnPotentialUncolonizedInstead)
    {
        if(additionalColonizersToBuild >= 0 && !returnPotentialUncolonizedInstead)
            return additionalColonizersToBuild;
        double additional = 0;
        int colonizerRange = empire.shipDesignerAI().BestDesignToColonize().range();
        List<StarSystem> alreadyCounted = new ArrayList<>();
        for(StarSystem sys : empire.uncolonizedPlanetsInRange(colonizerRange))
        {
            if(empire.sv.isColonized(sys.id) && !amSieging(sys))
                continue;
            if(sys.monster() == null)
            {
                additional+=colonizationProbability(sys);
                alreadyCounted.add(sys);
            }
        }
        for(StarSystem sys : empire.unexploredSystems())
        {
            if(empire.sv.isColonized(sys.id))
                continue;
            if(empire.sv.distance(sys.id) > colonizerRange)
                continue;
            if(sys.monster() != null)
                continue;
            additional+=colonizationProbability(sys);
            //System.out.print("\n"+empire.name()+" "+sys.name()+" counted as uncolonized.");
            alreadyCounted.add(sys);
        }
        //System.out.print("\n"+empire.name()+" "+additional+" from uncolonized scouted without en-route.");
        //ail: when we have huge colonizer, don't count the unlocks for how many we need since we don't want to spam them like normal one's
        if(empire.shipDesignerAI().BestDesignToColonize().size() < 3)
        {
            for(ShipFleet fleet:empire.allFleets())
            {
                if(!fleet.hasColonyShip())
                {
                    continue;
                }
                if(fleet.destination() != null)
                {
                    for(StarSystem sys : galaxy().systemsInRange(fleet.destination(), empire.shipRange()))
                    {
                        if(alreadyCounted.contains(sys))
                        {
                            break;
                        }
                        if(sys.colony() != null)
                        {
                            continue;
                        }
                        if(!empire.sv.inShipRange(sys.id))
                        {
                            if(empire.canColonize(sys.id)
                                    || empire.unexploredSystems().contains(sys))
                            {
                                additional+=colonizationProbability(sys);
                                alreadyCounted.add(sys);
                            }
                        }
                    }
                }
            }
        }
        if(returnPotentialUncolonizedInstead)
            return (int)Math.ceil(additional);
        boolean knowSomeoneAtWar = false;
        for(EmpireView contact : empire.contacts())
        {
            if(!empire.inShipRange(contact.empId()))
                continue;
            if(!contact.empire().warEnemies().isEmpty())
                knowSomeoneAtWar = true;
        }
        if(knowSomeoneAtWar)
            additional = max((int)Math.ceil(additional), empire.numColonies() / 5);
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" required colonizers: "+additional);
        int[] counts = galaxy().ships.shipDesignCounts(empire.id);
        for (int i=0;i<counts.length;i++) 
        {
            if(empire.shipLab().design(i).hasColonySpecial())
            {
                if(empire.shipLab().design(i).range() < empire.shipDesignerAI().BestDesignToColonize().range())
                    continue;
                //ail: no idea how this can be null, but I have a savegame from /u/Elkad, where this is the case
                if(empire.tech().topControlEnvironmentTech() == null)
                    additional -= counts[i];
                else if(empire.shipLab().design(i).colonySpecial().tech().level == empire.tech().topControlEnvironmentTech().level
                        || empire.ignoresPlanetEnvironment())
                    additional -= counts[i];
                //System.out.println("\n"+empire.name()+" available: "+counts[i]+" "+empire.shipLab().design(i).name());
            }
        }
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" after substracting the already existing ones: "+additional);
        additional = max((float)additional, 0);
        additionalColonizersToBuild = (int)Math.ceil(additional);
        return additionalColonizersToBuild;
    }
    public float colonizationProbability(StarSystem sys)
    {
        if(sys.orbitingFleets().size() == 1)
        {
            for(ShipFleet fl : sys.orbitingFleets())
            {
                if(fl.isArmed() && fl.empire() == empire)
                    return 1;
            }
        }
        float myProduction = empire.totalPlanetaryProduction();
        float myDistance = colonyCenter(empire).distanceTo(sys);
        float myScore = myProduction / myDistance;
        float totalScore = myScore;
        for(Empire emp : empire.contactedEmpires())
        {
            if(empire.sv.planetType(sys.id) != null && !emp.canColonize(empire.sv.planetType(sys.id)))
                continue;
            float currentProd = emp.totalPlanetaryProduction();
            float currentDistance = colonyCenter(emp).distanceTo(sys);
            totalScore += currentProd / currentDistance;
        }
        float colProb = myScore / totalScore;
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" colonization-probability for "+empire.sv.name(sys.id)+": "+colProb);
        return colProb;
    }
    public int fightersToBuild()
    {
        int fighterNeed = 0;
        if(empire.hasAnyContact())
        {
            fighterNeed = empire.allColonizedSystems().size();
        }
        int[] counts = galaxy().ships.shipDesignCounts(empire.id);
        for (int i=0;i<counts.length;i++) 
        {
            if(empire.shipLab().design(i).isArmedForShipCombat())
                fighterNeed -= counts[i];
        }
        return fighterNeed;
    }
    @Override
    public boolean strongEnoughToAttack()
    {
        float attackThreshold = empire.totalPlanetaryProduction();
        if(totalArmedFleetCost < 0)
        {
            int[] counts = galaxy().ships.shipDesignCounts(empire.id);
            for (int i=0;i<ShipDesignLab.MAX_DESIGNS; i++) {
                ShipDesign d = empire.shipLab().design(i);
                if (d.active() && d.isArmed() && !d.isColonyShip()) 
                    totalArmedFleetCost += (counts[i] * d.cost());
            }
        }
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" "+totalArmedFleetCost+" / "+attackThreshold+" "+" milRank: "+empire.diplomatAI().militaryRank(empire)+" popcaprank: "+empire.diplomatAI().popCapRank());
        if(totalArmedFleetCost > attackThreshold && empire.diplomatAI().militaryRank(empire, false) <= empire.diplomatAI().popCapRank(empire, false))
            return true;
        return false;
    }
    @Override
    public boolean allowedToBomb(StarSystem sys) { 
        Empire emp = sys.empire();
        if(empire.enemies().contains(emp))
        {
            if(empire.transportsInTransit(sys) > troopsNecessaryToTakePlanet(empire.viewForEmpire(emp), sys) + expectedEnemyTransportKillPower(sys) * (1 - empire.fleetCommanderAI().bridgeHeadConfidence(sys)))
                return false;
            return true;
        }
        return false;
    }
    @Override
    public boolean isInvader()
    {
        if(empire.race().groundAttackBonus() > 0 || empire.race().growthRateMod() > 1)
            return true;
        return false;
    }
    @Override
    public boolean isRusher()
    {
        if(empire.race().shipAttackBonus() > 0 
                || empire.race().shipDefenseBonus() > 0 
                || isInvader()
                || empire.race().spyInfiltrationAdj() > 0)
            return true;
        return false;
    }
    @Override
    public boolean isExpander()
    {
        if(empire.race().ignoresPlanetEnvironment() || empire.race().growthRateMod() > 1)
            return true;
        return false;
    }
    @Override
    public boolean isSpy()
    {
        if(empire.race().spyInfiltrationAdj() > 0 || empire.leader().isTechnologist() || empire.leader().isPacifist())
            return true;
        return false;
    }
    @Override
    public boolean isTrader()
    {
        if(empire.race().tradePctBonus() > 0 || empire.leader().isDiplomat() || empire.leader().isPacifist())
            return true;
        return false;
    }
    @Override
    public int minTransportSize()
    {
        return 1;
    }
    @Override
    public Location fleetCenter(Empire emp)
    {
        float x = 0;
        float y = 0;
        float totalValue = 0;
        for(ShipFleet fleet: emp.allFleets())
        {
            x += fleet.x() * fleet.bcValue();
            y += fleet.y() * fleet.bcValue();
            totalValue += fleet.bcValue();
        }
        x /= totalValue;
        y /= totalValue;
        Location center = new Location(x, y);
        if(totalValue == 0)
            center = colonyCenter(emp);
        return center;
    }
    @Override
    public Location colonyCenter(Empire emp)
    {
        float x = 0;
        float y = 0;
        float totalPopCap = 0;
        for(StarSystem sys: emp.allColonizedSystems())
        {
            x += sys.x() * sys.colony().population();
            y += sys.y() * sys.colony().population();
            totalPopCap += sys.colony().population();
        }
        x /= totalPopCap;
        y /= totalPopCap;
        Location center = new Location(x, y);
        return center;
    }
    public Location uncolonizedCenter(Empire emp)
    {
        if(updatedUncolonizedCenterThisTurn)
            return uncolonizedCenter;
        float x = 0;
        float y = 0;
        float totalPopCap = 0;
        for(StarSystem sys: emp.uncolonizedPlanetsInRange(empire.shipDesignerAI().BestDesignToColonize().range()))
        {
            x += sys.x() * sys.planet().currentSize();
            y += sys.y() * sys.planet().currentSize();
            totalPopCap += sys.planet().currentSize();
        }
        if(totalPopCap > 0)
        {
            x /= totalPopCap;
            y /= totalPopCap;
        }
        Location center = new Location(x, y);
        if(center.x() == 0 && center.y() == 0)
            center = colonyCenter(emp);
        uncolonizedCenter = center;
        updatedUncolonizedCenterThisTurn = true;
        return center; 
    }
    @Override
    public boolean needScoutRepellers(boolean potential)
    {
        if(empire.tech().topFuelRangeTech().unlimited == true || empire.scanPlanets() || !empire.shipLab().needScouts)
            return false;
        if((empire.enemies().isEmpty() && !empire.enemyFleets().isEmpty()) || potential)
            return true;
        return false;
    }
    @Override
    public boolean sensePotentialAttack()
    {
        boolean senseDanger = false;
        for(Ship sh : empire.visibleShips())
        {
            if(empire.aggressiveWith(sh.empId()))
            {
                if(!sh.nullDest() && galaxy().system(sh.destSysId()).empire() == empire)
                {
                    if(sh.isTransport())
                    {
                        senseDanger = true;
                        break;
                    }
                    else
                    {
                        ShipFleet sf = (ShipFleet)sh;
                        if(sf.isArmed())
                        {
                            senseDanger = true;
                            break;
                        }
                    }
                }
            }
        }
        if(!senseDanger)
        {
            for(Empire contact : empire.contactedEmpires())
            {
                if(!contact.inShipRange(empire.id))
                    continue;
                if(contact.atWar())
                    continue;
                if(contact.alliedWith(empire.id))
                    continue;
                float bestScoreForContactToAttack = 0;
                Empire bestTargetOfContact = null;
                for(Empire contactOfContact : contact.contactedEmpires())
                {
                    if(!contact.inShipRange(contactOfContact.id))
                        continue;
                    if(contactOfContact.alliedWith(contact.id))
                        continue;
                    float score = 1 / fleetCenter(contact).distanceTo(colonyCenter(contactOfContact));
                    if(score > bestScoreForContactToAttack)
                    {
                        bestTargetOfContact = contactOfContact;
                        bestScoreForContactToAttack = score;
                    }
                }
                if(bestTargetOfContact == empire && contact.militaryPowerLevel() > smartPowerLevel())
                {
                    senseDanger = true;
                    break;
                }
            }
        }
        /*if(senseDanger)
            System.out.println(galaxy().currentTurn()+" "+empire.name()+" fears being attacked.");*/
        return senseDanger;
    }
    @Override
    public Empire biggestThreat()
    {
        Empire biggestThreat = empire;
        float highestThreat = 0;
        for(Empire emp : empire.contactedEmpires())
        {
            if(!empire.enemies().isEmpty() && !empire.enemies().contains(emp))
                continue;
            if(emp == empire)
                continue;
            if(empire.alliedWith(emp.id))
                continue;
            float threat = emp.powerLevel(emp) * 1 / (fleetCenter(emp).distanceTo(colonyCenter(empire)));
            //System.out.println(galaxy().currentTurn()+" "+empire.name()+" fear-level of: "+emp.name()+": "+threat);
            if(!empire.inShipRange(emp.id))
                threat /= 2;
            if(threat > highestThreat)
            {
                highestThreat = threat;
                biggestThreat = emp;
            }
        }
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" highest Threat: "+biggestThreat.name()+": "+highestThreat);
        return biggestThreat;
    }
    @Override
    public float absolution() { return 1f; }
    @Override
    public float smartPowerLevel()
    {
        if(smartPower > -1)
            return smartPower;
        float power = 0;
        int[] counts = galaxy().ships.shipDesignCounts(empire.id);
        for (int i=0;i<ShipDesignLab.MAX_DESIGNS; i++) {
            ShipDesign d = empire.shipLab().design(i);
            if (d.active() && d.isArmed() && !d.isColonyShip()) 
            {
                float keepScore = (1 - d.availableSpace()/d.totalSpace()) * (float)d.engine().warp() / (float)empire.shipLab().fastestEngine().warp();
                keepScore *= keepScore;
                //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+d.name()+" keepScore: "+keepScore);
                power += (counts[i] *d.hullPoints() * keepScore);
            }
        }
        power *= empire.tech().avgTechLevel();
        smartPower = power;
        return smartPower;
    }
    @Override
    public float highestProdScore()
    {
        if(highestProdScore > 0)
            return highestProdScore;
        float highest = 0;
        for(StarSystem sys: empire.allColonizedSystems())
        {
            float curr = empire.governorAI().productionScore(sys);
            if(curr > highest)
                highest = curr;
        }
        highestProdScore = highest;
        return highestProdScore;
    }
    @Override
    public float gameProgress()
    {
        float fastestVictory = Float.MAX_VALUE;
        float myOwnedPerc = 0;
        float highestOwnedPerc = 0;
        for(Empire emp: galaxy().activeEmpires())
        {
            if(emp == empire || empire.contacts().contains(empire.viewForEmpire(emp)))
            {
                float ownedPerc = (float)emp.numColonies() / (float)galaxy().systemCount;
                ownedPerc *= 3.0f/2.0f;
                if(emp == empire)
                    myOwnedPerc = ownedPerc;
                if(ownedPerc > highestOwnedPerc)
                    highestOwnedPerc = ownedPerc;
                float victoryTurn = galaxy().currentTurn() / ownedPerc;
                //System.out.println(galaxy().currentTurn()+" "+empire.name()+" thinks "+emp.name()+" will win at turn "+victoryTurn);
                if(victoryTurn < fastestVictory)
                    fastestVictory = victoryTurn;
            }
        }
        float defeatTurn = fastestVictory * myOwnedPerc / highestOwnedPerc;
        float gameEndTurn = min(fastestVictory, defeatTurn);
        float progress = galaxy().currentTurn() / gameEndTurn;
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" thinks the game will end at turn "+gameEndTurn+" current Progress: "+progress);
        return progress;
    }
}
