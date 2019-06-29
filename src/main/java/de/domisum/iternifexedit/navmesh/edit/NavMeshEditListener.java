package de.domisum.iternifexedit.navmesh.edit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

class NavMeshEditListener implements Listener
{

	// DEPENDENCIES
	private final NavMeshEditCoordinator navMeshEditCoordinator;


	// INIT
	public NavMeshEditListener(Plugin plugin, NavMeshEditCoordinator navMeshEditCoordinator)
	{
		this.navMeshEditCoordinator = navMeshEditCoordinator;
		registerListener(plugin);
	}

	private void registerListener(Plugin plugin)
	{
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	// EVENTS
	@EventHandler
	public void playerUseEditItem(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();

		if(event.getAction() == Action.PHYSICAL)
			return;

		ItemStack itemStack = event.getItem();
		if(itemStack == null)
			return;

		if(!navMeshEditCoordinator.isActiveFor(player))
			return;

		if(itemStack.isSimilar(navMeshEditCoordinator.getCreatePointItemStack()))
			navMeshEditCoordinator.getEditor(player).createPoint();
		else if(itemStack.isSimilar(navMeshEditCoordinator.getDeletePointItemStack()))
			navMeshEditCoordinator.getEditor(player).deletePoint();
		else if(itemStack.isSimilar(navMeshEditCoordinator.getSelectPointItemStack()))
			navMeshEditCoordinator.getEditor(player).selectPoint();
		else if(itemStack.isSimilar(navMeshEditCoordinator.getDeselectPointItemStack()))
			navMeshEditCoordinator.getEditor(player).deselectPoint();
		else if(itemStack.isSimilar(navMeshEditCoordinator.getCreateTriangleItemStack()))
			navMeshEditCoordinator.getEditor(player).createTriangle();
		else if(itemStack.isSimilar(navMeshEditCoordinator.getDeleteTriangleItemStack()))
			navMeshEditCoordinator.getEditor(player).deleteTriangle();
		else if(itemStack.isSimilar(navMeshEditCoordinator.getInfoItemStack()))
			navMeshEditCoordinator.getEditor(player).info();
		else if(itemStack.isSimilar(navMeshEditCoordinator.getLadderItemStack()))
			navMeshEditCoordinator.getEditor(player).ladder();
		else
			return;

		event.setCancelled(true);
	}

	@EventHandler
	public void playerDropEditItem(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		ItemStack itemStack = event.getItemDrop().getItemStack();
		itemStack.setAmount(1); // set to 1 so equels works, in case player drops itemstack with amount bigger than 1

		if(!navMeshEditCoordinator.getEditItemStacks().contains(itemStack))
			return;

		navMeshEditCoordinator.endEditMode(player);
		event.getItemDrop().remove();
	}

}
