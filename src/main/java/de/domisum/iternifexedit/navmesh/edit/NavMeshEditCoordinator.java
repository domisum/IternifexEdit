package de.domisum.iternifexedit.navmesh.edit;

import de.domisum.lib.auxiliumspigot.util.ItemStackBuilder;
import de.domisum.lib.auxiliumspigot.util.player.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NavMeshEditCoordinator
{

	// CONSTANTS
	private static final int TASK_INTERVAL_TICKS = 3;

	// REFERENCES
	private final Set<NavMeshEditor> editors = new HashSet<>();
	private BukkitTask updateTask;

	// ITEMSTACKS
	ItemStack createPointItemStack;
	ItemStack deletePointItemStack;
	ItemStack selectPointItemStack;
	ItemStack deselectPointItemStack;
	ItemStack createTriangleItemStack;
	ItemStack deleteTriangleItemStack;
	ItemStack movePointItemStack;
	ItemStack infoItemStack;
	ItemStack ladderItemStack;
	List<ItemStack> editItemStacks = new ArrayList<>();

	// STATUS
	private int updateCount;


	// -------
	// CONSTRUCTOR
	// -------
	public NavMeshEditCoordinator()
	{

	}

	public void initialize()
	{
		createEditItemStacks();
		registerCommand();
		new NavMeshEditListener();
	}

	public void terminate()
	{
		stopUpdateTask();

		// copying the set to avoid ConcurrentModificaiton
		for(NavMeshEditor editor : new HashSet<>(editors))
			endEditMode(editor.getPlayer());
	}


	private void createEditItemStacks()
	{
		createPointItemStack = new ItemStackBuilder(Material.WHEAT_SEEDS).displayName(ChatColor.GREEN+"Create new point").build();
		deletePointItemStack = new ItemStackBuilder(Material.NETHER_WART)
				.displayName(ChatColor.RED+"Delete closest point")
				.build();
		selectPointItemStack = new ItemStackBuilder(Material.GOLD_INGOT)
				.displayName(ChatColor.YELLOW+"Select closest point")
				.build();
		deselectPointItemStack = new ItemStackBuilder(Material.IRON_INGOT)
				.displayName(ChatColor.GOLD+"Deselect closest point")
				.lore("Sneak while using to deselect all")
				.build();

		createTriangleItemStack = new ItemStackBuilder(Material.EMERALD).displayName(ChatColor.GREEN+"Create triangle").build();
		deleteTriangleItemStack = new ItemStackBuilder(Material.BLAZE_POWDER)
				.displayName(ChatColor.RED+"Delete triangle")
				.build();
		movePointItemStack = new ItemStackBuilder(Material.SADDLE).displayName(ChatColor.DARK_AQUA+"Move point").build();
		infoItemStack = new ItemStackBuilder(Material.BOOK).displayName(ChatColor.AQUA+"Point/triangle info").build();
		ladderItemStack = new ItemStackBuilder(Material.STICK).displayName(ChatColor.GOLD+"Create ladder").build();

		editItemStacks.add(createPointItemStack);
		editItemStacks.add(deletePointItemStack);
		editItemStacks.add(selectPointItemStack);
		editItemStacks.add(deselectPointItemStack);
		editItemStacks.add(createTriangleItemStack);
		editItemStacks.add(deleteTriangleItemStack);
		editItemStacks.add(movePointItemStack);
		editItemStacks.add(infoItemStack);
		editItemStacks.add(ladderItemStack);
	}

	private void registerCommand()
	{
		((CraftServer) CompitumLib.getPlugin().getServer()).getCommandMap().register("editNavMesh", new EditNavMeshCommand());
	}


	// GETTERS
	private boolean isUpdateTaskRunning()
	{
		return updateTask != null;
	}

	int getUpdateCount()
	{
		return updateCount;
	}

	boolean isActiveFor(Player player)
	{
		return getEditor(player) != null;
	}


	NavMeshEditor getEditor(Player player)
	{
		for(NavMeshEditor editor : editors)
			if(editor.getPlayer() == player)
				return editor;

		return null;
	}


	// UPDATE
	private void startUpdateTask()
	{
		if(updateTask != null)
			return;

		updateTask = Bukkit.getScheduler().runTaskTimer(CompitumLib.getPlugin(), this::update, 5, TASK_INTERVAL_TICKS);
	}

	private void stopUpdateTask()
	{
		if(updateTask == null)
			return;

		updateTask.cancel();
		updateTask = null;
	}

	private void update()
	{
		if(editors.size() == 0)
		{
			stopUpdateTask();
			return;
		}

		Iterator<NavMeshEditor> iterator = editors.iterator();
		while(iterator.hasNext())
		{
			NavMeshEditor editor = iterator.next();
			if(!editor.getPlayer().isOnline())
			{
				iterator.remove();
				PlayerUtil.removeItemStacksFromInventory(editor.getPlayer(), editItemStacks);
				continue;
			}

			editor.update();
		}

		updateCount++;
	}


	// -------
	// EDITING MODE
	// -------
	void executeCommand(Player player, String[] args)
	{
		if(args.length == 0)
		{
			if(isActiveFor(player))
				endEditMode(player);
			else
				startEditMode(player);

			return;
		}

		if(!isActiveFor(player))
			startEditMode(player);

		getEditor(player).executeCommand(args);
	}

	private void startEditMode(Player player)
	{
		if(isActiveFor(player))
			return;

		NavMeshEditor editor = new NavMeshEditor(player);
		editors.add(editor);
		if(!isUpdateTaskRunning())
			startUpdateTask();

		for(ItemStack is : editItemStacks)
			player.getInventory().addItem(is);

		player.sendMessage("NavMesh editing activated");
	}

	void endEditMode(Player player)
	{
		if(!isActiveFor(player))
			return;

		editors.remove(getEditor(player));
		PlayerUtil.removeItemStacksFromInventory(player, editItemStacks);

		player.sendMessage("NavMesh editing deactivated");
		CompitumLib.getNavMeshManager().saveMeshes();
	}

}
