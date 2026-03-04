/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.mod;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;

public final class ModHooks {

	private ModHooks() {
	}

	public static boolean blockEarnExp(Hero hero) {
		return ModInventory.isExpLocked();
	}

	public static boolean blockLevelUp(Hero hero) {
		if (!ModInventory.isLevelLocked()) {
			return false;
		}
		if (hero != null && hero.exp >= hero.maxExp()) {
			hero.exp = hero.maxExp() - 1;
		}
		return true;
	}

	public static void applyHpLock(Hero hero) {
		ModInventory.applyHpLock(hero);
	}

	public static boolean shouldBlockHeroDamage(Char target) {
		return target instanceof Hero && ModInventory.isHpLocked();
	}

	public static int baseAccuracy(Hero hero) {
		return hero == null ? 0 : hero.baseAccuracy();
	}

	public static int baseEvasion(Hero hero) {
		return hero == null ? 0 : hero.baseEvasion();
	}

	public static int finalAccuracy(Hero hero) {
		return hero == null ? 0 : hero.attackSkill(null);
	}

	public static int finalEvasion(Hero hero) {
		return hero == null ? 0 : hero.defenseSkill(null);
	}

	public static void adjustBaseAccuracy(Hero hero, int delta) {
		if (hero == null) {
			return;
		}
		hero.adjustBaseAccuracy(delta);
	}

	public static void adjustBaseEvasion(Hero hero, int delta) {
		if (hero == null) {
			return;
		}
		hero.adjustBaseEvasion(delta);
	}

	public static int talentPointsAvailable(Hero hero, int tier) {
		if (hero == null) {
			return 0;
		}
		boolean tierLocked = hero.lvl < (Talent.tierLevelThresholds[tier] - 1)
				|| (tier == 3 && hero.subClass == HeroSubClass.NONE)
				|| (tier == 4 && hero.armorAbility == null);
		if (tierLocked && !ModInventory.isTalentTierUnlocked()) {
			return 0;
		}
		int spent = hero.talentPointsSpent(tier);
		int bonus = bonusTalentPoints(hero, tier);
		if (ModInventory.isTalentPointsUnlocked()) {
			int totalNeeded = 0;
			if (hero.talents != null && tier - 1 >= 0 && tier - 1 < hero.talents.size()) {
				for (Talent talent : hero.talents.get(tier - 1).keySet()) {
					totalNeeded += talent.maxPoints();
				}
			}
			return Math.max(0, totalNeeded - spent);
		} else if (hero.lvl >= Talent.tierLevelThresholds[tier+1]){
			return Talent.tierLevelThresholds[tier+1] - Talent.tierLevelThresholds[tier] - spent + bonus;
		} else {
			return Math.max(0, 1 + hero.lvl - Talent.tierLevelThresholds[tier] - spent + bonus);
		}
	}

	public static int bonusTalentPoints(Hero hero, int tier) {
		if (hero == null) {
			return 0;
		}
		boolean tierLocked = hero.lvl < (Talent.tierLevelThresholds[tier]-1)
				|| (tier == 3 && hero.subClass == HeroSubClass.NONE)
				|| (tier == 4 && hero.armorAbility == null);
		if (tierLocked && !ModInventory.isTalentTierUnlocked()) {
			return 0;
		} else if (hero.buff(PotionOfDivineInspiration.DivineInspirationTracker.class) != null
				&& hero.buff(PotionOfDivineInspiration.DivineInspirationTracker.class).isBoosted(tier)) {
			return 2;
		} else {
			return 0;
		}
	}
}
