package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;

import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.book.BookPlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.SKTradingRecipe;
import com.nisovin.shopkeepers.shopkeeper.offers.SKBookOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.BookItems;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

public class SKBookPlayerShopkeeper extends AbstractPlayerShopkeeper implements BookPlayerShopkeeper {

	// Contains only one offer for a specific book (book title):
	private final List<SKBookOffer> offers = new ArrayList<>();
	private final List<SKBookOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * Creates a not yet initialized {@link SKBookPlayerShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected SKBookPlayerShopkeeper(int id) {
		super(id);
	}

	protected SKBookPlayerShopkeeper(int id, PlayerShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super(id);
		this.initOnCreation(shopCreationData);
	}

	protected SKBookPlayerShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		super(id);
		this.initOnLoad(configSection);
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.EDITOR()) == null) {
			this.registerUIHandler(new BookPlayerShopEditorHandler(this));
		}
		if (this.getUIHandler(DefaultUITypes.TRADING()) == null) {
			this.registerUIHandler(new BookPlayerShopTradingHandler(this));
		}
		super.setup();
	}

	@Override
	protected void loadFromSaveData(ConfigurationSection configSection) throws ShopkeeperCreateException {
		super.loadFromSaveData(configSection);
		// Load offers:
		this._clearOffers();
		// TODO Remove legacy: Load offers from old format (bookTitle -> price mapping) (since late MC 1.14.4).
		List<SKBookOffer> legacyOffers = SKBookOffer.loadFromLegacyConfig(configSection, "offers", "Shopkeeper " + this.getId());
		if (!legacyOffers.isEmpty()) {
			Log.info("Shopkeeper " + this.getId() + ": Importing old book offers.");
			this._addOffers(legacyOffers);
			this.markDirty();
		}
		this._addOffers(SKBookOffer.loadFromConfig(configSection, "offers", "Shopkeeper " + this.getId()));
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		// Save offers:
		SKBookOffer.saveToConfig(configSection, "offers", this.getOffers());
	}

	@Override
	public BookPlayerShopType getType() {
		return SKDefaultShopTypes.PLAYER_BOOK();
	}

	@Override
	public List<? extends SKTradingRecipe> getTradingRecipes(Player player) {
		Map<String, ItemStack> containerBooksByTitle = this.getCopyableBooksFromContainer();
		boolean hasBlankBooks = this.hasContainerBlankBooks();
		List<SKBookOffer> offers = this.getOffers();
		List<SKTradingRecipe> recipes = new ArrayList<>(offers.size());
		offers.forEach(bookOffer -> {
			String bookTitle = bookOffer.getBookTitle();
			ItemStack bookItem = containerBooksByTitle.get(bookTitle);
			boolean outOfStock = !hasBlankBooks;
			if (bookItem == null) {
				outOfStock = true;
				bookItem = this.createDummyBook(bookTitle);
			} else {
				// Create a copy of the book from the container:
				assert BookItems.isCopyableBook(bookItem);
				bookItem = BookItems.copyBook(bookItem);
			}
			assert bookItem != null;

			SKTradingRecipe recipe = this.createSellingRecipe(bookItem, bookOffer.getPrice(), outOfStock);
			if (recipe != null) {
				recipes.add(recipe);
			} // Else: Price is invalid (cannot be represented by currency items).
		});
		return Collections.unmodifiableList(recipes);
	}

	/**
	 * Gets the {@link BookItems#isCopyableBook(ItemStack) copyable} {@link BookItems#isWrittenBook(ItemStack) written
	 * book} items from the shopkeeper's {@link PlayerShopkeeper#getContainer() container}.
	 * <p>
	 * Book items without title are omitted. If multiple book items share the same title, only the first encountered
	 * book item with that title is returned.
	 * 
	 * @return the book items mapped by their title, or an empty Map if the container is not found
	 */
	protected Map<String, ItemStack> getCopyableBooksFromContainer() {
		// Linked Map: Preserves the order of encountered items.
		Map<String, ItemStack> booksByTitle = new LinkedHashMap<>();
		ItemStack[] contents = this.getContainerContents(); // Empty if the container is not found
		for (ItemStack itemStack : contents) {
			BookMeta bookMeta = BookItems.getBookMeta(itemStack);
			if (bookMeta == null) continue; // Not a written book
			if (!BookItems.isCopyable(bookMeta)) continue;
			String title = BookItems.getTitle(bookMeta);
			if (title == null) continue;

			// The item is ignored if we already encountered another book item with the same title before:
			booksByTitle.putIfAbsent(title, itemStack);
		}
		return booksByTitle;
	}

	/**
	 * Checks if the shopkeeper's container contains any blank books (i.e. items of type
	 * {@link Material#WRITABLE_BOOK}).
	 * 
	 * @return <code>true</code> if the container is found and contains blank books
	 */
	protected boolean hasContainerBlankBooks() {
		Inventory containerInventory = this.getContainerInventory();
		if (containerInventory == null) return false; // Container not found
		return containerInventory.contains(Material.WRITABLE_BOOK);
	}

	/**
	 * Creates a dummy book {@link ItemStack} that acts as substitute representation of the book item with the given
	 * title.
	 * <p>
	 * This dummy book item is used as a replacement in the shopkeeper editor and trading interface if no actual book
	 * item with the given title is found in the shopkeeper's container.
	 * 
	 * @param title
	 *            the book title
	 * @return the dummy book item
	 */
	protected ItemStack createDummyBook(String title) {
		ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();
		bookMeta.setTitle(title);
		bookMeta.setAuthor(Messages.unknownBookAuthor);
		bookMeta.setGeneration(Generation.TATTERED);
		bookItem.setItemMeta(bookMeta);
		return bookItem;
	}

	/**
	 * Checks if the given {@link BookMeta} corresponds to a {@link #createDummyBook(String) dummy book item}.
	 * 
	 * @param bookMeta
	 *            the book meta, not <code>null</code>
	 * @return <code>true</code> if the book meta corresponds to a dummy book item
	 */
	protected static boolean isDummyBook(BookMeta bookMeta) {
		assert bookMeta != null;
		Generation generation = BookItems.getGeneration(bookMeta);
		return (generation == Generation.TATTERED);
	}

	// OFFERS:

	@Override
	public List<SKBookOffer> getOffers() {
		return offersView;
	}

	@Override
	public SKBookOffer getOffer(ItemStack bookItem) {
		String bookTitle = BookItems.getBookTitle(bookItem);
		if (bookTitle == null) return null; // Not a written book, or has no title
		return this.getOffer(bookTitle);
	}

	@Override
	public SKBookOffer getOffer(String bookTitle) {
		Validate.notNull(bookTitle, "bookTitle is null");
		for (SKBookOffer offer : this.getOffers()) {
			if (offer.getBookTitle().equals(bookTitle)) {
				return offer;
			}
		}
		return null;
	}

	@Override
	public void removeOffer(String bookTitle) {
		Validate.notNull(bookTitle, "bookTitle is null");
		Iterator<SKBookOffer> iterator = offers.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getBookTitle().equals(bookTitle)) {
				iterator.remove();
				this.markDirty();
				break;
			}
		}
	}

	@Override
	public void clearOffers() {
		this._clearOffers();
		this.markDirty();
	}

	private void _clearOffers() {
		offers.clear();
	}

	@Override
	public void setOffers(List<? extends BookOffer> offers) {
		Validate.notNull(offers, "Offers is null!");
		Validate.noNullElements(offers, "Offers contains null elements!");
		this._setOffers(offers);
		this.markDirty();
	}

	private void _setOffers(List<? extends BookOffer> offers) {
		assert offers != null && !offers.contains(null);
		this._clearOffers();
		this._addOffers(offers);
	}

	@Override
	public void addOffer(BookOffer offer) {
		Validate.notNull(offer, "Offer is null!");
		this._addOffer(offer);
		this.markDirty();
	}

	private void _addOffer(BookOffer offer) {
		assert offer != null;
		Validate.isTrue(offer instanceof SKBookOffer, "offer is not of type SKBookOffer");
		SKBookOffer skOffer = (SKBookOffer) offer;

		// Remove any previous offer for the same book:
		String bookTitle = offer.getBookTitle();
		this.removeOffer(bookTitle);

		// Add the new offer:
		offers.add(skOffer);
	}

	@Override
	public void addOffers(List<? extends BookOffer> offers) {
		Validate.notNull(offers, "Offers is null!");
		Validate.noNullElements(offers, "Offers contains null elements!");
		this._addOffers(offers);
		this.markDirty();
	}

	private void _addOffers(List<? extends BookOffer> offers) {
		assert offers != null && !offers.contains(null);
		// This replaces any previous offers for the same books:
		offers.forEach(this::_addOffer);
	}
}
