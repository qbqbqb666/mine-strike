import dev.minestrike.skins.SkinNetwork;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;

public class SkinNetworkTest {
    public static void main(String[] args) {
        FriendlyByteBuf requestBytes = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf stateBytes = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SkinNetwork.Request request = new SkinNetwork.Request(true, "{\"ak\":\"mcs2:cs_ak_asiimov\"}");
            SkinNetwork.Request.encode(request, requestBytes);
            if (!request.equals(SkinNetwork.Request.decode(requestBytes))) throw new AssertionError("Selection request round trip");
            SkinNetwork.State state = new SkinNetwork.State("{\"groups\":[]}", request.choices(), true, false, "本局外观已锁定。");
            SkinNetwork.State.encode(state, stateBytes);
            if (!state.equals(SkinNetwork.State.decode(stateBytes))) throw new AssertionError("Chinese text and lock state round trip");
            SkinNetwork.LobbyAction action = new SkinNetwork.LobbyAction("ready");
            requestBytes.clear(); SkinNetwork.LobbyAction.encode(action, requestBytes);
            if (!action.equals(SkinNetwork.LobbyAction.decode(requestBytes))) throw new AssertionError("Lobby action round trip");
            SkinNetwork.LobbyState lobby = new SkinNetwork.LobbyState("{\"total\":3,\"readyCount\":2,\"message\":\"等待全员准备\"}");
            stateBytes.clear(); SkinNetwork.LobbyState.encode(lobby, stateBytes);
            if (!lobby.equals(SkinNetwork.LobbyState.decode(stateBytes))) throw new AssertionError("Lobby state round trip");
            stateBytes.clear();
            SkinNetwork.ShopShape shape = new SkinNetwork.ShopShape(new int[]{8,4,7,5,5,0});
            SkinNetwork.ShopShape.encode(shape,stateBytes);
            if (!java.util.Arrays.equals(shape.counts(),SkinNetwork.ShopShape.decode(stateBytes).counts())) throw new AssertionError("Independent counts round trip");
            requestBytes.clear();
            var edit = new SkinNetwork.ShopEditAction(7,"delete",5,20);
            SkinNetwork.ShopEditAction.encode(edit,requestBytes);
            if (!edit.equals(SkinNetwork.ShopEditAction.decode(requestBytes))) throw new AssertionError("Independent delete request");
            requestBytes.clear();
            var save = new SkinNetwork.ShopSave(3,"神秘武器 · 测试",16000,120,-1,2,true);
            SkinNetwork.ShopSave.encode(save,requestBytes);
            if (!save.equals(SkinNetwork.ShopSave.decode(requestBytes))) throw new AssertionError("Item name and fields round trip");
            requestBytes.clear();
            var pick = new SkinNetwork.ShopPick(4,35);
            SkinNetwork.ShopPick.encode(pick,requestBytes);
            if (!pick.equals(SkinNetwork.ShopPick.decode(requestBytes))) throw new AssertionError("Inventory selection round trip");
            requestBytes.clear();
            var back = new SkinNetwork.ShopBack(4);
            SkinNetwork.ShopBack.encode(back,requestBytes);
            if (!back.equals(SkinNetwork.ShopBack.decode(requestBytes))) throw new AssertionError("Back request round trip");
            boolean rejected = false;
            try { SkinNetwork.Request.encode(new SkinNetwork.Request(true, "x".repeat(8193)), requestBytes); }
            catch (EncoderException e) { rejected = true; }
            if (!rejected) throw new AssertionError("Packet size limit");
            System.out.println("PASS: selection request, Chinese state message, lock flag and packet size bounds");
        } finally {
            requestBytes.release(); stateBytes.release();
        }
    }
}
