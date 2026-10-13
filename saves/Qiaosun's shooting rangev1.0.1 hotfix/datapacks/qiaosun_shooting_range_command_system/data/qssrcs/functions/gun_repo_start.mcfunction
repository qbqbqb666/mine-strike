execute as @e[tag=gun_repo,type=interaction] if data entity @s interaction run tag @s add interactok
execute as @e[tag=gun_repo,type=interaction] if data entity @s attack run tag @s add interactok

execute if entity @e[tag=interactok] run function qssrcs:gun_repo