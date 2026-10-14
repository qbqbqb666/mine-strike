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
