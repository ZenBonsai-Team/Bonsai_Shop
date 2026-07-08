package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.BonsaiOrder;
import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Modifying;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
// import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OrderRepository extends JpaRepository<BonsaiOrder, Integer> {

}
