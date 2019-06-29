package de.domisum.iternifexedit.navmesh.edit;

import de.domisum.iternifexedit.IternifexEdit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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


	// -------
	// EVENTS
	// -------
	@EventHandler
	public void playerUseEditItem(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();

		if(event.getAction() == Action.PHYSICAL)
			return;

		ItemStack itemStack = event.getItem();
		if(itemStack == null)
			return;

		NavMeshEditCoordinator editManager = IternifexEdit.getNavMeshEditCoordinator();
		if(!editManager.isActiveFor(player))
			return;

		if(itemStack.isSimilar(editManager.createPointItemStack))
			editManager.getEditor(player).createPoint();
		else if(itemStack.isSimilar(editManager.deletePointItemStack))
			editManager.getEditor(player).deletePoint();
		else if(itemStack.isSimilar(editManager.selectPointItemStack))
			editManager.getEditor(player).selectPoint();
		else if(itemStack.isSimilar(editManager.deselectPointItemStack))
			editManager.getEditor(player).deselectPoint();
		else if(itemStack.isSimilar(editManager.createTriangleItemStack))
			editManager.getEditor(player).createTriangle();
		else if(itemStack.isSimilar(editManager.deleteTriangleItemStack))
			editManager.getEditor(player).deleteTriangle();
		else if(itemStack.isSimilar(editManager.movePointItemStack))
			editManager.getEditor(player).movePoint();
		else if(itemStack.isSimilar(editManager.infoItemStack))
			editManager.getEditor(player).info();
		else if(itemStack.isSimilar(editManager.ladderItemStack))
			editManager.getEditor(player).ladder();
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

		NavMeshEditCoordinator editManager = IternifexEdit.getNavMeshEditCoordinator();
		if(!editManager.editItemStacks.contains(itemStack))
			return;

		editManager.endEditMode(player);
		event.getItemDrop().remove();
	}

	@EventHandler
	public void test(AsyncPlayerChatEvent event)
	{
		/*Player player = event.getPlayer();
		String[] args = event.getMessage().split("\\s+");
		if(args.length != 2)
			return;

		NavMesh mesh = CompitumLib.getNavMeshManager().getNavMeshAt(player.getLocation());
		NavMeshTriangle start = mesh.getTriangle(args[0]);
		NavMeshTriangle end = mesh.getTriangle(args[1]);

		List<NavMeshTriangle> path = mesh.findPath(start, end);

		for(NavMeshTriangle triangle : path)
			DebugUtil.say(triangle.id);*/
	}

}
