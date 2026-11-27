package cl.duoc.ep3.controller;

import cl.duoc.ep3.dto.PurchaseRequest;
import cl.duoc.ep3.dto.PurchaseResponse;
import cl.duoc.ep3.dto.RedemptionRequest;
import cl.duoc.ep3.dto.RedemptionResponse;
import cl.duoc.ep3.model.Account;
import cl.duoc.ep3.service.RewardsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class RewardsController {

    private final RewardsService rewardsService;

    public RewardsController(RewardsService rewardsService) {
        this.rewardsService = rewardsService;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse purchase(
            @AuthenticationPrincipal Account account,
            @Valid @RequestBody PurchaseRequest request
    ) {
        return rewardsService.purchase(account, request);
    }

    @PostMapping("/rewards/redemptions")
    @ResponseStatus(HttpStatus.CREATED)
    public RedemptionResponse redeem(
            @AuthenticationPrincipal Account account,
            @Valid @RequestBody RedemptionRequest request
    ) {
        return rewardsService.redeem(account, request);
    }
}
