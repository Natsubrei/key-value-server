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

@Slf4j
public class NioServer {
    public static void main(String[] args) {
        Thread.currentThread().setName("boss");
        try (ServerSocketChannel ssc = ServerSocketChannel.open();
             Selector bossSelector = Selector.open()) {
            ssc.bind(new InetSocketAddress(8080));
            ssc.configureBlocking(false);
            ssc.register(bossSelector, SelectionKey.OP_ACCEPT);

            Worker[] workers = new Worker[12];
            for (int i = 0; i < workers.length; i++) {
                workers[i] = new Worker("worker-" + i);
                new Thread(workers[i]).start();
            }

            AtomicInteger atomicInteger = new AtomicInteger();
            while (true) {
                bossSelector.select();
                Iterator<SelectionKey> iterator = bossSelector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        SocketChannel sc = ssc.accept();
                        sc.configureBlocking(false);
                        Worker worker = workers[atomicInteger.getAndIncrement() % workers.length];
                        worker.register(sc);
                    }
                }
            }
        } catch (IOException e) {
            log.error("server error");
        }
    }

    static class Worker implements Runnable {
        private Selector selector;
        private String name;
        private ConcurrentLinkedQueue<SocketChannel> registerQueue = new ConcurrentLinkedQueue<>();

        public Worker(String name) throws IOException {
            this.name = name;
            this.selector = Selector.open();
        }

        public void register(SocketChannel sc) {
            registerQueue.offer(sc);
            // 唤醒阻塞在select方法的selector
            selector.wakeup();
        }

        @Override
        public void run() {
            Charset charset = Charset.defaultCharset();
            while (true) {
                try {
                    // 先处理注册队列
                    SocketChannel sc;
                    while ((sc = registerQueue.poll()) != null) {
                        sc.register(selector, SelectionKey.OP_READ, ByteBuffer.allocateDirect(256));
                    }
                    selector.select();
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isReadable()) {
                            SocketChannel channel = (SocketChannel) key.channel();
                            ByteBuffer buffer = (ByteBuffer) key.attachment();
                            int readBytes = channel.read(buffer);
                            // 客户端断开连接
                            if (readBytes == -1) {
                                key.cancel();
                                channel.close();
                                continue;
                            }
                            buffer.flip();
                            // 命令以换行符分隔
                            while (buffer.hasRemaining()) {
                                int pos = buffer.position();
                                int limit = buffer.limit();
                                boolean foundLine = false;
                                for (int i = pos; i < limit; i++) {
                                    if (buffer.get(i) == '\n') {
                                        int length = i - pos + 1;
                                        byte[] bytes = new byte[length];
                                        buffer.get(bytes);
                                        String command = new String(bytes, charset).trim();
                                        processCommand(channel, command, charset);
                                        foundLine = true;
                                        break;
                                    }
                                }
                                if (!foundLine) {
                                    // 没有完整命令则退出循环，等待下次读入
                                    break;
                                }
                            }
                            buffer.compact();
                        }
                    }
                } catch (IOException e) {
                    log.error("{} encountered an error: {}", name, e.getMessage());
                }
            }
        }

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
