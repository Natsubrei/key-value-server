package handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import store.StoreFactory;

/**
 * 请求处理器，用于处理 NettyServer 中接收到的指令
 */
@Slf4j
@ChannelHandler.Sharable
public class RequestHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String command = (String) msg;
        // log.debug("command: {}", command);
        String[] s = command.split(" ");

        switch (s[0]) {
            case "PUT":
                StoreFactory.getMap().put(s[1], s[2]);
                ctx.writeAndFlush("OK\n");
                break;
            case "GET":
                ctx.writeAndFlush(StoreFactory.getMap().get(s[1]) + "\n");
                break;
            case "DELETE":
                StoreFactory.getMap().remove(s[1]);
                ctx.writeAndFlush("OK\n");
                break;
            case "EXIT":
                ctx.channel().close();
                break;
            default:
                ctx.writeAndFlush("ERROR: Unknown command\n");
        }
    }
}
