# SchematicManager
The schematic system FlakePVP uses to ensure performance.
- Clipboards are cached. You won't need to reasd the disk again.
- Schematics are queued to ensure that no two schematics load at the same time, ensuring performance no matter what.

These changes to schematic loading ensure that the server will run smoothly.
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
