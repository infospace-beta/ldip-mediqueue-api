package com.springapi.contact;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse submitContactMessage(
            @Valid @RequestBody ContactRequest request) {
        return contactService.submit(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public List<ContactResponse> getContactMessages() {
        return contactService.getAllMessages();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ContactResponse updateContactMessageStatus(
            @PathVariable Long id,
            @Valid @RequestBody ContactStatusRequest request) {
        return contactService.updateStatus(id, request.status());
    }
}
