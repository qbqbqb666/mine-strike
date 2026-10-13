tp @a[x=25,y=74,z=439,dx=2,dy=1,dz=1] 55.864 85.5 455.201
title @p[x=55,y=85,z=455] actionbar "Finished!"
setblock 22 71 431 minecraft:redstone_block
clone -182 34 445 -151 50 498 14 63 436 replace force
kill @e[type=item]
#优化：如何保留战绩，hit
#tag @e[tag=targetkh1] remove hit
tag @e[tag=kh1p] remove kh1p
#BUGFIX
fill 51 86 454 51 87 455 air