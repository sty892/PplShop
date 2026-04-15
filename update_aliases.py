import json
import codecs

with codecs.open('src/client/resources/assets/pplshop/default-config/item_aliases.json', 'r', 'utf-8') as f:
    data = json.load(f)

new_aliases = {
  "minecraft:trial_key": ["обычный ключ", "обычные ключи", "ключа"],
  "minecraft:sniffer_egg": ["яйцо нюхача", "голубые яйца"],
  "minecraft:ancient_debris": ["незер обломки", "незеритовые обломки", "незерит лом", "обломок незерит"],
  "minecraft:mace": ["булава", "чар булавы", "булавы"],
  "minecraft:silence_armor_trim_smithing_template": ["отделка тишина шаблон", "отделка тишина"],
  "minecraft:sculk_shrieker": ["скалкавая штука", "скалк крикун"],
  "minecraft:mycelium": ["мицелий", "грибы мицелий"],
  "minecraft:chorus_fruit": ["плоды хоруса", "хорус"],
  "minecraft:nether_quartz_ore": ["незерский кварц", "незеркварц", "кварцевая руда"],
  "minecraft:potato": ["картофель", "блок картошки", "картошка", "печеный картофель"],
  "minecraft:turtle_helmet": ["черепаш панцирь", "панцирь черепахи"],
  "minecraft:music_disc_creator": ["лена рейн creator", "creator пластинка"],
  "minecraft:resin_brick": ["смоляные слитки", "смоляной кирпич"],
  "minecraft:tnt": ["патроны мелкая дробь", "динамит"],
  "minecraft:totem_of_undying": ["тотемыыфе", "тотем"],
  "minecraft:emerald": ["изумруды", "золотоизумруды"],
  "minecraft:gold_ingot": ["золото", "золотоизумруды"],
  "minecraft:magician_book": ["книга и перо"], # Assuming customizable books
  "minecraft:written_book": ["война и мир", "книга и перо"],
  "minecraft:netherite_chestplate": ["кастомная нг броня"],
  "minecraft:netherite_sword": ["незеритовыеf мечи"],
  "minecraft:netherite_axe": ["топоры"],
  "minecraft:pumpkin_pie": ["пироги", "тыквенный пирог", "пироги с тыквой"],
  "minecraft:potion": ["анти эндер пойло", "эссенция фембоя"],
  "minecraft:blue_ice": ["хрустики блоки снега", "плотный лед"],
  "minecraft:tall_grass": ["низкая трава", "высокая трава", "низкая высокая сухая трава"]
}

for key, aliases_list in new_aliases.items():
    if key not in data:
        data[key] = aliases_list
    else:
        if isinstance(data[key], dict):
            existing_aliases = data[key].get("aliases", [])
            for alias in aliases_list:
                if alias not in existing_aliases:
                    existing_aliases.append(alias)
            data[key]["aliases"] = existing_aliases
        elif isinstance(data[key], list):
            for alias in aliases_list:
                if alias not in data[key]:
                    data[key].append(alias)

for key in data:
    if isinstance(data[key], list):
        data[key] = list(dict.fromkeys(data[key]))

with codecs.open('src/client/resources/assets/pplshop/default-config/item_aliases.json', 'w', 'utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("BATCH 5 MERGE DONE")
