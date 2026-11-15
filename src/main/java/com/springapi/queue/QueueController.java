package com.springapi.queue;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.springapi.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Queue tickets")
@SecurityRequirement(name = "bearerAuth")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/departments/{departmentId}/queue-tickets")
    @PreAuthorize("@queueAuthorization.canManageDepartment(authentication, #p0)")
    public PageResponse<QueueTicketResponse> getDepartmentQueue(
            @PathVariable Long departmentId,
            @RequestParam(required = false) QueueStatus status,
            @ParameterObject Pageable pageable) {
        return queueService.getDepartmentQueue(
                departmentId, status, pageable);
    }

    @PostMapping("/departments/{departmentId}/queue-tickets")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<QueueTicketResponse> joinQueue(
            @PathVariable Long departmentId,
            @AuthenticationPrincipal UserDetails currentUser) {
        QueueTicketResponse created = queueService.joinQueue(
                departmentId, currentUser.getUsername());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/queue-tickets/{ticketId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/queue-tickets/{ticketId}")
    @PreAuthorize("@queueAuthorization.canAccessTicket(authentication, #p0)")
    public QueueTicketResponse getTicket(@PathVariable Long ticketId) {
        return queueService.getTicket(ticketId);
    }

    @DeleteMapping("/queue-tickets/{ticketId}")
    @PreAuthorize("@queueAuthorization.canAccessTicket(authentication, #p0)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelTicket(@PathVariable Long ticketId) {
        queueService.cancelTicket(ticketId);
    }

    @PostMapping("/departments/{departmentId}/queue/call-next")
    @PreAuthorize("@queueAuthorization.canManageDepartment(authentication, #p0)")
    public QueueTicketResponse callNext(@PathVariable Long departmentId) {
        return queueService.callNext(departmentId);
    }

    @PostMapping(
            "/departments/{departmentId}/queue-tickets/{ticketId}/skip")
    @PreAuthorize("@queueAuthorization.canManageDepartment(authentication, #p0)")
    public QueueTicketResponse skipTicket(
            @PathVariable Long departmentId,
            @PathVariable Long ticketId) {
        return queueService.skipTicket(departmentId, ticketId);
    }

    @PostMapping(
            "/departments/{departmentId}/queue-tickets/{ticketId}/complete")
    @PreAuthorize("@queueAuthorization.canManageDepartment(authentication, #p0)")
    public QueueTicketResponse completeTicket(
            @PathVariable Long departmentId,
            @PathVariable Long ticketId) {
        return queueService.completeTicket(departmentId, ticketId);
    }
}
