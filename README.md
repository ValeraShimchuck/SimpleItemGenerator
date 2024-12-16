# SimpleItemGenerator

## Information
SimpleItemGenerator is a lightweight item generator with simple, but functional configurations. Those configurations support PlaceholderAPI, ItemsAdder and MiniMessage. Another feature of the plugin is big variety of versions starting from 1.8 and ending up to the latest version of the game. 

## Commands
To be able to see simpleitemgenerator command you should have permission `simpleitemgenerator.commands.general`

`/simpleitemgenerator reload` - reloads the plugin. Requires `simpleitemgenerator.commands.reload` permission

`/simpleitemgenerator give <item> <player>` - item is your key of the item that you configured earlier, player is optional.  Requires `simpleitemgenerator.commands.give` permission

## Examples
There are some examples of configuring the plugin
### Simple Item
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
      item-flags: # item flags. Optional
      - HIDE_ATTRIBUTES
      - HIDE_ENCHANTS
      enchantments: # enchantments, optional. For 1.12 and below can vary
        minecraft:luck_of_the_sea: 1
```

### ItemsAdder usage
```yaml
items: # usage of items adder as a display of the item
  item: '[itemsadder] examplespace:key'
```

### Simple functionality
```yaml
items:
  item1:
    item: ... # item section
    usage: '[console] say %player% hi, you used me!' # commands can be executed as a console or as a player
  item2:
    item: ... # item section
    usage: # this way you can issue multiple commands
    - '[console] say %player% hi, you used me!'
    - '[player] open_some_menu'
  item3:
    item: ... # item section
    usage: # this way you can issue multiple commands
      predicate: '[button] right' # works only on right click. Can be left|right|drop
      cancel: false # by default, it's true, but if you want the item to be used you can set it to false 
      commands: '[player] your_open_menu_command_for_instance'
```

### Advanced functionality
```yaml
items:
  item1:
    item: ... # item section
    usage: # this way you can issue multiple commands
      cooldown: 5s # written in 10m5s like format, support h-hours, m-minutes, s-seconds and millis if unit not specified
      commands: # also you can use %minimessage_(your minimessage text here)% to replace it with json when command is issued
      - '[console] tellraw %player% %minimessage_<red>I am using minimessage in placeholder%'
      - '[console] tellraw %player% %minimessage_<bold><green>So you don''t have to use json%'
  item2:
    item: ... # item section
    cooldown: 5s
    on-cooldown: 
    - '[console] say %player% item is on cooldown! Wait %time_s.2f%s to use again!' # you can use time placeholder to display in ever way you want
    - '[console] tellraw %player% %minimessage_<red>Another example of the time placeholder %time_t% %time_h.5f% %time_m.3f% %'
    freezetime: 1s # this field can keep you for a while from spamming on-cooldown commands after them being issued
    commands: '[console] say %player% hi, you used me!'
    predicate: '[at] block' # only works when click at a block. Also supports air|entity|player
  item3:
    item: ... # item section
    usage:
      cooldown: 3h 5m 3s 200 # also you can use some spaces between time tokens 
      commands:
      - '[console] tellraw %player% %minimessage_<red>I am using minimessage in placeholder%'
      - '[console] tellraw %player% %minimessage_<bold><green>So you don''t have to use json%'
      predicate: # predicates can be used with at and button together. In that case it will only work when clicked at air with left mouse button
        at: air
        button: left
```

### Complex functionality
Well, I will show you how hard item settings can be

```yaml
items:
  omega-item:
    item: ... # item section
    usage: # by the way, you can create multiple usages
    - commands: '[console] tellraw %minimessage_<red>You clicked somewhere%'
      cooldown: 5s
      on-cooldown: '[console] say %player% item is on cooldown! Wait %time_s.2f%s to use again!'
      freezetime: 100 # 100 millis
      predicate: # predicate can be a list. In that case it works when clicked at air with LMB or when clicked at block
      - at: air
        button: left
      - '[at] block'
    - commands: '[console] tellraw %minimessage_<green>You clicked at entity%'
      predicate: '[at] entity'
      cancel: false
    - '[console] tellraw %minimessage_<green>I am issued every time you interact with me%player%'
```

# Contacts
You have encountered a bag, or you have an idea of a feature you can create an issue in GitHub.

If you need help, or have any issues when dealing with the plugin you can always reach out me on Discord: valerii_dev or if for some reason you can't, then you can always write me to the email: balerii.work@gmail.com (I always check it, because of potential job offers, so you won't be ignored)

Also, my spigot forum account: valera6666