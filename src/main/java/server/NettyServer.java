package server;

import handler.RequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyServer {
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        StringDecoder STRING_DECODER = new StringDecoder();
        StringEncoder STRING_ENCODER = new StringEncoder();
        RequestHandler REQUEST_HANDLER = new RequestHandler();
        ByteBuf[] NEW_LINE = Delimiters.lineDelimiter();
        try {
            Channel channel = new ServerBootstrap()
                    .channel(NioServerSocketChannel.class)
                    .group(boss, worker)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(256, NEW_LINE));
                            ch.pipeline().addLast(STRING_DECODER);
                            ch.pipeline().addLast(STRING_ENCODER);
                            ch.pipeline().addLast(REQUEST_HANDLER);
                        }
                    })
                    .bind(8080)
                    .sync()
                    .channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("server error");
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
