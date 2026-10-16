"""Technical atlas normalization: no repainting; snap generated UVs to native base regions."""
from pathlib import Path
import json
import numpy as np
from PIL import Image

def pack(source, destination):
    im=Image.open(source).convert('RGBA').resize((512,512),Image.Resampling.LANCZOS)
    arr=np.array(im)
    # Generated head island can have a margin; align its occupied horizontal extent.
    occupied=np.argwhere(arr[:120,:272,3]>160)
    if len(occupied):
        x0,x1=int(occupied[:,1].min()),int(occupied[:,1].max())+1
        head=Image.fromarray(arr[:128,x0:x1]).resize((256,128),Image.Resampling.LANCZOS)
        arr[:128]=0
        arr[:128,:256]=np.array(head)
    # Snap the variable-height generated strips to the native four-pixel caps
    # and twelve-pixel sides. This is UV packing, not a change to the artwork.
    raw=arr.copy()
    counts=(raw[:,:,3]>160).sum(1)
    upper=np.where((np.arange(512)>=140)&(np.arange(512)<330)&(counts>370))[0]
    if len(upper):
        side,end=int(upper[0]),int(upper[-1])+1
        arr[128:160]=np.array(Image.fromarray(raw[128:side]).resize((512,32),Image.Resampling.LANCZOS))
        arr[160:256]=np.array(Image.fromarray(raw[side:end]).resize((512,96),Image.Resampling.LANCZOS))
        lower=np.where((np.arange(512)>end+20)&(counts>195))[0]
        if len(lower):
            start_side,end_low=int(lower[0]),int(lower[-1])+1
            tops=np.where((np.arange(512)>end+20)&(np.arange(512)<start_side)&(counts>70))[0]
            top=int(tops[0]) if len(tops) else start_side-32
            arr[384:416]=np.array(Image.fromarray(raw[top:start_side]).resize((512,32),Image.Resampling.LANCZOS))
            arr[416:512]=np.array(Image.fromarray(raw[start_side:end_low]).resize((512,96),Image.Resampling.LANCZOS))
    # The reliable right-arm and left-leg drawings provide matching opposite
    # limbs. Mirror faces and exchange side faces, rather than copying jeans
    # into the left-arm slot as some generated sources do.
    def rects(u,v):
        return [(u+4,v,u+8,v+4),(u+8,v,u+12,v+4),(u,v+4,u+4,v+16),(u+4,v+4,u+8,v+16),(u+8,v+4,u+12,v+16),(u+12,v+4,u+16,v+16)]
    for source_uv,target_uv in [((40,16),(32,48)),((16,48),(0,16))]:
        src=rects(*source_uv);dst=rects(*target_uv)
        for index,other in enumerate([0,1,4,3,2,5]):
            x0,y0,x1,y1=src[other];a,b,c,d=dst[index]
            arr[b*8:d*8,a*8:c*8]=arr[y0*8:y1*8,x0*8:x1*8][:,::-1].copy()
    mask=np.zeros((512,512),bool)
    reports=[]
    for name,u,v,w,h,d in [('head',0,0,8,8,8),('body',16,16,8,12,4),('right_arm',40,16,4,12,4),('right_leg',0,16,4,12,4),('left_leg',16,48,4,12,4),('left_arm',32,48,4,12,4)]:
        part=np.zeros_like(mask)
        for x0,y0,x1,y1 in [(u+d,v,u+d+2*w,v+d),(u,v+d,u+2*d+2*w,v+d+h)]:
            part[y0*8:y1*8,x0*8:x1*8]=True
        good=part&(arr[:,:,3]>160)
        coverage=float(good.sum()/part.sum())
        # Fill only transparent edge gaps from nearest opaque pixel within this UV island.
        missing=np.argwhere(part&~good); candidates=np.argwhere(good)
        if not len(candidates):raise ValueError(name+' is empty')
        for y,x in missing:
            nearby=candidates[np.argmin(((candidates-[y,x])**2).sum(1))]
            arr[y,x,:3]=arr[nearby[0],nearby[1],:3]
        mask|=part
        reports.append({'part':name,'source_opaque_coverage':round(coverage,4),'edge_pixels_filled':len(missing)})
    arr[:,:,3]=np.where(mask,255,0)
    arr[~mask,:3]=0
    Image.fromarray(arr).save(destination)
    return reports

if __name__=='__main__':
    import sys
    print(json.dumps(pack(sys.argv[1],sys.argv[2])))
