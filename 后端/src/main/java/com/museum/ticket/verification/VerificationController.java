package com.museum.ticket.verification;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {
    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/verify")
    public VerificationResponse verify(
            @RequestHeader(value = "X-Verification-Key", required = false) String verificationKey,
            @Valid @RequestBody VerificationRequest request) {
        return verificationService.verify(verificationKey, request);
    }
}
