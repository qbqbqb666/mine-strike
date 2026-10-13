#被交互过的实体给最近的玩家添加gunplayer标签
execute as @e[tag=interactok] at @s run tag @p add gunplayer
#给那个玩家赋值
execute as @e[tag=interactok] run scoreboard players operation @p[tag=gunplayer] gunindex = @s gunindex
execute as @a[tag=gunplayer] run function qssrcs:give
tag @a[tag=gunplayer] remove gunplayer
#清理交互实体tag
execute as @e[tag=interactok] run data remove entity @s interaction
execute as @e[tag=interactok] run data remove entity @s attack
tag @e[tag=interactok] remove interactok