
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Queue And Caching Schematic Management System.
 * Use a global version of this in a Core plugin.
 *
 * @author Critical <3
 * @version 1.0
 */
public class SchematicManager {

    private final JavaPlugin plugin; // Reference to your main plugin class
    private final Queue<PasteTask> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isPasting = new AtomicBoolean(false);

    // Cache for schematics, mapping the schematic file's name to its Clipboard.
    private final HashMap<String, Clipboard> cacheSchem = new HashMap<>();

    // Basic constructor, pass in your JavaPlugin class. (The variable "this" inside your main class)
    public SchematicManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Enqueue a schematic paste task.
     * Schematic files are put in a cache once pasted.
     *
     * @param location where to paste the schematic
     * @param file     schematic file
     * @param air      whether to paste air blocks (true = include air blocks)
     */
    public void enqueueSchematic(Location location, File file, boolean air) {
        queue.offer(new PasteTask(location, file, air));
        processNext();
    }

    /**
     * Process the next schematic in the queue, if any.
     */
    private void processNext() {
        if (isPasting.get() || queue.isEmpty()) {
            return;
        }
        if (!isPasting.compareAndSet(false, true)) {  // Atomic check to ensure only one paste is active.
            return;
        }
        final PasteTask task = queue.poll();
        if (task == null) {
            isPasting.set(false);
            return;
        }
        // Run the paste operation asynchronously so as not to block the main thread.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = pasteHelper(task.location, task.file, task.air);
            if (!success) {
                plugin.getLogger().warning("Failed to paste schematic: " + task.file.getName());
            }
            // Optionally add a short delay before processing the next task.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                isPasting.set(false);
                plugin.getLogger().info("Finished pasting queued schematic.");
                processNext();
            }, 5L);
        });
    }

    /**
     * Paste a schematic at the given location.
     * Returns true if the paste operation was successful, false otherwise.
     *
     * @param location where to paste the schematic
     * @param file     schematic file
     * @param air      whether to paste air blocks (true = include air blocks)
     * @return success state as a boolean.
     */
    public boolean pasteHelper(Location location, File file, boolean air) {
        // Try to get the cached clipboard.
        Clipboard clipboard = cacheSchem.get(file.getName());

        // Find the appropriate format for the schematic file.
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            plugin.getLogger().warning("Unsupported schematic file: " + file.getName());
            return false;
        }

        // Compute the paste position as a BlockVector3.
        BlockVector3 pastePos = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        try {
            if (location.getWorld() == null) {
                plugin.getLogger().warning("World is null for location: " + location);
                return false;
            }

            // If clipboard isn't already cached, load it from file and cache it.
            if (clipboard == null) {
                plugin.getLogger().warning("The schematic pasted was not cached. Caching.");
                try (FileInputStream fis = new FileInputStream(file);
                     ClipboardReader reader = format.getReader(fis)) {
                    clipboard = reader.read();
                    cacheSchem.put(file.getName(), clipboard);
                }
            }

            World weWorld = BukkitAdapter.adapt(location.getWorld());
            // Create a new edit session with fastMode enabled.
            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder().world(weWorld).fastMode(true).build()) {
                // Build and complete the paste operation.
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                Operation op = holder.createPaste(editSession)
                        .to(pastePos)
                        .ignoreAirBlocks(!air)
                        .build();
                Operations.complete(op);
                editSession.flushQueue();
            }
            return true;
        } catch (IOException | WorldEditException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Internal class to hold schematic paste tasks.
     */
    private static class PasteTask {
        final Location location;
        final File file;
        final boolean air;

        PasteTask(Location location, File file, boolean air) {
            this.location = location;
            this.file = file;
            this.air = air;
        }
    }
}
