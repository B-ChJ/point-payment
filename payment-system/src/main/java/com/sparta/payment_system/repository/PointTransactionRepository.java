package com.sparta.payment_system.repository;

import com.sparta.payment_system.entity.PointTransaction;
import com.sparta.payment_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    List<PointTransaction> findAllByUser(User user);

    List<PointTransaction> findAllByUserOrderByCreatedAtDesc(User user);
}
