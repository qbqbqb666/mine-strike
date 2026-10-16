"""Render final agent PNGs on the current player geometry; no Minecraft files are changed."""
from pathlib import Path
import json
import numpy as np
from PIL import Image, ImageDraw, ImageFont
root=Path(__file__).resolve().parent
ratios=json.loads((root.parents[1]/"build/player-shape-mesh.json").read_text())
corners = np.array([[-1,-1,-1],[1,-1,-1],[1,1,-1],[-1,1,-1],[-1,-1,1],[1,-1,1],[1,1,1],[-1,1,1]], float)
faces = [[5,4,0,1],[2,3,7,6],[0,4,7,3],[1,0,3,2],[5,1,2,6],[4,5,6,7]]

def cube(origin, size, uv_origin, pivot, scale=(1,1,1), inflate=0):
    w,h,d = size
    u,v = uv_origin
    rects = [(u+d,v,u+d+w,v+d),(u+d+w,v+d,u+d+2*w,v),
             (u,v+d,u+d,v+d+h),(u+d,v+d,u+d+w,v+d+h),
             (u+d+w,v+d,u+2*d+w,v+d+h),(u+2*d+w,v+d,u+2*d+2*w,v+d+h)]
    xyz = (corners+1)/2 * (np.array(size)+2*inflate)+np.array(origin)-inflate
    result = []
    for indices, (u1,v1,u2,v2) in zip(faces, rects):
        pos = xyz[indices]
        normal = np.cross(pos[1]-pos[0],pos[2]-pos[0]); normal /= np.linalg.norm(normal)
        uv = np.array([[u2,v1],[u1,v1],[u1,v2],[u2,v2]])/64
        result.append((pos*np.array(scale)+np.array(pivot),uv,np.tile(normal,(4,1))))
    return result

def figure(changed):
    width, head, arm, height = (ratios["body"],ratios["head"],ratios["arm"],ratios["height"]) if changed else (1,1,1,1)
    quads=[]
    for origin,size,uv,pivot in [
        ((-4,0,-2),(8,12,4),(16,16),(0,0,0)),
        ((-3,-2,-2),(4,12,4),(40,16),(-5,2,0)),
        ((-1,-2,-2),(4,12,4),(32,48),(5,2,0)),
        ((-2,0,-2),(4,12,4),(0,16),(-1.9,12,0)),
        ((-2,0,-2),(4,12,4),(16,48),(1.9,12,0)),
    ]:
        is_arm=abs(pivot[0])==5
        local_width=arm if is_arm else width
        correction=np.sign(pivot[0])*(width-arm) if is_arm else 0
        pivot=(pivot[0]*width-correction,24+(pivot[1]-24)*height,pivot[2]*width)
        quads.extend(cube(origin,size,uv,pivot,(local_width,height,local_width)))
    quads.extend(cube((-4,-8,-4),(8,8,8),(0,0),(0,24*(1-height),0),(head,head,head)))
    quads.extend(cube((-4,-8,-4),(8,8,8),(32,0),(0,24*(1-height),0),(head,head,head),inflate=.5))
    return quads

def render(quads, angle, pixels=17, offset=80):
    w,h=260,540
    image=np.full((h,w,3),[24,32,43],np.uint8); depth=np.full((h,w),np.inf)
    a=np.deg2rad(angle)
    rotation=np.array([[np.cos(a),0,np.sin(a)],[0,1,0],[-np.sin(a),0,np.cos(a)]])
    light=np.array([-.4,-.7,-1]); light/=np.linalg.norm(light)
    for pos,uv,normal in quads:
        p=pos@rotation.T; n=normal@rotation.T
        screen=p[:,:2]*pixels+np.array([w/2,offset])
        for ids in ([0,1,2],[0,2,3]):
            xy=screen[ids]; z=p[ids,2]; tex=uv[ids]; norms=n[ids]
            lo=np.maximum(np.floor(xy.min(0)).astype(int),0); hi=np.minimum(np.ceil(xy.max(0)).astype(int),[w-1,h-1])
            if (lo>hi).any(): continue
            xx,yy=np.meshgrid(np.arange(lo[0],hi[0]+1)+.5,np.arange(lo[1],hi[1]+1)+.5)
            denominator=(xy[1,1]-xy[2,1])*(xy[0,0]-xy[2,0])+(xy[2,0]-xy[1,0])*(xy[0,1]-xy[2,1])
            if abs(denominator)<1e-8:continue
            b0=((xy[1,1]-xy[2,1])*(xx-xy[2,0])+(xy[2,0]-xy[1,0])*(yy-xy[2,1]))/denominator
            b1=((xy[2,1]-xy[0,1])*(xx-xy[2,0])+(xy[0,0]-xy[2,0])*(yy-xy[2,1]))/denominator
            bary=np.stack([b0,b1,1-b0-b1],axis=-1)
            zz=bary@z; coords=np.clip((bary@tex*skin.shape[0]).astype(int),0,skin.shape[0]-1)
            rgba=skin[coords[:,:,1],coords[:,:,0]]
            region=depth[lo[1]:hi[1]+1,lo[0]:hi[0]+1]
            mask=(bary>=-1e-7).all(-1)&(zz<region)&(rgba[:,:,3]>127)
            normals=bary@norms; normals/=np.maximum(np.linalg.norm(normals,axis=-1,keepdims=True),1e-8)
            brightness=.55+.45*np.maximum(0,normals@light)
            color=(rgba[:,:,:3]*brightness[:,:,None]).astype(np.uint8)
            region[mask]=zz[mask]
            image[lo[1]:hi[1]+1,lo[0]:hi[0]+1][mask]=color[mask]
    return Image.fromarray(image)

if __name__ == "__main__":
    manifest=json.loads((root/"manifest.json").read_text())
    font=ImageFont.load_default(size=22)
    small=ImageFont.load_default(size=16)
    for team in ("ct","t"):
        agents=[a for a in manifest["agents"] if a["id"].startswith(team)]
        sheet=Image.new("RGB",(260*4,600*len(agents)+85),(17,24,34));draw=ImageDraw.Draw(sheet)
        draw.text((24,16),f"{team.upper()} AGENTS / head 55% / width 65% / length 85%",font=font,fill="white")
        draw.text((24,48),"Final texture on current geometry | front / 3-quarter / side / back | offline preview",font=small,fill=(170,185,200))
        for row,agent in enumerate(agents):
            skin=np.array(Image.open(root/agent["texture"]).convert("RGBA"))
            y=85+600*row
            for col,angle in enumerate((0,35,90,180)):
                sheet.paste(render(figure(True),angle),(col*260,y+30))
            draw.text((24,y+8),agent["label"]+"  /  "+agent["name"],font=font,fill=(120,200,255) if team=="ct" else (240,180,100))
        sheet.save(root/(team+"-preview.png"))
        print(root/(team+"-preview.png"))
    overview=Image.new("RGB",(1300,1280),(17,24,34));draw=ImageDraw.Draw(overview)
    draw.text((24,18),"MINE STRIKE / AGENT SKINS V1 / CURRENT PLAYER GEOMETRY",font=font,fill="white")
    draw.text((24,50),"CT above / T below | head 55%, width 65%, body length 85% | offline texture preview",font=small,fill=(170,185,200))
    for index,agent in enumerate(manifest["agents"]):
        x=(index%5)*260;y=90+(index//5)*590
        skin=np.array(Image.open(root/agent["texture"]).convert("RGBA"))
        overview.paste(render(figure(True),0),(x,y+20))
        draw.text((x+12,y+2),agent["label"],font=font,fill=(120,200,255) if agent["id"].startswith("ct") else (240,180,100))
    overview.save(root/"overview.png")
    print(root/"overview.png")
