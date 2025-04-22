# SchematicManager
The schematic system FlakePVP uses to ensure performance.
- Clipboards are cached. You won't need to reasd the disk again.
- Schematics are queued to ensure that no two schematics load at the same time, ensuring performance no matter what.

These changes to schematic loading ensure that the server will run smoothly.
## Results
```
-- Server Specs (Dev Server) --
CPU: Ampere 3.0 GHz ARM Processor (unoptimized for Minecraft)
RAM: 24 GB RAM
SSD: 200 GB Storage

-- Task --
TASK: for(int i = 0; i < 18; i++) { schematicManager.enqueueSchematic(duel.getMotherLocation(), schematicFile, pasteAir); }
pasteAir: true
Expanded vertically: true
Blocks: ~5,078,400 * 18

-- Result --
Average CPU Usage: 80-100%/400% (mostly 80%)
Average Paste Time Per Arena: 0.6 seconds
Average Blocks Per Second: 8,464,000
Total Paste Time: 10.8 seconds
```
These results were grabbed with FlakePVP's Duel System, a remix of MetalMC's.

## NOTICE
Ensure you have **ONE** instance of this class **GLOBALLY!** Or else you'll have a separate queue for each plugin.
You may need to add a getter for your plugin.
### Field, Getter, and Setter Examples
```java
/* !! DEFINE THIS IN A CORE PLUGIN OR A PLUGIN THAT HANDLES SCHEMATICS !! */

// Field
SchematicManager schematicManager = new SchematicManager();

// Getter
public SchematicManager getSchematicManager() {
  return schematicManager;
}

// A setter may not be needed, but here's an example.
// Setter
public void setSchematicManager(SchematicManager schematicManager) {
  this.schematicManager = schematicManager;
}
```
