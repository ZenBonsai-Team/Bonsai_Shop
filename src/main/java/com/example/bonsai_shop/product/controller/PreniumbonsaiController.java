package com.example.bonsai_shop.product.controller;
import com.example.bonsai_shop.product.dto.ProductCardDTO;
import com.example.bonsai_shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
@RequiredArgsConstructor

public class PreniumbonsaiController {
   private final ProductService productService;

   @GetMapping("/bonsai-luxury")
    public String prenium(
            @RequestParam(defaultValue = "0") int page
           ,Model model) {
       Page<ProductCardDTO> product = productService.getPreniumProducts(PageRequest.of(page, 12));
       model.addAttribute("product", product);
       return "/product/bonsai-luxury";
   }

   @GetMapping("/bonsai_luxury_detail/{productId}")
   public String preniumDetail(
           @PathVariable Integer productId
           , Model model) {
      ProductCardDTO product =
              productService.getPreniumProductsById(productId);
   model.addAttribute("product", product);
   return "/product/bonsai_luxury_detail";
   }
}
