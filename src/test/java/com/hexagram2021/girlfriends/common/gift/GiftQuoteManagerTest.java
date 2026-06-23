package com.hexagram2021.girlfriends.common.gift;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Sets;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GiftQuoteManagerTest {
	@Test
	void parseCompleteQuotesYieldsAllTiers() {
		GiftQuoteManager.GiftQuotes quotes = new GiftQuoteManager.GiftQuotes(
				ImmutableListMultimap.<Identifier, String>builder()
						.put(Identifier.fromNamespaceAndPath("minecraft", "honeycomb"), "q.fav.0")
						.put(Identifier.fromNamespaceAndPath("minecraft", "honeycomb"), "q.fav.1")
						.build(),
				List.of("q.liked.0", "q.liked.1"),
				List.of("q.accepted.0"),
				List.of("q.rejected.0"),
				List.of("q.disliked.0")
		);
		assertFalse(quotes.favoriteQuotes().isEmpty());
		assertEquals(2, quotes.favoriteQuotes().get(Identifier.fromNamespaceAndPath("minecraft", "honeycomb")).size());
		assertEquals(2, quotes.likedQuotes().size());
		assertEquals(1, quotes.acceptedQuotes().size());
		assertEquals(1, quotes.rejectedQuotes().size());
		assertEquals(1, quotes.dislikedQuotes().size());
	}

	@Test
	void parseEmptyJsonYieldsEmptyCollections() {
		GiftQuoteManager.GiftQuotes quotes = new GiftQuoteManager.GiftQuotes(
				ImmutableListMultimap.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		);
		assertTrue(quotes.favoriteQuotes().isEmpty());
		assertTrue(quotes.likedQuotes().isEmpty());
		assertTrue(quotes.acceptedQuotes().isEmpty());
		assertTrue(quotes.rejectedQuotes().isEmpty());
		assertTrue(quotes.dislikedQuotes().isEmpty());
	}

	@Test
	void randomQuoteCoversAllEntries() {
		List<String> pool = List.of("a", "b", "c", "d", "e", "f", "g", "h");
		Set<String> seen = Sets.newHashSet();
		Random random = new Random(42);
		for (int i = 0; i < 100; i++) {
			seen.add(pool.get(random.nextInt(pool.size())));
		}
		assertEquals(pool.size(), seen.size(), "100 draws from 8 entries should hit all");
	}

	@Test
	void unknownCharacterReturnsEmpty() {
		GiftQuoteManager manager = GiftQuoteManager.INSTANCE;
		Identifier unknownId = Identifier.fromNamespaceAndPath("girlfriends", "nonexistent");
		assertTrue(manager.getRandomLikedQuote(unknownId).isEmpty());
		assertTrue(manager.getRandomAcceptedQuote(unknownId).isEmpty());
		assertTrue(manager.getRandomRejectedQuote(unknownId).isEmpty());
		assertTrue(manager.getRandomDislikedQuote(unknownId).isEmpty());
		assertTrue(manager.getRandomFavoriteQuote(unknownId,
				Identifier.fromNamespaceAndPath("minecraft", "stone")).isEmpty());
	}
}
