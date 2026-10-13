#【大门】
execute if block 25 65 19 minecraft:redstone_block run setblock 23 64 19 minecraft:redstone_block
execute as @e[tag=gate1,limit=1,scores={tt=10}] run fill 45 64 17 34 64 18 air


execute as @e[tag=gate1,limit=1,scores={tt=20}] run fill 45 65 17 34 66 18 air


execute as @e[tag=gate1,limit=1,scores={tt=30}] run fill 45 67 17 34 68 18 air


execute as @e[tag=gate1,limit=1,scores={tt=40}] run fill 45 69 17 34 70 18 air



execute as @e[tag=gate1,limit=1,scores={tt=40}] run setblock 23 64 19 minecraft:air
execute as @e[tag=gate1,limit=1,scores={tt=40}] run scoreboard players reset @e[tag=gate1] tt

execute if block 25 65 19 minecraft:redstone_block run setblock 23 64 19 minecraft:redstone_block
execute as @e[tag=gate1,limit=1,scores={tt=10}] run fill 45 64 17 34 64 18 air



execute as @e[tag=gate1,limit=1,scores={tt=20}] run fill 45 65 17 34 66 18 air



execute as @e[tag=gate1,limit=1,scores={tt=30}] run fill 45 67 17 34 68 18 air


execute as @e[tag=gate1,limit=1,scores={tt=40}] run fill 45 69 17 34 70 18 air


execute as @e[tag=gate1,limit=1,scores={tt=40}] run setblock 23 64 19 minecraft:air
execute as @e[tag=gate1,limit=1,scores={tt=40}] run scoreboard players reset @e[tag=gate1] tt