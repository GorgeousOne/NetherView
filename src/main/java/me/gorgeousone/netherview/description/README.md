nobody reads this

## What happens in NetherView  
This is a simplified description of what happens in NetherView in order to display another world through a nether portal:

**1. Locating portals**

When a player uses nether portal the 2 locations of the teleport are given to the PortalLocator to find out position and size of each portal.
The information is wrapped in instances of the Portal class and stored by the PortalHandler singelton.

**2. Saving the blocks around a portal**

In the next step a cache of blocks for each portal is created. 
This BlockCache, modelled by the BlockCacheFactory, is a copy of all blocks in a cuboid area around the portal. 
Again, the PortalHandler stores the block caches for each portal so other portals can access these BlockCopys later.

**3. Linking two portals**

So as soon as a player teleports with a portal it is clear from which other portal the blocks need to be displayed in the portal frame for a player. 
Therefore, a PortalLink is created to store the connection between a portal and it's counter portal.
The link calculates a Transform which contains information about translation and rotation needed to move the blocks from one portal to the other one.  
(The rotation is needed when portals are crosswise to each other and because one always gets spun around by 180 degrees when going through a portal)

The transform is then applied to a copy of the original block cache of the counter portal which stored inside the PortalLink (which is also stored by the PortalHandler).
With this copy it is already possible to tell which blocks are availabe to be displayed at exact locations behind the portal.  

**4. Displaying the blocks to the player**

To create the illusion for a player that another world is visible behind a portal when standing infront of it 
the plugin needs to narrow down the exact area a player can see through a portal frame.
Since a portal frame always forms a rectangle the visible area can be described very well with a viewing frustum.
This ViewingFrustum is being calculated by the ViewingFrustumFactory and used in the ViewingHandler.

In the last step the ViewingHandler simply has to iterate through the BlockCopies in the copied BlockCache and check which blocks are 
contained by a player's viewing frustum or at least scratching it with any corner. 
The filtered blocks are then displayed to the player as fake blocks with Player#sendBlockChange().
