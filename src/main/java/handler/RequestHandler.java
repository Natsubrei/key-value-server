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
        ConcurrentHashMap<String, String> map = StoreFactory.getMap();

        String command = (String) msg;
        String[] s = command.split(" ");
        // log.debug("command: {}", command);

        switch (s[0]) {
            case "PUT":
                map.put(s[1], s[2]);
                ctx.writeAndFlush("OK\n");
                break;
            case "GET":
                ctx.writeAndFlush(map.get(s[1]) + '\n');
                break;
            case "DELETE":
                map.remove(s[1]);
                ctx.writeAndFlush("OK\n");
                break;
            case "EXIT":
                ctx.channel().close();
                break;
        }
    }
}
