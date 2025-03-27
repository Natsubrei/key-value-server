package handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import store.StoreFactory;

import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
public class RequestHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String command = (String) msg;
        // log.debug("command: {}", command);
        String[] s = command.split(" ");
        ConcurrentHashMap<String, String> map = StoreFactory.getMap();
        String OPERATE_SUCCESS = "OK\n";
        switch (s[0]) {
            case "PUT":
                map.put(s[1], s[2]);
                ctx.writeAndFlush(OPERATE_SUCCESS);
                break;
            case "GET":
                ctx.writeAndFlush(map.get(s[1]) + '\n');
                break;
            case "DELETE":
                map.remove(s[1]);
                ctx.writeAndFlush(OPERATE_SUCCESS);
                break;
            case "EXIT":
                ctx.channel().close();
                break;
        }
    }
}
