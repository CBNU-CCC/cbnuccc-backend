package com.cbnuccc.cbnuccc.Util;

import java.util.Map;

import org.springframework.data.domain.Page;

public class PaginationUtil {
    public static <T> Object makePaginationMap(Page<T> input) {
        return Map.of(
                "data", input.getContent(), // data
                "length", input.getNumberOfElements(), // size of current slice's elements
                "pageAt", input.getNumber(), // current page's number of all pages
                "totalPage", input.getTotalPages(), // total number of all pages
                "totalElement", input.getTotalElements()); // total number of all elements
    }
}
