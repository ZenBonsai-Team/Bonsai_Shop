package com.example.bonsai_shop.seller.service;

import com.example.bonsai_shop.customer.repository.ViewingAppointmentRepository;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.seller.dto.SellerAppointmentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class SellerAppointmentService {
    private final ViewingAppointmentRepository viewingAppointmentRepository;
    private final ProductRepository productRepository;
    public List<SellerAppointmentDTO> findAllAppointmentsBySeller(User seller) {
        return viewingAppointmentRepository.findAllAppointmentsBySeller(seller) ;
    }
}
