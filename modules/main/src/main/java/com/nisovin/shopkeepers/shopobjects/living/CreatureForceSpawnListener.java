package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class CreatureForceSpawnListener implements Listener {

	private Location nextSpawnLocation = null;
	private EntityType nextEntityType = null;

	CreatureForceSpawnListener() {
	}

	// This listener tries to bypass other plugins which block the spawning of living shopkeeper entities.
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	void onCreatureSpawn(CreatureSpawnEvent event) {
		if (nextSpawnLocation == null) return;
		if (this.matchesForcedCreatureSpawn(event)) {
			event.setCancelled(false);
		} else {
			// This shouldn't normally be reached..
			Log.debug(() -> "Shopkeeper entity-spawning seems to be out of sync: spawn-force was activated for an entity of type "
					+ nextEntityType.name() + " at location " + nextSpawnLocation + ", but a (different) entity of type "
					+ event.getEntityType().name() + " was spawned at location " + event.getLocation() + ".");
		}
		nextSpawnLocation = null;
		nextEntityType = null;
	}

	private boolean matchesForcedCreatureSpawn(CreatureSpawnEvent event) {
		if (event.getEntityType() != nextEntityType) {
			return false;
		}
		if (!LocationUtils.isEqualPosition(nextSpawnLocation, event.getLocation())) {
			return false;
		}
		return true;
	}

		this.nextSpawnLocation = location;
		this.nextEntityType = entityType;
	}
}
