#重置
scoreboard players reset @e[tag=kh1] tt
scoreboard players reset @e[tag=kh1] s
scoreboard players reset @e[tag=kh1] m
#优化
tag @e[tag=targetkh1] remove hit

execute as @p at @s if block ~ ~-1 ~ minecraft:yellow_wool run tag @p add kh1p
setblock 52 68 460 minecraft:redstone_block
playsound minecraft:entity.experience_orb.pickup master @a[tag=kh1p] 50 86 455 0.1 2

scoreboard objectives setdisplay sidebar board