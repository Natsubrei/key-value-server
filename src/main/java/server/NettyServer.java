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

/**
 * 用 Netty 框架实现的 Key-Value存储服务器
 */
@Slf4j
public class NettyServer {
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup(12);
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
                            // 添加帧解码器（以换行符作为分隔符）
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(256, NEW_LINE));
                            // 添加字符串解码器，将接收到的 ByteBuf 转化为 String
                            ch.pipeline().addLast(STRING_DECODER);
                            // 添加字符串编码器，将 String 转化为 ByteBuf 发送
                            ch.pipeline().addLast(STRING_ENCODER);
                            // 添加请求处理器，用于解析指令
                            ch.pipeline().addLast(REQUEST_HANDLER);
                        }
                    })
                    .bind(8080)
                    .sync()
                    .channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Server error");
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
