package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.AbstractShopObjectType;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.util.Utils;

public class CitizensShopObjectType extends AbstractShopObjectType {

	CitizensShopObjectType() {
		super("citizen", "shopkeeper.citizen");
	}

	@Override
	protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
		return new CitizensShop(shopkeeper, creationData);
	}

	@Override
	public boolean isEnabled() {
		return Settings.enableCitizenShops && CitizensHandler.isEnabled();
	}

	@Override
	public boolean matches(String identifier) {
		identifier = Utils.normalize(identifier);
		if (super.matches(identifier)) return true;
		return identifier.startsWith("citizen") || identifier.startsWith("npc");
	}

	@Override
	protected void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedCitizenShop);
	}

	@Override
	public boolean needsSpawning() {
		return false; // spawning and despawning is handled by citizens
	}
}
