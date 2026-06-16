package com.hexagram2021.girlfriends.common.network;

import com.google.common.collect.Lists;
import com.hexagram2021.girlfriends.common.blessing.FollowMode;
import com.hexagram2021.girlfriends.common.gift.GiftPreferenceLevel;
import com.hexagram2021.girlfriends.common.network.InteractionSummary.KnownGiftPreferenceSummary;
import com.hexagram2021.girlfriends.common.network.InteractionSummary.QuestContentSummary;
import com.hexagram2021.girlfriends.common.quest.QuestState;
import com.hexagram2021.girlfriends.common.quest.QuestType;
import com.hexagram2021.girlfriends.common.relationship.AffectionStage;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Girlfriends 网络编解码工具喵~
 *
 * @author liudongyu
 */
public final class NetworkCodecs {
	private NetworkCodecs() {
	}

	/**
	 * 写入交互摘要喵~
	 *
	 * @param buffer 网络缓冲区喵~
	 * @param summary 交互摘要喵~
	 */
	public static void writeInteractionSummary(RegistryFriendlyByteBuf buffer, InteractionSummary summary) {
		buffer.writeIdentifier(summary.girlfriendTypeId());
		buffer.writeEnum(summary.stage());
		buffer.writeDouble(summary.stageProgress());
		buffer.writeBoolean(summary.canGiveGift());
		buffer.writeBoolean(summary.canAcceptQuest());
		buffer.writeBoolean(summary.canFollow());
		buffer.writeBoolean(summary.canInviteHome());
		buffer.writeEnum(summary.followMode());
		writeKnownGiftPreferences(buffer, summary.knownGiftPreferences());
		writeQuestContentSummary(buffer, summary.currentQuest());
		buffer.writeBoolean(summary.needsIntimacyConfirmation());
	}

	/**
	 * 读取交互摘要喵~
	 *
	 * @param buffer 网络缓冲区喵~
	 * @return 交互摘要喵~
	 */
	public static InteractionSummary readInteractionSummary(RegistryFriendlyByteBuf buffer) {
		return new InteractionSummary(
				buffer.readIdentifier(),
				buffer.readEnum(AffectionStage.class),
				buffer.readDouble(),
				buffer.readBoolean(),
				buffer.readBoolean(),
				buffer.readBoolean(),
				buffer.readBoolean(),
				buffer.readEnum(FollowMode.class),
				readKnownGiftPreferences(buffer),
				readQuestContentSummary(buffer),
				buffer.readBoolean()
		);
	}

	/**
	 * 写入委托图标摘要喵~
	 *
	 * @param buffer 网络缓冲区喵~
	 * @param summary 委托图标摘要喵~
	 */
	public static void writeQuestIconSummary(RegistryFriendlyByteBuf buffer, QuestIconSummary summary) {
		buffer.writeIdentifier(summary.girlfriendTypeId());
		buffer.writeIdentifier(summary.questId());
		buffer.writeEnum(summary.questType());
		buffer.writeEnum(summary.questState());
		buffer.writeUtf(summary.titleKey());
		writeStrings(buffer, summary.objectiveSummaryKeys());
	}

	/**
	 * 读取委托图标摘要喵~
	 *
	 * @param buffer 网络缓冲区喵~
	 * @return 委托图标摘要喵~
	 */
	public static QuestIconSummary readQuestIconSummary(RegistryFriendlyByteBuf buffer) {
		return new QuestIconSummary(
				buffer.readIdentifier(),
				buffer.readIdentifier(),
				buffer.readEnum(QuestType.class),
				buffer.readEnum(QuestState.class),
				buffer.readUtf(),
				readStrings(buffer)
		);
	}

	private static void writeKnownGiftPreferences(RegistryFriendlyByteBuf buffer, List<KnownGiftPreferenceSummary> summaries) {
		buffer.writeVarInt(summaries.size());
		for(KnownGiftPreferenceSummary summary : summaries) {
			buffer.writeIdentifier(summary.itemOrTagId());
			buffer.writeEnum(summary.level());
			buffer.writeBoolean(summary.tag());
		}
	}

	private static List<KnownGiftPreferenceSummary> readKnownGiftPreferences(RegistryFriendlyByteBuf buffer) {
		int size = buffer.readVarInt();
		List<KnownGiftPreferenceSummary> summaries = Lists.newArrayListWithCapacity(size);
		for(int i = 0; i < size; i++) {
			summaries.add(new KnownGiftPreferenceSummary(buffer.readIdentifier(), buffer.readEnum(GiftPreferenceLevel.class), buffer.readBoolean()));
		}
		return List.copyOf(summaries);
	}

	private static void writeQuestContentSummary(RegistryFriendlyByteBuf buffer, @Nullable QuestContentSummary summary) {
		buffer.writeBoolean(summary != null);
		if(summary == null) {
			return;
		}
		buffer.writeIdentifier(summary.questId());
		buffer.writeEnum(summary.questType());
		buffer.writeEnum(summary.questState());
		buffer.writeUtf(summary.titleKey());
		buffer.writeUtf(summary.descriptionKey());
		writeStrings(buffer, summary.objectiveSummaryKeys());
		buffer.writeBoolean(summary.questCompleted());
	}

	@Nullable
	private static QuestContentSummary readQuestContentSummary(RegistryFriendlyByteBuf buffer) {
		if(!buffer.readBoolean()) {
			return null;
		}
		return new QuestContentSummary(
				buffer.readIdentifier(),
				buffer.readEnum(QuestType.class),
				buffer.readEnum(QuestState.class),
				buffer.readUtf(),
				buffer.readUtf(),
				readStrings(buffer),
				buffer.readBoolean()
		);
	}

	private static void writeStrings(RegistryFriendlyByteBuf buffer, List<String> values) {
		buffer.writeVarInt(values.size());
		for(String value : values) {
			buffer.writeUtf(value);
		}
	}

	private static List<String> readStrings(RegistryFriendlyByteBuf buffer) {
		int size = buffer.readVarInt();
		List<String> values = Lists.newArrayListWithCapacity(size);
		for(int i = 0; i < size; i++) {
			values.add(buffer.readUtf());
		}
		return List.copyOf(values);
	}
}
