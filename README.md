# SimpleItemGenerator

## Information
SimpleItemGenerator is a lightweight item generator with simple, but functional configurations. Those configurations support PlaceholderAPI, ItemsAdder and MiniMessage. Another feature of the plugin is big variety of versions starting from 1.8 and ending up to the latest version of the game. 

## Commands
To be able to see simpleitemgenerator command you should have permission `simpleitemgenerator.commands.general`

`/simpleitemgenerator reload` - reloads the plugin. Requires `simpleitemgenerator.commands.reload` permission

`/simpleitemgenerator give <item> <player>` - item is your key of the item that you configured earlier, player is optional.  Requires `simpleitemgenerator.commands.give` permission

## Example
There are some examples of configuring the plugin
An example of a simple item without any functionality. Every field except for material is optional.
```yaml
items:
  item: # item key. Later will be used in commands
    item: # item section
      material: DIAMOND 
      name: <red><bold>Cool diamond # display name. Supports PlaceholderAPI and MiniMessage. Optional
      lore: # lore, same features as above
      - <green>Your name %player_name% # papi support
      - <red>Second lore
      cmd: 1 # custom model data For 1.14+, optional
      unbreakable: true # makes item unbreakable. Set to else by default.
      item-flags: # item flags. Optional
      - HIDE_ATTRIBUTES
      - HIDE_ENCHANTS
      enchantments: # enchantments, optional. For 1.12 and below can vary
        minecraft:luck_of_the_sea: 1
    is-ingredient: true # whether the item can be used in crafting or not. By default, it is false
```

Other examples and information can be found in [wiki](https://github.com/ValeraShimchuck/SimpleItemGenerator/wiki).


## Contacts
You have encountered a bag, or you have an idea of a feature you can create an issue in GitHub.

If you need help, or have any issues when dealing with the plugin you can always reach out me on my Discord Server: https://discord.gg/ksXEuxCqdC.