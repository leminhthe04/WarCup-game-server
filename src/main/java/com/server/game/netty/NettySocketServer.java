package com.server.game.netty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.server.game.util.Util;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NettySocketServer {

    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    // This class will be talked specifically in Server-Side-Only documentation
    @Autowired
    private SocketChannelInitializer socketChannelInitializer;


    public synchronized void start() throws Exception {
        
        if (serverChannel != null && serverChannel.isActive()) {
            log.info("Socket server is already running.");
            return;
        }

        int port = Util.getNettyServerPort(); // Get the port from .properties or environment variable



        // =====================
        Class<? extends ServerChannel> channelClass;
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup(); 
            channelClass = EpollServerSocketChannel.class;
            log.info("Đang khởi động Netty với Native Epoll (Tối ưu CPU cực tốt trên Linux)!");
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            channelClass = NioServerSocketChannel.class;
            log.info("Đang khởi động Netty với Nio mặc định (Không có Epoll)");
        }


        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                     .channel(channelClass) // Dùng class đã chọn ở trên
                     .childHandler(socketChannelInitializer)
                     
                     // 2. TỐI ƯU MẠNG GAME: Tắt thuật toán Nagle (Giảm độ trễ)
                     .childOption(ChannelOption.TCP_NODELAY, true) 
                     
                     // 3. TỐI ƯU RAM: Dùng Pool bộ nhớ thay vì tạo mới liên tục
                     .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                     
                     .option(ChannelOption.SO_BACKLOG, 1024) 
                     .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = bootstrap.bind(port).sync(); 

            this.serverChannel = f.channel();

            log.info("Socket server started at port=" + port);

            
        } catch (Exception e) {
            log.error("Failed to start Socket server: " + e.getMessage());
            // Dọn dẹp luồng nếu bind port thất bại
            if (bossGroup != null) bossGroup.shutdownGracefully();
            if (workerGroup != null) workerGroup.shutdownGracefully();
            throw e;
        }


        // =====================

        // bossGroup = new NioEventLoopGroup(1);
        // workerGroup = new NioEventLoopGroup();

        // try {
        //     ServerBootstrap bootstrap = new ServerBootstrap();
        //     bootstrap.group(bossGroup, workerGroup)
        //              .channel(NioServerSocketChannel.class)
        //              .childHandler(socketChannelInitializer)
        //              .option(ChannelOption.SO_BACKLOG, 128) // Set the backlog size
        //              .childOption(ChannelOption.SO_KEEPALIVE,true)
        //     ;

        //     ChannelFuture f = bootstrap.bind(port).sync(); 

        //     this.serverChannel = f.channel();

        //     log.info("Socket server started at port=" + port);


        //     f.channel().closeFuture().sync();
        // } catch (Exception e) {
        //     log.error("Failed to start Socket server: " + e.getMessage());
        //     throw e;
        // }
    }

    
    @PreDestroy   
    public void stop() {
        log.info("Shutting down Netty server...");
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (InterruptedException e) {
            log.error("Error closing Netty channel", e);
        } finally {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            log.info("Netty server shut down.");
        }
    }
}
