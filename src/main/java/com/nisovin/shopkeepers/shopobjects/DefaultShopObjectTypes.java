package com.nisovin.shopkeepers.shopobjects;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.AbstractShopObjectType;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityObjectTypes;

public class DefaultShopObjectTypes {

	private final LivingEntityObjectTypes livingEntityObjectTypes = new LivingEntityObjectTypes();
	private final AbstractShopObjectType signShopObjectType = new SignShopObjectType();
	private final AbstractShopObjectType citizensShopObjectType = new CitizensShopObjectType();

	// TODO maybe change object type permissions to 'shopkeeper.object.<type>'?

	public DefaultShopObjectTypes() {
	}

	public List<AbstractShopObjectType> getAllObjectTypes() {
		List<AbstractShopObjectType> shopObjectTypes = new ArrayList<>();
		shopObjectTypes.addAll(livingEntityObjectTypes.getAllObjectTypes());
		shopObjectTypes.add(signShopObjectType);
		shopObjectTypes.add(citizensShopObjectType);
		return shopObjectTypes;
	}

	public LivingEntityObjectTypes getLivingEntityObjectTypes() {
		return livingEntityObjectTypes;
	}

	public ShopObjectType getSignShopObjectType() {
		return signShopObjectType;
	}

	public ShopObjectType getCitizensShopObjectType() {
		return citizensShopObjectType;
	}

	// STATICS (for convenience):

	public static LivingEntityObjectTypes MOBS() {
		return ShopkeepersPlugin.getInstance().getDefaultShopObjectTypes().getLivingEntityObjectTypes();
	}

	public static ShopObjectType SIGN() {
		return ShopkeepersPlugin.getInstance().getDefaultShopObjectTypes().getSignShopObjectType();
	}

	public static ShopObjectType CITIZEN() {
		return ShopkeepersPlugin.getInstance().getDefaultShopObjectTypes().getCitizensShopObjectType();
	}
}
