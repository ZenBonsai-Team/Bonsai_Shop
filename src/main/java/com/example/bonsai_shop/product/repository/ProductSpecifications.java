package com.example.bonsai_shop.product.repository;

import com.example.bonsai_shop.entity.Product;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecifications {
    public static Specification<Product> filterProducts(
            String keyword,
            String status,
            Boolean availableOnly,
            String segment,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<String> ages,
            List<String> species,
            List<String> styles,
            List<Integer> tagIds,
            List<String> priceRanges) {
        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            // Avoid fetch joins for count queries (used by Spring Data JPA pagination count
            // query)
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("variety", JoinType.INNER);
                root.fetch("createdBy", JoinType.LEFT);
            }

            // Customer marketplace must not expose artisan-only product states.
            predicates.add(cb.notEqual(root.get("productStatus"), "DRAFT"));
            predicates.add(cb.isTrue(cb.coalesce(root.get("isVisible"), true)));

            // Exclude Elite segment products (segmentId = 3) from the marketplace
            predicates.add(cb.notEqual(root.get("segment").get("segmentId"), 3));

            // keyword (matches product name or code)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("productName")), pattern),
                        cb.like(cb.lower(root.get("productCode")), pattern)));
            }

            if (Boolean.TRUE.equals(availableOnly)) {
                predicates.add(cb.equal(root.get("productStatus"), "AVAILABLE"));
            } else if (status != null && !status.trim().isEmpty()) {
                String normalizedStatus = status.trim().toUpperCase();
                if (!"DRAFT".equals(normalizedStatus)) {
                    predicates.add(cb.equal(root.get("productStatus"), normalizedStatus));
                }
            }

            // segment (checks segmentId or segmentName)
            if (segment != null && !segment.trim().isEmpty()) {
                try {
                    Integer segmentId = Integer.parseInt(segment);
                    predicates.add(cb.equal(root.get("segment").get("segmentId"), segmentId));
                } catch (NumberFormatException e) {
                    predicates.add(cb.equal(
                            cb.lower(root.get("segment").get("segmentName")),
                            segment.trim().toLowerCase()));
                }
            }

            // category (checks categoryId or categoryName via variety)
            if (category != null && !category.trim().isEmpty()) {
                try {
                    Integer categoryId = Integer.parseInt(category);
                    predicates.add(cb.equal(root.get("variety").get("category").get("categoryId"), categoryId));
                } catch (NumberFormatException e) {
                    predicates.add(cb.equal(root.get("variety").get("category").get("categoryName"), category));
                }
            }

            // minPrice
            if (minPrice != null) {
                predicates.add(cb.ge(root.get("price"), minPrice));
            }

            // maxPrice
            if (maxPrice != null) {
                predicates.add(cb.le(root.get("price"), maxPrice));
            }

            if (priceRanges != null && !priceRanges.isEmpty()) {
                List<Predicate> pricePredicates = new ArrayList<>();
                for (String range : priceRanges) {
                    if (range != null) {
                        switch (range) {
                            case "under1M":
                                pricePredicates.add(cb.lessThan(root.get("price"), new BigDecimal(1000000)));
                                break;
                            case "1Mto5M":
                                pricePredicates.add(cb.between(root.get("price"), new BigDecimal(1000000),
                                        new BigDecimal(5000000)));
                                break;
                            case "5Mto10M":
                                pricePredicates.add(cb.between(root.get("price"), new BigDecimal(5000000),
                                        new BigDecimal(10000000)));
                                break;
                            case "10Mto30M":
                                pricePredicates.add(cb.between(root.get("price"), new BigDecimal(10000000),
                                        new BigDecimal(30000000)));
                                break;
                            case "30Mto100M":
                                pricePredicates.add(cb.between(root.get("price"), new BigDecimal(30000000),
                                        new BigDecimal(100000000)));
                                break;
                            case "over100M":
                                pricePredicates.add(cb.greaterThan(root.get("price"), new BigDecimal(100000000)));
                                break;
                        }
                    }
                }
                if (!pricePredicates.isEmpty()) {
                    predicates.add(cb.or(pricePredicates.toArray(new Predicate[0])));
                }
            }

            // ages (multiple checkboxes: OR logic between selected ranges)
            if (ages != null && !ages.isEmpty()) {
                List<Predicate> agePredicates = new ArrayList<>();
                for (String ageRange : ages) {
                    if (ageRange != null) {
                        switch (ageRange) {
                            case "under5":
                                agePredicates.add(cb.lessThan(root.get("age"), 5));
                                break;
                            case "5to10":
                                agePredicates.add(cb.between(root.get("age"), 5, 10));
                                break;
                            case "11to20":
                                agePredicates.add(cb.between(root.get("age"), 11, 20));
                                break;
                            case "over20":
                                agePredicates.add(cb.greaterThan(root.get("age"), 20));
                                break;
                        }
                    }
                }
                if (!agePredicates.isEmpty()) {
                    predicates.add(cb.or(agePredicates.toArray(new Predicate[0])));
                }
            }

            if (species != null && !species.isEmpty()) {
                predicates.add(root.get("variety").get("varietyName").in(species));
            }

            if (tagIds != null && !tagIds.isEmpty()) {
                Join<Object, Object> productTagJoin = root.join("productTags", JoinType.INNER);
                predicates.add(productTagJoin.get("tag").get("tagId").in(tagIds));
            }

            // if (styles != null && !styles.isEmpty()) {
            // predicates.add(root.get("style").in(styles));
            // }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
