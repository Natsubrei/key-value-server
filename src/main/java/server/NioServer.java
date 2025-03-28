package server;

import lombok.extern.slf4j.Slf4j;
import store.StoreFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用 Java NIO 实现的键值服务器
 */
@Slf4j
public class NioServer {
    public static void main(String[] args) {
        // 将当前线程命名为 boss，负责接收客户端的连接
        Thread.currentThread().setName("boss");
        try (ServerSocketChannel ssc = ServerSocketChannel.open();
             Selector bossSelector = Selector.open()) {
            // 绑定端口8080
            ssc.bind(new InetSocketAddress(8080));
            // 设置为非阻塞模式
            ssc.configureBlocking(false);
            // 将 ServerSocketChannel 注册到 bossSelector 上，监听accept事件
            ssc.register(bossSelector, SelectionKey.OP_ACCEPT);

            // 创建一组工作线程来处理客户端请求
            Worker[] workers = new Worker[12];
            for (int i = 0; i < workers.length; i++) {
                workers[i] = new Worker("worker-" + i);
                new Thread(workers[i]).start();
            }

            AtomicInteger atomicInteger = new AtomicInteger();
            while (true) {
                // 监听accept事件
                bossSelector.select();
                Iterator<SelectionKey> iterator = bossSelector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        // 建立连接
                        SocketChannel sc = ssc.accept();
                        // 设置为非阻塞模式
                        sc.configureBlocking(false);
                        // 轮询选择工作线程来处理该连接
                        Worker worker = workers[atomicInteger.getAndIncrement() % workers.length];
                        // 将客户端的 SocketChannel 注册到工作线程的 Selector 上
                        worker.register(sc);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Server error");
        }
    }

    /**
     * 工作线程类，用于处理客户端的 I/O 操作
     */
    static class Worker implements Runnable {
        // 每个工作线程对应一个Selector
        private Selector selector;
        // 线程名称
        private String name;
        // 线程安全的队列，用于存放待注册的SocketChannel
        private ConcurrentLinkedQueue<SocketChannel> registerQueue = new ConcurrentLinkedQueue<>();

        public Worker(String name) throws IOException {
            this.name = name;
            this.selector = Selector.open();
        }

        /**
         * 将新的客户端连接放入注册队列，并唤醒Selector
         */
        public void register(SocketChannel sc) {
            registerQueue.offer(sc);
            // 唤醒阻塞在 select() 方法的selector
            selector.wakeup();
        }

        @Override
        public void run() {
            // 使用默认字符集
            Charset charset = Charset.defaultCharset();
            while (true) {
                try {
                    // 处理注册队列中的新连接
                    SocketChannel sc;
                    while ((sc = registerQueue.poll()) != null) {
                        // 为每一个新连接分配一个Attachment
                        sc.register(selector, SelectionKey.OP_READ, ByteBuffer.allocateDirect(256));
                    }
                    // 阻塞，并监听事件
                    selector.select();
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        // 如果有数据可读
                        if (key.isReadable()) {
                            SocketChannel channel = (SocketChannel) key.channel();
                            // 获取与Channel绑定的Attachment
                            ByteBuffer buffer = (ByteBuffer) key.attachment();
                            // 读取数据到ByteBuffer
                            int readBytes = channel.read(buffer);
                            // 返回 -1 则说明客户端断开连接
                            if (readBytes == -1) {
                                key.cancel();
                                channel.close();
                                continue;
                            }
                            // 切换 ByteBuffer 模式
                            buffer.flip();
                            // 按行（以换行符分隔）处理命令
                            while (buffer.hasRemaining()) {
                                int pos = buffer.position();
                                int limit = buffer.limit();
                                boolean foundLine = false;
                                for (int i = pos; i < limit; i++) {
                                    if (buffer.get(i) == '\n') {
                                        int length = i - pos + 1;
                                        byte[] bytes = new byte[length];
                                        buffer.get(bytes);
                                        // 将字节数组转化为字符串并去除首尾空白符
                                        String command = new String(bytes, charset).trim();
                                        // 处理命令
                                        processCommand(channel, command, charset);
                                        foundLine = true;
                                        break;
                                    }
                                }
                                // 没有完整命令则退出循环，等待下次读入
                                if (!foundLine) {
                                    break;
                                }
                            }
                            // 清除 buffer 中已经处理过的数据
                            buffer.compact();
                        }
                    }
                } catch (IOException e) {
                    log.error("{} encountered an error: {}", name, e.getMessage());
                }
            }
        }

        /**
         * 根据客户端发送的命令执行相应操作
         * 支持的命令：
         *  PUT key value  —— 存储键值对
         *  GET key        —— 根据 key 获取对应的 value
         *  DELETE key     —— 删除指定 key
         *  EXIT           —— 关闭连接
         */
        private void processCommand(SocketChannel channel, String command, Charset charset) throws IOException {
            String[] s = command.split(" ");
            // log.debug("{} received command: {}", name, command);
            switch (s[0]) {
                case "PUT":
                    StoreFactory.getMap().put(s[1], s[2]);
                    channel.write(charset.encode("OK\n"));
                    break;
                case "GET":
                    channel.write(charset.encode(StoreFactory.getMap().get(s[1]) + "\n"));
                    break;
                case "DELETE":
                    StoreFactory.getMap().remove(s[1]);
                    channel.write(charset.encode("OK\n"));
                    break;
                case "EXIT":
                    channel.close();
                    break;
                default:
                    channel.write(charset.encode("ERROR: Unknown command\n"));
            }
        }
    }
}
