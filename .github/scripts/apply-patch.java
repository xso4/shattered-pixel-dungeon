package com.shatteredpixel.patch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

class ApplyPatch {
	private static final Path DUNGEON = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Dungeon.java");
	private static final Path HERO_CLASS = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/HeroClass.java");
	private static final Path HERO = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java");
	private static final Path CHAR = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/Char.java");
	private static final Path WND_GAME = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndGame.java");
	private static final Path JOURNAL = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/journal/Journal.java");
	private static final Path BADGES = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Badges.java");
	private static final Path TEMPLATE_DIR = Paths.get(".github/scripts/templates");
	private static final Path FATE_LOCK = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/FateLock.java");
	private static final Path MOD_INVENTORY = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/mod/ModInventory.java");
	private static final Path MOD_HOOKS = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/mod/ModHooks.java");
	private static final Path WND_MOD_INVENTORY = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndModInventory.java");
	private static final Path FATE_LOCK_TEMPLATE = TEMPLATE_DIR.resolve("FateLock.java");
	private static final Path MOD_INVENTORY_TEMPLATE = TEMPLATE_DIR.resolve("ModInventory.java");
	private static final Path MOD_HOOKS_TEMPLATE = TEMPLATE_DIR.resolve("ModHooks.java");
	private static final Path WND_MOD_INVENTORY_TEMPLATE = TEMPLATE_DIR.resolve("WndModInventory.java");
	private static final Path ITEMS_PROPS = Paths.get(
			"core/src/main/assets/messages/items/items.properties");
	private static final Path ITEMS_ZH_PROPS = Paths.get(
			"core/src/main/assets/messages/items/items_zh.properties");
	private static final Path WINDOWS_PROPS = Paths.get(
			"core/src/main/assets/messages/windows/windows.properties");
	private static final Path WINDOWS_ZH_PROPS = Paths.get(
			"core/src/main/assets/messages/windows/windows_zh.properties");
	private static final Path TALENTS_PANE = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/TalentsPane.java");
	private static final Path BAG = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/bags/Bag.java");
	private static final Path POTION_BANDOLIER = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/bags/PotionBandolier.java");
	private static final Path VELVET_POUCH = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/bags/VelvetPouch.java");
	private static final Path SCROLL_HOLDER = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/bags/ScrollHolder.java");
	private static final Path MAGICAL_HOLSTER = Paths.get(
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/bags/MagicalHolster.java");

	public static void main(String[] args) throws Exception {
		patchDungeon();
		patchHeroClass();
		patchHero();
		patchChar();
		patchWndGame();
		patchJournal();
		patchBadges();
		writeFile(FATE_LOCK, readTemplate(FATE_LOCK_TEMPLATE));
		writeFile(MOD_INVENTORY, readTemplate(MOD_INVENTORY_TEMPLATE));
		writeFile(MOD_HOOKS, readTemplate(MOD_HOOKS_TEMPLATE));
		writeFile(WND_MOD_INVENTORY, readTemplate(WND_MOD_INVENTORY_TEMPLATE));
		patchItemsProps();
		patchItemsZhProps();
		patchWindowsProps();
		patchWindowsZhProps();
		patchTalentUi();
		patchBagCapacity();
	}

	private static void patchDungeon() throws IOException {
		CompilationUnit cu = parseWithLexical(DUNGEON);

		addImportIfMissing(cu, "com.shatteredpixel.shatteredpixeldungeon.mod.ModInventory");

		ClassOrInterfaceDeclaration dungeon = cu.getClassByName("Dungeon")
				.orElseThrow(() -> new IllegalStateException("Dungeon class not found"));

		Optional<MethodDeclaration> initOpt = dungeon.getMethodsByName("init").stream()
				.filter(m -> m.getParameters().isEmpty())
				.findFirst();
		MethodDeclaration init = initOpt
				.orElseThrow(() -> new IllegalStateException("init() method not found"));

		BlockStmt body = init.getBody()
				.orElseThrow(() -> new IllegalStateException("init() body not found"));

		if (!hasApplyStartupGrants(body)) {
			int insertAt = findInitHeroIndex(body.getStatements());
			if (insertAt >= 0) {
				Statement call = StaticJavaParser.parseStatement("ModInventory.applyStartupGrants();");
				body.getStatements().add(insertAt + 1, call);
			} else {
				throw new IllegalStateException("initHero(...) call not found in init()");
			}
		}

		Files.writeString(DUNGEON, LexicalPreservingPrinter.print(cu));
	}

	private static void patchHeroClass() throws IOException {
		CompilationUnit cu = parseWithLexical(HERO_CLASS);

		addImportIfMissing(cu, "com.shatteredpixel.shatteredpixeldungeon.items.FateLock");

		TypeDeclaration<?> heroType = cu.getTypes().stream()
				.filter(t -> "HeroClass".equals(t.getNameAsString()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("HeroClass not found"));

		Optional<MethodDeclaration> initOpt = heroType.getMethodsByName("initHero").stream()
				.filter(m -> m.getParameters().size() == 1)
				.findFirst();
		MethodDeclaration initHero = initOpt
				.orElseThrow(() -> new IllegalStateException("initHero(Hero) not found"));

		BlockStmt body = initHero.getBody()
				.orElseThrow(() -> new IllegalStateException("initHero body not found"));

		if (!hasFateLockGrant(body)) {
			int insertAt = findScrollIdentifyIndex(body.getStatements());
			if (insertAt >= 0) {
				Statement call = StaticJavaParser.parseStatement("new FateLock().collect();");
				body.getStatements().add(insertAt + 1, call);
			} else {
				throw new IllegalStateException("ScrollOfIdentify marker not found in initHero");
			}
		}

		Files.writeString(HERO_CLASS, LexicalPreservingPrinter.print(cu));
	}

	private static void patchHero() throws IOException {
		CompilationUnit cu = parseWithLexical(HERO);
		addImportIfMissing(cu, "com.shatteredpixel.shatteredpixeldungeon.mod.ModHooks");
		addImportIfMissing(cu, "com.shatteredpixel.shatteredpixeldungeon.mod.ModInventory");

		ClassOrInterfaceDeclaration hero = cu.getClassByName("Hero")
				.orElseThrow(() -> new IllegalStateException("Hero class not found"));

		boolean changed = false;

		changed |= addExpLockGuard(hero);
		changed |= addLevelLockGuard(hero);
		changed |= addHpLockHooks(hero);
		changed |= addStatHelpers(hero);
		changed |= patchTalentUnlocks(hero);

		if (changed) {
			Files.writeString(HERO, LexicalPreservingPrinter.print(cu));
		}
	}

	private static void patchChar() throws IOException {
		String source = Files.readString(CHAR);
		if (source.contains("ModHooks.shouldBlockHeroDamage") || source.contains("ModInventory.isHpLocked")) {
			return;
		}

		CompilationUnit cu = parseWithLexical(source, CHAR);
		addImportIfMissing(cu, "com.shatteredpixel.shatteredpixeldungeon.mod.ModHooks");

		ClassOrInterfaceDeclaration clazz = cu.getClassByName("Char")
				.orElseThrow(() -> new IllegalStateException("Char class not found"));
		MethodDeclaration damage = clazz.getMethodsByName("damage").stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("damage method not found"));
		BlockStmt body = damage.getBody()
				.orElseThrow(() -> new IllegalStateException("damage body not found"));

		Statement guard = StaticJavaParser.parseStatement(
				"if (ModHooks.shouldBlockHeroDamage(this)) {\n" +
						"\treturn;\n" +
						"}");
		boolean inserted = false;
		for (IfStmt ifStmt : body.findAll(IfStmt.class)) {
			String condition = ifStmt.getCondition().toString();
			if (condition.contains("!isAlive()") && condition.contains("dmg < 0")) {
				java.util.Optional<com.github.javaparser.ast.Node> parent = ifStmt.getParentNode();
				if (parent.isPresent() && parent.get() instanceof BlockStmt) {
					BlockStmt block = (BlockStmt) parent.get();
					int idx = block.getStatements().indexOf(ifStmt);
					if (idx >= 0) {
						block.getStatements().add(idx + 1, guard);
						inserted = true;
						break;
					}
				}
			}
		}
		if (!inserted) {
			throw new IllegalStateException("damage marker not found");
		}

		Files.writeString(CHAR, LexicalPreservingPrinter.print(cu));
	}

	private static boolean hasApplyStartupGrants(BlockStmt body) {
		return body.findAll(MethodCallExpr.class).stream()
				.anyMatch(call -> "applyStartupGrants".equals(call.getNameAsString())
						&& call.getScope().isPresent()
						&& "ModInventory".equals(call.getScope().get().toString()));
	}

	private static boolean hasFateLockGrant(BlockStmt body) {
		return body.findAll(MethodCallExpr.class).stream()
				.anyMatch(call -> "collect".equals(call.getNameAsString())
						&& call.getScope().isPresent()
						&& isNewType(call.getScope().get(), "FateLock"));
	}

	private static int findScrollIdentifyIndex(List<Statement> statements) {
		for (int i = 0; i < statements.size(); i++) {
			Statement st = statements.get(i);
			if (!st.isExpressionStmt()) {
				continue;
			}
			MethodCallExpr call = st.asExpressionStmt().getExpression().toMethodCallExpr().orElse(null);
			if (call == null || !"identify".equals(call.getNameAsString()) || !call.getScope().isPresent()) {
				continue;
			}
			if (isNewType(call.getScope().get(), "ScrollOfIdentify")) {
				return i;
			}
		}
		return -1;
	}

	private static boolean isNewType(Expression expression, String typeName) {
		if (!expression.isObjectCreationExpr()) {
			return false;
		}
		ObjectCreationExpr create = expression.asObjectCreationExpr();
		return typeName.equals(create.getType().getNameAsString());
	}

	private static int findInitHeroIndex(List<Statement> statements) {
		for (int i = 0; i < statements.size(); i++) {
			Statement st = statements.get(i);
			if (st.isExpressionStmt()) {
				MethodCallExpr call = st.asExpressionStmt().getExpression().toMethodCallExpr().orElse(null);
				if (call != null && "initHero".equals(call.getNameAsString())) {
					return i;
				}
			}
		}
		return -1;
	}

	private static void addImportIfMissing(CompilationUnit cu, String fqcn) {
		if (cu.getImports().stream().noneMatch(i -> i.getNameAsString().equals(fqcn))) {
			cu.addImport(fqcn);
		}
	}

	private static CompilationUnit parseWithLexical(Path path) throws IOException {
		return parseWithLexical(Files.readString(path), path);
	}

	private static CompilationUnit parseWithLexical(String source, Path path) {
		JavaParser parser = new JavaParser(new ParserConfiguration());
		ParseResult<CompilationUnit> result = parser.parse(source);
		CompilationUnit cu = result.getResult()
				.orElseThrow(() -> new IllegalStateException("Failed to parse " + path));
		LexicalPreservingPrinter.setup(cu);
		return cu;
	}

	private static boolean addExpLockGuard(ClassOrInterfaceDeclaration hero) {
		boolean changed = false;
		for (MethodDeclaration method : hero.getMethodsByName("earnExp")) {
			if (method.getBody().isEmpty()) {
				continue;
			}
			BlockStmt body = method.getBody().get();
			if (hasMethodCall(body, "ModHooks", "blockEarnExp") || hasMethodCall(body, "ModInventory", "isExpLocked")) {
				continue;
			}
			Statement guard = StaticJavaParser.parseStatement(
					"if (ModHooks.blockEarnExp(this)) {\n" +
							"\treturn;\n" +
							"}");
			body.getStatements().add(0, guard);
			changed = true;
		}
		return changed;
	}

	private static boolean addLevelLockGuard(ClassOrInterfaceDeclaration hero) {
		for (MethodDeclaration method : hero.getMethods()) {
			if (method.getBody().isEmpty()) {
				continue;
			}
			BlockStmt body = method.getBody().get();
			if (hasMethodCall(body, "ModHooks", "blockLevelUp") || hasMethodCall(body, "ModInventory", "isLevelLocked")) {
				return false;
			}
			List<Statement> statements = body.getStatements();
			for (int i = 0; i < statements.size(); i++) {
				Statement st = statements.get(i);
				if (isLevelUpDeclaration(st)) {
					Statement guard = StaticJavaParser.parseStatement(
							"if (ModHooks.blockLevelUp(this)) {\n" +
									"\treturn;\n" +
									"}");
					statements.add(i, guard);
					return true;
				}
			}
		}
		throw new IllegalStateException("levelUp marker not found");
	}

	private static boolean addHpLockHooks(ClassOrInterfaceDeclaration hero) {
		boolean changed = false;
		boolean updateFound = false;
		boolean actFound = false;

		for (MethodDeclaration method : hero.getMethods()) {
			if (method.getBody().isEmpty()) {
				continue;
			}
			BlockStmt body = method.getBody().get();
			boolean hasHook = hasMethodCall(body, "ModHooks", "applyHpLock")
					|| hasMethodCall(body, "ModInventory", "applyHpLock");
			List<Statement> statements = body.getStatements();
			for (int i = 0; i < statements.size(); i++) {
				Statement st = statements.get(i);
				if (!updateFound && isHpMinAssign(st)) {
					updateFound = true;
					if (!hasHook) {
						statements.add(i + 1, StaticJavaParser.parseStatement("ModHooks.applyHpLock(this);"));
						changed = true;
					}
				}
				if (!actFound && isFieldOfViewAssign(st)) {
					actFound = true;
					if (!hasHook) {
						statements.add(i + 1, StaticJavaParser.parseStatement("ModHooks.applyHpLock(this);"));
						changed = true;
					}
				}
			}
		}

		if (!updateFound) {
			throw new IllegalStateException("updateHT marker not found");
		}
		if (!actFound) {
			throw new IllegalStateException("act marker not found");
		}
		return changed;
	}

	private static boolean addStatHelpers(ClassOrInterfaceDeclaration hero) {
		boolean hasAny = !hero.getMethodsByName("baseAccuracy").isEmpty()
				|| !hero.getMethodsByName("baseEvasion").isEmpty()
				|| !hero.getMethodsByName("adjustBaseAccuracy").isEmpty()
				|| !hero.getMethodsByName("adjustBaseEvasion").isEmpty();
		if (hasAny) {
			return false;
		}

		CompilationUnit tmp = StaticJavaParser.parse(
				"class _Tmp {\n" +
						"public int baseAccuracy() {\n" +
						"\treturn attackSkill;\n" +
						"}\n" +
						"\n" +
						"public int baseEvasion() {\n" +
						"\treturn defenseSkill;\n" +
						"}\n" +
						"\n" +
						"public void adjustBaseAccuracy(int delta) {\n" +
						"\tattackSkill = Math.max(1, attackSkill + delta);\n" +
						"}\n" +
						"\n" +
						"public void adjustBaseEvasion(int delta) {\n" +
						"\tdefenseSkill = Math.max(1, defenseSkill + delta);\n" +
						"}\n" +
						"}");
		ClassOrInterfaceDeclaration tmpClass = tmp.getClassByName("_Tmp")
				.orElseThrow(() -> new IllegalStateException("temp class parse failed"));
		List<BodyDeclaration<?>> helpers = tmpClass.getMembers();

		MethodDeclaration attackSkill = hero.getMethodsByName("attackSkill").stream()
				.findFirst()
				.orElse(null);
		if (attackSkill != null) {
			int idx = hero.getMembers().indexOf(attackSkill);
			if (idx < 0) {
				for (BodyDeclaration<?> helper : helpers) {
					hero.getMembers().add(helper.clone());
				}
			} else {
				int insertAt = idx;
				for (BodyDeclaration<?> helper : helpers) {
					hero.getMembers().add(insertAt, helper.clone());
					insertAt++;
				}
			}
		} else {
			for (BodyDeclaration<?> helper : helpers) {
				hero.getMembers().add(helper.clone());
			}
		}
		return true;
	}

	private static boolean patchTalentUnlocks(ClassOrInterfaceDeclaration hero) {
		boolean changed = false;

		MethodDeclaration talentPoints = hero.getMethodsByName("talentPointsAvailable").stream()
				.findFirst()
				.orElse(null);
		if (talentPoints != null && talentPoints.getBody().isPresent()) {
			BlockStmt body = talentPoints.getBody().get();
			boolean hasUnlock = hasMethodCall(body, "ModHooks", "talentPointsAvailable")
					|| hasMethodCall(body, "ModInventory", "isTalentTierUnlocked")
					|| hasMethodCall(body, "ModInventory", "isTalentPointsUnlocked");
			if (!hasUnlock) {
				talentPoints.setBody(StaticJavaParser.parseBlock("{\n" +
						"\treturn ModHooks.talentPointsAvailable(this, tier);\n" +
						"}"));
				changed = true;
			}
		}

		MethodDeclaration bonusPoints = hero.getMethodsByName("bonusTalentPoints").stream()
				.findFirst()
				.orElse(null);
		if (bonusPoints != null && bonusPoints.getBody().isPresent()) {
			BlockStmt body = bonusPoints.getBody().get();
			boolean hasUnlock = hasMethodCall(body, "ModHooks", "bonusTalentPoints")
					|| hasMethodCall(body, "ModInventory", "isTalentTierUnlocked");
			if (!hasUnlock) {
				bonusPoints.setBody(StaticJavaParser.parseBlock("{\n" +
						"\treturn ModHooks.bonusTalentPoints(this, tier);\n" +
						"}"));
				changed = true;
			}
		}

		return changed;
	}

	private static boolean hasMethodCall(BlockStmt body, String scopeName, String methodName) {
		return body.findAll(MethodCallExpr.class).stream()
				.anyMatch(call -> methodName.equals(call.getNameAsString())
						&& call.getScope().isPresent()
						&& scopeName.equals(call.getScope().get().toString()));
	}

	private static boolean isLevelUpDeclaration(Statement st) {
		if (!st.isExpressionStmt()) {
			return false;
		}
		Expression expr = st.asExpressionStmt().getExpression();
		if (!expr.isVariableDeclarationExpr()) {
			return false;
		}
		VariableDeclarationExpr decl = expr.asVariableDeclarationExpr();
		return decl.getVariables().stream().anyMatch(var ->
				"levelUp".equals(var.getNameAsString())
						&& var.getInitializer().isPresent()
						&& var.getInitializer().get() instanceof BooleanLiteralExpr
						&& ((BooleanLiteralExpr) var.getInitializer().get()).getValue() == false);
	}

	private static boolean isHpMinAssign(Statement st) {
		if (!st.isExpressionStmt()) {
			return false;
		}
		Expression expr = st.asExpressionStmt().getExpression();
		if (!expr.isAssignExpr()) {
			return false;
		}
		AssignExpr assign = expr.asAssignExpr();
		if (!(assign.getTarget() instanceof NameExpr)) {
			return false;
		}
		NameExpr target = (NameExpr) assign.getTarget();
		if (!"HP".equals(target.getNameAsString())) {
			return false;
		}
		if (!assign.getValue().isMethodCallExpr()) {
			return false;
		}
		MethodCallExpr call = assign.getValue().asMethodCallExpr();
		return "min".equals(call.getNameAsString())
				&& call.getScope().isPresent()
				&& "Math".equals(call.getScope().get().toString());
	}

	private static boolean isFieldOfViewAssign(Statement st) {
		if (!st.isExpressionStmt()) {
			return false;
		}
		Expression expr = st.asExpressionStmt().getExpression();
		if (!expr.isAssignExpr()) {
			return false;
		}
		AssignExpr assign = expr.asAssignExpr();
		if (!(assign.getTarget() instanceof NameExpr)) {
			return false;
		}
		NameExpr target = (NameExpr) assign.getTarget();
		if (!"fieldOfView".equals(target.getNameAsString())) {
			return false;
		}
		if (assign.getValue() instanceof FieldAccessExpr) {
			return assign.getValue().toString().contains("Dungeon.level.heroFOV");
		}
		return assign.getValue().toString().contains("Dungeon.level.heroFOV");
	}

	private static void patchWndGame() throws IOException {
		String source = Files.readString(WND_GAME);
		boolean hasModItems = source.contains("mod_items") || source.contains("WndModInventory");

		CompilationUnit cu = parseWithLexical(source, WND_GAME);

		ClassOrInterfaceDeclaration clazz = cu.getClassByName("WndGame")
				.orElseThrow(() -> new IllegalStateException("WndGame class not found"));
		boolean inserted = false;

		if (!hasModItems) {
			Statement addModItems = StaticJavaParser.parseStatement(
					"if (Dungeon.hero != null && Dungeon.hero.isAlive()) {\n" +
							"\taddButton( curBtn = new RedButton( Messages.get(this, \"mod_items\") ) {\n" +
							"\t\t@Override\n" +
							"\t\tprotected void onClick() {\n" +
							"\t\t\thide();\n" +
							"\t\t\tGameScene.show(new WndModInventory());\n" +
							"\t\t}\n" +
							"\t});\n" +
							"\tcurBtn.icon(Icons.get(Icons.PLUS));\n" +
							"}");

			Optional<ConstructorDeclaration> ctor = clazz.getConstructors().stream()
					.filter(c -> c.getParameters().isEmpty())
					.findFirst();
			if (ctor.isEmpty()) {
				throw new IllegalStateException("WndGame() ctor not found");
			}
			BlockStmt body = ctor.get().getBody();
			int insertAt = -1;
			for (int i = 0; i < body.getStatements().size(); i++) {
				Statement st = body.getStatement(i);
				if (isAddButtonCall(st)) {
					insertAt = i;
					if (i + 1 < body.getStatements().size() && isCurBtnIcon(body.getStatement(i + 1))) {
						insertAt = i + 1;
					}
					break;
				}
			}
			if (insertAt >= 0) {
				body.getStatements().add(insertAt + 1, addModItems);
				inserted = true;
			}
			if (!inserted) {
				throw new IllegalStateException("WndGame marker not found");
			}
		}

		boolean removedJournal = false;
		for (MethodDeclaration method : clazz.getMethods()) {
			if (method.getBody().isEmpty()) {
				continue;
			}
			BlockStmt body = method.getBody().get();
			for (int i = 0; i < body.getStatements().size(); i++) {
				Statement st = body.getStatement(i);
				if (!st.isExpressionStmt()) {
					continue;
				}
				String text = st.toString();
				if (text.contains("journal_unlock")) {
					body.getStatements().remove(i);
					removedJournal = true;
					break;
				}
			}
			if (removedJournal) {
				break;
			}
		}

		Files.writeString(WND_GAME, LexicalPreservingPrinter.print(cu));
	}

	private static boolean isAddButtonCall(Statement st) {
		if (!st.isExpressionStmt()) {
			return false;
		}
		MethodCallExpr call = st.asExpressionStmt().getExpression().toMethodCallExpr().orElse(null);
		return call != null && "addButton".equals(call.getNameAsString());
	}

	private static boolean isCurBtnIcon(Statement st) {
		if (!st.isExpressionStmt()) {
			return false;
		}
		MethodCallExpr call = st.asExpressionStmt().getExpression().toMethodCallExpr().orElse(null);
		return call != null
				&& "icon".equals(call.getNameAsString())
				&& call.getScope().isPresent()
				&& "curBtn".equals(call.getScope().get().toString());
	}

	private static void patchJournal() throws IOException {
		String source = Files.readString(JOURNAL);
		if (source.contains("unlockAll")) {
			return;
		}

		CompilationUnit cu = parseWithLexical(source, JOURNAL);
		addImportIfMissing(cu, "com.shatteredpixel.shatteredpixeldungeon.Badges");

		ClassOrInterfaceDeclaration clazz = cu.getClassByName("Journal")
				.orElseThrow(() -> new IllegalStateException("Journal class not found"));
		if (clazz.getMethodsByName("unlockAll").isEmpty()) {
			BodyDeclaration<?> method = StaticJavaParser.parseBodyDeclaration(
					"public static void unlockAll(){\n" +
							"\tloadGlobal();\n" +
							"\tfor (Catalog cat : Catalog.values()){\n" +
							"\t\tfor (Class<?> item : cat.items()){\n" +
							"\t\t\tCatalog.setSeen(item);\n" +
							"\t\t}\n" +
							"\t}\n" +
							"\tfor (Bestiary cat : Bestiary.values()){\n" +
							"\t\tfor (Class<?> entity : cat.entities()){\n" +
							"\t\t\tBestiary.setSeen(entity);\n" +
							"\t\t}\n" +
							"\t}\n" +
							"\tfor (Document doc : Document.values()){\n" +
							"\t\tfor (String page : doc.pageNames()){\n" +
							"\t\t\tdoc.readPage(page);\n" +
							"\t\t}\n" +
							"\t}\n" +
							"\tBadges.unlockAll();\n" +
							"\tBadges.saveGlobal(true);\n" +
							"\tsaveGlobal(true);\n" +
							"}");
			clazz.addMember(method);
		}

		Files.writeString(JOURNAL, LexicalPreservingPrinter.print(cu));
	}

	private static void patchBadges() throws IOException {
		String source = Files.readString(BADGES);
		if (source.contains("unlockAll()")) {
			return;
		}

		CompilationUnit cu = parseWithLexical(source, BADGES);

		ClassOrInterfaceDeclaration clazz = cu.getClassByName("Badges")
				.orElseThrow(() -> new IllegalStateException("Badges class not found"));
		if (clazz.getMethodsByName("unlockAll").isEmpty()) {
			BodyDeclaration<?> method = StaticJavaParser.parseBodyDeclaration(
					"public static void unlockAll(){\n" +
							"\tloadGlobal();\n" +
							"\tfor (Badge badge : Badge.values()){\n" +
							"\t\tif (badge.type != BadgeType.HIDDEN){\n" +
							"\t\t\tglobal.add(badge);\n" +
							"\t\t}\n" +
							"\t}\n" +
							"\tsaveNeeded = true;\n" +
							"}");
			clazz.addMember(method);
		}

		Files.writeString(BADGES, LexicalPreservingPrinter.print(cu));
	}

	private static void patchItemsProps() throws IOException {
		ensureProperty(ITEMS_PROPS, "items.fatelock.name", "fate lock");
		ensureProperty(ITEMS_PROPS, "items.fatelock.ac_reset", "RESET FLOOR");
		ensureProperty(ITEMS_PROPS, "items.fatelock.desc",
				"This curious lock can rewind the current dungeon floor. Using it will reset the floor instantly.");
	}

	private static void patchItemsZhProps() throws IOException {
		ensureProperty(ITEMS_ZH_PROPS, "items.fatelock.name", "命运枷锁");
		ensureProperty(ITEMS_ZH_PROPS, "items.fatelock.ac_reset", "重置楼层");
		ensureProperty(ITEMS_ZH_PROPS, "items.fatelock.desc",
				"这枚奇异的枷锁可以回溯当前地牢楼层。使用它会立刻重置本层。");
	}

	private static void patchWindowsProps() throws IOException {
		ensureProperty(WINDOWS_PROPS, "windows.wndgame.mod_items", "Enhanced Mod");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.body",
				"Enter item name (CN/EN) or class name, then quantity.");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.add", "Add Item");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.close", "Close");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.item_title", "Item Name");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.item_body",
				"Use CN/EN display name or class name (e.g. PotionOfHealing).");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.qty_title", "Quantity");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.qty_body", "Enter a positive integer.");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.switch", "Switch Input");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.confirm", "Add");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.cancel", "Cancel");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.lock_exp", "Lock Exp");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.lock_level", "Lock Level");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.lock_hp", "Lock HP");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.unlock_talent_tiers", "Unlock Talent Tiers");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.unlock_talent_points", "Max Talent Points");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.bag_capacity", "Backpack Expand");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.journal_unlock", "Unlock Journal");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.journal_unlocked", "Journal unlocked.");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.journal_confirm_title", "Unlock Journal");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.journal_confirm_body", "Unlock all journal entries?");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.journal_confirm_yes", "Unlock");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.journal_confirm_no", "Cancel");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.acc_label", "Accuracy: %1$d -> %2$d");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.eva_label", "Evasion: %1$d -> %2$d");
		ensureProperty(WINDOWS_PROPS, "windows.wndgame.journal_unlock", "Unlock Journal");
		ensureProperty(WINDOWS_PROPS, "windows.wndgame.journal_unlocked", "Journal unlocked.");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.bad_item",
				"Unknown or ambiguous item. Use a class name if needed.");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.bad_qty", "Invalid quantity.");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.added", "Added %1$d item(s).");
		ensureProperty(WINDOWS_PROPS, "windows.wndmodinventory.failed",
				"Could not add items (inventory full or item blocked).");
	}

	private static void patchWindowsZhProps() throws IOException {
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndgame.mod_items", "增强mod功能");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.body", "输入物品中文名/英文名或类名，再输入数量。");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.add", "添加物品");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.close", "关闭");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.item_title", "物品名称");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.item_body",
				"可输入中文/英文显示名或类名（例如 PotionOfHealing）。");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.qty_title", "数量");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.qty_body", "请输入正整数。");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.switch", "切换输入");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.confirm", "添加");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.cancel", "取消");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.lock_exp", "锁定经验");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.lock_level", "锁定等级");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.lock_hp", "锁定生命");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.bag_capacity", "背包扩容");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.journal_unlock", "解锁日志");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.journal_unlocked", "日志已解锁。");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.journal_confirm_title", "解锁日志");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.journal_confirm_body", "确定要解锁全部日志内容吗？");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.journal_confirm_yes", "解锁");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.journal_confirm_no", "取消");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.unlock_talent_tiers", "解锁天赋层");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.unlock_talent_points", "天赋点全满");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.acc_label", "精准：%1$d -> %2$d");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.eva_label", "闪避：%1$d -> %2$d");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndgame.journal_unlock", "解锁日志");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndgame.journal_unlocked", "日志已解锁。");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.bad_item",
				"物品不存在或名称不唯一，请尝试使用类名。");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.bad_qty", "数量无效。");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.added", "已添加 %1$d 个物品。");
		ensureProperty(WINDOWS_ZH_PROPS, "windows.wndmodinventory.failed",
				"添加失败（背包已满或物品无法加入）。");
	}

	private static void patchTalentUi() throws IOException {
		patchTalentsPane();
	}

	private static void patchTalentsPane() throws IOException {
		String source = Files.readString(TALENTS_PANE);
		boolean hasTierUnlock = source.contains("isTalentTierUnlocked");
		boolean hasEmptyTierGuard = source.contains("buttons.isEmpty()")
				&& source.contains("height = title.bottom() - y");
		if (hasTierUnlock && hasEmptyTierGuard) {
			return;
		}

		CompilationUnit cu = parseWithLexical(source, TALENTS_PANE);
		addImportIfMissing(cu, "com.shatteredpixel.shatteredpixeldungeon.mod.ModInventory");

		Optional<ClassOrInterfaceDeclaration> clazz = cu.getClassByName("TalentsPane");
		if (clazz.isEmpty()) {
			throw new IllegalStateException("TalentsPane class not found");
		}

		Optional<ConstructorDeclaration> ctor = clazz.get().getConstructors().stream()
				.filter(c -> c.getParameters().size() == 2)
				.findFirst();
		if (ctor.isEmpty()) {
			throw new IllegalStateException("TalentsPane(TalentButton.Mode, ArrayList) ctor not found");
		}

		BlockStmt body = ctor.get().getBody();

		if (!hasTierUnlock) {
			boolean insertedTierUnlock = false;
			for (com.github.javaparser.ast.stmt.IfStmt ifStmt : body.findAll(com.github.javaparser.ast.stmt.IfStmt.class)) {
				boolean hasTierCond = ifStmt.getCondition().findAll(com.github.javaparser.ast.expr.NameExpr.class).stream()
						.anyMatch(expr -> "tiersAvailable".equals(expr.getNameAsString()));
				boolean hasSubclassNone = ifStmt.getCondition().findAll(com.github.javaparser.ast.expr.FieldAccessExpr.class)
						.stream().anyMatch(expr -> "HeroSubClass.NONE".equals(expr.toString()));
				boolean hasArmorAbility = ifStmt.getCondition().findAll(com.github.javaparser.ast.expr.FieldAccessExpr.class)
						.stream().anyMatch(expr -> expr.toString().endsWith("armorAbility"));
				boolean elseHasArmorAbility = false;
				if (ifStmt.getElseStmt().isPresent() && ifStmt.getElseStmt().get().isIfStmt()) {
					com.github.javaparser.ast.stmt.IfStmt elseIf = ifStmt.getElseStmt().get().asIfStmt();
					elseHasArmorAbility = elseIf.getCondition()
							.findAll(com.github.javaparser.ast.expr.FieldAccessExpr.class)
							.stream().anyMatch(expr -> expr.toString().endsWith("armorAbility"));
				}
				if (hasTierCond && hasSubclassNone && (hasArmorAbility || elseHasArmorAbility)) {
					Statement unlockStmt = StaticJavaParser.parseStatement(
							"if (ModInventory.isTalentTierUnlocked()) {\n" +
									"\ttiersAvailable = Talent.MAX_TALENT_TIERS;\n" +
									"}");
					java.util.Optional<com.github.javaparser.ast.Node> parent = ifStmt.getParentNode();
					if (parent.isPresent() && parent.get() instanceof BlockStmt) {
						BlockStmt block = (BlockStmt) parent.get();
						int idx = block.getStatements().indexOf(ifStmt);
						if (idx >= 0) {
							block.getStatements().add(idx + 1, unlockStmt);
							insertedTierUnlock = true;
							break;
						}
					}
				}
			}
			if (!insertedTierUnlock) {
				throw new IllegalStateException("TalentsPane tier limit block not found");
			}
		}

		if (!hasTierUnlock) {
			boolean patchedEmptyCheck = false;
			for (com.github.javaparser.ast.stmt.IfStmt ifStmt : body.findAll(com.github.javaparser.ast.stmt.IfStmt.class)) {
				if (patchedEmptyCheck) {
					break;
				}
				if (!ifStmt.getThenStmt().isContinueStmt()) {
					continue;
				}
				if (!ifStmt.getCondition().isMethodCallExpr()) {
					continue;
				}
				com.github.javaparser.ast.expr.MethodCallExpr cond = ifStmt.getCondition().asMethodCallExpr();
				if (!"isEmpty".equals(cond.getNameAsString())) {
					continue;
				}
				if (!cond.getScope().isPresent() || !cond.getScope().get().isMethodCallExpr()) {
					continue;
				}
				com.github.javaparser.ast.expr.MethodCallExpr scope = cond.getScope().get().asMethodCallExpr();
				if (!"get".equals(scope.getNameAsString())) {
					continue;
				}
				if (!scope.getScope().isPresent() || !scope.getScope().get().isNameExpr()) {
					continue;
				}
				if (!"talents".equals(scope.getScope().get().asNameExpr().getNameAsString())) {
					continue;
				}
				ifStmt.setCondition(StaticJavaParser.parseExpression(
						"talents.get(i).isEmpty() && !ModInventory.isTalentTierUnlocked()"));
				patchedEmptyCheck = true;
			}
			if (!patchedEmptyCheck) {
				throw new IllegalStateException("TalentsPane empty tier check not found");
			}
		}

		if (!hasEmptyTierGuard) {
			java.util.Optional<ClassOrInterfaceDeclaration> tierPaneOpt = clazz.get().getMembers().stream()
					.filter(m -> m.isClassOrInterfaceDeclaration()
							&& "TalentTierPane".equals(m.asClassOrInterfaceDeclaration().getNameAsString()))
					.map(m -> m.asClassOrInterfaceDeclaration())
					.findFirst();
			if (tierPaneOpt.isEmpty()) {
				throw new IllegalStateException("TalentTierPane class not found");
			}
			ClassOrInterfaceDeclaration tierPane = tierPaneOpt.get();
			java.util.Optional<MethodDeclaration> layoutOpt = tierPane.getMethodsByName("layout").stream()
					.filter(m -> m.getParameters().isEmpty())
					.findFirst();
			if (layoutOpt.isEmpty()) {
				throw new IllegalStateException("TalentTierPane.layout() not found");
			}
			BlockStmt layoutBody = layoutOpt.get().getBody()
					.orElseThrow(() -> new IllegalStateException("TalentTierPane.layout() body not found"));
			Statement guard = StaticJavaParser.parseStatement(
					"if (buttons.isEmpty()) {\n" +
							"\theight = title.bottom() - y;\n" +
							"\treturn;\n" +
							"}");
			layoutBody.getStatements().add(0, guard);
		}

		Files.writeString(TALENTS_PANE, LexicalPreservingPrinter.print(cu));
	}

	private static void patchBagCapacity() throws IOException {
		patchBagCapacity(BAG, 20);
		patchBagCapacity(POTION_BANDOLIER, 19);
		patchBagCapacity(VELVET_POUCH, 19);
		patchBagCapacity(SCROLL_HOLDER, 19);
		patchBagCapacity(MAGICAL_HOLSTER, 19);
	}

	private static void patchBagCapacity(Path path, int currentValue) throws IOException {
		String source = Files.readString(path);
		if (source.contains("ModInventory.bagCapacityOverride")) {
			return;
		}

		CompilationUnit cu = parseWithLexical(source, path);
		addImportIfMissing(cu, "com.shatteredpixel.shatteredpixeldungeon.mod.ModInventory");

		ClassOrInterfaceDeclaration clazz = cu.getClassByName(path.getFileName().toString().replace(".java", ""))
				.orElseGet(() -> cu.findFirst(ClassOrInterfaceDeclaration.class)
						.orElseThrow(() -> new IllegalStateException("class not found in " + path)));
		MethodDeclaration capacity = clazz.getMethodsByName("capacity").stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("capacity() not found in " + path));
		capacity.setBody(StaticJavaParser.parseBlock("{\n\treturn ModInventory.bagCapacityOverride(" + currentValue + ");\n}"));

		Files.writeString(path, LexicalPreservingPrinter.print(cu));
	}

	private static void ensureProperty(Path path, String key, String value) throws IOException {
		String source = Files.readString(path);
		String entry = key + "=";
		if (source.contains(entry)) {
			return;
		}
		String updated = source;
		if (!updated.endsWith("\n")) {
			updated += "\n";
		}
		updated += entry + value + "\n";
		Files.writeString(path, updated);
	}


	private static void writeFile(Path path, String content) throws IOException {
		Files.createDirectories(path.getParent());
		if (Files.exists(path)) {
			String existing = Files.readString(path);
			if (existing.equals(content)) {
				return;
			}
		}
		Files.writeString(path, content);
	}

	private static String readTemplate(Path path) throws IOException {
		return Files.readString(path);
	}

}
