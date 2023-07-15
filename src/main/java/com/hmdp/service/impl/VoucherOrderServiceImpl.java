package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.FlashSaleVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    IVoucherService voucherService;

    @Resource
    FlashSaleVoucherServiceImpl flashSaleVoucherService;

    @Resource
    RedisIdWorker redisIdWorker;

    @Override
    public Result flashVoucher(Long voucherId) {
        // 1. Query Voucher
        FlashSaleVoucher voucher = flashSaleVoucherService.getById(voucherId);
        // 2.1 check if event has started
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("The event has not started!");
        }
        // 2.2 check if event has ended
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("The event has ended");
        }
        if (voucher.getStock() < 1) {
            return Result.fail("Out of stock");
        }


        // 3. check if the user has got voucher
        Long userId = 1L;    // UserHolder.getUser().getId();
        Long orderId = null;
        synchronized (userId.toString().intern()) {
            Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count > 0) {
                return Result.fail("You have got one");
            }

            // 4. Deduct
            boolean flag = flashSaleVoucherService.update().setSql("stock=stock - 1")
                    .eq("voucher_id", voucherId).gt("stock", 0).update();
            if (!flag) {
                return Result.fail("out of stock");
            }
            // 5. create order
            VoucherOrder voucherOrder = new VoucherOrder();
            // Order id
            orderId = redisIdWorker.nextId("order");
            voucherOrder.setId(orderId);
            // User Id
            voucherOrder.setUserId(userId);
            // Voucher ID
            voucherOrder.setVoucherId(voucherId);
            save(voucherOrder);
        }

        return Result.ok(orderId);
    }
}
