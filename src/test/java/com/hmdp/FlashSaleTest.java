package com.hmdp;

import cn.hutool.core.util.RandomUtil;
import com.hmdp.controller.VoucherController;
import com.hmdp.controller.VoucherOrderController;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author huangzihe
 * @date 2023/7/15 6:43 AM
 */
@SpringBootTest
@Slf4j
public class FlashSaleTest {
    @Resource
    VoucherController voucherController;

    @Resource
    VoucherOrderController voucherOrderController;

    @Test
    public void addFlashSaleVoucher() {
        addFlashSaleVoucherAndReturnId();
    }

    private Long addFlashSaleVoucherAndReturnId() {
        Voucher voucher = new Voucher();
        long voucherId = RandomUtil.randomLong(1, 1000);
        voucher.setId(voucherId);
        voucher.setBeginTime(LocalDateTime.of(2023, 7, 15, 7, 0, 0));
        voucher.setEndTime(LocalDateTime.of(2023, 7, 15, 8, 0, 0));
        voucher.setStock(1);
        voucher.setType(1);
        voucher.setTitle("test");
        voucher.setPayValue(5L);
        voucher.setActualValue(500L);
        voucherController.addFlashSaleVoucher(voucher);
        return voucherId;
    }

    @Test void mockFlashSale() {

        Long voucherId= addFlashSaleVoucherAndReturnId();

        ThreadFactory daemonThreadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
//                thread.setDaemon(true); // This is where we set the daemon flag
//                thread.setUncaughtExceptionHandler((t, e) -> {
//                    e.printStackTrace();
//                });
                return thread;
            }
        };

        ExecutorService es = Executors.newFixedThreadPool(36, daemonThreadFactory);
        for (int i = 0; i < 10; i++) {
            es.execute(() -> {
                for (int j = 0; j < 10; j++) {
                    Result result = voucherOrderController.getVoucher(voucherId);
                    if (result.getSuccess()) {
                        System.out.println("Got voucher!");
                    }
                }
            });
        }

        System.out.println("xxx");

        es.shutdown();
        try {
            if (!es.awaitTermination(60, TimeUnit.SECONDS)) {
                es.shutdownNow();
                if (!es.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("ExecutorService did not terminate");
            }
        } catch (InterruptedException ie) {
            es.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
