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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.mod.ModInventory;
import com.shatteredpixel.shatteredpixeldungeon.mod.ModHooks;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.TextInput;

public class WndModInventory extends WndTabbed {

	private static final int WIDTH_P = 160;
	private static final int WIDTH_L = 220;
	private static final int INPUT_HEIGHT = 14;
	private static final int BTN_HEIGHT = 16;
	private static final int GAP = 1;
	private static final int MARGIN = 2;

	private enum Mode {
		ITEM,
		QTY
	}

	private final RenderedTextBlock body;
	private final RenderedTextBlock label;
	private final TextInput textInput;
	private final RedButton primaryBtn;
	private final RedButton addBtn;
	private final CheckBox lockExpBox;
	private final CheckBox lockLevelBox;
	private final CheckBox lockHpBox;
	private final CheckBox unlockTierBox;
	private final CheckBox unlockPointsBox;
	private final CheckBox bagCapacityBox;
	private final RedButton journalBtn;
	private final RenderedTextBlock accLabel;
	private final RenderedTextBlock evaLabel;
	private final RedButton accMinusBtn;
	private final RedButton accPlusBtn;
	private final RedButton evaMinusBtn;
	private final RedButton evaPlusBtn;
	private final PointerArea inputArea;
	private final int windowWidth;

	private Tab itemTab;
	private Tab qtyTab;

	private Mode mode = Mode.ITEM;
	private String itemValue = "";
	private String qtyValue = "1";

	public WndModInventory() {
		super();

		offset(0, 0);

		int maxWidth = (int)(PixelScene.uiCamera.width - chrome.marginHor() - 2);
		int desiredWidth = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		if (maxWidth > 0) {
			desiredWidth = Math.min(desiredWidth, maxWidth);
		}
		windowWidth = desiredWidth;

		body = PixelScene.renderTextBlock("", 6);
		body.maxWidth(windowWidth - MARGIN * 2);
		add(body);

		label = PixelScene.renderTextBlock("", 6);
		label.maxWidth(windowWidth - MARGIN * 2);
		add(label);

		int textSize = (int) PixelScene.uiCamera.zoom * 9;
		textInput = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, textSize);
		textInput.setMaxLength(120);
		add(textInput);

		primaryBtn = new RedButton(Messages.get(this, "switch")) {
			@Override
			protected void onClick() {
				if (mode == Mode.QTY) {
					switchToItem();
				} else {
					switchToQty();
				}
			}
		};
		add(primaryBtn);

		addBtn = new RedButton(Messages.get(this, "confirm")) {
			@Override
			protected void onClick() {
				applyInput();
			}
		};
		add(addBtn);

		lockExpBox = new CheckBox(Messages.get(this, "lock_exp")) {
			@Override
			protected void onClick() {
				super.onClick();
				ModInventory.setExpLocked(checked());
			}
		};
		lockExpBox.checked(ModInventory.isExpLocked());
		add(lockExpBox);

		lockLevelBox = new CheckBox(Messages.get(this, "lock_level")) {
			@Override
			protected void onClick() {
				super.onClick();
				ModInventory.setLevelLocked(checked());
			}
		};
		lockLevelBox.checked(ModInventory.isLevelLocked());
		add(lockLevelBox);

		lockHpBox = new CheckBox(Messages.get(this, "lock_hp")) {
			@Override
			protected void onClick() {
				super.onClick();
				ModInventory.setHpLocked(checked());
			}
		};
		lockHpBox.checked(ModInventory.isHpLocked());
		add(lockHpBox);

		unlockTierBox = new CheckBox(Messages.get(this, "unlock_talent_tiers")) {
			@Override
			protected void onClick() {
				super.onClick();
				ModInventory.setTalentTierUnlocked(checked());
			}
		};
		unlockTierBox.checked(ModInventory.isTalentTierUnlocked());
		add(unlockTierBox);

		unlockPointsBox = new CheckBox(Messages.get(this, "unlock_talent_points")) {
			@Override
			protected void onClick() {
				super.onClick();
				ModInventory.setTalentPointsUnlocked(checked());
			}
		};
		unlockPointsBox.checked(ModInventory.isTalentPointsUnlocked());
		add(unlockPointsBox);

		bagCapacityBox = new CheckBox(Messages.get(this, "bag_capacity")) {
			@Override
			protected void onClick() {
				super.onClick();
				ModInventory.setBagCapacityBoosted(checked());
			}
		};
		bagCapacityBox.checked(ModInventory.isBagCapacityBoosted());
		add(bagCapacityBox);

		journalBtn = new RedButton(Messages.get(this, "journal_unlock")) {
			@Override
			protected void onClick() {
				if (Dungeon.hero == null || !Dungeon.hero.isAlive()) {
					return;
				}
				GameScene.show(new WndOptions(
						Messages.get(WndModInventory.this, "journal_confirm_title"),
						Messages.get(WndModInventory.this, "journal_confirm_body"),
						Messages.get(WndModInventory.this, "journal_confirm_yes"),
						Messages.get(WndModInventory.this, "journal_confirm_no")) {
					@Override
					protected void onSelect(int index) {
						if (index == 0) {
							Journal.unlockAll();
							GameScene.show(new WndMessage(Messages.get(WndModInventory.this, "journal_unlocked")));
						}
					}
				});
			}
		};
		add(journalBtn);

		accLabel = PixelScene.renderTextBlock("", 6);
		accLabel.maxWidth(windowWidth);
		add(accLabel);

		evaLabel = PixelScene.renderTextBlock("", 6);
		evaLabel.maxWidth(windowWidth);
		add(evaLabel);

		accMinusBtn = new RedButton("-") {
			@Override
			protected void onClick() {
				adjustBaseAccuracy(-1);
			}
		};
		add(accMinusBtn);

		accPlusBtn = new RedButton("+") {
			@Override
			protected void onClick() {
				adjustBaseAccuracy(1);
			}
		};
		add(accPlusBtn);

		evaMinusBtn = new RedButton("-") {
			@Override
			protected void onClick() {
				adjustBaseEvasion(-1);
			}
		};
		add(evaMinusBtn);

		evaPlusBtn = new RedButton("+") {
			@Override
			protected void onClick() {
				adjustBaseEvasion(1);
			}
		};
		add(evaPlusBtn);

		inputArea = makeInputArea(0, 0, 0, 0);
		add(inputArea);

		itemTab = add(new LabeledTab(Messages.get(this, "item_title")));
		qtyTab = add(new LabeledTab(Messages.get(this, "qty_title")));
		layoutTabs();

		setMode(Mode.ITEM);
		super.select(itemTab);
		updateStatText();
		PointerEvent.clearKeyboardThisPress = false;
	}

	@Override
	protected void onClick(Tab tab) {
		if (tab == itemTab) {
			switchToItem();
		} else if (tab == qtyTab) {
			switchToQty();
		} else {
			super.onClick(tab);
		}
	}

	private void switchToItem() {
		if (mode == Mode.ITEM) {
			return;
		}
		saveCurrentInput();
		setMode(Mode.ITEM);
		super.select(itemTab);
	}

	private void switchToQty() {
		if (mode == Mode.QTY) {
			return;
		}
		saveCurrentInput();
		setMode(Mode.QTY);
		super.select(qtyTab);
	}

	private void setMode(Mode target) {
		mode = target;
		if (mode == Mode.QTY) {
			body.text(Messages.get(this, "qty_body"), windowWidth);
			label.text(Messages.get(this, "qty_title"), windowWidth);
			textInput.setMaxLength(9);
			textInput.setText(qtyValue == null ? "1" : qtyValue);
			primaryBtn.text(Messages.get(this, "switch"));
		} else {
			body.text(Messages.get(this, "item_body"), windowWidth);
			label.text(Messages.get(this, "item_title"), windowWidth);
			textInput.setMaxLength(120);
			textInput.setText(itemValue == null ? "" : itemValue);
			primaryBtn.text(Messages.get(this, "switch"));
		}
		layoutForMode();
	}

	private void layoutForMode() {
		int pos = 0;
		int contentWidth = windowWidth - MARGIN * 2;
		int half = (contentWidth - GAP) / 2;

		lockExpBox.setRect(MARGIN, pos, half, BTN_HEIGHT);
		lockLevelBox.setRect(MARGIN + half + GAP, pos, contentWidth - half - GAP, BTN_HEIGHT);
		pos += BTN_HEIGHT + GAP;
		lockHpBox.setRect(MARGIN, pos, half, BTN_HEIGHT);
		unlockTierBox.setRect(MARGIN + half + GAP, pos, contentWidth - half - GAP, BTN_HEIGHT);
		pos += BTN_HEIGHT + GAP;
		unlockPointsBox.setRect(MARGIN, pos, half, BTN_HEIGHT);
		bagCapacityBox.setRect(MARGIN + half + GAP, pos, contentWidth - half - GAP, BTN_HEIGHT);
		pos += BTN_HEIGHT + GAP;

		body.setPos(MARGIN, pos);
		pos = (int) body.bottom() + GAP;
		label.setPos(MARGIN, pos);
		pos = (int) label.bottom() + GAP;

		int inputTop = pos;
		textInput.setRect(MARGIN, inputTop, half, INPUT_HEIGHT);
		primaryBtn.setRect(MARGIN + half + GAP, inputTop, contentWidth - half - GAP, INPUT_HEIGHT);
		inputArea.x = textInput.left();
		inputArea.y = textInput.top();
		inputArea.width = textInput.width();
		inputArea.height = textInput.height();
		pos += INPUT_HEIGHT + GAP;

		addBtn.setRect(MARGIN, pos, contentWidth, BTN_HEIGHT);
		pos += BTN_HEIGHT + GAP;

		updateStatText();
		accLabel.setPos(MARGIN, pos);
		pos = (int) accLabel.bottom() + GAP;
		accMinusBtn.setRect(MARGIN, pos, half, BTN_HEIGHT);
		accPlusBtn.setRect(MARGIN + half + GAP, pos, contentWidth - half - GAP, BTN_HEIGHT);
		pos += BTN_HEIGHT + GAP;

		evaLabel.setPos(MARGIN, pos);
		pos = (int) evaLabel.bottom() + GAP;
		evaMinusBtn.setRect(MARGIN, pos, half, BTN_HEIGHT);
		evaPlusBtn.setRect(MARGIN + half + GAP, pos, contentWidth - half - GAP, BTN_HEIGHT);
		pos += BTN_HEIGHT + GAP;

		journalBtn.setRect(MARGIN, pos, contentWidth, BTN_HEIGHT);
		pos += BTN_HEIGHT;

		resize(windowWidth, pos);
		layoutTabs();
	}

	private void adjustBaseAccuracy(int delta) {
		ModHooks.adjustBaseAccuracy(Dungeon.hero, delta);
		updateStatText();
	}

	private void adjustBaseEvasion(int delta) {
		ModHooks.adjustBaseEvasion(Dungeon.hero, delta);
		updateStatText();
	}

	private void updateStatText() {
		if (Dungeon.hero == null) {
			accLabel.text("", windowWidth);
			evaLabel.text("", windowWidth);
			return;
		}
		int baseAcc = ModHooks.baseAccuracy(Dungeon.hero);
		int finalAcc = ModHooks.finalAccuracy(Dungeon.hero);
		int baseEva = ModHooks.baseEvasion(Dungeon.hero);
		int finalEva = ModHooks.finalEvasion(Dungeon.hero);
		accLabel.text(Messages.get(this, "acc_label", baseAcc, finalAcc), windowWidth);
		evaLabel.text(Messages.get(this, "eva_label", baseEva, finalEva), windowWidth);
	}

	private void saveCurrentInput() {
		String text = textInput.getText() == null ? "" : textInput.getText();
		if (mode == Mode.ITEM) {
			itemValue = text;
		} else {
			qtyValue = text;
		}
	}

	private void applyInput() {
		saveCurrentInput();
		String itemId = itemValue == null ? "" : itemValue.trim();
		if (itemId.isEmpty()) {
			GameScene.show(new WndMessage(Messages.get(this, "bad_item")));
			return;
		}

		int quantity;
		try {
			quantity = Integer.parseInt((qtyValue == null ? "" : qtyValue).trim());
		} catch (NumberFormatException ex) {
			GameScene.show(new WndMessage(Messages.get(this, "bad_qty")));
			return;
		}

		if (quantity <= 0) {
			GameScene.show(new WndMessage(Messages.get(this, "bad_qty")));
			return;
		}

		Class<? extends Item> itemClass = ModInventory.resolveItemClass(itemId);
		if (itemClass == null) {
			GameScene.show(new WndMessage(Messages.get(this, "bad_item")));
			return;
		}

		int added = ModInventory.addToBackpack(itemClass, quantity, true);
		if (added <= 0) {
			GameScene.show(new WndMessage(Messages.get(this, "failed")));
			return;
		}
		GameScene.show(new WndMessage(Messages.get(this, "added", added)));
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		if (textInput != null) {
			textInput.setRect(textInput.left(), textInput.top(), textInput.width(), textInput.height());
		}
	}

	private PointerArea makeInputArea(float x, float y, float width, float height) {
		PointerArea area = new PointerArea(x, y, width, height) {
			@Override
			protected void onPointerDown(PointerEvent event) {
				PointerEvent.clearKeyboardThisPress = false;
			}

			@Override
			protected void onPointerUp(PointerEvent event) {
				PointerEvent.clearKeyboardThisPress = false;
			}
		};
		area.blockLevel = PointerArea.NEVER_BLOCK;
		area.givePointerPriority();
		return area;
	}
}
