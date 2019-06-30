package de.domisum.iternifexedit.navmesh.edit;

import de.domisum.lib.auxilium.util.StringUtil;
import de.domisum.lib.auxiliumspigot.util.ItemStackBuilder;
import de.domisum.lib.auxiliumspigot.util.player.PlayerUtil;
import de.domisum.lib.iternifex.navmesh.NavMeshRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class NavMeshEditCoordinator
{

	// CONSTANTS
	private static final int TASK_INTERVAL_TICKS = 1;

	// DEPENDENCIES
	private final JavaPlugin plugin;
	private final NavMeshRegistry navMeshRegistry;

	// ITEMSTACKS
	@Getter
	private ItemStack createPointItemStack;
	@Getter
	private ItemStack deletePointItemStack;
	@Getter
	private ItemStack selectPointItemStack;
	@Getter
	private ItemStack deselectPointItemStack;
	@Getter
	private ItemStack createTriangleItemStack;
	@Getter
	private ItemStack deleteTriangleItemStack;
	@Getter
	private ItemStack infoItemStack;
	@Getter
	private ItemStack ladderItemStack;
	@Getter
	private final List<ItemStack> editItemStacks = new ArrayList<>();

	// STATUS
	private final Set<NavMeshEditor> editors = new HashSet<>();
	private BukkitTask tickTask;
	@Getter
	private int tickCount;


	// INIT
	public void initialize()
	{
		createEditItemStacks();
		registerCommand();

		new NavMeshEditListener(plugin, this);
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

		createTriangleItemStack = new ItemStackBuilder(Material.EMERALD)
				.displayName(ChatColor.GREEN+"Create triangle")
				.lore("If 2 points selected, create 3rd point at current location")
				.build();
		deleteTriangleItemStack = new ItemStackBuilder(Material.BLAZE_POWDER)
				.displayName(ChatColor.RED+"Delete triangle")
				.build();
		infoItemStack = new ItemStackBuilder(Material.BOOK).displayName(ChatColor.AQUA+"Point/triangle info").build();
		ladderItemStack = new ItemStackBuilder(Material.STICK)
				.displayName(ChatColor.GOLD+"Create ladder")
				.lore("Sneak while using to cancel ladder creation/ delete ladder")
				.build();

		editItemStacks.add(createPointItemStack);
		editItemStacks.add(deletePointItemStack);
		editItemStacks.add(selectPointItemStack);
		editItemStacks.add(deselectPointItemStack);
		editItemStacks.add(createTriangleItemStack);
		editItemStacks.add(deleteTriangleItemStack);
		editItemStacks.add(infoItemStack);
		editItemStacks.add(ladderItemStack);
	}

	private void registerCommand()
	{
		PluginCommand command = plugin.getCommand("editNavMesh");
		if(command == null)
			throw new IllegalStateException("command 'editNavMesh' not registered in plugin.yml");

		command.setExecutor(new EditNavMeshCommand(this));
	}


	// GETTERS
	public NavMeshEditor getEditor(Player player)
	{
		for(NavMeshEditor editor : editors)
			if(editor.getPlayer() == player)
				return editor;

		return null;
	}

	public boolean isActiveFor(Player player)
	{
		return getEditor(player) != null;
	}

	private boolean isUpdateTaskRunning()
	{
		return tickTask != null;
	}


	// UPDATE
	private void startUpdateTask()
	{
		if(tickTask != null)
			return;

		tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0, TASK_INTERVAL_TICKS);
	}

	private void stopUpdateTask()
	{
		if(tickTask == null)
			return;

		tickTask.cancel();
		tickTask = null;
	}

	private void update()
	{
		if(editors.isEmpty())
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

		tickCount++;
	}


	// EDITING MODE
	public void executeCommand(Player player, String[] args)
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

		if(args.length == 1)
		{
			NavMeshEditor editor = getEditor(player);
			if("snap".equalsIgnoreCase(args[0]))
			{
				editor.setSnapPointsToBlockCorner(!editor.getSnapPointsToBlockCorner());
				player.sendMessage("Snap to block center: "+editor.getSnapPointsToBlockCorner());
				return;
			}
			if("con".equalsIgnoreCase(args[0]))
			{
				editor.setShowTriangleConnections(!editor.getShowTriangleConnections());
				player.sendMessage("Show triangle connections: "+editor.getShowTriangleConnections());
				return;
			}
		}

		String argsRecombined = StringUtil.concat(" ", args);
		player.sendMessage("The arguments '"+argsRecombined+"' are invalid.");
	}

	private void startEditMode(Player player)
	{
		if(isActiveFor(player))
			return;

		NavMeshEditor editor = new NavMeshEditor(this, navMeshRegistry, player);
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
		navMeshRegistry.save();
	}

}
