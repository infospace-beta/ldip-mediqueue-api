package com.springapi.author;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.springapi.author.dto.AuthorRequest;
import com.springapi.author.dto.AuthorResponse;
import com.springapi.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/authors")
@Tag(name = "Authors")
public class AuthorController {

	private final AuthorService authorService;

	public AuthorController(AuthorService authorService) {
		this.authorService = authorService;
	}

	@GetMapping
	public PageResponse<AuthorResponse> findAll(@ParameterObject @SortDefault("id") Pageable pageable) {
		return authorService.findAll(pageable);
	}

	@GetMapping("/{id}")
	public AuthorResponse findById(@PathVariable Long id) {
		return authorService.findById(id);
	}

	@PostMapping
	public ResponseEntity<AuthorResponse> create(@Valid @RequestBody AuthorRequest request) {
		AuthorResponse created = authorService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}").buildAndExpand(created.id()).toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	public AuthorResponse update(@PathVariable Long id, @Valid @RequestBody AuthorRequest request) {
		return authorService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		authorService.delete(id);
	}

}
