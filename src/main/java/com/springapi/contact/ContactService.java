package com.springapi.contact;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {

    private final ContactMessageRepository contactMessageRepository;

    public ContactService(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    @Transactional
    public ContactResponse submit(ContactRequest request) {
        ContactMessage contactMessage = new ContactMessage();

        contactMessage.setName(request.name().trim());
        contactMessage.setEmail(request.email().trim());
        contactMessage.setMessage(request.message().trim());

        ContactMessage savedMessage =
                contactMessageRepository.save(contactMessage);

        return ContactResponse.from(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> getAllMessages() {
        return contactMessageRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ContactResponse::from)
                .toList();
    }

    @Transactional
    public ContactResponse updateStatus(Long id, String status) {
        ContactMessage contactMessage = contactMessageRepository
                .findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Contact message not found: " + id));

        contactMessage.setStatus(status);

        ContactMessage updatedMessage =
                contactMessageRepository.save(contactMessage);

        return ContactResponse.from(updatedMessage);
    }
}
