import java.util.Random;

public class AveragedZoneSimulation extends ZoneSimulation {

    private final static int zoneSimulationRepeatAmount = 1000;

    @Override
    public double getExpectedTime(final double cellDelay,
            final double attackDelay, final double damageFactor,
            final double critChance, final double critDamage,
            final double okFactor, final double corruptMod,
            final int corruptionStart, final int zone) {
        double acc = 0;
        for (int x = 0; x < zoneSimulationRepeatAmount; x++) {
            EnemyType[] zoneArray = createZone(TrimpsSimulation.getNumCorrupt(zone, corruptionStart));
            double res = 0;
            int cell = 1;
            Random random = new Random();
            double hp = getHPModifier(cell, zoneArray[cell - 1], corruptMod)
                    * getHPFactor(zoneArray[cell - 1], random.nextDouble());
            while (cell <= 100) {
                // +1 hit
                // zoneStats[cell-1][0] += 1;
                boolean crit = random.nextDouble() < critChance;
                double damage = (crit) ? damageFactor * critDamage
                        : damageFactor;
                damage *= (1 + 0.2 * random.nextDouble());
                boolean dodge = zoneArray[cell - 1] == EnemyType.AGILITY
                        && random.nextDouble() < 0.3;
                if (dodge) {
                    res += attackDelay;
                    continue;
                }
                if (damage >= hp) {
                    cell++;
                    damage -= hp;
                    double overkillDamage = damage * okFactor;
                    // +OK dmg
                    // zoneStats[cell-2][crit ? 5 : 3] += 1;
                    // zoneStats[cell-2][crit ? 4 : 2] += overkillDamage;
                    if (cell == 101) {
                        res += cellDelay;
                        break;
                    } else {
                        hp = getHPModifier(cell, zoneArray[cell - 1],
                                corruptMod)
                                * getHPFactor(zoneArray[cell - 1],
                                        random.nextDouble());
                    }
                    hp -= overkillDamage;
                    if (hp <= 0) {
                        // overkilled this cell, so next cell is fresh
                        // zoneStats[cell-1][6] += 1;
                        cell++;
                        if (cell == 101) {
                            res += cellDelay;
                            break;
                        } else {
                            res += cellDelay;
                            hp = getHPModifier(cell, zoneArray[cell - 1],
                                    corruptMod)
                                    * getHPFactor(zoneArray[cell - 1],
                                            random.nextDouble());
                        }
                    } else {
                        // didn't overkill, so count this cell for expHits
                        // zoneStats[cell-1][1] += 1;
                        res += cellDelay;
                    }
                } else {
                    res += attackDelay;
                    hp -= damage;
                }
            }
            acc += res;
        }
        return acc/zoneSimulationRepeatAmount;
    }

    // TODO do normal remove better
    private double getHPModifier(final double pCell, final EnemyType enemyType,
            final double corruptMod) {
        // TODO properly implement
        double cellMod = (0.5 + 0.8 * (pCell / 100d)) / 0.508;
        if (enemyType == EnemyType.NORMAL) {
            return cellMod / corruptMod;
        }
        if (pCell < 100) {
            return cellMod * (enemyType == EnemyType.TOUGH ? 5 : 1);
        } else {
            return cellMod * 6;
        }
    }

    // TODO look over and optimize
    private double getHPFactor(final EnemyType enemyType, final double random) {
        if (enemyType != EnemyType.NORMAL) {
            return 1.0;
        }
        double r = random;
        double mainImpProb = (1 - 0.004666666 - 0.15) / 8;
        if (r < 0.004666666) {
            return 1.6;
        }
        r -= 0.004666666;
        if (r < 0.15) {
            return 1;
        }
        r -= 0.15;
        if (r < mainImpProb * 2) {
            return 0.7;
        }
        r -= mainImpProb * 2;
        if (r < mainImpProb * 2) {
            return 1.3;
        }
        r -= mainImpProb * 2;
        if (r < mainImpProb) {
            return 1;
        }
        r -= mainImpProb;
        if (r < mainImpProb) {
            return 0.8;
        }
        r -= mainImpProb;
        if (r < mainImpProb) {
            return 1.1;
        }
        r -= mainImpProb;
        if (r < mainImpProb) {
            return 1.5;
        }
        r -= mainImpProb;
        return 1;
    }

    private EnemyType[] createZone(int numberCorrupted) {
        int numberNormal = 99 - numberCorrupted;
        EnemyType[] result = new EnemyType[100];
        Random random = new Random();
        result[99] = EnemyType.IMPROBABILITY;
        for (int x = 0; x < 99; x++) {
            int r = random.nextInt(numberNormal + numberCorrupted);
            if (r < numberNormal) {
                result[x] = EnemyType.NORMAL;
                numberNormal--;
            } else {
                r = random.nextInt(6);
                if (r == 0) {
                    result[x] = EnemyType.TOUGH;
                } else if (r == 1) {
                    result[x] = EnemyType.AGILITY;
                } else {
                    result[x] = EnemyType.COORUPTED;
                }
                numberCorrupted--;
            }
        }
        return result;
    }

}
