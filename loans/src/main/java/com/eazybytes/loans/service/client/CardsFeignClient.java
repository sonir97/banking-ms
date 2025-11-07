package com.eazybytes.loans.service.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("cards")
public interface CardsFeignClient {

}
