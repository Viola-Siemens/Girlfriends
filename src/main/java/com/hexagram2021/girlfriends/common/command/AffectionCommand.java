package com.hexagram2021.girlfriends.common.command;

import com.hexagram2021.girlfriends.common.entity.GirlfriendEntity;
import com.hexagram2021.girlfriends.common.persist.GirlfriendsWorldData;
import com.hexagram2021.girlfriends.common.relationship.AffectionChangeSource;
import com.hexagram2021.girlfriends.common.relationship.PlayerCharacterRelation;
import com.hexagram2021.girlfriends.common.relationship.RelationshipService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * /affection 命令喵~
 * <p>
 * 用于调试时读写角色好感度喵~
 * <ul>
 *   <li>/affection get &lt;girlfriend&gt; &lt;player&gt; — 查询好感度喵~</li>
 *   <li>/affection set &lt;girlfriend&gt; &lt;player&gt; &lt;value&gt; — 设置好感度喵~</li>
 *   <li>/affection add &lt;girlfriend&gt; &lt;player&gt; &lt;delta&gt; — 变动好感度喵~</li>
 * </ul>
 * 权限等级 GAMEMASTERS，后续可配置化喵~
 *
 * @author liudongyu
 */
public final class AffectionCommand {
	private static final float MIN_AFFECTION = 0.0F;
	private static final float MAX_AFFECTION = 1000.0F;

	/** 指定实体不是女友角色喵~ */
	private static final SimpleCommandExceptionType ERROR_NOT_GIRLFRIEND = new SimpleCommandExceptionType(
			Component.translatable("commands.girlfriends.affection.not_a_girlfriend")
	);

	private AffectionCommand() {
	}

	/**
	 * 注册命令喵~
	 *
	 * @param dispatcher 命令调度器喵~
	 */
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal("affection")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("get")
								.then(Commands.argument("girlfriend", EntityArgument.entity())
										.then(Commands.argument("player", EntityArgument.player())
												.executes(AffectionCommand::getAffection)
										)
								)
						)
						.then(Commands.literal("set")
								.then(Commands.argument("girlfriend", EntityArgument.entity())
										.then(Commands.argument("player", EntityArgument.player())
												.then(Commands.argument("value", FloatArgumentType.floatArg(MIN_AFFECTION, MAX_AFFECTION))
														.executes(AffectionCommand::setAffection)
												)
										)
								)
						)
						.then(Commands.literal("add")
								.then(Commands.argument("girlfriend", EntityArgument.entity())
										.then(Commands.argument("player", EntityArgument.player())
												.then(Commands.argument("delta", FloatArgumentType.floatArg())
														.executes(AffectionCommand::addAffection)
												)
										)
								)
						)
		);
	}

	/**
	 * 查询好感度喵~
	 */
	private static int getAffection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = EntityArgument.getPlayer(context, "player");
		GirlfriendEntity girlfriend = resolveGirlfriend(context);

		Identifier girlfriendTypeId = girlfriend.getGirlfriendTypeId();
		GirlfriendsWorldData data = getWorldData(player);
		RelationshipService service = new RelationshipService(data);
		PlayerCharacterRelation relation = service.getRelation(player.getUUID(), girlfriendTypeId);

		source.sendSuccess(() -> Component.translatable(
				"commands.girlfriends.affection.get",
				girlfriend.getDisplayName(),
				player.getDisplayName(),
				String.format("%.1f", relation.getAffection())
		), false);

		return 1;
	}

	/**
	 * 设置好感度喵~
	 */
	private static int setAffection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = EntityArgument.getPlayer(context, "player");
		float targetValue = FloatArgumentType.getFloat(context, "value");
		GirlfriendEntity girlfriend = resolveGirlfriend(context);

		Identifier girlfriendTypeId = girlfriend.getGirlfriendTypeId();
		GirlfriendsWorldData data = getWorldData(player);
		RelationshipService service = new RelationshipService(data);
		PlayerCharacterRelation relation = service.getRelation(player.getUUID(), girlfriendTypeId);
		float oldValue = relation.getAffection();
		relation.setAffection(targetValue);
		data.setDirty();

		source.sendSuccess(() -> Component.translatable(
				"commands.girlfriends.affection.set",
				girlfriend.getDisplayName(),
				player.getDisplayName(),
				String.format("%.1f", oldValue),
				String.format("%.1f", targetValue)
		), true);

		return 1;
	}

	/**
	 * 变动好感度喵~
	 */
	private static int addAffection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = EntityArgument.getPlayer(context, "player");
		float delta = FloatArgumentType.getFloat(context, "delta");
		GirlfriendEntity girlfriend = resolveGirlfriend(context);

		Identifier girlfriendTypeId = girlfriend.getGirlfriendTypeId();
		GirlfriendsWorldData data = getWorldData(player);
		RelationshipService service = new RelationshipService(data);
		float newValue = service.changeAffection(player.getUUID(), girlfriendTypeId, AffectionChangeSource.COMMAND, delta);

		source.sendSuccess(() -> Component.translatable(
				"commands.girlfriends.affection.add",
				girlfriend.getDisplayName(),
				player.getDisplayName(),
				delta >= 0 ? "+" : "",
				String.format("%.1f", delta),
				String.format("%.1f", newValue)
		), true);

		return 1;
	}

	/**
	 * 解析 GirlfriendEntity 参数喵~
	 *
	 * @param context 命令上下文喵~
	 * @return 角色实体喵~
	 * @throws CommandSyntaxException 实体不是角色时抛出喵~
	 */
	private static GirlfriendEntity resolveGirlfriend(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Entity entity = EntityArgument.getEntity(context, "girlfriend");
		if (entity instanceof GirlfriendEntity girlfriend) {
			return girlfriend;
		}
		throw ERROR_NOT_GIRLFRIEND.create();
	}

	/**
	 * 获取世界数据喵~
	 *
	 * @param player 玩家喵~
	 * @return 世界数据喵~
	 */
	private static GirlfriendsWorldData getWorldData(ServerPlayer player) {
		ServerLevel level = player.level().getServer().overworld();
		return level.getDataStorage().computeIfAbsent(GirlfriendsWorldData.TYPE);
	}
}
