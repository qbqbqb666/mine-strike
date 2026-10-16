import dev.minestrike.skins.ShopRows;
import java.util.*;

public class ShopRowsTest {
    public static void main(String[] args) {
        List<List<Object>> columns=new ArrayList<>();
        for(int col=0;col<6;col++)columns.add(new ArrayList<>());
        Object first=new Object(),second=new Object(),third=new Object(),other=new Object();
        columns.get(1).addAll(List.of(first,second,third));columns.get(4).add(other);
        ShopRows.remove(columns.get(1),1);
        assert columns.get(1).equals(List.of(first,third)):"Only following goods in the selected category move up";
        assert columns.get(4).equals(List.of(other)):"Other categories unchanged";
        Object added=new Object();ShopRows.add(columns.get(5),added);
        assert columns.get(5).equals(List.of(added))&&columns.get(0).isEmpty();
        ShopRows.remove(columns.get(5),0);assert columns.get(5).isEmpty();
        assert ShopRows.rows(columns)==2;
        for(int row=0;row<=32;row++)for(int col=0;col<6;col++) {
            int index=ShopRows.index(row,col);assert ShopRows.row(index)==row&&ShopRows.column(index)==col;
        }
        assert ShopRows.index(5,5)==35;
        List<Object> full=new ArrayList<>();for(int i=0;i<32;i++)ShopRows.add(full,new Object());
        boolean rejected=false;try{ShopRows.add(full,new Object());}catch(IllegalArgumentException e){rejected=true;}assert rejected;
        for(int rows:new int[]{0,1,5,8,32})for(int[] screen:new int[][]{{1920,1080},{960,540},{640,360}}) {
            float scale=ShopRows.scale(screen[0],screen[1],rows);
            assert (190+ShopRows.bodyHeight(rows))*scale<=screen[1]+0.01;
            assert 1080*scale<=screen[0]+0.01;
        }
        System.out.println("PASS: independent category add/delete, following-goods compaction, empty categories, sixth-column indices, limits and viewport fit");
    }
}
