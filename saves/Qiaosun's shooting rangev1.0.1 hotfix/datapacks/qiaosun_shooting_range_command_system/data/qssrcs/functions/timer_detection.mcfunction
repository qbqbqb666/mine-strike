#【CQB】
#开始计时，运行
#-------------------------------------------------------------------------------------------------------------------
#BUGFIX：增加tag无需重复执行
execute as @p[x=50,y=85,z=454,dy=2,dz=2,tag=!kh1p] at @s if block ~ ~-1 ~ minecraft:yellow_wool run function qssrcs:timer_reset

execute if block 52 68 460 minecraft:redstone_block run fill 51 86 454 51 87 455 minecraft:red_concrete
#s进位
execute as @e[tag=kh1,limit=1,scores={tt=20}] run function qssrcs:timer_s

#m进位
execute as @e[tag=kh1,scores={s=60}] run function qssrcs:timer_m

#挡路
#BUGFIX:你到底怎么设计的这么乱
#execute if block 52 68 460 minecraft:air run fill 51 86 454 51 87 455 air
#------------------------------------------------------------------------------------------------------------------

execute store result score @e[tag=kh1] hit run execute if entity @e[tag=hit]


#停止
#------------------------------------------------------------------------------------
#BUGFIX：调整执行顺序
execute as @p at @s if block ~ ~-1 ~ minecraft:red_wool run setblock 52 68 460 minecraft:air
execute as @a[x=25,y=74,z=439,dx=2,dy=1,dz=1] run function qssrcs:timer_stop

#无用指令
#execute as @p at @s if block ~ ~-1 ~ minecraft:red_wool run setblock 52 72 458 minecraft:air



#------------------------------------------------------------------------------------


#赋值
scoreboard players operation Hit board = @e[tag=kh1] hit
scoreboard players operation Second board = @e[tag=kh1,limit=1] s
scoreboard players operation Min board = @e[tag=kh1,limit=1] m
#--------------------------------------------------------------------------------------------------------------------