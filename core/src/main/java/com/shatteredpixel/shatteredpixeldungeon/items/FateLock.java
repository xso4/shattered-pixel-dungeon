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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.Game;

import java.util.ArrayList;

public class FateLock extends Item {

	private static final String AC_RESET = "RESET";
	{
		image = ItemSpriteSheet.BEACON;
		unique = true;
		identify();
		defaultAction = AC_RESET;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_RESET);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		return actions;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (AC_RESET.equals(action)) {
			resetQuestsIfPresent();
			Level.beforeTransition();
			InterlevelScene.mode = InterlevelScene.Mode.RESET;
			Game.switchScene(InterlevelScene.class);
		}
	}

	private void resetQuestsIfPresent() {
		boolean resetGhost = false;
		boolean resetWandmaker = false;
		boolean resetBlacksmith = false;
		boolean resetImp = false;

		for (Mob mob : Dungeon.level.mobs) {
			if (!resetGhost && mob instanceof Ghost) {
				Ghost.Quest.reset();
				resetGhost = true;
			} else if (!resetWandmaker && mob instanceof Wandmaker) {
				Wandmaker.Quest.reset();
				resetWandmaker = true;
			} else if (!resetBlacksmith && mob instanceof Blacksmith) {
				Blacksmith.Quest.reset();
				resetBlacksmith = true;
			} else if (!resetImp && mob instanceof Imp) {
				Imp.Quest.reset();
				resetImp = true;
			}
		}
	}
}
