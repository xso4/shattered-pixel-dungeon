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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.I18NBundle;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public final class ModInventory {

	private static final List<Grant> STARTUP_GRANTS = new ArrayList<>();
	private static HashMap<String, Class<? extends Item>> nameIndex;
	private static I18NBundle englishItems;
	private static I18NBundle chineseItems;
	private static boolean lockExp;
	private static boolean lockLevel;
	private static boolean lockHp;
	private static boolean unlockTalentTiers;
	private static boolean unlockTalentPoints;
	private static boolean bagCapacityBoost;
	private static int lockedHp = -1;

	private ModInventory() {
	}

	public static boolean isExpLocked() {
		return lockExp;
	}

	public static void setExpLocked(boolean locked) {
		lockExp = locked;
	}

	public static boolean isLevelLocked() {
		return lockLevel;
	}

	public static void setLevelLocked(boolean locked) {
		lockLevel = locked;
	}

	public static boolean isHpLocked() {
		return lockHp;
	}

	public static void setHpLocked(boolean locked) {
		lockHp = locked;
		if (!lockHp) {
			lockedHp = -1;
			return;
		}
		if (Dungeon.hero != null) {
			lockedHp = Dungeon.hero.HP;
		}
	}

	public static boolean isTalentTierUnlocked() {
		return unlockTalentTiers;
	}

	public static void setTalentTierUnlocked(boolean unlocked) {
		unlockTalentTiers = unlocked;
	}

	public static boolean isTalentPointsUnlocked() {
		return unlockTalentPoints;
	}

	public static void setTalentPointsUnlocked(boolean unlocked) {
		unlockTalentPoints = unlocked;
	}

	public static boolean isBagCapacityBoosted() {
		return bagCapacityBoost;
	}

	public static void setBagCapacityBoosted(boolean boosted) {
		bagCapacityBoost = boosted;
	}

	public static int bagCapacityOverride(int defaultValue) {
		return bagCapacityBoost ? 30 : defaultValue;
	}

	public static void applyHpLock(Hero hero) {
		if (!lockHp || hero == null) {
			return;
		}
		if (lockedHp < 0) {
			lockedHp = hero.HP;
		}
		if (lockedHp > hero.HT) {
			lockedHp = hero.HT;
		}
		if (lockedHp < 0) {
			lockedHp = 0;
		}
		hero.HP = (int)Math.min(hero.HT, Math.max(0, lockedHp));
	}

	public static void queueStartupGrant(Class<? extends Item> itemClass, int quantity, boolean identify) {
		if (itemClass == null || quantity <= 0) {
			return;
		}
		STARTUP_GRANTS.add(new Grant(itemClass, quantity, identify));
	}

	public static void queueStartupGrant(String fqcn, int quantity, boolean identify) {
		Class<? extends Item> itemClass = resolveItemClass(fqcn);
		if (itemClass != null) {
			queueStartupGrant(itemClass, quantity, identify);
		}
	}

	public static int applyStartupGrants() {
		if (Dungeon.hero == null || Dungeon.hero.belongings == null) {
			return 0;
		}
		int added = 0;
		for (Grant grant : STARTUP_GRANTS) {
			added += addToBackpack(grant.itemClass, grant.quantity, grant.identify);
		}
		STARTUP_GRANTS.clear();
		return added;
	}

	public static int addToBackpack(Class<? extends Item> itemClass, int quantity, boolean identify) {
		if (Dungeon.hero == null || Dungeon.hero.belongings == null) {
			return 0;
		}
		return addToBag(Dungeon.hero.belongings.backpack, itemClass, quantity, identify);
	}

	public static int addToBackpack(String itemId, int quantity, boolean identify) {
		Class<? extends Item> itemClass = resolveItemClass(itemId);
		if (itemClass == null) {
			return 0;
		}
		return addToBackpack(itemClass, quantity, identify);
	}

	public static int addToBag(Bag bag, Class<? extends Item> itemClass, int quantity, boolean identify) {
		if (bag == null || itemClass == null || quantity <= 0) {
			return 0;
		}

		Item first = Reflection.newInstance(itemClass);
		if (first == null) {
			return 0;
		}

		if (first.stackable) {
			if (identify) {
				first.identify();
			}
			first.quantity(quantity);
			return first.collect(bag) ? quantity : 0;
		}

		int added = 0;
		if (identify) {
			first.identify();
		}
		if (first.collect(bag)) {
			added++;
		} else {
			return 0;
		}

		for (int i = 1; i < quantity; i++) {
			Item item = Reflection.newInstance(itemClass);
			if (item == null) {
				break;
			}
			if (identify) {
				item.identify();
			}
			if (!item.collect(bag)) {
				break;
			}
			added++;
		}
		return added;
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends Item> resolveItemClass(String id) {
		if (id == null || id.trim().isEmpty()) {
			return null;
		}

		String trimmed = id.trim();
		if (trimmed.indexOf('.') >= 0) {
			try {
				Class<?> raw = Class.forName(trimmed);
				if (!Item.class.isAssignableFrom(raw)) {
					return null;
				}
				return (Class<? extends Item>) raw;
			} catch (ClassNotFoundException ex) {
				return null;
			}
		}

		ensureNameIndex();
		String key = normalize(trimmed);
		if (key.isEmpty()) {
			return null;
		}

		if (!nameIndex.containsKey(key)) {
			return null;
		}
		return nameIndex.get(key);
	}

	private static void ensureNameIndex() {
		if (nameIndex != null) {
			return;
		}
		nameIndex = new HashMap<>();
		for (Generator.Category category : Generator.Category.values()) {
			for (Class<?> raw : category.classes) {
				if (!Item.class.isAssignableFrom(raw)) {
					continue;
				}
				Class<? extends Item> cls = (Class<? extends Item>) raw;
				indexName(cls.getSimpleName(), cls);

				Item sample = Reflection.newInstance(cls);
				if (sample != null) {
					indexName(sample.name(), cls);
				}

				String englishName = englishNameFor(cls);
				if (englishName != null) {
					indexName(englishName, cls);
				}

				String chineseName = chineseNameFor(cls);
				if (chineseName != null) {
					indexName(chineseName, cls);
				}
			}
		}
		for (Catalog cat : Catalog.values()) {
			for (Class<?> raw : cat.items()) {
				if (!Item.class.isAssignableFrom(raw)) {
					continue;
				}
				@SuppressWarnings("unchecked")
				Class<? extends Item> cls = (Class<? extends Item>) raw;
				indexName(cls.getSimpleName(), cls);

				Item sample = Reflection.newInstance(cls);
				if (sample != null) {
					indexName(sample.name(), cls);
				}

				String englishName = englishNameFor(cls);
				if (englishName != null) {
					indexName(englishName, cls);
				}

				String chineseName = chineseNameFor(cls);
				if (chineseName != null) {
					indexName(chineseName, cls);
				}
			}
		}
	}

	private static void indexName(String name, Class<? extends Item> cls) {
		if (name == null || name.trim().isEmpty()) {
			return;
		}
		String key = normalize(name);
		if (key.isEmpty()) {
			return;
		}
		if (nameIndex.containsKey(key)) {
			Class<? extends Item> existing = nameIndex.get(key);
			if (existing != null && existing != cls) {
				nameIndex.put(key, null);
			}
			return;
		}
		nameIndex.put(key, cls);
	}

	private static String normalize(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}

	private static String englishNameFor(Class<? extends Item> cls) {
		try {
			I18NBundle bundle = englishItems();
			if (bundle == null) {
				return null;
			}
			String key = cls.getName()
					.replace("com.shatteredpixel.shatteredpixeldungeon.", "")
					.toLowerCase(Locale.ENGLISH) + ".name";
			String value = bundle.get(key);
			if (value == null || value.equals(key)) {
				return null;
			}
			return value;
		} catch (Exception ex) {
			return null;
		}
	}

	private static String chineseNameFor(Class<? extends Item> cls) {
		try {
			I18NBundle bundle = chineseItems();
			if (bundle == null) {
				return null;
			}
			String key = cls.getName()
					.replace("com.shatteredpixel.shatteredpixeldungeon.", "")
					.toLowerCase(Locale.ENGLISH) + ".name";
			String value = bundle.get(key);
			if (value == null || value.equals(key)) {
				return null;
			}
			return value;
		} catch (Exception ex) {
			return null;
		}
	}

	private static I18NBundle englishItems() {
		if (englishItems != null) {
			return englishItems;
		}
		englishItems = I18NBundle.createBundle(Gdx.files.internal(Assets.Messages.ITEMS), Locale.ROOT);
		return englishItems;
	}

	private static I18NBundle chineseItems() {
		if (chineseItems != null) {
			return chineseItems;
		}
		Locale locale = new Locale("zh");
		Languages lang = Messages.lang();
		if (lang != null && "zh".equalsIgnoreCase(lang.code())) {
			locale = new Locale(lang.code());
		}
		chineseItems = I18NBundle.createBundle(Gdx.files.internal(Assets.Messages.ITEMS), locale);
		return chineseItems;
	}

	private static final class Grant {
		private final Class<? extends Item> itemClass;
		private final int quantity;
		private final boolean identify;

		private Grant(Class<? extends Item> itemClass, int quantity, boolean identify) {
			this.itemClass = itemClass;
			this.quantity = quantity;
			this.identify = identify;
		}
	}
}
